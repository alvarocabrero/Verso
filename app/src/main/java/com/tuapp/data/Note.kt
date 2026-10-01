// =================================================================================================
// FILE: Note.kt
// -------------------------------------------------------------------------------------------------
// WHAT IS THIS FILE FOR?
// It describes what a "note" (a poem or a song) is, and how it is stored in the database.
//
// It contains:
//   - `NoteType`: the kind of note (poem or song).
//   - `Note`: one note with its title, text, type, colour, pinned flag and dates. Room (the
//     database library) turns it into a table called "notas", where each note is one row.
//   - `Converters`: tells Room how to store a `NoteType` as text in the database and how to
//     read it back.
//
// Related files:
//   - data/NoteDao.kt: the database queries for notes (read, search, insert, update, delete).
//   - data/NotesRepository.kt: the layer the screens use to work with notes.
//   - data/VersoDatabase.kt: the database that lists `Note` as a table and uses `Converters`.
//   - ui/theme/ (NotePalette): the list of colours that `color` points into.
//
// WHAT IS ROOM? Room is an Android library that sits on top of SQLite (a small database stored
// in one file on the phone). You describe your tables with Kotlin classes and "annotations"
// (labels that start with @), and Room writes the boring database code for you.
// =================================================================================================

// The package ("logical folder") of this file: everything about data lives in `com.tuapp.data`.
package com.tuapp.data

// Room annotations used below (each one is explained where it is used).
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/**
 * The kind of a note: a poem or a song.
 *
 * - `enum class` declares an "enumeration": a type with a fixed, closed list of possible values.
 *   A `NoteType` can only be `POEM` or `SONG`, nothing else.
 * - `(val label: String)` gives every value a property: [label] is the name shown in the
 *   (Spanish) UI. `String` is the type for text.
 * - Each value passes its label in brackets: `POEM("Poema")` means "the POEM value, whose
 *   label is Poema".
 */
enum class NoteType(val label: String) {
    // A poem. Shown as "Poema" on screen.
    POEM("Poema"),
    // A song. Shown as "Canción" on screen.
    SONG("Canción")
}

/**
 * One note of the notebook. Each `Note` object is one row of the "notas" table.
 *
 * The table and column names keep their original Spanish names so that databases created by
 * version 0.1.0 keep working without a migration (a migration is a script that changes the
 * shape of an existing database). That is why the Kotlin names are in English (`title`) but
 * the columns are in Spanish ("titulo").
 *
 * KOTLIN AND ROOM SYNTAX:
 * - `@Entity(tableName = "notas")`: tells Room "this class is a table, and it is called notas".
 * - `data class`: a class made mainly to hold data. Kotlin gives it useful extras for free:
 *   comparing two notes by their content, printing them, and `copy(...)` to create a changed
 *   copy (for example `note.copy(title = "New")`).
 * - The list in brackets `( ... )` is the "primary constructor": the properties of the note.
 *   Each one is `val name: Type = defaultValue`. `val` means it cannot be changed after
 *   creation (to "change" a note, you create a copy). The `= ...` part is a default value
 *   used when nobody gives one.
 * - `@ColumnInfo(name = "...")`: the name of the column in the table, when it differs from
 *   the Kotlin name.
 */
@Entity(tableName = "notas")
data class Note(
    // Unique number of the note. `@PrimaryKey` means this column identifies each row (no two
    // notes share it). `autoGenerate = true`: the database picks the number by itself when a
    // new note is inserted. `Long` is a whole number that can be very large. The default 0
    // means "not saved yet" (the database will replace it).
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // The title. Stored in the "titulo" column. Empty text by default.
    @ColumnInfo(name = "titulo") val title: String = "",
    // The text of the poem or song. Stored in the "contenido" column.
    @ColumnInfo(name = "contenido") val content: String = "",
    // Poem or song. Stored in the "tipo" column as text, thanks to `Converters` (below).
    @ColumnInfo(name = "tipo") val type: NoteType = NoteType.POEM,
    // The card colour: a position (index) in the `NotePalette` colour list (0 = first colour).
    // `Int` is a normal whole number.
    val color: Int = 0,
    // Whether the note is pinned to the top of the list. `Boolean` is true or false.
    @ColumnInfo(name = "fijada") val pinned: Boolean = false,
    // When the note was created, as milliseconds since 1 January 1970 (the usual way computers
    // store dates). `System.currentTimeMillis()` gives "now" in that format.
    @ColumnInfo(name = "creada") val created: Long = System.currentTimeMillis(),
    // When the note was last changed, in the same format. Used to sort the list.
    @ColumnInfo(name = "modificada") val modified: Long = System.currentTimeMillis()
)

/**
 * Tells Room how to store [NoteType] in the database, using the values from version 0.1.0
 * ("POEMA", "CANCION"). SQLite does not know our `NoteType`, only simple types like text and
 * numbers, so we translate it to text on the way in and back on the way out.
 *
 * VersoDatabase.kt registers this class with `@TypeConverters(Converters::class)`.
 */
class Converters {
    /**
     * From `NoteType` to text (used when saving).
     *
     * - `@TypeConverter` marks the function as a converter for Room.
     * - `fun fromNoteType(type: NoteType): String` receives a `NoteType` and returns a
     *   `String` (the type after the last colon is the return type).
     * - `= when (...) { ... }`: the `=` means the function's body is just this one expression,
     *   and its value is what the function returns.
     * - `when` is like a "switch": it checks the value and picks the matching branch.
     *   `A -> B` means "if it is A, the result is B".
     */
    @TypeConverter
    fun fromNoteType(type: NoteType): String = when (type) {
        NoteType.POEM -> "POEMA"
        NoteType.SONG -> "CANCION"
    }

    /**
     * From text back to `NoteType` (used when reading). "CANCION" becomes a song; anything
     * else (`else`, the "any other case" branch) becomes a poem, so an unknown value never
     * crashes the app.
     */
    @TypeConverter
    fun toNoteType(value: String): NoteType = when (value) {
        "CANCION" -> NoteType.SONG
        else -> NoteType.POEM
    }
}
