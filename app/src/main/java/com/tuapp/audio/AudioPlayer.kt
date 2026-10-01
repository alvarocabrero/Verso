// ============================================================================================
// FILE: AudioPlayer.kt
// --------------------------------------------------------------------------------------------
// What is it for?
//   It is the audio PLAYER for the whole app. It can play, pause, jump to a point and stop an
//   audio, and it always publishes "what is playing and at which second" so the screens can
//   show it (play/pause button, progress bar...).
//
// What role does it play in the app?
//   There is only ONE player for the whole app (it is created once in com.tuapp.VersoApp).
//   This way two audios never sound at the same time: if you start another one, the previous
//   one stops, whether you started it from the audio list or from a note.
//
// What is it connected to?
//   - VersoApp.kt: creates it and keeps it.
//   - ui/audios/AudiosViewModel.kt and the note editor: ask it to play/pause and read its
//     state (`state`).
//
// MediaPlayer (Android):
//   It is the tool Android provides to play sound or video. Its typical life cycle:
//     1. Create it: `MediaPlayer()`.
//     2. Tell it which file to play: `setDataSource(path)`.
//     3. Prepare it: `prepare()` (reads the file header, finds out the duration, etc.).
//     4. `start()` to play, `pause()` to pause, `seekTo(ms)` to jump to a point.
//     5. `release()` when done: frees the memory and the access to the sound hardware. It is
//        VERY important to release it; otherwise system resources are "leaked" (lost).
//
// Coroutines and Flow (Kotlin):
//   - A COROUTINE is a task that can "wait" without freezing the app (for example "wait
//     200 ms and look again"). It is like setting a kitchen timer while you keep doing other
//     things.
//   - A `StateFlow` is a "box holding a value that tells you when it changes". Screens
//     subscribe to it and redraw themselves every time the value changes.
// ============================================================================================
package com.tuapp.audio

// The Android class that plays audio (explained above).
import android.media.MediaPlayer
// `CoroutineScope`: a "scope" or container where coroutines are launched; if the scope is
// cancelled, all its tasks are cancelled too.
import kotlinx.coroutines.CoroutineScope
// `Dispatchers`: decides on which "thread" (line of execution) a coroutine runs.
import kotlinx.coroutines.Dispatchers
// `Job`: the "receipt" of a launched coroutine; used to cancel it later.
import kotlinx.coroutines.Job
// `SupervisorJob`: a kind of Job where, if one task fails, the others are not cancelled.
import kotlinx.coroutines.SupervisorJob
// `delay`: waits some time without blocking (it can only be used inside a coroutine).
import kotlinx.coroutines.delay
// `MutableStateFlow`: the changeable version of the "box that notifies".
import kotlinx.coroutines.flow.MutableStateFlow
// `StateFlow`: the read-only version of that box.
import kotlinx.coroutines.flow.StateFlow
// `asStateFlow()`: turns the changeable box into a read-only one.
import kotlinx.coroutines.flow.asStateFlow
// `launch`: starts a new coroutine.
import kotlinx.coroutines.launch
// `File`: represents a file on disk.
import java.io.File

/**
 * Describes what is playing right now. If [audioId] is `null`, nothing is loaded.
 *
 * Kotlin syntax:
 * - `data class` is a class meant only to hold data. Kotlin adds for free: comparison (`==`
 *   compares the content), a debug text, and the `copy(...)` method, which makes a copy
 *   changing only some fields.
 * - Each `val name: Type = value` inside the parentheses is a field (property) filled in when
 *   the object is created; the `= value` part is its default value.
 * - `Long?` means "a long whole number, or `null`".
 */
data class PlaybackState(
    // Identifier (unique number in the database) of the loaded audio, or null if none.
    val audioId: Long? = null,
    // true if it is sounding right now; false if it is paused or stopped.
    val playing: Boolean = false,
    // Where playback is, in milliseconds.
    val positionMs: Long = 0,
    // Total length of the loaded audio, in milliseconds.
    val durationMs: Long = 0
)

/**
 * App-wide player: only one audio sounds at a time, whether it was started from the audio
 * list or from a note. It lives (is created and kept) in [com.tuapp.VersoApp].
 *
 * Syntax: `class AudioPlayer { ... }` declares a class, that is, a "mould" to create objects
 * that have data (properties) and actions (functions). This one receives nothing when created.
 */
class AudioPlayer {

    // The player's own scope for its coroutines.
    // `SupervisorJob()` stops a failure in one task from killing the others.
    // `Dispatchers.Main.immediate` makes them run on the main thread (the user-interface
    // thread), which is where the MediaPlayer and the state read by the screen should be
    // touched. The `+` combines both settings.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    // The Android MediaPlayer in use, or null if there is none.
    // Syntax: `var` declares a variable that CAN be changed later (unlike `val`).
    // `MediaPlayer?` allows null. `private` = only visible inside this class.
    private var player: MediaPlayer? = null
    // The repeating task that updates the position (the "tick-tock"), or null if none.
    private var ticker: Job? = null

