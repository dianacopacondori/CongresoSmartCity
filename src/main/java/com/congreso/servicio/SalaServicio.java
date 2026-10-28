package com.congreso.servicio;

import com.congreso.dao.SalaDAO;
import com.congreso.modelo.Sala;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para la gestión de salas.
 * Aplica el principio de responsabilidad única (SRP) de SOLID.
 *
 * @author Equipo de Desarrollo
 * @version 1.0.0
 */
@Service
@Transactional
public class SalaServicio {

    private final SalaDAO salaDAO;

    @Autowired
    public SalaServicio(SalaDAO salaDAO) {
        this.salaDAO = salaDAO;
    }

    /**
     * Crea una nueva sala.
     *
     * @param sala la sala a crear
     * @return la sala creada
     */
    public Sala crearSala(Sala sala) {
        return salaDAO.save(sala);
    }

    /**
     * Actualiza una sala existente.
     *
     * @param sala la sala con los datos actualizados
     * @return la sala actualizada
     * @throws IllegalArgumentException si la sala no existe
     */
    public Sala actualizarSala(Sala sala) {
        if (!salaDAO.existsById(sala.getCodigo())) {
            throw new IllegalArgumentException("La sala no existe");
        }
        return salaDAO.save(sala);
    }

    /**
     * Elimina una sala.
     *
     * @param codigoSala el código de la sala a eliminar
     * @throws IllegalArgumentException si la sala no existe o tiene actividades asignadas
     */
    public void eliminarSala(Long codigoSala) {
        Sala sala = salaDAO.findById(codigoSala)
                .orElseThrow(() -> new IllegalArgumentException("La sala no existe"));

        if (!sala.getActividades().isEmpty()) {
            throw new IllegalArgumentException("No se puede eliminar la sala porque tiene actividades asignadas");
        }

        salaDAO.delete(sala);
    }

    /**
     * Busca una sala por su código.
     *
     * @param codigo el código de la sala
     * @return un Optional con la sala encontrada, o vacío si no existe
     */
    public Optional<Sala> buscarPorId(Long codigo) {
        return salaDAO.findById(codigo);
    }

    /**
     * Obtiene todas las salas.
     *
     * @return una lista de todas las salas
     */
    public List<Sala> obtenerTodasLasSalas() {
        return salaDAO.findAll();
    }

    /**
     * Verifica si una sala está disponible en un horario específico.
     *
     * @param salaId el código de la sala
     * @param fecha la fecha a verificar
     * @param inicio la hora de inicio
     * @param fin la hora de fin
     * @return true si la sala está disponible, false en caso contrario
     */
    public boolean estaDisponible(Long salaId, LocalDate fecha, LocalTime inicio, LocalTime fin) {
        return salaDAO.estaDisponible(salaId, fecha, inicio, fin);
    }

    /**
     * Busca salas por capacidad mínima.
     *
     * @param capacidadMinima la capacidad mínima requerida
     * @return una lista de salas con capacidad suficiente
     */
    public List<Sala> buscarPorCapacidadMinima(Integer capacidadMinima) {
        return salaDAO.findByCapacidadGreaterThanEqual(capacidadMinima);
    }
}
