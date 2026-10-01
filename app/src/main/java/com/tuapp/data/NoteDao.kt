// =================================================================================================
// FILE: NoteDao.kt
// -------------------------------------------------------------------------------------------------
// WHAT IS THIS FILE FOR?
// It lists every database operation for notes: get all, search, get one, insert, update and
// delete.
//
// WHAT IS A DAO? "Data Access Object". It is an "interface" (a list of functions with no code
// inside) where each function is labelled with the database operation it must do. Room reads
// those labels and writes the real code for us when the app is built. We never write the
// body of these functions.
//
// The operations are written in SQL, the standard language to talk to databases. A quick
// guide to the SQL words used here:
//   - SELECT ... FROM table: "give me these columns from this table". `*` means "all columns".
//   - WHERE condition: "only the rows where the condition is true".
//   - ORDER BY column DESC: sort the results by that column, from biggest to smallest
//     (DESC = descending; ASC would be smallest to biggest).
//   - DELETE FROM table WHERE ...: remove the rows that match.
//   - `:name` inside the SQL is filled with the function parameter of the same name.
//
// Related files:
//   - data/Note.kt: the `Note` class / "notas" table.
//   - data/VersoDatabase.kt: creates the database and gives out this DAO (`noteDao()`).
//   - data/NotesRepository.kt: the only place that calls this DAO.
// =================================================================================================

package com.tuapp.data

// Room annotations: `@Dao`, `@Insert`, `@Query` and `@Update` (explained below).
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
// `Flow`: a stream of values over time (explained at `observeAll`).
import kotlinx.coroutines.flow.Flow

/**
 * Database operations for notes.
 *
 * - `@Dao` tells Room "this is a DAO: write the code for it".
 * - `interface` declares a list of functions without a body. Room creates, behind the scenes,
 *   a class that fills in those bodies.
 */
@Dao
interface NoteDao {

    /**
     * Watches all notes, pinned ones first and then the most recently changed.
     *
     * SQL: `SELECT * FROM notas` takes every column of every note.
     * `ORDER BY fijada DESC, modificada DESC` sorts first by the "pinned" column, descending
     * (pinned = 1 comes before not pinned = 0), and, when two notes tie, by the modification
     * date, newest first.
     *
     * - `@Query("...")` gives Room the SQL to run for this function.
     * - Returns `Flow<List<Note>>`: a `Flow` is like a "live feed". Instead of giving the list
     *   once, it sends a new list every time the table changes, so the screen updates by
     *   itself. `List<Note>` is a list of notes; the part in angle brackets `< >` says what
     *   kind of things the list (or the Flow) holds (this is called a "generic" type).
     */
    @Query("SELECT * FROM notas ORDER BY fijada DESC, modificada DESC")
    fun observeAll(): Flow<List<Note>>

    /**
     * Watches the notes whose title or text contains [text], in the same order as above.
     *
     * SQL: `WHERE titulo LIKE '%' || :text || '%' OR contenido LIKE ...` keeps only the notes
     * where the title OR the content contains the searched text.
     * - `LIKE` compares text with a pattern; `%` means "any text, of any length".
     * - `||` joins pieces of text in SQL. So `'%' || :text || '%'` builds the pattern
     *   "%searched%": "anything, then the searched text, then anything", which means
     *   "contains". (In SQLite, LIKE ignores upper/lower case for plain English letters.)
     * - The SQL is written between triple quotes `"""` so it can span several lines.
     */
    @Query(
        """
        SELECT * FROM notas
        WHERE titulo LIKE '%' || :text || '%' OR contenido LIKE '%' || :text || '%'
        ORDER BY fijada DESC, modificada DESC
        """
    )
    fun search(text: String): Flow<List<Note>>

    /**
     * Gets one note by its [id], or `null` if it does not exist.
     *
     * SQL: `SELECT * FROM notas WHERE id = :id` takes the row whose id equals the parameter.
     *
     * - `suspend` marks a "suspending function": it can only run inside a coroutine (a
     *   background task) and it may pause while waiting for the database without freezing the
     *   screen. Room runs it off the main thread.
     * - `Note?`: the `?` means the result may be `null` ("nothing found").
     */
    @Query("SELECT * FROM notas WHERE id = :id")
    suspend fun get(id: Long): Note?

    /**
     * Adds a new note to the table and returns the id the database gave it.
     * `@Insert` needs no SQL: Room writes the "INSERT INTO notas ..." for us.
     */
    @Insert
    suspend fun insert(note: Note): Long

    /**
     * Saves the changes of an existing note. Room finds the row by its id (the primary key)
     * and overwrites its columns. `@Update` also needs no SQL.
     */
    @Update
    suspend fun update(note: Note)

    /**
     * Deletes the note with the given [id].
     *
     * SQL: `DELETE FROM notas WHERE id = :id` removes the row whose id matches. Its links to
     * audios (table note_audios) are removed too, because that table uses
     * "ON DELETE CASCADE" (see Audio.kt).
     */
    @Query("DELETE FROM notas WHERE id = :id")
    suspend fun delete(id: Long)
}
