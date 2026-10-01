// ============================================================================================
// FILE: NotesViewModel.kt
// --------------------------------------------------------------------------------------------
// What is it for?
//   It is the "brain" of the Notes section: it keeps the search text, gives the screen the
//   list of notes matching it, and counts how many audios each note has (to show a small
//   headphones counter on each card).
//
// What is a ViewModel? (Android concept)
//   On Android a screen can be destroyed and rebuilt at any time (for example when the phone
//   is rotated). A `ViewModel` is an object that SURVIVES those rebuilds and keeps the
//   screen's data. Think of the screen as a shop window that is redone often, and the
//   ViewModel as the back room, which stays the same.
//
// What is it connected to?
//   - ui/notes/NotesScreen.kt: the screen that shows this data.
//   - data/NotesRepository: reads notes from the database.
//   - data/AudioRepository: counts the audios linked to each note.
//   - VersoApp.kt: holds the repositories passed in by `Factory`.
//
// Flow (Kotlin): a `Flow` is a stream of values over time, like a news feed. A `StateFlow`
// is a Flow that always has a "current value" and tells its listeners when it changes. The
// screen listens to these StateFlows and redraws itself on every change.
// ============================================================================================
package com.tuapp.ui.notes

// The base class of every ViewModel.
import androidx.lifecycle.ViewModel
// Key used to get the Application object (VersoApp) when the ViewModel is created.
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
// A coroutine scope tied to this ViewModel: its work is cancelled when the ViewModel goes away.
import androidx.lifecycle.viewModelScope
// Helpers to describe how to build this ViewModel (see `Factory`).
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
// Our own classes.
import com.tuapp.VersoApp
import com.tuapp.data.Note
import com.tuapp.data.AudioRepository
import com.tuapp.data.NotesRepository
// Markers that accept using features still labelled "experimental".
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
// Flow tools (explained where they are used).
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel of the Notes section.
 *
 * Syntax: the parentheses after the class name are the constructor (what you must give to
 * create it). `private val repo` is kept as a private property; `audioRepo` (with no `val`)
 * is only used while creating the object, to set up `audioCounts` below. `: ViewModel()`
 * means this class extends (inherits from) `ViewModel`.
 *
 * @param repo access to the notes.
 * @param audioRepo access to the audios (only to count them per note).
 */
class NotesViewModel(private val repo: NotesRepository, audioRepo: AudioRepository) : ViewModel() {

    // The search text. A private changeable box (`_query`) and a public read-only view of it
    // (`query`), so only this class can change it. `val` = cannot be reassigned. It starts as
    // "" (empty: no search).
    private val _query = MutableStateFlow("")
    // `StateFlow<String>`: the `< >` say what the box holds (a text). This is a "generic" type.
    val query: StateFlow<String> = _query.asStateFlow()

    /**
     * The notes to show, filtered by the search text. It is `null` while loading for the first
     * time (so the screen can tell "still loading" apart from "no notes").
     * `List<Note>?` means "a list of notes, or null".
     */
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val notes: StateFlow<List<Note>?> = _query
        // Wait until the user stops typing for 200 ms before searching (no wait if the text
        // is empty). `it` is the automatic name for the lambda's single parameter (the text).
        // `0L`/`200L` are `Long` numbers (the `L` marks the type).
        .debounce { if (it.isEmpty()) 0L else 200L }
        // For each text, follow the repository's live list of matching notes; when a new text
        // arrives, drop the previous search and follow only the latest one.
        .flatMapLatest { repo.notes(it) }
        // Keep it as a StateFlow in `viewModelScope`, starting with null. It keeps working
        // while someone watches it, plus 5 seconds (so a quick rotation does not restart it).
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Number of linked audios per note id, for the cards.
     * `Map<Long, Int>` is a dictionary: note id (`Long`) -> number of audios (`Int`).
     */
    val audioCounts: StateFlow<Map<Long, Int>> = audioRepo.audioCounts()
        // Turn the list of (note id, count) pairs into a dictionary. `associate { a to b }`
        // builds one entry per item, where `to` joins a key and its value into a pair.
        .map { l -> l.associate { it.noteId to it.count } }
        // Starts as an empty dictionary.
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /**
     * Called by the screen when the search text changes. It only stores the text; the `notes`
     * chain above reacts automatically.
     */
    fun search(text: String) { _query.value = text }

    /**
     * Syntax: a `companion object` holds things that belong to the class itself, not to each
     * object (like "static" in other languages). Used as `NotesViewModel.Factory`.
     */
    companion object {
        /**
         * The "factory": tells Android how to build a `NotesViewModel`. It is needed because
         * the constructor receives repositories that Android cannot guess.
         */
        val Factory = viewModelFactory {
            initializer {
                // Get the app object and treat it as `VersoApp` (`as` changes the type), then
                // build the ViewModel with its repositories.
                val app = this[APPLICATION_KEY] as VersoApp
                NotesViewModel(app.repository, app.audioRepository)
            }
        }
    }
}