    // A common Kotlin pattern: a private changeable property with an underscore (`_state`)
    // and a public read-only one (`state`). This way only this class can change the state,
    // but anyone can read it and subscribe to its changes.
    // `MutableStateFlow(PlaybackState())` creates the box with an empty starting state
    // ("nothing loaded").
    private val _state = MutableStateFlow(PlaybackState())
    // Syntax: `StateFlow<PlaybackState>` is a GENERIC type: the `< >` say what type of thing is
    // inside (a box that holds a PlaybackState).
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    /**
     * Plays or pauses the audio [audioId]. If a different audio from the loaded one is asked
     * for, it stops the current one and starts the new one.
     *
     * @param audioId identifier of the audio in the database.
     * @param file the sound file on disk.
     */
    fun toggle(audioId: Long, file: File) {
        // Read the current state and the current player into short local variables.
        // `.value` is the value inside the StateFlow right now.
        val s = _state.value
        val p = player
        // Syntax: `when { ... }` is like a chain of "if... / else if...". The conditions are
        // checked in order and the first one that is true runs (the code after the `->`).
        when {
            // Case 1: same audio, there is a player and it is sounding -> pause.
            // `;` lets you put two instructions on the same line.
            s.audioId == audioId && p != null && s.playing -> { p.pause(); update(playing = false) }
            // Case 2: same audio and there is a player, but it is paused -> resume and start
            // the "tick-tock" that updates the position again.
            s.audioId == audioId && p != null -> { p.start(); update(playing = true); tick() }
            // Case 3 (`else` = in any other case): it is another audio -> start it from zero.
            else -> start(audioId, file)
        }
    }

    /**
     * Jumps to a point in the audio (for example, when the user drags the progress bar).
     *
     * @param ms the target position in milliseconds.
     */
    fun seekTo(ms: Long) {
        // Syntax: `?.` is the "safe call": if `player` is null it does nothing; otherwise it
        // calls `seekTo`. MediaPlayer wants an `Int` (normal whole number), hence `toInt()`.
        player?.seekTo(ms.toInt())
        // Updates the published state with the new position, copying everything else as is.
        _state.value = _state.value.copy(positionMs = ms)
    }

    /**
     * Stops [audioId] only if it is the loaded one (for example, before deleting it, so we do
     * not keep playing a file that is about to disappear).
     */
    fun stopIf(audioId: Long) {
        if (_state.value.audioId == audioId) stop()
    }

    /**
     * Stops playback completely and releases the MediaPlayer. The state goes back to
     * "nothing loaded".
     */
    fun stop() {
        // Cancels the "tick-tock" if there is one.
        ticker?.cancel()
        // Releases the MediaPlayer's resources if there is one.
        player?.release()
        // Forgets the player.
        player = null
        // Publishes an empty state (default values).
        _state.value = PlaybackState()
    }

    /**
     * Loads [file] and starts playing it from the beginning.
     */
    private fun start(audioId: Long, file: File) {
        // First stop whatever was playing.
        stop()
        // `runCatching { ... }` runs the block and "catches" any error instead of letting the
        // app crash. `.getOrNull()` returns the result, or null if there was an error.
        // Syntax: `?:` is the "Elvis" operator: if the left side is null, use the right side.
        // Here, if it failed, `return` leaves the function without doing anything else (for
        // example if the file does not exist or is damaged).
        val p = runCatching {
            // Syntax: `apply { ... }` runs the block "on" the newly created object (inside it,
            // calls refer to that object) and returns that same object. Handy to set something
            // up right after creating it.
            MediaPlayer().apply {
                // Says which file to play, using its full path on disk.
                setDataSource(file.absolutePath)
                // Prepares it (reads the file); after this it can be played.
                prepare()
            }
        }.getOrNull() ?: return
        // What to do when the audio reaches the end: stop the "tick-tock", go back to second 0
        // and mark that it is not sounding. The lambda receives the player itself as `it`.
        p.setOnCompletionListener {
            ticker?.cancel()
            it.seekTo(0)
            _state.value = _state.value.copy(playing = false, positionMs = 0)
        }
        // Keep the new player, start it and publish the new state: this audio, playing, at
        // position 0 and with its total length (`p.duration`, in ms).
        player = p
        p.start()
        _state.value = PlaybackState(audioId, true, 0, p.duration.toLong())
        // Start updating the position regularly.
        tick()
    }

    /**
     * Updates in the state whether it is playing or not, and also the current position.
     *
     * Syntax: in `player?.currentPosition?.toLong() ?: 0`, if `player` is null the whole chain
     * gives null and the `?:` puts 0 in its place.
     */
    private fun update(playing: Boolean) {
        _state.value = _state.value.copy(playing = playing, positionMs = player?.currentPosition?.toLong() ?: 0)
    }

    /**
     * Starts the "tick-tock": a coroutine that every 200 ms copies the player's position into
     * the state, so the progress bar on screen moves forward.
     */
    private fun tick() {
        // If one was already running, cancel it so there are never two at once.
        ticker?.cancel()
        // `scope.launch { ... }` launches a coroutine and returns its Job, which we keep.
        ticker = scope.launch {
            // Endless loop: repeats until the coroutine is cancelled or `break` leaves it.
            while (true) {
                // If there is no player any more, `break` leaves the loop and the coroutine
                // ends.
                val p = player ?: break
                // Publishes the current position.
                _state.value = _state.value.copy(positionMs = p.currentPosition.toLong())
                // Waits 200 ms without freezing the app. It is a `suspend` function: it can
                // "pause itself" and continue later, and only works inside a coroutine.
                delay(200)
            }
        }
    }
}
