package com.congreso.interfaces;

import java.util.List;
import java.util.Optional;

public interface DAO<T, ID> {

    /**
     * Guarda una entidad en la base de datos.
     *
     * @param entidad la entidad a guardar
     * @return la entidad guardada
     */
    T guardar(T entidad);

    /**
     * Actualiza una entidad existente en la base de datos.
     *
     * @param entidad la entidad a actualizar
     * @return la entidad actualizada
     */
    T actualizar(T entidad);

    /**
     * Elimina una entidad de la base de datos.
     * @param id el identificador de la entidad a eliminar
     */
    void eliminar(ID id);

    /**
     * Busca una entidad por su identificador.
     * @param id el identificador de la entidad
     * @return un Optional con la entidad encontrada, o vacío si no existe
     */
    Optional<T> buscarPorId(ID id);

    /**
     * Obtiene todas las entidades de la base de datos.
     * @return una lista con todas las entidades
     */
    List<T> obtenerTodos();
}
