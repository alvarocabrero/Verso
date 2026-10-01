// ============================================================================================
// FILE: AudioFormat.kt
// --------------------------------------------------------------------------------------------
// What is it for?
//   It groups small "helper" functions that turn audio data into nice texts to show on screen
//   (the texts are in Spanish, because the app's users speak Spanish):
//     - a duration in milliseconds -> "2:31"
//     - a date and time -> the default name of a recording, "Grabación 30 sep 21:22"
//     - a timestamp -> a readable date, "30 sep 2026"
//     - the name of an imported file -> a display name, "maqueta final"
//     - the name of a file -> its extension, "mp3"
//
// What role does it play in the app?
//   It is "pure Kotlin": it uses nothing from Android (no screens, no microphone...). That is
//   why it can be tested with normal tests on the computer (they live in
//   app/src/test/java/com/tuapp/audio/).
//
// What is it connected to?
//   - The audio screens (ui/audios/AudiosScreen.kt, AudioComponents.kt) use it to show
//     durations and dates.
//   - AudiosViewModel.kt uses it to name new recordings and imported files.
//
// Kotlin note: the first line, `package`, says in which "logical folder" this code lives
// (com.tuapp.audio). The `import` lines bring in pieces from other libraries so we can use them
// here by their short name (for example `Instant` instead of `java.time.Instant`).
// ============================================================================================
package com.tuapp.audio

// `Instant`: one exact moment in time (with no time zone), like a mark on a ruler.
import java.time.Instant
// `LocalDateTime`: a "calendar" date and time (day, month, year, hour, minute) with no zone.
import java.time.LocalDateTime
// `ZoneId`: a time zone (for example "Europe/Madrid").
import java.time.ZoneId

/**
 * Formatting helpers for audios. Pure Kotlin, tested on the JVM (the computer, without a
 * phone); the texts it produces are in Spanish.
 *
 * Kotlin syntax: `object` declares a "single object". It is like a class that has only one
 * copy, created automatically. You never write `AudioFormat()`: you use its name directly, for
 * example `AudioFormat.duration(5000)`. It is ideal for grouping utilities that do not need to
 * keep their own data.
 */
object AudioFormat {

