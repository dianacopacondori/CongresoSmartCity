# Auditoría de seguridad — Gestor Congreso Smart City

Auditoría básica de la aplicación sobre el entorno local, usando **OWASP ZAP** y **Nmap**
(herramientas autorizadas por el requisito del proyecto). Las herramientas se ejecutaron en
versiones portables, sin permisos de administrador, desde `%TEMP%\opencode\audit`.

## Alcance

- Objetivo: `http://localhost:8080` (Spring Boot + Tomcat + PostgreSQL en `localhost:5433`).
- Tipo de pruebas: reconocimiento de red (Nmap) y DAST automatizado (ZAP quick scan).
- Autorización: código y entorno propiedad del proyecto, ejecutados en local.
- Fecha: 17/11/2026.

## Metodología y comandos

### 1. Nmap — reconocimiento de superficie de ataque

```powershell
nmap.exe -sV -sT -Pn --version-light -p 80,443,5432,5433,8080 127.0.0.1
```

Evidencia: `security/nmap-report.txt`.

### 2. OWASP ZAP 2.17.0 — escaneo DAST

```powershell
zap.bat -cmd -silent -notel -port 8090 `
  -quickurl http://localhost:8080 `
  -quickout C:\Diana\GestorCongresoSmartCity\security\zap-report.html `
  -quickprogress
```

> `-port 8090` es necesario porque el proxy interno de ZAP usa 8080 por defecto y
> ese puerto ya está ocupado por la aplicación.

Evidencia: `security/zap-report.html` (escaneo inicial), `security/zap-report-after.html`
(primera iteración) y `security/zap-report-final.html` (**reporte final**).

## Resultados del escaneo inicial (antes de las correcciones)

| Risk level | Alerts |
|---|---:|
| High | 0 |
| Medium | 2 |
| Low | 1 |
| Informational | 3 |

| ID ZAP | Hallazgo | Riesgo | Estado |
|---|---|---|---|
| 10038 | Content Security Policy (CSP) Header Not Set | Medio | **Corregido y verificado**: CSP estricta añadida en `ConfiguracionSeguridad`. |
| 90003 | Sub Resource Integrity Attribute Missing | Medio | **Corregido y verificado**: se eliminaron scripts CDN y enlaces a Google Fonts; solo recursos locales. |
| 10054 | Cookie without SameSite Attribute | Bajo | **Corregido y verificado**: `server.servlet.session.cookie.same-site=lax`. |
| 10027 | Information Disclosure - Suspicious Comments | Informativo | Revisado: comentarios de código, sin datos sensibles. |
| 10112 | Session Management Response Identified | Informativo | No vulnerable: cookie `HttpOnly`, `SameSite=Lax`. |
| 10104 | User Agent Fuzzer | Informativo | No aplica. |

## Hallazgos adicionales (revisión de código, no detectables por DAST)

| Hallazgo | Riesgo | Acción tomada |
|---|---|---|
| Contraseña de BD y credenciales SMTP escritas en `application.properties` y presentes en el historial de Git | Alto | Credenciales externalizadas a variables de entorno y a `application-local.properties` (ignorado por Git; usar `application-local.properties.example` como plantilla). **Pendiente: rotar la contraseña filtrada en el historial.** |
| `spring.jpa.show-sql=true` podía volcar datos personales en logs | Medio | Desactivado. |
| `ReporteServlet` devolvía `e.getMessage()` al usuario | Medio | Mensaje genérico y detalle enviado solo al log. |
| Instancia PostgreSQL 9.4.1 expuesta en el puerto 5432 | Medio | Recomendación operativa: deshabilitarla o limitar su acceso; la app usa 5433. |
| Scripts inline y atributos `onclick`/`onchange` | Medio | Movidos a `static/js/*.js` para permitir CSP sin `unsafe-inline`. |

## Controles que ya estaban correctos

- Contraseñas almacenadas con BCrypt (`EncriptadorPassword`, `PasswordEncoder`).
- Consultas JPA parametrizadas (`@Query` con `:parametro`), sin concatenación SQL.
- CSRF de Spring Security habilitado por defecto; los formularios usan `th:action`, por lo que
  Thymeleaf inyecta el token automáticamente.
