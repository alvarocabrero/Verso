// ============================================================================================
// FILE: AudiosViewModel.kt
// --------------------------------------------------------------------------------------------
// What is it for?
//   It is the "brain" of the Audios section. The screen (AudiosScreen.kt) only draws things
//   and passes on what the user taps; this class decides what to do: search audios, play and
//   pause them, record new ones, import files, rename, delete, and link audios to notes.
//
// What is a ViewModel? (Android concept)
//   On Android, a screen can be destroyed and rebuilt at any moment (for example when you
//   rotate the phone). A `ViewModel` is an object that SURVIVES those rebuilds: it keeps the
//   data and the ongoing work of a screen. Think of the screen as a shop window that is redone
//   often, and the ViewModel as the shop's back room, which stays the same.
//
// What is it connected to?
//   - AudiosScreen.kt / AudioComponents.kt: the screen that shows this data and calls these
//     functions.
//   - data/AudioRepository and data/NotesRepository: they read and write the database and
//     the audio files (a "repository" is the single place that knows how data is stored).
//   - audio/AudioPlayer.kt (plays sound) and audio/AudioRecorder.kt (records sound).
//   - audio/AudioFormat.kt: gives recordings their default name.
//   - VersoApp.kt: holds the shared objects this ViewModel receives (see `Factory` below).
// ============================================================================================
package com.tuapp.ui.audios

// `Uri`: an Android "address" pointing to a piece of content (for example a file chosen by
// the user in the system's file picker).
import android.net.Uri
// `getValue` / `setValue`: let us write `var x by mutableStateOf(...)` (explained below).
import androidx.compose.runtime.getValue
// Compose "state" boxes for a Float and a Long number (explained below).
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
// Compose "state" box for any type of value.
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
// The base class every ViewModel extends.
import androidx.lifecycle.ViewModel
// A key used to get the Application object (VersoApp) when the ViewModel is created.
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
// `viewModelScope`: a coroutine scope tied to the ViewModel; its tasks are cancelled
// automatically when the ViewModel is thrown away.
import androidx.lifecycle.viewModelScope
// Helpers to describe how to build this ViewModel (see `Factory`).
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
// Our own app classes.
import com.tuapp.VersoApp
import com.tuapp.audio.AudioFormat
import com.tuapp.audio.AudioPlayer
import com.tuapp.audio.AudioRecorder
import com.tuapp.data.Audio
import com.tuapp.data.AudioNoteLink
import com.tuapp.data.AudioRepository
import com.tuapp.data.Note
import com.tuapp.data.NotesRepository
// Coroutine tools (a coroutine is a task that can wait without freezing the app).
import kotlinx.coroutines.CoroutineScope
// Markers that say "we accept using a feature that is still experimental".
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
// Flow tools. A Flow is a stream of values over time, like a news feed; a StateFlow is a Flow
// that always has a current value.
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
 *
 * Syntax:
 * - The list in parentheses after the class name is the constructor: the things you must give
 *   to create an `AudiosViewModel`. `private val` ones are kept as private properties; `val
 *   player` (without `private`) is kept as a public property, so the screen can read it too.
 * - `: ViewModel()` after the parentheses means "this class EXTENDS (inherits from)
 *   `ViewModel`": it gets all of ViewModel's behaviour and adds its own.
 *
 * @param repo access to audios in the database and on disk.
 * @param notesRepo access to notes (to link audios to notes).
 * @param player the app-wide audio player.
 * @param recorder the microphone recorder.
 * @param appScope an app-wide coroutine scope that lives as long as the app, used for work
 *   that must finish even if the user leaves the screen (saving, deleting).
 */
