package com.tuapp.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tuapp.VersoApp
import com.tuapp.analisis.AnalisisPoema
import com.tuapp.analisis.Recursos
import com.tuapp.audio.AudioPlayer
import com.tuapp.data.Audio
import com.tuapp.data.AudioRepository
import com.tuapp.data.Note
import com.tuapp.data.NoteType
import com.tuapp.data.NotesRepository
import com.tuapp.data.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Editor state with autosave:
 * - saves 600 ms after the last change;
 * - saves when leaving (in the app scope, which outlives the screen);
 * - a note left empty is discarded, as in Keep.
 * The Mutex stops two simultaneous saves from inserting the same note twice.
 *
 * The analysis (syllables, rhymes, literary devices) is recomputed 300 ms
 * after the last change, off the main thread.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class EditorViewModel(
    private val repo: NotesRepository,
    private val audioRepo: AudioRepository,
    val player: AudioPlayer,
    private val appScope: CoroutineScope,
    val preferences: Preferences,
    state: SavedStateHandle
) : ViewModel() {

    private var id: Long = state["id"] ?: 0L
    private var created = System.currentTimeMillis()

    var title by mutableStateOf(""); private set
    var content by mutableStateOf(""); private set
    var type by mutableStateOf(NoteType.POEM); private set
    var color by mutableIntStateOf(0); private set
    var pinned by mutableStateOf(false); private set
    var loading by mutableStateOf(id != 0L); private set

    /** Latest analysis; it may lag behind the text while typing. */
    var analysis by mutableStateOf<AnalisisPoema.Resultado?>(null); private set
    var selectedDevice by mutableStateOf<Recursos.Recurso?>(null); private set

    private var hasChanges = false
    private var deleted = false
    private var pendingSave: Job? = null
    private val mutex = Mutex()

    // ---------- Linked audios ----------

    /** The note's id as a flow: it changes from 0 when a new note is first saved. */
    private val noteId = MutableStateFlow(id)

    /** Audios linked to this note. */
    val noteAudios: StateFlow<List<Audio>> = noteId
        .flatMapLatest { if (it == 0L) flowOf(emptyList()) else audioRepo.audiosOfNote(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Every audio, for the link dialog. */
    val allAudios: StateFlow<List<Audio>> = audioRepo.audios("")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun togglePlay(audio: Audio) = player.toggle(audio.id, audioRepo.fileOf(audio))
    fun seek(ms: Long) = player.seekTo(ms)

    /** Links exactly [audioIds] to this note. A new note is saved first so it has an id. */
    fun setAudioLinks(audioIds: Set<Long>) {
        val current = noteAudios.value.map { it.id }.toSet()
        viewModelScope.launch {
            val nid = ensureSaved()
            (audioIds - current).forEach { audioRepo.link(nid, it) }
            (current - audioIds).forEach { audioRepo.unlink(nid, it) }
        }
    }

    fun unlinkAudio(audio: Audio) {
        if (id == 0L) return
        viewModelScope.launch { audioRepo.unlink(id, audio.id) }
    }

    private suspend fun ensureSaved(): Long = mutex.withLock {
        if (id == 0L) {
            withContext(NonCancellable) { id = repo.save(note()) }
            noteId.value = id
            hasChanges = false
        }
        id
    }

    init {
        if (id != 0L) viewModelScope.launch {
            repo.get(id)?.let {
                title = it.title; content = it.content; type = it.type
                color = it.color; pinned = it.pinned; created = it.created
            }
            loading = false
        }
        viewModelScope.launch {
            snapshotFlow { Triple(content, preferences.seseo, preferences.showAnalysis) }
                .filter { it.third }
                .debounce(300)
                .mapLatest { (text, seseo) ->
                    withContext(Dispatchers.Default) { AnalisisPoema.analizar(text, seseo) }
                }
                .collect { r ->
                    analysis = r
                    if (selectedDevice !in r.recursos) selectedDevice = null
                }
        }
    }

    fun updateTitle(t: String) { title = t; scheduleSave() }
    fun updateContent(t: String) { content = t; scheduleSave() }
    fun updateType(t: NoteType) { type = t; scheduleSave() }
    fun updateColor(c: Int) { color = c; scheduleSave() }
    fun togglePinned() { pinned = !pinned; scheduleSave() }

    /** Tapping a device highlights it; tapping it again clears the selection. */
    fun selectDevice(d: Recursos.Recurso) {
        selectedDevice = if (selectedDevice == d) null else d
    }

    fun delete() {
        deleted = true
        pendingSave?.cancel()
        appScope.launch { mutex.withLock { if (id != 0L) repo.delete(id) } }
    }

    private fun scheduleSave() {
        hasChanges = true
        pendingSave?.cancel()
        pendingSave = viewModelScope.launch {
            delay(600)
            save()
        }
    }

    /** Nothing written and no audios: such a note is discarded, as in Keep. */
    private fun isEmpty() = title.isBlank() && content.isBlank() && noteAudios.value.isEmpty()

    private fun note() = Note(
        id = id, title = title, content = content, type = type,
        color = color, pinned = pinned, created = created, modified = System.currentTimeMillis()
    )

    private suspend fun save() = mutex.withLock {
        if (!hasChanges || deleted || isEmpty()) return@withLock
        // NonCancellable: if the note gets inserted, the new id must not be lost
        withContext(NonCancellable) { id = repo.save(note()) }
        noteId.value = id
        hasChanges = false
    }

    override fun onCleared() {
        pendingSave?.cancel()
        if (deleted) return
        appScope.launch {
            mutex.withLock {
                when {
                    isEmpty() && id != 0L -> repo.delete(id)
                    !isEmpty() && hasChanges -> id = repo.save(note())
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as VersoApp
                EditorViewModel(
                    app.repository, app.audioRepository, app.audioPlayer,
                    app.appScope, app.preferences, createSavedStateHandle()
                )
            }
        }
    }
}
