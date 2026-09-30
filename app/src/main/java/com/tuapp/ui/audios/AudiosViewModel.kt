package com.tuapp.ui.audios

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tuapp.VersoApp
import com.tuapp.audio.AudioFormat
import com.tuapp.audio.AudioPlayer
import com.tuapp.audio.AudioRecorder
import com.tuapp.data.Audio
import com.tuapp.data.AudioNoteLink
import com.tuapp.data.AudioRepository
import com.tuapp.data.Note
import com.tuapp.data.NotesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime

/**
 * The audio section: list and search, playback, recording, importing, renaming,
 * deleting and linking audios to notes.
 */
class AudiosViewModel(
    private val repo: AudioRepository,
    private val notesRepo: NotesRepository,
    val player: AudioPlayer,
    private val recorder: AudioRecorder,
    private val appScope: CoroutineScope
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** null while loading for the first time. */
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val audios: StateFlow<List<Audio>?> = _query
        .debounce { if (it.isEmpty()) 0L else 200L }
        .flatMapLatest { repo.audios(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Notes each audio is linked to, by audio id. */
    val links: StateFlow<Map<Long, List<AudioNoteLink>>> = repo.noteLinks()
        .map { l -> l.groupBy { it.audioId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** All notes, for the link dialog. */
    val notes: StateFlow<List<Note>> = notesRepo.notes("")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun search(text: String) { _query.value = text }

    // ---------- Playback ----------

    fun togglePlay(audio: Audio) = player.toggle(audio.id, repo.fileOf(audio))
    fun seek(ms: Long) = player.seekTo(ms)

    // ---------- Editing ----------

    fun rename(audio: Audio, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repo.rename(audio.id, name) }
    }

    fun delete(audio: Audio) {
        player.stopIf(audio.id)
        appScope.launch { repo.delete(audio) }
    }

    fun setLinks(audio: Audio, noteIds: Set<Long>) {
        val current = links.value[audio.id].orEmpty().map { it.noteId }.toSet()
        viewModelScope.launch {
            (noteIds - current).forEach { repo.link(it, audio.id) }
            (current - noteIds).forEach { repo.unlink(it, audio.id) }
        }
    }

    // ---------- Importing ----------

    /** Message to show after an import fails (Spanish); null when there is none. */
    var importError by mutableStateOf<String?>(null); private set

    fun import(uri: Uri) {
        viewModelScope.launch {
            if (repo.import(uri) == null) importError = "No se ha podido añadir ese archivo de audio."
        }
    }

    fun clearImportError() { importError = null }

    // ---------- Recording ----------

    var recording by mutableStateOf(false); private set
    var recordingPaused by mutableStateOf(false); private set
    var elapsedMs by mutableLongStateOf(0L); private set
    var level by mutableFloatStateOf(0f); private set

    private var recordingFile: File? = null
    private var meter: Job? = null

    /** Starts recording. Returns false if the microphone couldn't be opened. */
    fun startRecording(): Boolean {
        if (recording) return true
        player.stop()
        val file = repo.newRecordingFile()
        val ok = runCatching { recorder.start(file) }.isSuccess
        if (!ok) { file.delete(); return false }
        recordingFile = file
        recording = true
        recordingPaused = false
        meter = viewModelScope.launch {
            while (true) {
                elapsedMs = recorder.elapsedMs()
                level = recorder.level()
                delay(100)
            }
        }
        return true
    }

    fun pauseRecording() {
        if (!recording || recordingPaused) return
        recorder.pause()
        recordingPaused = true
    }

    fun resumeRecording() {
        if (!recording || !recordingPaused) return
        recorder.resume()
        recordingPaused = false
    }

    /** Stops and saves the recording with a default name ("Grabación 30 sep 21:22"). */
    fun saveRecording() {
        val file = recordingFile ?: return
        stopRecorder()
        appScope.launch {
            val real = repo.durationOf(file)
            if (real == null || real <= 0) { file.delete(); return@launch }
            repo.saveRecording(file, AudioFormat.recordingName(LocalDateTime.now()), real)
        }
    }

    fun discardRecording() {
        val file = recordingFile ?: return
        stopRecorder()
        file.delete()
    }

    private fun stopRecorder(): Long {
        meter?.cancel()
        val duration = recorder.stop()
        recording = false
        recordingPaused = false
        recordingFile = null
        elapsedMs = 0
        level = 0f
        return duration
    }

    override fun onCleared() {
        // Leaving the screen for good while recording: keep what was recorded
        if (recording) saveRecording()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as VersoApp
                AudiosViewModel(app.audioRepository, app.repository, app.audioPlayer, AudioRecorder(app), app.appScope)
            }
        }
    }
}
