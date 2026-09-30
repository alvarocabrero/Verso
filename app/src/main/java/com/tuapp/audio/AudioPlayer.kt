package com.tuapp.audio

import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/** What is playing: [audioId] null means nothing is loaded. */
data class PlaybackState(
    val audioId: Long? = null,
    val playing: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0
)

/**
 * App-wide player: only one audio sounds at a time, whether it was started from
 * the audio list or from a note. Lives in [com.tuapp.VersoApp].
 */
class AudioPlayer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var player: MediaPlayer? = null
    private var ticker: Job? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    /** Plays or pauses [audioId]; starting a different audio stops the current one. */
    fun toggle(audioId: Long, file: File) {
        val s = _state.value
        val p = player
        when {
            s.audioId == audioId && p != null && s.playing -> { p.pause(); update(playing = false) }
            s.audioId == audioId && p != null -> { p.start(); update(playing = true); tick() }
            else -> start(audioId, file)
        }
    }

    fun seekTo(ms: Long) {
        player?.seekTo(ms.toInt())
        _state.value = _state.value.copy(positionMs = ms)
    }

    /** Stops [audioId] if it is the one loaded (e.g. before deleting it). */
    fun stopIf(audioId: Long) {
        if (_state.value.audioId == audioId) stop()
    }

    fun stop() {
        ticker?.cancel()
        player?.release()
        player = null
        _state.value = PlaybackState()
    }

    private fun start(audioId: Long, file: File) {
        stop()
        val p = runCatching {
            MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
            }
        }.getOrNull() ?: return
        p.setOnCompletionListener {
            ticker?.cancel()
            it.seekTo(0)
            _state.value = _state.value.copy(playing = false, positionMs = 0)
        }
        player = p
        p.start()
        _state.value = PlaybackState(audioId, true, 0, p.duration.toLong())
        tick()
    }

    private fun update(playing: Boolean) {
        _state.value = _state.value.copy(playing = playing, positionMs = player?.currentPosition?.toLong() ?: 0)
    }

    private fun tick() {
        ticker?.cancel()
        ticker = scope.launch {
            while (true) {
                val p = player ?: break
                _state.value = _state.value.copy(positionMs = p.currentPosition.toLong())
                delay(200)
            }
        }
    }
}
