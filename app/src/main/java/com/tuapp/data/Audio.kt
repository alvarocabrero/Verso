// =================================================================================================
// FILE: Audio.kt
// -------------------------------------------------------------------------------------------------
// WHAT IS THIS FILE FOR?
// It describes the audio data of the app and how it is stored in the database:
//   - `Audio`: one sound clip (recorded in the app or imported). Table "audios".
//   - `NoteAudio`: a link between one note and one audio. Table "note_audios".
//   - `AudioNoteLink` and `NoteAudioCount`: small "result shapes" for two queries. They are
//     NOT tables; they just hold the columns that those queries return.
//
// The sound itself is NOT stored in the database: it is a file in the app's private storage
// (folder "files/audios"). The database only keeps the file name, the clip name and its length.
//
// Related files:
//   - data/AudioDao.kt: the database queries for audios and links.
//   - data/AudioRepository.kt: the layer the screens use (it also handles the files).
//   - data/VersoDatabase.kt: registers `Audio` and `NoteAudio` as tables (and the migration
//     that created them in version 2).
//   - data/Note.kt: the `Note` class that `NoteAudio` points to.
//
// ROOM REMINDER: Room is the Android database library. `@Entity` marks a class as a table;
// each object of that class is one row, and each property is one column.
// =================================================================================================

package com.tuapp.data

// Room annotations and helpers used below.
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * An audio clip (recorded in the app or imported). The file lives in the app's private
 * storage.
 *
 * - `@Entity(tableName = "audios")`: this class is the table "audios".
 * - `data class`: a class made to hold data; Kotlin adds for free comparing by content,
 *   printing, and `copy(...)` to create a changed copy.
 * - Each property is written `val name: Type`; `val` means it cannot change after creation.
 *   Properties with `= value` have a default value and can be left out when creating one.
 */
@Entity(tableName = "audios")
data class Audio(
    // Unique number of the audio. `@PrimaryKey` = the column that identifies each row.
    // `autoGenerate = true` = the database picks the number when the row is inserted, so 0
    // just means "not saved yet". `Long` is a large whole number.
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // The name shown to the user (for example "Grabación 30 sep 21:22"). `String` = text.
    val name: String,
    // The name of the sound file inside the "audios" folder (a random unique name, like
    // "3f2a...-...m4a"). The column is called "file_name" (`@ColumnInfo` sets that name).
    @ColumnInfo(name = "file_name") val fileName: String,
    // How long the audio lasts, in milliseconds (1000 ms = 1 second). Column "duration_ms".
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    // When it was added, as milliseconds since 1 January 1970 (the usual computer format).
    // `System.currentTimeMillis()` gives "now".
    val created: Long = System.currentTimeMillis()
)

/**
 * Many-to-many link: a note can have several audios, and an audio can be in several notes.
 * Each row says "this note (note_id) has this audio (audio_id)".
 *
 * Details of the `@Entity` settings:
 * - `tableName = "note_audios"`: the table name.
 * - `primaryKeys = ["note_id", "audio_id"]`: the PAIR of columns identifies the row, so the
 *   same note-audio link cannot be stored twice. `[ ... ]` here is a list of values.
 * - `foreignKeys = [...]`: "foreign keys" are rules that tie this table to others:
 *   - `note_id` must be the `id` of an existing row in the `Note` table ("notas").
 *   - `audio_id` must be the `id` of an existing row in the `Audio` table ("audios").
 *   - `onDelete = ForeignKey.CASCADE`: when that note or that audio is deleted, the database
 *     deletes its links here automatically ("cascade" = the deletion flows down).
 *   - `Note::class` is Kotlin's way to refer to the class itself (not to one note object).
 * - `indices = [Index("audio_id")]`: an "index" is like the index at the back of a book: it
 *   makes looking up rows by `audio_id` fast. (The primary key already acts as an index that
 *   starts with note_id.)
 */
@Entity(
    tableName = "note_audios",
    primaryKeys = ["note_id", "audio_id"],
    foreignKeys = [
        ForeignKey(entity = Note::class, parentColumns = ["id"], childColumns = ["note_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Audio::class, parentColumns = ["id"], childColumns = ["audio_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("audio_id")]
)
data class NoteAudio(
    // The id of the note. Column "note_id".
    @ColumnInfo(name = "note_id") val noteId: Long,
    // The id of the audio. Column "audio_id".
    @ColumnInfo(name = "audio_id") val audioId: Long
)

/**
 * A note that an audio is linked to, as shown in the audio list (under each audio, the titles
 * of its notes). It is not a table: it is the shape of each row returned by
 * `AudioDao.noteLinks()`, which gives the audio id, the note id and the note title.
 * Written on one line because it has no annotations: just three read-only properties.
 */
data class AudioNoteLink(val audioId: Long, val noteId: Long, val title: String)

/**
 * How many audios a note has, for the note cards (they show a small audio counter).
 * Not a table: the shape of each row returned by `AudioDao.audioCounts()`.
 */
data class NoteAudioCount(val noteId: Long, val count: Int)
