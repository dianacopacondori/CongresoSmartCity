package com.congreso.seguridad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registro de auditoría de acciones administrativas sensibles.
 * Se usa un logger dedicado para poder separar estas líneas del log de aplicación.
 */
public final class RegistroAuditoria {

    private static final Logger AUDITORIA = LoggerFactory.getLogger("AUDITORIA");

    private RegistroAuditoria() {
    }

    /**
     * @param accion   acción realizada (SUSPENDER, ELIMINAR, CANCELAR, ...)
     * @param entidad tipo de entidad afectada
     * @param id       identificador de la entidad
     * @param actor    correo del administrador autenticado
     * @param detalle  información adicional sin datos personales
     */
    public static void registrar(String accion, String entidad, Object id, String actor, String detalle) {
        AUDITORIA.info("accion={} entidad={} id={} actor={} detalle={}",
                accion, entidad, id, actor == null ? "desconocido" : actor, detalle);
    }
}
