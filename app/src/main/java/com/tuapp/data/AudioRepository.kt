package com.tuapp.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.tuapp.audio.AudioFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Audios and their files. Files are kept in the app's private storage
 * (`files/audios/`), so an imported audio survives if the original is deleted.
 */
class AudioRepository(private val dao: AudioDao, private val context: Context) {

    private val dir: File get() = File(context.filesDir, "audios").apply { mkdirs() }

    fun audios(query: String): Flow<List<Audio>> =
        if (query.isBlank()) dao.observeAll() else dao.search(query.trim())

    fun fileOf(audio: Audio) = File(dir, audio.fileName)

    /** A new, empty file for a recording. */
    fun newRecordingFile(): File = File(dir, "${UUID.randomUUID()}.m4a")

    suspend fun saveRecording(file: File, name: String, durationMs: Long): Long =
        dao.insert(Audio(name = name, fileName = file.name, durationMs = durationMs))

    /** Copies the audio at [uri] into the app and adds it. Returns its id, or null if it can't be read. */
    suspend fun import(uri: Uri): Long? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: "audio"
        val ext = AudioFormat.extension(displayName) ?: "audio"
        val target = File(dir, "${UUID.randomUUID()}.$ext")
        val copied = runCatching {
            resolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } }
        }.getOrNull()
        if (copied == null) { target.delete(); return@withContext null }
        val duration = durationOf(target) ?: run { target.delete(); return@withContext null }
        dao.insert(Audio(name = AudioFormat.nameFromFile(displayName), fileName = target.name, durationMs = duration))
    }

    /** Duration of an audio file, or null if Android can't read it as audio. */
    fun durationOf(file: File): Long? = runCatching {
        MediaMetadataRetriever().run {
            try {
                setDataSource(file.absolutePath)
                extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
            } finally {
                release()
            }
        }
    }.getOrNull()

    suspend fun rename(id: Long, name: String) = dao.rename(id, name.trim())

    /** Deletes the audio, its links with notes and its file. */
    suspend fun delete(audio: Audio) {
        dao.delete(audio.id)
        withContext(Dispatchers.IO) { fileOf(audio).delete() }
    }

    // ---------- Links with notes ----------

    suspend fun link(noteId: Long, audioId: Long) = dao.link(NoteAudio(noteId, audioId))
    suspend fun unlink(noteId: Long, audioId: Long) = dao.unlink(noteId, audioId)
    fun audiosOfNote(noteId: Long) = dao.audiosOfNote(noteId)
    fun noteLinks() = dao.noteLinks()
    fun audioCounts() = dao.audioCounts()
    suspend fun audioCount(noteId: Long) = dao.audioCount(noteId)
}
