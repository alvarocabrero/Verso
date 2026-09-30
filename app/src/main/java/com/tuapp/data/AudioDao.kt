package com.tuapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioDao {

    @Query("SELECT * FROM audios ORDER BY created DESC")
    fun observeAll(): Flow<List<Audio>>

    @Query("SELECT * FROM audios WHERE name LIKE '%' || :text || '%' ORDER BY created DESC")
    fun search(text: String): Flow<List<Audio>>

    @Query("SELECT * FROM audios WHERE id = :id")
    suspend fun get(id: Long): Audio?

    @Insert
    suspend fun insert(audio: Audio): Long

    @Query("UPDATE audios SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM audios WHERE id = :id")
    suspend fun delete(id: Long)

    // ---------- Links with notes ----------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun link(link: NoteAudio)

    @Query("DELETE FROM note_audios WHERE note_id = :noteId AND audio_id = :audioId")
    suspend fun unlink(noteId: Long, audioId: Long)

    @Query(
        """
        SELECT audios.* FROM audios
        JOIN note_audios ON audios.id = note_audios.audio_id
        WHERE note_audios.note_id = :noteId
        ORDER BY audios.created DESC
        """
    )
    fun audiosOfNote(noteId: Long): Flow<List<Audio>>

    @Query(
        """
        SELECT note_audios.audio_id AS audioId, notas.id AS noteId, notas.titulo AS title
        FROM note_audios JOIN notas ON notas.id = note_audios.note_id
        ORDER BY notas.modificada DESC
        """
    )
    fun noteLinks(): Flow<List<AudioNoteLink>>

    @Query("SELECT note_id AS noteId, COUNT(*) AS count FROM note_audios GROUP BY note_id")
    fun audioCounts(): Flow<List<NoteAudioCount>>

    @Query("SELECT COUNT(*) FROM note_audios WHERE note_id = :noteId")
    suspend fun audioCount(noteId: Long): Int
}
