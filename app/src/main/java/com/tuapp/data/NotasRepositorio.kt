package com.tuapp.data

import kotlinx.coroutines.flow.Flow

class NotasRepositorio(private val dao: NotaDao) {

    fun notas(busqueda: String): Flow<List<Nota>> =
        if (busqueda.isBlank()) dao.observarTodas() else dao.buscar(busqueda.trim())

    suspend fun obtener(id: Long): Nota? = dao.obtener(id)

    /** Inserta si es nueva (id = 0) o actualiza. Devuelve el id definitivo. */
    suspend fun guardar(nota: Nota): Long =
        if (nota.id == 0L) dao.insertar(nota) else { dao.actualizar(nota); nota.id }

    suspend fun borrar(id: Long) = dao.borrar(id)
}
