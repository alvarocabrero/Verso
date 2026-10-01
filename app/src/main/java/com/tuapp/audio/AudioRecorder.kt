// ============================================================================================
// FILE: AudioRecorder.kt
// --------------------------------------------------------------------------------------------
// What is it for?
//   It is the app's RECORDER: it uses the phone's microphone to record sound into an .m4a
//   file. It can start, pause, resume and stop, it counts the recorded time (not counting
//   pauses), and it gives the input "level" (loudness) to draw a meter.
//
// What role does it play in the app?
//   ui/audios/AudiosViewModel.kt creates and drives it when the user opens the recording sheet
//   in the Audios section. To use it, the app needs the microphone PERMISSION (RECORD_AUDIO),
//   which is asked from the user in AudiosScreen.kt.
//
// MediaRecorder (Android):
//   It is the tool Android provides to record audio (or video). It must be set up in a strict
//   order; if you skip a step, it throws an error:
//     1. `setAudioSource`: where the sound comes from (the microphone).
//     2. `setOutputFormat`: the file's "container" type (MPEG-4, that is .m4a/.mp4).
//     3. `setAudioEncoder`: how the sound is compressed (AAC, a common, efficient format).
//     4. Other settings: channels, sample rate, quality (bitrate).
//     5. `setOutputFile`: where to save the file.
//     6. `prepare()` and then `start()` to begin recording.
//     7. `pause()` / `resume()` to pause and continue.
//     8. `stop()` to finish and close the file, and `release()` to free the microphone.
// ============================================================================================
package com.tuapp.audio

// `Context`: in Android, the app's "context", an object that gives access to system services
// (files, resources, hardware...). Many Android classes need it.
import android.content.Context
// The Android class that records sound (explained above).
import android.media.MediaRecorder
// `Build`: information about the phone's Android version.
import android.os.Build
// `SystemClock`: system clocks. `elapsedRealtime()` counts milliseconds since the phone was
// switched on; it does not change if the user changes the time, so it is good for measuring
// durations.
import android.os.SystemClock
// `File`: represents a file on disk.
import java.io.File

/**
 * Records AAC audio into an .m4a file (mono, 44.1 kHz, 128 kbps: about 1 MB per minute).
 * Keeps track of the elapsed time, excluding pauses.
 *
 * Syntax: `class AudioRecorder(private val context: Context)` declares the class and, at the
 * same time, its "constructor" (what you must give it to create it): a `Context`. Writing
 * `private val` in front stores that parameter as a private property of the class, usable in
 * any function inside it.
 */
class AudioRecorder(private val context: Context) {

    // The Android MediaRecorder in use, or null when not recording.
    private var recorder: MediaRecorder? = null
    // Moment (by SystemClock) when the current recording stretch began. `0L` is the number 0
    // of type `Long` (the final `L` means "long whole number").
    private var startedAt = 0L
    // Milliseconds collected from earlier stretches (before each pause).
    private var accumulated = 0L

    // true if the recording is paused.
    // Syntax: `private set` means that from outside it can be READ but not CHANGED; only this
    // class can change it. The `;` separates two things written on the same line.
    var isPaused = false; private set
    // true if a recording is in progress (even if paused).
    // Syntax: `get() = ...` defines a "computed" property: it stores nothing, it is worked out
    // again every time it is read. Here: "there is a recorder" means "we are recording".
    val isRecording get() = recorder != null

    /**
     * Starts recording into the file [output].
     *
     * @param output file where the recording will be saved.
     */
    fun start(output: File) {
        // `check(condition) { message }` throws an error with that message if the condition is
        // false. It prevents starting two recordings at once.
        check(recorder == null) { "Already recording" }
        // Creates the MediaRecorder. Since Android 12 (version "S") you must pass it the
        // context; older versions use the old constructor with no parameters.
        // `@Suppress("DEPRECATION")` tells the compiler not to warn that this constructor is
        // "deprecated" (outdated): we know, and we only use it on old phones.
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
                else @Suppress("DEPRECATION") MediaRecorder()
        // Sound source: the microphone.
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        // File format: MPEG-4 (the .m4a).
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        // Audio compression: AAC.
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        // A single channel (mono): enough for a voice and half the size of stereo.
        r.setAudioChannels(1)
        // 44,100 samples per second (CD quality). The `_` inside numbers is only there to make
        // them easier to read, like a thousands separator.
        r.setAudioSamplingRate(44_100)
        // 128,000 bits per second of compression quality.
        r.setAudioEncodingBitRate(128_000)
        // Where the file is written (full path).
        r.setOutputFile(output.absolutePath)
        // Prepare and start recording.
        r.prepare()
        r.start()
        // Keep the recorder and reset the time counters.
        recorder = r
        accumulated = 0
        startedAt = SystemClock.elapsedRealtime()
        isPaused = false
    }

    /**
     * Pauses the recording (if recording and not already paused).
     */
    fun pause() {
        // If there is no recorder, leave without doing anything (`?: return`).
        val r = recorder ?: return
        // If it is already paused, do nothing either.
        if (isPaused) return
        r.pause()
        // Add to the total how long this stretch lasted (now minus when it began).
        accumulated += SystemClock.elapsedRealtime() - startedAt
        isPaused = true
    }

    /**
     * Resumes the recording if it was paused.
     */
    fun resume() {
        val r = recorder ?: return
        // `!` means "not": if it is NOT paused, there is nothing to resume.
        if (!isPaused) return
        r.resume()
        // A new stretch begins: note the current moment.
        startedAt = SystemClock.elapsedRealtime()
        isPaused = false
    }

    /**
     * Recorded time so far, without pauses, in milliseconds.
     *
     * It is the collected total plus, if we are recording right now (not paused), the length
     * of the current stretch; otherwise plus 0.
     */
    fun elapsedMs(): Long =
        accumulated + if (isRecording && !isPaused) SystemClock.elapsedRealtime() - startedAt else 0

    /**
     * Microphone input level from 0 to 1, for drawing the level meter.
     *
     * @return 0 if not recording or paused; otherwise the loudest value since the last call,
     *   scaled to 0..1. (`0f` is the number 0 of type `Float`, a number with decimals.)
     */
    fun level(): Float {
        val r = recorder ?: return 0f
        if (isPaused) return 0f
        // `maxAmplitude` gives a value between 0 and 32767 (the maximum of a 16-bit number).
        // We divide to bring it to 0..1, and `coerceIn` makes sure it stays in that range.
        return (r.maxAmplitude / 32_767f).coerceIn(0f, 1f)
    }

    /**
     * Stops recording and finishes the file.
     *
     * @return the recorded duration in milliseconds.
     */
    fun stop(): Long {
        // Work out the duration BEFORE stopping (afterwards there would be no recorder).
        val duration = elapsedMs()
        // Syntax: `?.let { r -> ... }` runs the block only if `recorder` is not null, and
        // inside the block calls it `r`.
        recorder?.let { r ->
            // `stop()` throws an error if nothing was recorded; `runCatching` catches it so
            // the app does not crash (in that case the file is thrown away later).
            runCatching { r.stop() }
            // Frees the microphone and the resources.
            r.release()
        }
        // Get everything ready for a new recording and return the duration.
        recorder = null
        isPaused = false
        return duration
    }
}
