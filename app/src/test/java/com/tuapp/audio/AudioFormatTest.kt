// =================================================================================================
// FILE: AudioFormatTest.kt  (automatic tests for the audio text helpers)
// -------------------------------------------------------------------------------------------------
// WHAT IS THIS FILE FOR?
// It checks `AudioFormat` (audio/AudioFormat.kt): small helpers that turn audio data into
// text for the screen (durations like "2:31", dates like "30 sep 2026", default names for
// recordings) and that clean up file names of imported audios.
//
// WHAT IS A TEST? Code that runs part of the app with a known input and compares the result
// with the right answer; if they differ, the test "fails" and shows what went wrong.
// Developers run tests (for example with `gradlew testDebugUnitTest`) to be sure a change did
// not break anything. Tests are not part of the app the user installs.
//
// WHAT IS JUNIT? The most common library to write and run tests in Java and Kotlin:
//   - `@Test`: an annotation (a label that starts with @) that marks a function as a test.
//     JUnit finds every function with this label and runs it.
//   - `assertEquals(expected, actual)`: fails the test if the two values are different.
//   - `assertNull(value)`: fails the test if the value is not `null` (null = "nothing").
//   - A test passes if it finishes without any failed check.
// =================================================================================================

// Same package as `AudioFormat`, so the test can use it directly.
package com.tuapp.audio

// JUnit tools (explained above).
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
// Java date and time classes: `LocalDateTime` is a date and time without a time zone;
// `ZoneOffset` is a fixed time zone (here UTC, the world reference time).
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Tests for `AudioFormat`. JUnit runs every function marked with `@Test`. */
class AudioFormatTest {

    /**
     * `duration(ms)` turns milliseconds (1000 ms = 1 second) into "minutes:seconds", or
     * "hours:minutes:seconds" for an hour or more. Negative values show as "0:00".
     * In Kotlin, `_` inside a number is only for reading: `151_000` is 151000.
     */
    @Test fun duration() {
        assertEquals("0:00", AudioFormat.duration(0))
        assertEquals("0:07", AudioFormat.duration(7_000))
        // 7.6 seconds is rounded to the nearest second: 8.
        assertEquals("0:08", AudioFormat.duration(7_600))
        assertEquals("2:31", AudioFormat.duration(151_000))
        assertEquals("1:02:05", AudioFormat.duration(3_725_000))
        assertEquals("0:00", AudioFormat.duration(-5))
    }

    /**
     * `recordingName(time)` makes the default name of a new recording: "Grabación" (Spanish
     * for "Recording"), the day, the short Spanish month and the time with two digits
     * ("09:05"). `LocalDateTime.of(year, month, day, hour, minute)` builds a fixed moment so
     * the result is always the same.
     */
    @Test fun recordingName() {
        assertEquals("Grabación 30 sep 21:22", AudioFormat.recordingName(LocalDateTime.of(2026, 9, 30, 21, 22)))
        assertEquals("Grabación 5 ene 09:05", AudioFormat.recordingName(LocalDateTime.of(2027, 1, 5, 9, 5)))
    }

    /**
     * `date(ms, zone)` turns a stored timestamp (milliseconds since 1 January 1970) into
     * "day month year". The test first builds the timestamp for 30 Sep 2026 at noon in UTC
     * (`toInstant(...)` fixes it in UTC, `toEpochMilli()` gives the milliseconds), then checks
     * the text. Using UTC for both makes the test give the same result on any computer.
     */
    @Test fun date() {
        val ms = LocalDateTime.of(2026, 9, 30, 12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals("30 sep 2026", AudioFormat.date(ms, ZoneOffset.UTC))
    }

    /**
     * `nameFromFile(fileName)` makes a nice audio name from an imported file's name: it
     * removes the extension (".mp3"), turns "_" into spaces, and uses "Audio" if nothing is
     * left (a file called just ".mp3").
     */
    @Test fun nameFromFile() {
        assertEquals("maqueta final", AudioFormat.nameFromFile("maqueta final.mp3"))
        assertEquals("idea estribillo", AudioFormat.nameFromFile("idea_estribillo.m4a"))
        assertEquals("sin extension", AudioFormat.nameFromFile("sin extension"))
        assertEquals("Audio", AudioFormat.nameFromFile(".mp3"))
    }

    /**
     * `extension(fileName)` gives the file extension in small letters ("MP3" becomes "mp3"),
     * using only the part after the LAST dot ("a.b.m4a" gives "m4a"). It gives `null` when
     * there is no extension, or when it does not look like a real one (here "no válida"
     * has a space, so it is rejected).
     */
    @Test fun extension() {
        assertEquals("mp3", AudioFormat.extension("canción.MP3"))
        assertEquals("m4a", AudioFormat.extension("a.b.m4a"))
        assertNull(AudioFormat.extension("sin extension"))
        assertNull(AudioFormat.extension("raro.no válida"))
    }
}
