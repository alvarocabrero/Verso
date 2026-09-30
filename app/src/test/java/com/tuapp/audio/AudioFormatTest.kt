package com.tuapp.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class AudioFormatTest {

    @Test fun duration() {
        assertEquals("0:00", AudioFormat.duration(0))
        assertEquals("0:07", AudioFormat.duration(7_000))
        assertEquals("0:08", AudioFormat.duration(7_600))        // rounded to the nearest second
        assertEquals("2:31", AudioFormat.duration(151_000))
        assertEquals("1:02:05", AudioFormat.duration(3_725_000))
        assertEquals("0:00", AudioFormat.duration(-5))
    }

    @Test fun recordingName() {
        assertEquals("Grabación 30 sep 21:22", AudioFormat.recordingName(LocalDateTime.of(2026, 9, 30, 21, 22)))
        assertEquals("Grabación 5 ene 09:05", AudioFormat.recordingName(LocalDateTime.of(2027, 1, 5, 9, 5)))
    }

    @Test fun date() {
        val ms = LocalDateTime.of(2026, 9, 30, 12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals("30 sep 2026", AudioFormat.date(ms, ZoneOffset.UTC))
    }

    @Test fun nameFromFile() {
        assertEquals("maqueta final", AudioFormat.nameFromFile("maqueta final.mp3"))
        assertEquals("idea estribillo", AudioFormat.nameFromFile("idea_estribillo.m4a"))
        assertEquals("sin extension", AudioFormat.nameFromFile("sin extension"))
        assertEquals("Audio", AudioFormat.nameFromFile(".mp3"))
    }

    @Test fun extension() {
        assertEquals("mp3", AudioFormat.extension("canción.MP3"))
        assertEquals("m4a", AudioFormat.extension("a.b.m4a"))
        assertNull(AudioFormat.extension("sin extension"))
        assertNull(AudioFormat.extension("raro.no válida"))
    }
}