    /**
     * Turns a duration in milliseconds (ms) into a clock-like text.
     * Examples: 0:07, 2:31, 1:02:05 (hours only when needed).
     *
     * Syntax: `fun` declares a function. `ms: Long` means "it receives a parameter called `ms`
     * of type `Long`" (a big whole number). `: String` after the parentheses says that it
     * returns a text.
     *
     * @param ms the duration in milliseconds (1 second = 1000 ms).
     * @return the formatted text.
     */
    fun duration(ms: Long): String {
        // `val` declares a variable that can NOT be changed later (a local constant).
        // `coerceAtLeast(0)` makes sure the number is not negative (if it is, 0 is used).
        // We add 500 before dividing by 1000 to ROUND to the nearest second instead of cutting
        // off (so 1600 ms is shown as 2 s and not as 1 s).
        // When you divide two whole numbers, Kotlin drops the decimals.
        val totalSeconds = (ms.coerceAtLeast(0) + 500) / 1000
        // Full hours: each hour has 3600 seconds.
        val h = totalSeconds / 3600
        // Minutes: `%` is the "remainder of the division". We remove the full hours and divide
        // what is left by 60.
        val m = (totalSeconds % 3600) / 60
        // Loose seconds: the remainder after removing the full minutes.
        val s = totalSeconds % 60
        // In Kotlin `if` can give back a value ("if this, then this value, else that value").
        // `"%d:%02d".format(m, s)` fills in a template: `%d` is a number and `%02d` is a number
        // with at least 2 digits, padded with zeros on the left (7 -> "07").
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    /**
     * Spanish month abbreviations. They are written by hand so they do not depend on the
     * language data installed on the phone (which could differ between devices).
     *
     * Syntax: `private` means it can only be used inside this object.
     * `listOf(...)` creates a list that cannot be changed. Position 0 is "ene" (January),
     * position 11 is "dic" (December).
     */
    private val MONTHS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

    /**
     * Builds the default name of a new recording from the date and time it was made.
     * Example: "Grabación 30 sep 21:22" ("Grabación" = "Recording").
     *
     * Syntax: when a function is a single expression, it can be written with `=` instead of
     * braces `{ ... }` and `return`. Whatever comes after the `=` is what it returns.
     *
     * @param time the date and time of the recording.
     * @return the recording name.
     */
    fun recordingName(time: LocalDateTime): String =
        // Fills the template with: day of the month, month abbreviation (months go from 1 to 12
        // and the list starts at 0, so we subtract 1), hour and minute with two digits.
        "Grabación %d %s %02d:%02d".format(time.dayOfMonth, MONTHS[time.monthValue - 1], time.hour, time.minute)

    /**
     * Turns a timestamp into a readable date. Example: "30 sep 2026".
     *
     * @param epochMs milliseconds since 1 January 1970 (the standard way computers store a
     *   moment in time).
     * @param zone time zone used to read that moment. Syntax: `= ZoneId...` is a DEFAULT VALUE:
     *   if the caller does not pass this parameter, the phone's time zone is used.
     * @return the date text.
     */
    fun date(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        // Turns the milliseconds into a moment and places it in the given time zone, so we can
        // ask for the "local" day, month and year.
        val d = Instant.ofEpochMilli(epochMs).atZone(zone)
        // Syntax: "string template". Inside a text in quotes, `${expression}` is replaced by
        // the value of that expression. Here it builds "day month year".
        return "${d.dayOfMonth} ${MONTHS[d.monthValue - 1]} ${d.year}"
    }

    /**
     * Gets the display name for an imported file by removing the folder and the extension.
     * Example: "maqueta final.mp3" → "maqueta final".
     *
     * @param fileName the name (or path) of the file.
     * @return the clean name; if it ends up empty, it returns "Audio".
     */
    fun nameFromFile(fileName: String): String {
        // `substringAfterLast('/')` keeps what comes after the last slash (removes folders).
        // Then `substringBeforeLast('.', ...)` keeps what comes before the last dot (removes
        // the extension); the second argument is what it returns when there is NO dot: in
        // that case, the whole name without folders.
        val base = fileName.substringAfterLast('/').substringBeforeLast('.', fileName.substringAfterLast('/'))
        // Replaces underscores with spaces ("mi_cancion" -> "mi cancion"), removes spaces at
        // the start and end (`trim`) and, if the result is empty, uses "Audio".
        // Syntax: `ifEmpty { ... }` receives a "lambda" (a small piece of code in braces that
        // is passed as an argument) and only runs it if the text is empty.
        return base.replace('_', ' ').trim().ifEmpty { "Audio" }
    }

    /**
     * Returns the lower-case extension of [fileName] ("mp3") if it looks like a real
     * extension, or `null` if there is none.
     *
     * Syntax: `String?` (with a question mark) means "a text OR `null`". `null` is the value
     * "nothing / no value". In Kotlin a type without `?` can never be `null`, so the compiler
     * forces you to handle the "no value" case.
     *
     * @param fileName the file name.
     * @return the extension or `null`.
     */
    fun extension(fileName: String): String? =
        // Takes what comes after the last dot (or "" if there is no dot) and makes it lower
        // case. `takeIf { condition }` returns the value itself if the condition is true, and
        // `null` if not. Inside the lambda, `it` is the automatic name of the value checked.
        // The condition: length between 1 and 5 (`1..5` is a RANGE that includes both ends,
        // and `in` asks whether a value is inside it) and every character is a letter or a
        // digit. `Char::isLetterOrDigit` is a "function reference": it passes that existing
        // function as if it were a lambda.
        fileName.substringAfterLast('.', "").lowercase().takeIf { it.length in 1..5 && it.all(Char::isLetterOrDigit) }
}
