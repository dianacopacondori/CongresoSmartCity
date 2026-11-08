package com.congreso.controlador;

import com.congreso.modelo.Asistente;
import com.congreso.modelo.Administrador;
import com.congreso.modelo.Usuario;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.servicio.ActividadServicio;
import com.congreso.servicio.AsistenciaServicio;
import com.congreso.servicio.ColaboradorServicio;
import com.congreso.servicio.InscripcionServicio;
import com.congreso.servicio.PagoServicio;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;

/**
 * Controlador principal para las páginas públicas del congreso.
 * Solo maneja rutas que NO están en otros servlets.
 *
 * @author Equipo de Desarrollo
 * @version 1.0.0
 */
@Controller
public class CongresoController {

    private final ActividadServicio actividadServicio;
    private final ColaboradorServicio colaboradorServicio;
    private final InscripcionServicio inscripcionServicio;
    private final AsistenciaServicio asistenciaServicio;
    private final PagoServicio pagoServicio;

    public CongresoController(ActividadServicio actividadServicio, ColaboradorServicio colaboradorServicio,
                              InscripcionServicio inscripcionServicio, AsistenciaServicio asistenciaServicio,
                              PagoServicio pagoServicio) {
        this.actividadServicio = actividadServicio;
        this.colaboradorServicio = colaboradorServicio;
        this.inscripcionServicio = inscripcionServicio;
        this.asistenciaServicio = asistenciaServicio;
        this.pagoServicio = pagoServicio;
    }

    /**
     * Página de inicio (landing page pública).
     */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("actividadesDestacadas", actividadServicio.obtenerActividadesActivas().stream().limit(3).toList());
        model.addAttribute("ponentes", colaboradorServicio.listarPonentes());
        return "index";
    }

    /**
     * Página de ponentes (ruta exclusiva, no está en ActividadServlet).
     */
    @GetMapping("/ponentes")
    public String ponentes(Model model) {
        model.addAttribute("ponentes", colaboradorServicio.listarPonentes());
        return "ponentes";
    }

    /**
     * Página de contacto.
     */
    @GetMapping("/contacto")
    public String contacto() {
        return "contacto";
    }

    /**
     * Dashboard del asistente (ruta exclusiva).
     */
    @GetMapping("/dashboard-asistente")
    public String dashboardAsistente(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/usuarios/login";
        if (usuario instanceof Administrador && usuario.getRol() == RolUsuario.ADMINISTRADOR) {
            return "redirect:/admin/dashboard";
        }
        if (!(usuario instanceof Asistente asistente) || usuario.getRol() != RolUsuario.ASISTENTE) {
            session.invalidate();
            return "redirect:/usuarios/login";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("actividades", inscripcionServicio.obtenerAgendaPersonalizada(usuario.getCodigo()));
        model.addAttribute("asistencias", asistenciaServicio.listarAsistenciasPorAsistente(usuario.getCodigo()));
        var inscripciones = inscripcionServicio.listarInscripcionesPorAsistente(asistente);
        model.addAttribute("inscripcionEstado", inscripciones.isEmpty() ? "SIN INSCRIPCIÓN"
                : inscripciones.get(inscripciones.size() - 1).getEstado());
        var pagos = pagoServicio.listarTodosLosPagos(usuario.getCodigo());
        model.addAttribute("pagoEstado", pagos.isEmpty() ? "SIN PAGOS" : pagos.get(0).getEstado());
        return "dashboard-asistente";
    }

    /**
     * Dashboard del administrador (ruta exclusiva).
     */
    @GetMapping("/admin/dashboard")
    public String dashboardAdmin(Model model, HttpSession session) {
        model.addAttribute("usuario", session.getAttribute("usuario"));
        // TODO: Cargar estadísticas reales desde la base de datos
        model.addAttribute("totalAsistentes", 1250);
        model.addAttribute("registradosHoy", 85);
        model.addAttribute("totalIngresos", "125,000");
        model.addAttribute("pagosPendientes", 12);
        model.addAttribute("actividades", new ArrayList<>());
        model.addAttribute("pagos", new ArrayList<>());
        return "dashboard-admin";
    }
}
