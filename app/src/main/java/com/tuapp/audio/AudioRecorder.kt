package com.tuapp.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import java.io.File

/**
 * Records AAC audio into an .m4a file (mono, 44.1 kHz, 128 kbps: about 1 MB per
 * minute). Keeps track of the elapsed time, excluding pauses.
 */
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var startedAt = 0L
    private var accumulated = 0L

    var isPaused = false; private set
    val isRecording get() = recorder != null

    fun start(output: File) {
        check(recorder == null) { "Already recording" }
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
                else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioChannels(1)
        r.setAudioSamplingRate(44_100)
        r.setAudioEncodingBitRate(128_000)
        r.setOutputFile(output.absolutePath)
        r.prepare()
        r.start()
        recorder = r
        accumulated = 0
        startedAt = SystemClock.elapsedRealtime()
        isPaused = false
    }

    fun pause() {
        val r = recorder ?: return
        if (isPaused) return
        r.pause()
        accumulated += SystemClock.elapsedRealtime() - startedAt
        isPaused = true
    }

    fun resume() {
        val r = recorder ?: return
        if (!isPaused) return
        r.resume()
        startedAt = SystemClock.elapsedRealtime()
        isPaused = false
    }

    /** Recorded time so far, without pauses. */
    fun elapsedMs(): Long =
        accumulated + if (isRecording && !isPaused) SystemClock.elapsedRealtime() - startedAt else 0

    /** Input level from 0 to 1, for the level meter. */
    fun level(): Float {
        val r = recorder ?: return 0f
        if (isPaused) return 0f
        return (r.maxAmplitude / 32_767f).coerceIn(0f, 1f)
    }

    /** Stops and finalises the file. Returns the recorded duration. */
    fun stop(): Long {
        val duration = elapsedMs()
        recorder?.let { r ->
            runCatching { r.stop() }   // throws if nothing was recorded; the file is discarded then
            r.release()
        }
        recorder = null
        isPaused = false
        return duration
    }
}
