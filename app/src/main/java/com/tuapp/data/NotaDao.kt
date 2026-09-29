package com.tuapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NotaDao {

    @Query("SELECT * FROM notas ORDER BY fijada DESC, modificada DESC")
    fun observarTodas(): Flow<List<Nota>>

    @Query(
        """
        SELECT * FROM notas
        WHERE titulo LIKE '%' || :texto || '%' OR contenido LIKE '%' || :texto || '%'
        ORDER BY fijada DESC, modificada DESC
        """
    )
    fun buscar(texto: String): Flow<List<Nota>>

    @Query("SELECT * FROM notas WHERE id = :id")
    suspend fun obtener(id: Long): Nota?

    @Insert
    suspend fun insertar(nota: Nota): Long

    @Update
    suspend fun actualizar(nota: Nota)

    @Query("DELETE FROM notas WHERE id = :id")
    suspend fun borrar(id: Long)
}
