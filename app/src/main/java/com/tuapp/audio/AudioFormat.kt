package com.tuapp.audio

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Formatting helpers for audios. Pure Kotlin, tested on the JVM; texts in Spanish. */
object AudioFormat {

    /** 0:07, 2:31, 1:02:05 */
    fun duration(ms: Long): String {
        val totalSeconds = (ms.coerceAtLeast(0) + 500) / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    /** Spanish month abbreviations, fixed so they don't depend on the device's locale data. */
    private val MONTHS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

    /** "Grabación 30 sep 21:22" */
    fun recordingName(time: LocalDateTime): String =
        "Grabación %d %s %02d:%02d".format(time.dayOfMonth, MONTHS[time.monthValue - 1], time.hour, time.minute)

    /** "30 sep 2026" */
    fun date(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val d = Instant.ofEpochMilli(epochMs).atZone(zone)
        return "${d.dayOfMonth} ${MONTHS[d.monthValue - 1]} ${d.year}"
    }

    /** Display name for an imported file: "maqueta final.mp3" → "maqueta final". */
    fun nameFromFile(fileName: String): String {
        val base = fileName.substringAfterLast('/').substringBeforeLast('.', fileName.substringAfterLast('/'))
        return base.replace('_', ' ').trim().ifEmpty { "Audio" }
    }

    /** Lower-case extension of [fileName] if it looks like one ("mp3"), or null. */
    fun extension(fileName: String): String? =
        fileName.substringAfterLast('.', "").lowercase().takeIf { it.length in 1..5 && it.all(Char::isLetterOrDigit) }
}
