// =================================================================================================
// FILE: AudioRepository.kt
// -------------------------------------------------------------------------------------------------
// WHAT IS THIS FILE FOR?
// The "repository" is the door the screens use to work with audios. It does two jobs:
//   1. Talks to the database through the DAO (data/AudioDao.kt): list, search, add, rename,
//      delete audios, and link or unlink them with notes.
//   2. Handles the real sound FILES on the phone: where they are stored, creating a new file
//      for a recording, copying an imported audio into the app, reading its length, and
//      deleting the file.
//
// Files are kept in the app's private storage (folder "files/audios"), so an imported audio
// survives even if the original is deleted from the phone. Other apps cannot see this folder.
//
// Related files:
//   - data/Audio.kt: the `Audio` and `NoteAudio` classes.
//   - data/AudioDao.kt: the database operations used here.
//   - audio/AudioFormat.kt: helpers for file names and extensions (`extension`, `nameFromFile`).
//   - VersoApp.kt: creates the single `AudioRepository` (`audioRepository`).
//   - ui/audios/ and ui/editor/: the screens that use it.
// =================================================================================================

package com.tuapp.data

// `Context`: Android's "context", the door to files, system services and so on.
import android.content.Context
// `MediaMetadataRetriever`: an Android tool that reads information from a media file
// (here: its duration).
import android.media.MediaMetadataRetriever
// `Uri`: an "address" of a piece of content, for example a file chosen in the phone's
// file picker ("content://..."). It is not a normal file path.
import android.net.Uri
// `OpenableColumns`: names of the standard information about a chosen file (like its name).
import android.provider.OpenableColumns
// App helper to work out file extensions and clean names.
import com.tuapp.audio.AudioFormat
// Coroutine tools: `Dispatchers` (which threads to use) and `withContext` (switch threads).
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
// `File`: a file or folder on the phone's storage (standard Java class).
import java.io.File
// `UUID`: creates random unique codes, used here as file names so two files never clash.
import java.util.UUID

/**
 * Audios and their files. Files are kept in the app's private storage ("files/audios"),
 * so an imported audio survives if the original is deleted.
 *
 * The constructor receives:
 * - `dao`: the audio DAO, for the database.
 * - `context`: Android's Context, to reach the app's private folders and to read files the
 *   user picks.
 * Both are `private val`: kept inside the object and only usable from this class.
 */
class AudioRepository(private val dao: AudioDao, private val context: Context) {

    /**
     * The folder where audio files live ("audios" inside the app's private "files" folder).
     *
     * - `get() = ...` makes this a "computed property": the code runs EVERY time `dir` is
     *   read, instead of storing a value once.
     * - `context.filesDir` is the app's private "files" folder.
     * - `File(parent, "audios")` means "the item called audios inside that folder".
     * - `.apply { mkdirs() }`: `apply` runs the block on the object and then returns the same
     *   object. Here `mkdirs()` creates the folder if it does not exist yet. So `dir` always
     *   gives a folder that really exists.
     */
    private val dir: File get() = File(context.filesDir, "audios").apply { mkdirs() }

    /**
     * Live feed of audios that match the search text [query]: all audios if the search is
     * empty (or only spaces), otherwise the ones whose name contains it (spaces at the ends
     * are removed with `trim()`).
     * `if (...) A else B` gives back A or B, and that is the function's result.
     */
    fun audios(query: String): Flow<List<Audio>> =
        if (query.isBlank()) dao.observeAll() else dao.search(query.trim())

    /**
     * The file on the phone that holds the sound of this [audio].
     * The return type is not written: Kotlin works it out by itself (it is a `File`).
     */
    fun fileOf(audio: Audio) = File(dir, audio.fileName)

    /**
     * A new, empty file for a recording (the recorder will write into it).
     * The name is a random unique code plus ".m4a" (the audio format used for recordings).
     * `"${...}"` is a "string template": the expression inside `${ }` is replaced by its value.
     */
    fun newRecordingFile(): File = File(dir, "${UUID.randomUUID()}.m4a")

    /**
     * Saves a finished recording in the database: it creates an `Audio` with the given name,
     * the file's name and the duration, inserts it, and returns the new id.
     * `Audio(name = name, ...)` uses "named arguments" (`parameter = value`) for clarity.
     */
    suspend fun saveRecording(file: File, name: String, durationMs: Long): Long =
        dao.insert(Audio(name = name, fileName = file.name, durationMs = durationMs))

