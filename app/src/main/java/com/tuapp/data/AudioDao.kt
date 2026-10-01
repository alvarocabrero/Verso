// =================================================================================================
// FILE: AudioDao.kt
// -------------------------------------------------------------------------------------------------
// WHAT IS THIS FILE FOR?
// It lists every database operation for audios and for the links between notes and audios.
//
// WHAT IS A DAO? "Data Access Object": an "interface" (a list of functions with no code inside)
// where each function is labelled with the database operation it must do. Room (the database
// library) reads those labels and writes the real code for us when the app is built.
//
// The operations are written in SQL, the standard language to talk to databases. Quick guide
// to the SQL words used in this file:
//   - SELECT columns FROM table: "give me these columns from this table". `*` = all columns.
//   - WHERE condition: only the rows where the condition is true.
//   - ORDER BY column DESC: sort by that column from biggest to smallest (DESC = descending).
//   - UPDATE table SET column = value WHERE ...: change a value in the matching rows.
//   - DELETE FROM table WHERE ...: remove the matching rows.
//   - JOIN other ON condition: combine each row with the rows of another table that match.
//   - AS name: give a column of the result a different name.
//   - COUNT(*): count rows. GROUP BY column: make one group per value and count per group.
//   - `:name` inside the SQL is replaced by the function parameter with the same name.
//
// Related files:
//   - data/Audio.kt: the `Audio`, `NoteAudio`, `AudioNoteLink` and `NoteAudioCount` classes.
//   - data/VersoDatabase.kt: creates the database and gives out this DAO (`audioDao()`).
//   - data/AudioRepository.kt: the only place that calls this DAO.
// =================================================================================================

package com.tuapp.data

// Room annotations (`@Dao`, `@Insert`, `@Query`) and the conflict rule used by `link`.
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
// `Flow`: a "live feed" that sends a new value every time the data changes.
import kotlinx.coroutines.flow.Flow

/**
 * Database operations for audios and their links with notes.
 *
 * - `@Dao` tells Room "this is a DAO: write the code for it".
 * - `interface` = a list of functions without a body; Room fills them in.
 * - `suspend fun` = a function that must run inside a coroutine (a background task). It can
 *   pause while the database works, without freezing the screen.
 * - Functions that return `Flow<...>` do not need `suspend`: they return a live feed at once,
 *   and the feed delivers the results (again and again, whenever the tables change).
 * - `List<Audio>` = a list of audios. The type in angle brackets `< >` says what the list
 *   holds (a "generic" type).
 */
@Dao
interface AudioDao {

    /**
     * Watches all audios, newest first.
     *
     * SQL: `SELECT * FROM audios` takes every column of every audio;
     * `ORDER BY created DESC` sorts by creation date, from newest to oldest.
     */
    @Query("SELECT * FROM audios ORDER BY created DESC")
    fun observeAll(): Flow<List<Audio>>

    /**
     * Watches the audios whose name contains [text], newest first.
     *
     * SQL: `WHERE name LIKE '%' || :text || '%'` keeps the audios whose name contains the
     * searched text. `LIKE` compares with a pattern where `%` means "any text"; `||` joins
     * texts, so the pattern becomes "%searched%" = "contains". Then it sorts newest first.
     */
    @Query("SELECT * FROM audios WHERE name LIKE '%' || :text || '%' ORDER BY created DESC")
    fun search(text: String): Flow<List<Audio>>

    /**
     * Gets one audio by its [id], or `null` if it does not exist (the `?` in `Audio?` means
     * "may be null").
     *
     * SQL: `SELECT * FROM audios WHERE id = :id` takes the row whose id equals the parameter.
     */
    @Query("SELECT * FROM audios WHERE id = :id")
    suspend fun get(id: Long): Audio?

    /**
     * Adds a new audio and returns the id the database gave it.
     * `@Insert` needs no SQL: Room writes the "INSERT INTO audios ..." itself.
     */
    @Insert
    suspend fun insert(audio: Audio): Long

