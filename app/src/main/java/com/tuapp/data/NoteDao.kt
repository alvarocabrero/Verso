package com.tuapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notas ORDER BY fijada DESC, modificada DESC")
    fun observeAll(): Flow<List<Note>>

    @Query(
        """
        SELECT * FROM notas
        WHERE titulo LIKE '%' || :text || '%' OR contenido LIKE '%' || :text || '%'
        ORDER BY fijada DESC, modificada DESC
        """
    )
    fun search(text: String): Flow<List<Note>>

    @Query("SELECT * FROM notas WHERE id = :id")
    suspend fun get(id: Long): Note?

    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Query("DELETE FROM notas WHERE id = :id")
    suspend fun delete(id: Long)
}
