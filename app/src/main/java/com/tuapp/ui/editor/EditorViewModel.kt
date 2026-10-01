// ===============================================================================================
// FILE: EditorViewModel.kt
// -----------------------------------------------------------------------------------------------
// The `package` line says which "logical folder" this file belongs to. Everything declared here
// has the full name `com.tuapp.ui.editor.Something` (it matches the folder `ui/editor/`).
// ===============================================================================================
package com.tuapp.ui.editor

// `import` lines bring in names from other libraries and other files, so they can be used here
// by their short name. Groups:
// - `androidx.compose.runtime.*`: Compose "state" tools (values that the screen watches).
// - `androidx.lifecycle.*`: the `ViewModel` class and its helpers (see the explanation below).
// - `com.tuapp.*`: this app's own classes (database, audio, analysis engine, settings).
// - `kotlinx.coroutines.*`: coroutines and Flows, Kotlin's tools for work that takes time
//   (explained below, before the class).
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

// ===============================================================================================
// WHAT THIS FILE IS FOR
// -----------------------------------------------------------------------------------------------
// This is the "brain" of the note editor screen. The screen itself (`EditorScreen.kt`) only
// draws things; this class keeps the data being edited and does the work:
//   - loads the note from the database when you open an existing one;
//   - keeps the title, the text, the type (poem or song), the colour and "pinned";
//   - SAVES AUTOMATICALLY: 600 ms after you stop typing, and again when you leave the screen;
//   - deletes a note that was left empty (like Google Keep does);
//   - re-runs the poem analysis (syllables, rhymes, literary devices) 300 ms after you stop
//     typing, in the background so the screen does not freeze;
//   - manages the audios linked to the note.
//
// HOW IT CONNECTS TO OTHER FILES
//   - `EditorScreen.kt`: the screen that uses this ViewModel (reads its values, calls its
//     functions when the person types or taps).
//   - `data/NotesRepository` and `data/AudioRepository`: the access points to the database
//     (reading, saving and deleting notes and audios).
//   - `data/Note`: the note as stored in the database. `data/Preferences`: the app settings.
//   - `analisis/AnalisisPoema`: the analysis engine (in Spanish), which returns syllables,
//     rhymes and devices for a text.
//   - `audio/AudioPlayer`: the app-wide audio player.
//   - `VersoApp.kt`: creates all the shared objects above and the long-lived `appScope`.
//
// KEY IDEAS, EXPLAINED ONCE
// * ViewModel: an Android class meant to hold a screen's data. Its big advantage: it SURVIVES
//   screen rotations (Android destroys and recreates the screen when the phone rotates, but the
//   ViewModel stays). It is destroyed only when the person really leaves the screen; at that
//   moment Android calls its `onCleared()` function.
// * Coroutine: a piece of work that can PAUSE and continue later without blocking the app. For
//   example "wait 600 ms, then save" or "read the note from the database". While a coroutine
//   waits, the phone keeps drawing the screen and reacting to taps. You start one with
//   `scope.launch { ... }`.
// * `suspend fun`: a function that may pause (for example, while the database answers). It can
//   only be called from inside a coroutine or from another `suspend` function.
// * Scope (`CoroutineScope`): the "owner" of coroutines. When the owner is cancelled, all its
//   coroutines are stopped. `viewModelScope` belongs to this ViewModel and is cancelled when the
//   ViewModel dies. `appScope` belongs to the whole app and lives as long as the app does.
// * Job: a handle to a running coroutine, which lets you cancel it (`job.cancel()`).
// * Dispatcher: which thread (which "worker") runs the code. The main thread draws the screen;
//   `Dispatchers.Default` is a pool of background workers for heavy calculations.
// * Flow: a stream of values that arrive over time, like a conveyor belt. You can transform it
//   (filter, wait, map) and "collect" it (do something with each value that arrives).
// * StateFlow: a Flow that always has a CURRENT value (`.value`) and tells listeners when it
//   changes. `MutableStateFlow` is one you can change yourself.
// * Compose state (`mutableStateOf`): a box holding a value that the screen watches. When the
//   value changes, Compose redraws ("recomposes") the parts of the screen that read it.
// * Mutex ("mutual exclusion"): a lock. Only one coroutine at a time can hold it; the others
//   wait their turn. Here it stops two saves from running at the same time.
// ===============================================================================================