class AudiosViewModel(
    private val repo: AudioRepository,
    private val notesRepo: NotesRepository,
    val player: AudioPlayer,
    private val recorder: AudioRecorder,
    private val appScope: CoroutineScope
) : ViewModel() {

    // The text typed in the search field. Private changeable box + public read-only version,
    // so only this class can change it. It starts empty ("").
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /**
     * The list of audios to show, filtered by the search text. It is `null` while loading for
     * the first time (so the screen can tell "still loading" apart from "no audios").
     *
     * Syntax: `List<Audio>?` is "a list of Audio objects, or null".
     * `@OptIn(...)` accepts using `debounce` and `flatMapLatest`, which are marked
     * experimental.
     *
     * How the chain works, step by step (each line takes the previous stream and changes it):
     */
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val audios: StateFlow<List<Audio>?> = _query
        // 1. `debounce`: wait until the user stops typing for 200 ms before searching, so we
        //    do not search on every single key press. If the text is empty, do not wait (0).
        .debounce { if (it.isEmpty()) 0L else 200L }
        // 2. `flatMapLatest`: for each search text, ask the repository for the matching
        //    audios (that is itself a stream that updates when the database changes). If a new
        //    text arrives, forget the old search and follow only the latest one.
        .flatMapLatest { repo.audios(it) }
        // 3. `stateIn`: turn it into a StateFlow living in `viewModelScope`, starting with
        //    `null`. `WhileSubscribed(5_000)` keeps it running while the screen watches it,
        //    and for 5 more seconds after (so a quick screen rotation does not restart it).
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Notes each audio is linked to, grouped by audio id.
     *
     * Syntax: `Map<Long, List<AudioNoteLink>>` is a "map" (dictionary): for each key (the
     * audio id, a `Long`) it stores a value (a list of links to notes).
     */
    val links: StateFlow<Map<Long, List<AudioNoteLink>>> = repo.noteLinks()
        // `map` changes every value of the stream: here the flat list of links `l` is grouped
        // into a dictionary by audio id (`groupBy { it.audioId }`).
        .map { l -> l.groupBy { it.audioId } }
        // Starts as an empty dictionary.
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** All notes (search text "" = no filter), for the "link to notes" dialog. */
    val notes: StateFlow<List<Note>> = notesRepo.notes("")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Called by the screen every time the search text changes. It just stores the new text;
     * the `audios` chain above reacts to it automatically.
     */
    fun search(text: String) { _query.value = text }

    // ---------- Playback ----------

    /**
     * Plays or pauses [audio]. It asks the repository for the audio's file on disk and passes
     * it to the shared player.
     *
     * Syntax: `fun name(...) = expression` is a one-line function that returns the result of
     * the expression.
     */
    fun togglePlay(audio: Audio) = player.toggle(audio.id, repo.fileOf(audio))
    /** Jumps to position [ms] (milliseconds) in the audio that is playing. */
    fun seek(ms: Long) = player.seekTo(ms)

    // ---------- Editing ----------

    /**
     * Gives [audio] a new [name]. Blank names (empty or only spaces) are ignored.
     */
    fun rename(audio: Audio, name: String) {
        if (name.isBlank()) return
        // Writing to the database must not happen on the main thread, so it runs inside a
        // coroutine. `repo.rename` is a `suspend` function: it can only be called from a
        // coroutine, because it may need to wait.
        viewModelScope.launch { repo.rename(audio.id, name) }
    }

    /**
     * Deletes [audio] (its database row and its file).
     */
    fun delete(audio: Audio) {
        // First stop it, in case it is the one playing.
        player.stopIf(audio.id)
        // Uses `appScope` (not `viewModelScope`) so the deletion finishes even if the user
        // leaves the screen right away.
        appScope.launch { repo.delete(audio) }
    }

    /**
     * Sets which notes [audio] is linked to, so that it ends up linked to exactly [noteIds].
     *
     * @param noteIds the ids of the notes chosen in the dialog. `Set` is a collection with no
     *   repeated items and no order.
     */
    fun setLinks(audio: Audio, noteIds: Set<Long>) {
        // The ids of the notes linked right now: look up this audio in the dictionary
        // (`links.value[audio.id]`), use an empty list if it is not there (`orEmpty()`), take
        // each link's note id (`map { it.noteId }`) and make it a set (`toSet()`).
        val current = links.value[audio.id].orEmpty().map { it.noteId }.toSet()
        viewModelScope.launch {
            // Set subtraction: `noteIds - current` are the notes chosen now that were not
            // linked before -> link each one. `forEach { ... }` runs the block for each item.
            (noteIds - current).forEach { repo.link(it, audio.id) }
            // `current - noteIds` are notes that were linked but are no longer chosen ->
            // unlink them.
            (current - noteIds).forEach { repo.unlink(it, audio.id) }
        }
    }

    // ---------- Importing ----------

    /**
     * Message to show after an import fails (in Spanish); null when there is none.
     *
     * Compose concepts:
     * - `mutableStateOf(value)` creates a Compose STATE box. When its value changes, every
     *   `@Composable` function (piece of screen) that read it is redrawn automatically. This
     *   redrawing is called "recomposition".
     * - `by` is Kotlin "delegation": instead of writing `importError.value` every time, we
     *   write just `importError`, and the reads and writes are passed on ("delegated") to the
     *   state box.
     * - `<String?>` says the box holds "a text or null"; it starts as `null`.
     * - `private set`: the screen can read it, only this class can change it.
     */
    var importError by mutableStateOf<String?>(null); private set

    /**
     * Imports the audio file at [uri] (chosen by the user in the system's file picker) into
     * the app. If it fails, sets a Spanish error message ("That audio file could not be
     * added.") that the screen will show.
     */
    fun import(uri: Uri) {
        viewModelScope.launch {
            // `repo.import` returns null when it could not import the file.
            if (repo.import(uri) == null) importError = "No se ha podido añadir ese archivo de audio."
        }
    }

    /** Hides the import error message (called when the user closes it). */
    fun clearImportError() { importError = null }

    // ---------- Recording ----------

    // Compose state for the recording sheet (the screen redraws when they change):
    // - `recording`: true while a recording is in progress.
    var recording by mutableStateOf(false); private set
    // - `recordingPaused`: true while that recording is paused.
    var recordingPaused by mutableStateOf(false); private set
    // - `elapsedMs`: recorded time so far. `mutableLongStateOf` is the same idea as
    //   `mutableStateOf`, but optimised for a `Long` number.
    var elapsedMs by mutableLongStateOf(0L); private set
    // - `level`: microphone loudness from 0 to 1, for the level meter (optimised for `Float`).
    var level by mutableFloatStateOf(0f); private set

    // The file being recorded into, or null if not recording.
    private var recordingFile: File? = null
    // The repeating task that reads the time and level every 100 ms, or null.
    private var meter: Job? = null

    /**
     * Starts recording.
     *
     * @return true if recording started (or was already running); false if the microphone
     *   could not be opened.
     */
    fun startRecording(): Boolean {
        // Already recording: nothing to do.
        if (recording) return true
        // Stop any audio that is playing, so it is not recorded by the microphone.
        player.stop()
        // Ask the repository for a new empty file to record into.
        val file = repo.newRecordingFile()
        // Try to start the recorder; `isSuccess` is true if no error happened.
        val ok = runCatching { recorder.start(file) }.isSuccess
        // If it failed, delete the empty file and report failure.
        if (!ok) { file.delete(); return false }
        // Save the state: which file, recording, not paused.
        recordingFile = file
        recording = true
        recordingPaused = false
        // Start a coroutine that every 100 ms copies the recorded time and the loudness into
        // the state, so the screen's timer and meter move.
        meter = viewModelScope.launch {
            while (true) {
                elapsedMs = recorder.elapsedMs()
                level = recorder.level()
                delay(100)
            }
        }
        return true
    }

    /** Pauses the recording (only if recording and not already paused). */
    fun pauseRecording() {
        // `||` means "or": leave if not recording, or if already paused.
        if (!recording || recordingPaused) return
        recorder.pause()
        recordingPaused = true
    }

    /** Resumes a paused recording. */
    fun resumeRecording() {
        if (!recording || !recordingPaused) return
        recorder.resume()
        recordingPaused = false
    }

    /**
     * Stops and saves the recording with a default name ("Grabación 30 sep 21:22").
     */
    fun saveRecording() {
        // If there is no file, there is no recording: leave.
        val file = recordingFile ?: return
        stopRecorder()
        // Saving runs in the app-wide scope so it finishes even if the screen closes.
        appScope.launch {
            // Measure the real duration from the finished file.
            val real = repo.durationOf(file)
            // If it could not be measured, or it is empty, delete the file and stop here.
            // Syntax: `return@launch` leaves only this `launch` block (not the whole
            // function); the `@launch` "label" says which block to leave.
            if (real == null || real <= 0) { file.delete(); return@launch }
            // Save it in the database with a name built from the current date and time.
            repo.saveRecording(file, AudioFormat.recordingName(LocalDateTime.now()), real)
        }
    }

    /** Stops the recording and throws it away (deletes the file). */
    fun discardRecording() {
        val file = recordingFile ?: return
        stopRecorder()
        file.delete()
    }

    /**
     * Shared by save and discard: stops the meter task and the recorder, and resets all the
     * recording state.
     *
     * @return the recorded duration in milliseconds.
     */
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

    /**
     * Called by Android when this ViewModel is destroyed for good (the user has really left
     * the section, not just rotated the phone).
     *
     * Syntax: `override` means "replace the version of this function inherited from
     * `ViewModel` with this one".
     */
    override fun onCleared() {
        // Leaving the screen for good while recording: keep what was recorded.
        if (recording) saveRecording()
    }

    /**
     * Syntax: a `companion object` holds things that belong to the CLASS itself, not to each
     * object made from it (similar to "static" in other languages). You use it as
     * `AudiosViewModel.Factory`.
     */
    companion object {
        /**
         * The "factory": instructions that tell Android how to build an `AudiosViewModel`.
         * It is needed because this ViewModel receives things in its constructor, and
         * Android cannot guess them. The screen asks for the ViewModel with this factory.
         */
        val Factory = viewModelFactory {
            initializer {
                // Get the Application object and treat it as our `VersoApp` (`as` converts
                // the type), where the shared objects live.
                val app = this[APPLICATION_KEY] as VersoApp
                // Build the ViewModel with the shared repositories, player and scope, and a
                // new recorder (`AudioRecorder(app)`: the app works as the `Context`).
                AudiosViewModel(app.audioRepository, app.repository, app.audioPlayer, AudioRecorder(app), app.appScope)
            }
        }
    }
}