    /**
     * Changes the name of an audio.
     *
     * SQL: `UPDATE audios SET name = :name WHERE id = :id` writes the new name in the "name"
     * column, only in the row whose id matches.
     */
    @Query("UPDATE audios SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    /**
     * Deletes the audio with this [id]. (The file is deleted separately, by AudioRepository.)
     *
     * SQL: `DELETE FROM audios WHERE id = :id` removes that row. Its links in "note_audios"
     * disappear too, thanks to "ON DELETE CASCADE" (see Audio.kt).
     */
    @Query("DELETE FROM audios WHERE id = :id")
    suspend fun delete(id: Long)

    // ---------- Links with notes ----------

    /**
     * Links an audio to a note (adds one row to "note_audios").
     *
     * `onConflict = OnConflictStrategy.IGNORE`: if that exact link already exists (same
     * note and same audio), do nothing instead of failing with an error.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun link(link: NoteAudio)

    /**
     * Removes the link between one note and one audio. The note and the audio themselves stay.
     *
     * SQL: `DELETE FROM note_audios WHERE note_id = :noteId AND audio_id = :audioId` removes
     * the row where BOTH columns match (`AND` = both conditions must be true).
     */
    @Query("DELETE FROM note_audios WHERE note_id = :noteId AND audio_id = :audioId")
    suspend fun unlink(noteId: Long, audioId: Long)

    /**
     * Watches the audios linked to one note, newest first.
     *
     * SQL, line by line:
     * - `SELECT audios.* FROM audios`: we want all the columns of the "audios" table.
     *   (`audios.*` = "all columns of audios", because the JOIN adds columns from another table.)
     * - `JOIN note_audios ON audios.id = note_audios.audio_id`: pair each audio with its link
     *   rows, matching the audio id with the link's audio_id.
     * - `WHERE note_audios.note_id = :noteId`: keep only the links of this note.
     * - `ORDER BY audios.created DESC`: newest audio first.
     * The SQL is inside triple quotes `"""` so it can be written on several lines.
     */
    @Query(
        """
        SELECT audios.* FROM audios
        JOIN note_audios ON audios.id = note_audios.audio_id
        WHERE note_audios.note_id = :noteId
        ORDER BY audios.created DESC
        """
    )
    fun audiosOfNote(noteId: Long): Flow<List<Audio>>

    /**
     * Watches every note-audio link together with the note's title, for the audio list
     * (each audio shows the titles of the notes it is in).
     *
     * SQL, line by line:
     * - `SELECT note_audios.audio_id AS audioId, notas.id AS noteId, notas.titulo AS title`:
     *   take three columns and rename them with `AS` so they match the property names of
     *   `AudioNoteLink` (audioId, noteId, title). Room fills each object that way.
     * - `FROM note_audios JOIN notas ON notas.id = note_audios.note_id`: combine each link with
     *   its note (the "notas" table), to get the note's title.
     * - `ORDER BY notas.modificada DESC`: most recently changed notes first.
     */
    @Query(
        """
        SELECT note_audios.audio_id AS audioId, notas.id AS noteId, notas.titulo AS title
        FROM note_audios JOIN notas ON notas.id = note_audios.note_id
        ORDER BY notas.modificada DESC
        """
    )
    fun noteLinks(): Flow<List<AudioNoteLink>>

    /**
     * Watches how many audios each note has (only notes that have at least one).
     *
     * SQL: `SELECT note_id AS noteId, COUNT(*) AS count FROM note_audios GROUP BY note_id`
     * makes one group per note in the links table and counts its rows. Each result row
     * becomes a `NoteAudioCount(noteId, count)`.
     */
    @Query("SELECT note_id AS noteId, COUNT(*) AS count FROM note_audios GROUP BY note_id")
    fun audioCounts(): Flow<List<NoteAudioCount>>

    /**
     * How many audios one note has, read once (not a live feed). `Int` = whole number.
     *
     * SQL: `SELECT COUNT(*) FROM note_audios WHERE note_id = :noteId` counts the link rows of
     * that note.
     */
    @Query("SELECT COUNT(*) FROM note_audios WHERE note_id = :noteId")
    suspend fun audioCount(noteId: Long): Int
}
