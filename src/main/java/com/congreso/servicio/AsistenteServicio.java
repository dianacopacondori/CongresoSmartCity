package com.congreso.servicio;

import com.congreso.dao.AsistenteDAO;
import com.congreso.dto.PerfilAsistenteFormulario;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.enums.EstadoUsuario;
import com.congreso.modelo.enums.ModalidadPago;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.seguridad.RegistroAuditoria;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AsistenteServicio {

    private final AsistenteDAO asistenteDAO;

    @Autowired
    public AsistenteServicio(AsistenteDAO asistenteDAO) {
        this.asistenteDAO = asistenteDAO;
    }

    public Asistente registrarAsistente(Asistente asistente) {
        return asistenteDAO.save(asistente);
    }

    public Asistente buscarPorCodigo(Long codigo) {
        return asistenteDAO.findById(codigo).orElse(null);
    }

    public Asistente buscarPerfilPropio(String correoAutenticado) {
        Asistente asistente = asistenteDAO.findByCorreo(correoAutenticado)
                .orElseThrow(() -> new AccessDeniedException("Perfil de asistente no disponible"));
        if (!asistente.estaActivo() || asistente.getRol() != RolUsuario.ASISTENTE) {
            throw new AccessDeniedException("Perfil de asistente no disponible");
        }
        return asistente;
    }

    public Asistente actualizarPerfilPropio(String correoAutenticado, PerfilAsistenteFormulario perfil) {
        Asistente asistente = buscarPerfilPropio(correoAutenticado);
        asistente.setNombres(perfil.getNombres().trim());
        asistente.setApellidos(perfil.getApellidos().trim());
        asistente.setTelefono(perfil.getTelefono());
        asistente.setProcedencia(perfil.getProcedencia());
        asistente.setOcupacion(perfil.getOcupacion());
        asistente.setUniversidad(perfil.getUniversidad());
        asistente.setEmpresa(perfil.getEmpresa());
        asistente.setEdad(perfil.getEdad());
        asistente.setModalidadPagoPreferida(perfil.getModalidadPagoPreferida());
        return asistenteDAO.save(asistente);
    }

    public List<Asistente> listarAsistentes() {
        return asistenteDAO.findAll();
    }

    /**
     * NUEVO: Alias para el servlet (lista todos los asistentes).
     */
    public List<Asistente> obtenerTodosLosAsistentes() {
        return listarAsistentes();
    }

    /**
     * NUEVO: Búsqueda parcial por nombre o apellido.
     * Requiere que AsistenteDAO tenga el método buscarPorNombre con @Query.
     */
    public List<Asistente> buscarPorNombre(String nombre) {
        return asistenteDAO.buscarPorNombre(nombre);
    }

    public List<Asistente> buscarPorModalidadPago(ModalidadPago modalidad) {
        return asistenteDAO.findByModalidadPagoPreferida(modalidad);
    }

    public List<Asistente> buscarPorUniversidad(String universidad) {
        return asistenteDAO.findByUniversidad(universidad);
    }

    /**
     * Suspensión lógica: conserva la fila y sus relaciones para mantener trazabilidad.
     */
    public Asistente suspenderAsistente(Long codigo, String actor) {
        Asistente asistente = asistenteDAO.findById(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Asistente no encontrado"));
        if (asistente.getEstado() == EstadoUsuario.ELIMINADO) {
            throw new IllegalStateException("No se puede suspender un asistente dado de baja");
        }

        asistente.setEstado(EstadoUsuario.SUSPENDIDO);
        Asistente guardado = asistenteDAO.save(asistente);
        RegistroAuditoria.registrar("SUSPENDER", "ASISTENTE", codigo, actor,
                "estado=SUSPENDIDO; pagos e inscripciones conservados");
        return guardado;
    }

    /**
     * Baja lógica: no elimina la fila ni sus pagos/inscripciones. La política de
     * devoluciones o liberación de cupos por baja de asistente sigue pendiente (D03).
     */
    public Asistente eliminarAsistente(Long codigo, String actor) {
        Asistente asistente = asistenteDAO.findById(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Asistente no encontrado"));

        asistente.setEstado(EstadoUsuario.ELIMINADO);
        Asistente guardado = asistenteDAO.save(asistente);
        RegistroAuditoria.registrar("ELIMINAR", "ASISTENTE", codigo, actor,
                "baja-logica; pagos e inscripciones conservados sin modificacion");
        return guardado;
    }
}