- Autorización por roles en `ConfiguracionSeguridad`: `/admin/**` requiere `ADMINISTRADOR`
  (verificado: `GET /admin/dashboard` → 302 a `/usuarios/login`).
- Thymeleaf escapa la salida por defecto (`th:text`), mitigando XSS reflejado.

## Resultado final (después de las correcciones)

Escaneo repetido sobre la instancia corregida (`http://localhost:8081`):

| Risk level | Alerts |
|---|---:|
| High | 0 |
| Medium | 0 |
| Low | 0 |
| Informational | 3 |

Los tres avisos informativos restantes son esperados: comentarios sospechosos (comentarios
de código), gestión de sesión (cookie `HttpOnly` + `SameSite`) y User Agent Fuzzer (no aplica).
Reporte definitivo: `security/zap-report-final.html`.

## Verificación

- `mvn -DskipTests compile`: correcto con JDK 24.
- `mvn test`: **56 pruebas, 0 fallos con JDK 21**. Con JDK 24 fallan pruebas por
  incompatibilidad entre Byte Buddy 1.14.10 (Mockito) y Java 24; el proyecto declara Java 17
  y debe ejecutarse con JDK 17/21, no 24.
- `curl -I http://localhost:8081/`: confirma `Content-Security-Policy`, `Referrer-Policy` y
  `Permissions-Policy` después de las correcciones.
- La instancia definitiva se reinició en `http://localhost:8080` y responde HTTP 200 con todas
  las cabeceras anteriores (verificado el 17/11/2026).

## Pruebas de seguridad añadidas

| Clase de prueba | Controles verificados |
|---|---|
| `seguridad/CabecerasSeguridadTest` | CSP estricta (sin `unsafe-inline` ni orígenes externos), `X-Content-Type-Options`, `X-Frame-Options`, `Cache-Control`, `Referrer-Policy`, `Permissions-Policy`, HSTS sobre HTTPS, redirección al login, prohibición de acceso por rol, CSRF en logout y login válido/inválido. |
| `seguridad/EncriptadorPasswordTest` | BCrypt: hash sin texto plano, prefijo `$2`, verificación correcta/incorrecta y salt aleatorio. |
| `seguridad/ConfiguracionSecretoTest` | Credenciales externalizadas (DB y SMTP), `show-sql=false`, cookie `HttpOnly` + `SameSite=Lax`. |
| `seguridad/PlantillasSegurasTest` | Ninguna plantilla con JS inline, manejadores `on*` ni CDNs/fuentes externas; scripts locales presentes. |
| `seguridad/ConfiguracionSeguridadTest` | Rechazo de roles incoherentes, autoridad `ROLE_ADMINISTRADOR`, usuarios suspendidos deshabilitados y codificador BCrypt. |
| `controlador/ReporteServletSeguridadTest` | Los errores no filtran JDBC/credenciales y el acceso sin rol administrador se redirige. |

Ejecutar con el JDK 21 incluido en IntelliJ:

```powershell
$env:JAVA_HOME='C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2024.3.5\jbr'
& 'C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2024.3.5\plugins\maven\lib\maven3\bin\mvn.cmd' test
```

## Pendiente / recomendaciones finales

1. **Rotar** la contraseña de PostgreSQL que quedó en el historial de Git (`6006`) y el
   secreto SMTP placeholder. Limpiar el historial con `git filter-repo` si el repo se comparte.
2. Ejecutar un escaneo autenticado (ZAP con contexto y usuario `ADMINISTRADOR`) para cubrir
   `/admin/**`, `/reportes/**` y exportación de archivos.
3. En producción: HTTPS obligatorio para que `Secure` y HSTS apliquen; el escaneo actual corre
   por HTTP y por eso ZAP no reporta el flag `Secure` de la cookie.
4. Repetir el escaneo después de cada release y archivar el reporte en `security/`.