/**
 * State of the note editor, with autosave:
 * - it saves 600 ms after the last change;
 * - it saves when you leave the screen (in the app scope `appScope`, which outlives the screen);
 * - a note left empty is thrown away, as in Google Keep.
 * The [Mutex] stops two saves running at the same time from inserting the same note twice.
 *
 * The analysis (syllables, rhymes, literary devices) is recomputed 300 ms after the last
 * change, off the main thread (so the screen never freezes while it runs).
 *
 * SYNTAX:
 * - `@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)`: an annotation saying "I know
 *   I am using features still marked as preview/experimental" (`debounce` and `flatMapLatest`).
 *   Without it the compiler would warn. `::class` means "the class itself" as a value.
 * - `class EditorViewModel(...)`: declares a class. The brackets right after the name are the
 *   "primary constructor": the things you must give to create one. Writing `private val` or
 *   `val` in front of a parameter also turns it into a property (a stored field) of the class.
 *   `private val` = only this class can use it; `val` alone = others can read it too (the screen
 *   reads `player` and `preferences`). `state` has no `val`, so it is only used while building.
 * - `: ViewModel()`: this class "inherits" from Android's `ViewModel` class, so it IS a
 *   ViewModel and gets all its behaviour (like `viewModelScope` and `onCleared`).
 *
 * @param repo access to notes in the database.
 * @param audioRepo access to audios and to the links between notes and audios.
 * @param player the app-wide audio player (shared, so only one audio plays at a time).
 * @param appScope the app-wide coroutine scope, used for saving when the screen closes.
 * @param preferences the app settings (seseo, show analysis, colour rhymes...).
 * @param state saved navigation arguments; here it holds the note `id` to open (0 = new note).
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

    // The note's id in the database. `private var` = a private value that CAN change (a new
    // note gets an id the first time it is saved). `: Long` is the type (a big whole number).
    // `state["id"]` reads the "id" argument from the navigation route "editor/{id}". It may be
    // missing (null), and `?: 0L` means "if it is null, use 0" (`L` marks a Long number).
    // An id of 0 means "a new note that is not in the database yet".
    private var id: Long = state["id"] ?: 0L
    // When the note was created, in milliseconds since 1970 (the usual computer clock).
    // For a new note it is "now"; for an existing one it is replaced when the note loads.
    private var created = System.currentTimeMillis()

    // --- Values the screen shows. Each one is Compose state. ---
    // `var title by mutableStateOf("")` means:
    // - `mutableStateOf("")` creates a Compose state box whose first value is "" (empty text).
    // - `by` is "delegation": reading or writing `title` actually reads or writes the value
    //   inside that box. So you write `title = "x"` instead of `title.value = "x"`. The imports
    //   `getValue` and `setValue` are what make `by` work with state boxes.
    // - Because it is Compose state, every screen part that reads `title` is redrawn when it
    //   changes.
    // - `; private set`: anyone may READ `title`, but only this class may CHANGE it. The screen
    //   must call `updateTitle(...)` instead, so that saving is always scheduled too. (The `;`
    //   lets two statements share a line.)
    // The note's title.
    var title by mutableStateOf(""); private set
    // The note's text (the verses).
    var content by mutableStateOf(""); private set
    // Poem or song. `NoteType.POEM` is one value of an "enum" (a fixed list of options).
    var type by mutableStateOf(NoteType.POEM); private set
    // Background colour: a position (index) in the palette `NotePalette`. 0 = default colour.
    // `mutableIntStateOf` is the same as `mutableStateOf` but optimised for whole numbers.
    var color by mutableIntStateOf(0); private set
    // Whether the note is pinned to the top of the list.
    var pinned by mutableStateOf(false); private set
    // `true` while an existing note is being loaded from the database. `id != 0L` is `true` for
    // an existing note (we must load it) and `false` for a new one (nothing to load).
    var loading by mutableStateOf(id != 0L); private set

    /**
     * The latest analysis result. It may lag behind the text while typing (it arrives 300 ms
     * after the last keystroke). `mutableStateOf<AnalisisPoema.Resultado?>(null)`: the part in
     * `< >` is a "generic type": it says what kind of value the box holds, here "an analysis
     * result, or null" (`?`). It starts as `null` (no analysis yet).
     */
    var analysis by mutableStateOf<AnalisisPoema.Resultado?>(null); private set
    // The literary device the person tapped in the panel (to highlight it in the text), or null
    // if none is selected.
    var selectedDevice by mutableStateOf<Recursos.Recurso?>(null); private set

    // --- Internal bookkeeping (not shown on screen, so plain variables, not Compose state). ---
    // `true` when something changed since the last save.
    private var hasChanges = false
    // `true` once the person deleted the note: from then on, nothing must save it again.
    private var deleted = false
    // The coroutine waiting its 600 ms before saving, or null if none. `Job?` = "a Job or null".
    // It is kept so it can be cancelled when another change comes in (the timer restarts).
    private var pendingSave: Job? = null
    // The lock that makes saves take turns (see "Mutex" at the top of the file).
    private val mutex = Mutex()

    // ---------- Linked audios ----------

    /**
     * The note's id as a flow. It changes from 0 when a new note is saved for the first time,
     * and anything listening (like [noteAudios]) finds out right away.
     * `MutableStateFlow(id)` creates a StateFlow whose first value is the current `id`.
     */
    private val noteId = MutableStateFlow(id)

    /**
     * The audios linked to this note, kept always up to date.
     *
     * Step by step:
     * - `noteId.flatMapLatest { ... }`: every time the note's id changes, switch to a new
     *   stream built by the block, and forget the previous one ("latest" = only the newest
     *   matters). Inside the block, `it` is the id.
     * - `if (it == 0L) flowOf(emptyList()) else audioRepo.audiosOfNote(it)`: a new note has no
     *   audios (`flowOf(emptyList())` is a stream with a single empty list); otherwise, ask the
     *   database for a live stream of this note's audios (it emits again whenever they change).
     * - `.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())`: turn the stream into a
     *   StateFlow that always has a current value. It runs inside `viewModelScope`, starts right
     *   away (`Eagerly`) and begins with an empty list. It starts eagerly because `isEmpty()`
     *   below reads `noteAudios.value` even when no screen is watching.
     * - `StateFlow<List<Audio>>` is the type: a StateFlow holding a list of `Audio`.
     */
    val noteAudios: StateFlow<List<Audio>> = noteId
        .flatMapLatest { if (it == 0L) flowOf(emptyList()) else audioRepo.audiosOfNote(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Every audio in the app, for the "link audios" dialog. `audioRepo.audios("")` is the list
     * of audios filtered by an empty search (so, all of them). `WhileSubscribed(5_000)` means:
     * only keep listening to the database while someone on screen is watching, and stop 5
     * seconds (5,000 ms; the `_` is only there to make the number easier to read) after the
     * last watcher leaves. This saves work when the dialog is closed.
     */
    val allAudios: StateFlow<List<Audio>> = audioRepo.audios("")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Play or pause an audio. `fun name(...) = expression` is a short function whose body is
    // just one expression (its result is returned). It passes the audio's id and its file on
    // disk (`audioRepo.fileOf(audio)`) to the shared player.
    fun togglePlay(audio: Audio) = player.toggle(audio.id, audioRepo.fileOf(audio))
    // Jump to a position (in milliseconds) in the audio that is playing.
    fun seek(ms: Long) = player.seekTo(ms)

    /**
     * Links exactly [audioIds] to this note: afterwards the note has those audios and no others.
     * A new note is saved first, so that it has an id to link to.
     *
     * `Set<Long>` is a set (a collection with no repeated items, order does not matter) of ids.
     */
    fun setAudioLinks(audioIds: Set<Long>) {
        // The ids of the audios linked right now. `.map { it.id }` turns the list of audios into
        // a list of their ids; `.toSet()` turns that list into a set.
        val current = noteAudios.value.map { it.id }.toSet()
        // Start a coroutine (database work takes time, so it cannot block the screen).
        viewModelScope.launch {
            // Make sure the note exists in the database and get its id (saving it if new).
            val nid = ensureSaved()
            // `audioIds - current` = the ids that are wanted but not linked yet: link each.
            // `.forEach { ... }` runs the block once for every item, with `it` as the item.
            (audioIds - current).forEach { audioRepo.link(nid, it) }
            // `current - audioIds` = the ids that are linked but no longer wanted: unlink each.
            (current - audioIds).forEach { audioRepo.unlink(nid, it) }
        }
    }

    /** Removes the link between this note and [audio] (the audio file itself is not deleted). */
    fun unlinkAudio(audio: Audio) {
        // A new, unsaved note (id 0) cannot have links, so there is nothing to do.
        if (id == 0L) return
        viewModelScope.launch { audioRepo.unlink(id, audio.id) }
    }

    /**
     * Makes sure the note is in the database and returns its id. If the note is new (id 0), it
     * is saved now; otherwise nothing is done.
     *
     * - `private suspend fun ensureSaved(): Long`: private, may pause (`suspend`), returns a Long.
     * - `= mutex.withLock { ... }`: the whole body runs while holding the lock, so it cannot
     *   overlap with an autosave. The value of the last line of the block (`id`) is returned.
     */
    private suspend fun ensureSaved(): Long = mutex.withLock {
        if (id == 0L) {
            // `withContext(NonCancellable) { ... }`: run this block so that it CANNOT be
            // cancelled half-way. If the screen closed while inserting, the new id must not be
            // lost, or a later save would insert the same note a second time.
            // `repo.save(note())` inserts the note and returns its new id.
            withContext(NonCancellable) { id = repo.save(note()) }
            // Tell the id flow, so `noteAudios` starts following the new id.
            noteId.value = id
            // Everything is saved now.
            hasChanges = false
        }
        id
    }

    // `init { ... }` is code that runs once, right when the ViewModel is created (after the
    // properties above get their first values).
    init {
        // 1) For an existing note, load it from the database in a coroutine.
        if (id != 0L) viewModelScope.launch {
            // `repo.get(id)` returns the note, or null if it does not exist (for example, it was
            // deleted). `?.let { ... }` runs the block only if it is not null; inside, `it` is
            // the loaded note. The `;` separates several statements on one line.
            repo.get(id)?.let {
                title = it.title; content = it.content; type = it.type
                color = it.color; pinned = it.pinned; created = it.created
            }
            // Loading is finished: the screen can show the fields now.
            loading = false
        }
        // 2) Keep the analysis in step with the text, in a coroutine that lives as long as the
        //    ViewModel.
        viewModelScope.launch {
            // `snapshotFlow { ... }` turns Compose state into a Flow: every time any state read
            // inside the block changes, the block runs again and its new result is sent down the
            // stream. Here it reads the text, the seseo setting and the "show analysis" setting,
            // and bundles them together with `Triple(a, b, c)` (a group of three values;
            // `.first`, `.second`, `.third` read them).
            // (*Seseo*: pronouncing s, z and soft c the same, as in Latin America and parts of
            // Andalusia; it changes which words rhyme.)
            snapshotFlow { Triple(content, preferences.seseo, preferences.showAnalysis) }
                // Only go on while the analysis is switched on (`it.third` = showAnalysis).
                .filter { it.third }
                // `debounce(300)`: wait until no new value has arrived for 300 ms, then pass on
                // only the last one. So while you type fast, nothing is analysed; 300 ms after
                // the last keystroke, the analysis runs once.
                .debounce(300)
                // `mapLatest { ... }`: turn each value into an analysis result. "Latest" means
                // that if a newer value arrives while the previous analysis is still running,
                // the old one is cancelled and only the newest is computed.
                // `(text, seseo)` "destructures" the Triple: it takes its first two values and
                // names them `text` and `seseo` (the third is not needed).
                .mapLatest { (text, seseo) ->
                    // Run the engine on a background worker (`Dispatchers.Default`), because
                    // analysing a long poem could take long enough to freeze the screen.
                    withContext(Dispatchers.Default) { AnalisisPoema.analizar(text, seseo) }
                }
                // `collect { r -> ... }`: for every result `r` that comes out of the stream...
                .collect { r ->
                    // ...store it (the screen redraws because `analysis` is Compose state)...
                    analysis = r
                    // ...and if the selected device no longer exists in the new result (for
                    // example, the verses that formed it were edited), clear the selection.
                    // `!in` means "is not contained in".
                    if (selectedDevice !in r.recursos) selectedDevice = null
                }
        }
    }

    // --- Functions the screen calls when the person edits something. ---
    // Each one changes the value and schedules an autosave. `{ a; b }` on one line: the `;`
    // separates two statements.
    // New title typed.
    fun updateTitle(t: String) { title = t; scheduleSave() }
    // New text typed.
    fun updateContent(t: String) { content = t; scheduleSave() }
    // Poem / song chosen.
    fun updateType(t: NoteType) { type = t; scheduleSave() }
    // Colour chosen (index into the palette).
    fun updateColor(c: Int) { color = c; scheduleSave() }
    // Pin / unpin. `!pinned` means "the opposite of pinned" (true becomes false and back).
    fun togglePinned() { pinned = !pinned; scheduleSave() }

    /**
     * Tapping a device highlights it; tapping the same one again clears the selection.
     * `if (...) a else b` is used as a value: null if it was already selected, otherwise [d].
     */
    fun selectDevice(d: Recursos.Recurso) {
        selectedDevice = if (selectedDevice == d) null else d
    }

    /**
     * Deletes the note (the trash-can button). The screen closes right after this, so the
     * deletion runs in `appScope`, which is not cancelled when the screen goes away.
     */
    fun delete() {
        // Remember that it was deleted, so no later save brings it back.
        deleted = true
        // Cancel any save that was waiting its 600 ms. `?.` = only if there is one.
        pendingSave?.cancel()
        // Take the lock (wait for a save in progress to finish) and delete it from the database,
        // but only if it was ever saved (id not 0).
        appScope.launch { mutex.withLock { if (id != 0L) repo.delete(id) } }
    }

    /**
     * Schedules an autosave 600 ms from now. If it is called again before then, the previous
     * timer is cancelled and a new one starts, so the note is saved once, 600 ms after the LAST
     * change (not after every keystroke).
     */
    private fun scheduleSave() {
        hasChanges = true
        // Cancel the previous timer, if any.
        pendingSave?.cancel()
        // Start a new coroutine and keep its Job so it can be cancelled later.
        pendingSave = viewModelScope.launch {
            // Pause this coroutine for 600 ms (the app keeps running meanwhile).
            delay(600)
            save()
        }
    }

    /**
     * Nothing written and no audios: such a note is thrown away, as in Google Keep.
     * `isBlank()` is true for text that is empty or only spaces/line breaks. `&&` means "and".
     */
    private fun isEmpty() = title.isBlank() && content.isBlank() && noteAudios.value.isEmpty()

    /**
     * Builds a `Note` object (the database row) from the current values. The "modified" date is
     * set to right now. `Note(id = id, ...)` uses named arguments: `name = value`.
     */
    private fun note() = Note(
        id = id, title = title, content = content, type = type,
        color = color, pinned = pinned, created = created, modified = System.currentTimeMillis()
    )

    /**
     * Saves the note if there is something to save. Runs while holding the lock, so two saves
     * never overlap (otherwise a new note could be inserted twice, with two different ids).
     */
    private suspend fun save() = mutex.withLock {
        // Nothing changed, or the note was deleted, or it is empty: stop here.
        // `||` means "or". `return@withLock` leaves only the `withLock { }` block (a "labelled
        // return": it says WHICH block to leave).
        if (!hasChanges || deleted || isEmpty()) return@withLock
        // NonCancellable: if the note gets inserted, the new id must not be lost (even if this
        // coroutine is cancelled because a newer change arrived or the screen closed).
        withContext(NonCancellable) { id = repo.save(note()) }
        // Publish the (possibly new) id to the flow that follows the note's audios.
        noteId.value = id
        hasChanges = false
    }

    /**
     * Called by Android when the ViewModel is destroyed for good (the person left the editor).
     * `override` means we replace the version of this function that `ViewModel` already has.
     *
     * `viewModelScope` is already cancelled at this point, so the final save runs in `appScope`
     * (the app-wide scope), which keeps living after the screen is gone.
     */
    override fun onCleared() {
        // The 600 ms timer is no longer needed: we save (or delete) right now instead.
        pendingSave?.cancel()
        // A deleted note must not be saved again.
        if (deleted) return
        appScope.launch {
            mutex.withLock {
                // `when { condition -> action ... }` checks the conditions in order and runs the
                // first one that is true (like a chain of "if / else if").
                when {
                    // Left empty but already in the database: delete it (as Google Keep does).
                    isEmpty() && id != 0L -> repo.delete(id)
                    // Has content and unsaved changes: save them.
                    !isEmpty() && hasChanges -> id = repo.save(note())
                }
            }
        }
    }

    /**
     * `companion object`: things that belong to the CLASS itself, not to one particular
     * ViewModel object. You use them as `EditorViewModel.Factory`, without having a ViewModel.
     */
    companion object {
        /**
         * The "factory" that tells Android how to build an `EditorViewModel`. Android creates
         * ViewModels itself (so it can keep them through rotations), but it does not know which
         * repositories and settings to pass in. This recipe fills that gap. The app has no
         * dependency-injection library, so this is done by hand.
         *
         * - `viewModelFactory { initializer { ... } }`: helpers that build a factory from a
         *   block of code; the last line of the block is the new ViewModel.
         * - `this[APPLICATION_KEY]` gets the running Application object. `as VersoApp` says
         *   "treat it as our own `VersoApp` class" (a "cast"), which holds the shared objects.
         * - `createSavedStateHandle()` gives the saved navigation arguments (with the note id).
         */
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
