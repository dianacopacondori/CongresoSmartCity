package com.congreso.servicio;

import com.congreso.dao.ColaboradorDAO;
import com.congreso.dao.UsuarioDAO;
import com.congreso.dto.ActividadAsignadaVista;
import com.congreso.modelo.Actividad;
import com.congreso.modelo.Colaborador;
import com.congreso.modelo.Usuario;
import com.congreso.modelo.enums.EstadoUsuario;
import com.congreso.modelo.enums.RolUsuario;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@Transactional
public class ColaboradorServicio {

    private final ColaboradorDAO colaboradorDAO;
    private final UsuarioDAO usuarioDAO;

    @Autowired
    public ColaboradorServicio(ColaboradorDAO colaboradorDAO, UsuarioDAO usuarioDAO) {
        this.colaboradorDAO = colaboradorDAO;
        this.usuarioDAO = usuarioDAO;
    }

    public Colaborador registrarColaborador(Colaborador colaborador) {
        validarRolColaborador(colaborador);
        validarUnicidad(colaborador, null);
        return colaboradorDAO.saveAndFlush(colaborador);
    }

    public List<Colaborador> listarColaboradores() {
        return colaboradorDAO.findAll();
    }

    public List<Colaborador> buscarColaboradores(String busqueda, RolUsuario tipo, EstadoUsuario estado) {
        String texto = busqueda == null ? "" : busqueda.trim().toLowerCase(Locale.ROOT);
        Long codigo = null;
        if (!texto.isEmpty()) {
            try {
                codigo = Long.valueOf(texto);
            } catch (NumberFormatException ignored) {
                // La búsqueda de texto también incluye código, institución y empresa.
            }
        }
        final Long codigoBuscado = codigo;
        return colaboradorDAO.findAll().stream()
                .filter(c -> tipo == null || c.getTipoColaborador() == tipo)
                .filter(c -> estado == null || c.getEstado() == estado)
                .filter(c -> texto.isEmpty() || (codigoBuscado != null && codigoBuscado.equals(c.getCodigo()))
                        || contiene(c.getNombres(), texto) || contiene(c.getApellidos(), texto)
                        || contiene(c.getIdentificacion(), texto) || contiene(c.getCorreo(), texto)
                        || contiene(c.getInstitucion(), texto) || contiene(c.getEmpresa(), texto))
                .sorted(Comparator.comparing(Colaborador::getCodigo))
                .toList();
    }

    public Colaborador buscarPorId(Long codigo) {
        return colaboradorDAO.findById(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Colaborador no encontrado"));
    }

    public Colaborador actualizarColaborador(Long codigo, Colaborador datos) {
        Colaborador existente = buscarPorId(codigo);
        validarRolColaborador(datos);
        validarUnicidad(datos, codigo);
        if (existente.getTipoColaborador() != datos.getTipoColaborador()
                && (!existente.getPonencias().isEmpty() || !existente.getTalleres().isEmpty())) {
            throw new IllegalArgumentException(
                    "No se puede cambiar el tipo de un colaborador con actividades asignadas");
        }

        existente.setNombres(datos.getNombres());
        existente.setApellidos(datos.getApellidos());
        existente.setIdentificacion(datos.getIdentificacion());
        existente.setCorreo(datos.getCorreo());
        existente.setTelefono(datos.getTelefono());
        existente.setInstitucion(datos.getInstitucion());
        existente.setEmpresa(datos.getEmpresa());
        existente.setTipoColaborador(datos.getTipoColaborador());
        existente.setRol(datos.getTipoColaborador());
        existente.setEstado(datos.getEstado());
        if (datos.getContrasena() != null && !datos.getContrasena().isBlank()) {
            existente.setContrasena(datos.getContrasena());
        }
        return colaboradorDAO.saveAndFlush(existente);
    }

    public Colaborador darDeBaja(Long codigo) {
        Colaborador colaborador = buscarPorId(codigo);
        colaborador.setEstado(EstadoUsuario.ELIMINADO);
        return colaboradorDAO.saveAndFlush(colaborador);
    }

    public List<Colaborador> listarPonentes() {
        return colaboradorDAO.findByTipoColaboradorAndEstado(RolUsuario.PONENTE, EstadoUsuario.ACTIVO);
    }

    public List<Colaborador> listarInstructores() {
        return colaboradorDAO.findByTipoColaboradorAndEstado(RolUsuario.INSTRUCTOR, EstadoUsuario.ACTIVO);
    }

    public Colaborador actualizarColaborador(Colaborador colaborador) {
        return actualizarColaborador(colaborador.getCodigo(), colaborador);
    }

    public List<ActividadAsignadaVista> listarActividadesAsignadas(String correo, RolUsuario rolEsperado) {
        Colaborador colaborador = colaboradorDAO.findByCorreoIgnoreCase(correo)
                .filter(Usuario::estaActivo)
                .filter(c -> c.getRol() == rolEsperado && c.getTipoColaborador() == rolEsperado)
                .orElseThrow(() -> new AccessDeniedException("No hay acceso a estas asignaciones"));

        List<Actividad> actividades = new ArrayList<>();
        if (rolEsperado == RolUsuario.PONENTE || rolEsperado == RolUsuario.COLABORADOR) {
            actividades.addAll(colaborador.getPonencias());
        }
        if (rolEsperado == RolUsuario.INSTRUCTOR || rolEsperado == RolUsuario.COLABORADOR) {
            actividades.addAll(colaborador.getTalleres());
        }
        return actividades.stream()
                .sorted(Comparator.comparing(Actividad::getFecha)
                        .thenComparing(Actividad::getHorarioInicio))
                .map(ActividadAsignadaVista::desde)
                .toList();
    }

    private void validarRolColaborador(Colaborador colaborador) {
        RolUsuario tipo = colaborador.getTipoColaborador();
        if (tipo != RolUsuario.COLABORADOR && tipo != RolUsuario.INSTRUCTOR && tipo != RolUsuario.PONENTE) {
            throw new IllegalArgumentException("El tipo de colaborador no es válido");
        }
        if (colaborador.getRol() != tipo) {
            throw new IllegalArgumentException("El rol y el tipo de colaborador deben coincidir");
        }
    }

    private void validarUnicidad(Colaborador colaborador, Long codigoActual) {
        usuarioDAO.findByCorreoIgnoreCase(colaborador.getCorreo()).ifPresent(otro -> {
            if (!Objects.equals(otro.getCodigo(), codigoActual)) {
                throw new IllegalArgumentException("Ya existe un usuario con ese correo");
            }
        });
        usuarioDAO.findByIdentificacion(colaborador.getIdentificacion()).ifPresent(otro -> {
            if (!Objects.equals(otro.getCodigo(), codigoActual)) {
                throw new IllegalArgumentException("Ya existe un usuario con esa identificación");
            }
        });
    }

    private boolean contiene(String valor, String texto) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(texto);
    }
}
