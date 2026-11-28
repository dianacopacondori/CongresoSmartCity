# Congreso Internacional Smart City

Portal web académico para la inscripción, el pago **simulado**, la agenda de actividades,
los fotochecks y el control de asistencias. Implementado con Java 17, Spring Boot
3.2.0, Spring Security, Thymeleaf, Maven y PostgreSQL.

> **Aviso:** ninguna operación del módulo de pagos realiza cobros reales.

## Requisitos

- JDK 17 (también se ha utilizado JDK 21); evitar JDK 24 con la versión de Mockito incluida.
- Apache Maven, Git y PostgreSQL.
- Navegador web moderno. VS Code o IntelliJ IDEA son opcionales.
- Base de datos local llamada `congreso_smart_city` (crear desde pgAdmin).

## Instalación en Windows (PowerShell)

```powershell
git clone https://github.com/dianacopacondori/CongresoSmartCity.git
cd CongresoSmartCity
java -version
mvn -version
```

Copiar la plantilla local de configuración:

```powershell
Copy-Item .\src\main\resources\application-local.properties.example .\src\main\resources\application-local.properties
```

Editar **solo** `src/main/resources/application-local.properties`, que está excluido de Git:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/congreso_smart_city
spring.datasource.username=postgres
spring.datasource.password=CAMBIAR_POR_PASSWORD_LOCAL
```

El puerto habitual es `5432`. El proyecto usa `5433` como valor predeterminado,
por lo que debes configurar el puerto de tu servidor. Alternativamente puedes
utilizar las variables de entorno `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`.

### Primera ejecución con una base vacía

El perfil `dev` permite a Hibernate crear o actualizar las tablas de desarrollo.
`schema.sql` amplía tablas existentes; por eso se retrasa su ejecución hasta que
Hibernate haya inicializado el esquema:

```powershell
mvn clean compile
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=dev --spring.jpa.defer-datasource-initialization=true"
```

**No utilizar `ddl-auto=update` en entornos compartidos o de producción.**
Una vez inicializada la base, detener con `Ctrl+C` y usar:

```powershell
mvn spring-boot:run
```

Abrir <http://localhost:8080>. El registro público crea cuentas de **asistente**.
Las cuentas administrativas se deben provisionar por un procedimiento autorizado;
no se incluyen contraseñas ni administradores predeterminados en este repositorio.

## Funcionalidades principales

1. Registro e inicio de sesión de asistentes.
2. Inscripción al congreso con tipos Estudiante (S/ 50) o Externo (S/ 100).
3. Pago académico simulado: completo o dos cuotas estudiantiles de S/ 25.
4. Selección de ponencias/talleres después de confirmar el pago; control de cupos y cruces.
5. Agenda, fotocheck digital, validación y registro de asistencias.
6. Panel administrativo, filtros de pagos y reportes Excel/PDF.

El proyecto define roles `COLABORADOR`, `PONENTE` e `INSTRUCTOR`, pero sus recorridos
privados aún están pendientes. No deben tratarse como funcionalidades terminadas.

## Ejecutar pruebas

```powershell
mvn test
```

Pruebas seleccionadas de filtros, inscripción y registro:

```powershell
mvn "-Dtest=PagoFiltrosServicioTest,PagoFiltrosControllerTest,InscripcionServicioTest,UsuarioServletTest" test
```

Las clases `*PostgresIT` son pruebas de integración que requieren PostgreSQL local
configurado. Para ejecutar una de ellas explícitamente:

```powershell
mvn "-Dtest=PagoSimuladoPostgresIT" test
```

Utilizar siempre datos ficticios en pruebas y no editar ni eliminar registros reales.

## Esquema y migraciones

- Las entidades JPA están en `src/main/java/com/congreso/modelo`.
- `src/main/resources/schema.sql` contiene ampliaciones idempotentes necesarias al arrancar.
- `src/main/resources/db/migracion/` conserva los scripts SQL versionados de
  unicidad, historial de cuotas y auditoría de asistencias.
- Las migraciones de unicidad incluyen comprobaciones de duplicados y no borran datos.

No ejecutar migraciones manualmente sobre bases compartidas sin revisar primero
el estado real del esquema, los datos y una copia de seguridad.

## Demostración funcional recomendada

1. Registrar un asistente ficticio e iniciar sesión.
2. Crear su inscripción como Estudiante o Externo.
3. Simular un pago completo o parcial y comprobar el saldo.
4. Terminar las cuotas, seleccionar una actividad y consultar la agenda.
5. Emitir/verificar el fotocheck y registrar asistencia desde el acceso administrativo.
6. Filtrar pagos por nombre y estado, y exportar un reporte.

El archivo `AUDITORIA_PENDIENTES_CONGRESO_SMART_CITY.txt` recoge hallazgos cerrados,
pendientes y decisiones de negocio aún por aprobar.

## Seguridad y colaboración

- No subir `application-local.properties`, contraseñas ni datos de usuarios reales.
- Hacer cambios en ramas individuales; abrir Pull Requests hacia `main`.
- Ejecutar la compilación y las pruebas antes de pedir el merge.
- Los informes de OWASP ZAP y Nmap están en `security/`; un escaneo local sin alertas
  no garantiza seguridad completa ni sustituye pruebas autenticadas.