    /**
     * Copies the audio at [uri] into the app and adds it. Returns its id, or null if it can't
     * be read.
     *
     * [uri] is the address of the file the user picked in the phone's file picker.
     * The return type `Long?` means "a number, or null".
     *
     * `withContext(Dispatchers.IO) { ... }` runs the whole block on the "IO" threads (the
     * workers meant for reading and writing files and network), so the screen does not
     * freeze while a long file is copied. The value of the block's last line is the result.
     */
    suspend fun import(uri: Uri): Long? = withContext(Dispatchers.IO) {
        // The "content resolver" is Android's tool to read content from a Uri.
        val resolver = context.contentResolver
        // Ask for the file's display name (for example "maqueta final.mp3").
        // - `resolver.query(...)` returns a "cursor" (a pointer over result rows), or null.
        //   `arrayOf(OpenableColumns.DISPLAY_NAME)` asks only for the name column.
        // - `?.` is the "safe call": if the value on the left is null, skip the call and give
        //   null; otherwise call it normally.
        // - `.use { c -> ... }` runs the block with the cursor (named `c`) and closes the
        //   cursor afterwards, even if something goes wrong.
        // - `c.moveToFirst()` moves to the first row (false if there is none); then
        //   `c.getString(0)` reads the first column, the name. If there is no row: null.
        // - `?: "audio"` is the "Elvis operator": if everything on the left gave null, use
        //   "audio" instead.
        val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: "audio"
        // The extension of the original name ("mp3", "m4a"...), or "audio" if it has none.
        val ext = AudioFormat.extension(displayName) ?: "audio"
        // The file inside the app where the copy will go: a random unique name + extension.
        // `$ext` inside the text is replaced by the value of `ext`.
        val target = File(dir, "${UUID.randomUUID()}.$ext")
        // Copy the content. `runCatching { ... }` runs the block and catches any error
        // instead of crashing; `.getOrNull()` then gives the result, or null if it failed.
        // Inside: open the source for reading (`openInputStream`, may be null), open the
        // target for writing (`outputStream()`), and `input.copyTo(it)` copies all the bytes.
        // In a lambda with one parameter and no name, that parameter is called `it` (here, the
        // output stream). Both `use` blocks close their stream when done.
        val copied = runCatching {
            resolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } }
        }.getOrNull()
        // If the copy failed (null), delete the half-written file and stop, returning null.
        // `return@withContext null` means "leave the withContext block with the value null"
        // (the label `@withContext` says which block to leave).
        if (copied == null) { target.delete(); return@withContext null }
        // Read the duration of the copied file. If Android cannot read it as audio (null),
        // `?: run { ... }` runs the block: delete the file and return null. `run` just runs a
        // block of code where an expression is expected.
        val duration = durationOf(target) ?: run { target.delete(); return@withContext null }
        // Everything went well: add the audio to the database, with a clean name built from
        // the original file name (for example "idea_estribillo.m4a" becomes "idea estribillo").
        // This last line's value (the new id) is the result of the function.
        dao.insert(Audio(name = AudioFormat.nameFromFile(displayName), fileName = target.name, durationMs = duration))
    }

    /**
     * Duration of an audio file in milliseconds, or null if Android can't read it as audio.
     *
     * - `runCatching { ... }.getOrNull()`: any error gives null instead of a crash.
     * - `MediaMetadataRetriever().run { ... }`: creates the reader and runs the block "inside"
     *   it, so its functions (`setDataSource`, `extractMetadata`, `release`) can be called
     *   directly without writing the object's name. The block's last value is returned.
     * - `try { ... } finally { ... }`: the `finally` part ALWAYS runs, error or not. Here it
     *   calls `release()` to free the reader's resources.
     */
    fun durationOf(file: File): Long? = runCatching {
        MediaMetadataRetriever().run {
            try {
                // Point the reader at the file (by its full path on the phone).
                setDataSource(file.absolutePath)
                // Read the "duration" information. It comes as text (or null), so `?.toLong()`
                // turns it into a number only if it is not null.
                extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
            } finally {
                release()
            }
        }
    }.getOrNull()

    /** Renames an audio, removing spaces at the start and end of the new name. */
    suspend fun rename(id: Long, name: String) = dao.rename(id, name.trim())

    /**
     * Deletes the audio, its links with notes and its file.
     * First the database row (the links go with it thanks to "ON DELETE CASCADE"), then the
     * file, on the IO threads because it touches storage.
     */
    suspend fun delete(audio: Audio) {
        dao.delete(audio.id)
        withContext(Dispatchers.IO) { fileOf(audio).delete() }
    }

    // ---------- Links with notes ----------
    // These functions just pass the work to the DAO (see AudioDao.kt for what each one does).
    // Written as `fun name(...) = ...`: a one-line function whose result is that expression.

    // Links an audio to a note (does nothing if they are already linked).
    suspend fun link(noteId: Long, audioId: Long) = dao.link(NoteAudio(noteId, audioId))
    // Removes the link between a note and an audio (both stay).
    suspend fun unlink(noteId: Long, audioId: Long) = dao.unlink(noteId, audioId)
    // Live feed of the audios of one note.
    fun audiosOfNote(noteId: Long) = dao.audiosOfNote(noteId)
    // Live feed of all links with the notes' titles (for the audio list).
    fun noteLinks() = dao.noteLinks()
    // Live feed of how many audios each note has (for the note cards).
    fun audioCounts() = dao.audioCounts()
    // How many audios one note has, read once.
    suspend fun audioCount(noteId: Long) = dao.audioCount(noteId)
}
