// ============================================================================================
// FILE: NoteColors.kt
// --------------------------------------------------------------------------------------------
// What is it for?
//   It defines the colours a note can have (like the coloured cards in Google Keep): "no
//   colour" plus six soft tones (sage, lavender, sky, wheat, pink, graphite). Each colour has a
//   light version (for day mode) and a dark version (for night / dark mode).
//
// What is it connected to?
//   - data/Note stores the colour as a number (`color`): the position in `NotePalette`.
//   - ui/notes/NotesScreen.kt uses `noteBackground` to paint each card.
//   - The editor (ui/editor/) uses `NotePalette` to show the colour picker, with the colour
//     names, and `noteBackground` for the editor background.
//   - Theme.kt: colour 0 ("no colour") uses the theme's normal surface colour.
// ============================================================================================
package com.tuapp.ui.theme

// Tells whether the phone is in dark mode.
import androidx.compose.foundation.isSystemInDarkTheme
// The current Material theme (to read its colours).
import androidx.compose.material3.MaterialTheme
// The `@Composable` annotation (see `noteBackground`).
import androidx.compose.runtime.Composable
// `Color`: a colour value.
import androidx.compose.ui.graphics.Color

/**
 * Note colours: paper and faded-ink tones, each with a night version. [name] is shown in the
 * UI (in Spanish).
 *
 * Syntax: a `data class` is a class made only to hold data. Here each `NoteColor` holds:
 * @property name the colour's name shown to the user (e.g. "Salvia" = "Sage").
 * @property light the colour used in light (day) mode.
 * @property dark the colour used in dark (night) mode.
 */
data class NoteColor(val name: String, val light: Color, val dark: Color)

/**
 * The list of all note colours, in order. A note's `color` number is its position here
 * (0 = first). Do not reorder it: saved notes refer to colours by position.
 *
 * Colours are written as `Color(0xFFRRGGBB)`: `0x` starts a hexadecimal (base-16) number,
 * `FF` is full opacity, then two digits each for red, green and blue.
 * `Color.Unspecified` means "no colour chosen" (the theme decides).
 */
val NotePalette = listOf(
    // 0: "Sin color" (No colour).
    NoteColor("Sin color", Color.Unspecified, Color.Unspecified),
    // 1: "Salvia" (Sage, a soft green).
    NoteColor("Salvia", Color(0xFFE2ECDD), Color(0xFF28362A)),
    // 2: "Lavanda" (Lavender).
    NoteColor("Lavanda", Color(0xFFE7E2F3), Color(0xFF322D44)),
    // 3: "Cielo" (Sky blue).
    NoteColor("Cielo", Color(0xFFDDEAF4), Color(0xFF233441)),
    // 4: "Trigo" (Wheat, a pale yellow).
    NoteColor("Trigo", Color(0xFFF3ECCF), Color(0xFF3A3524)),
    // 5: "Rosa" (Pink).
    NoteColor("Rosa", Color(0xFFF4DFE3), Color(0xFF42272D)),
    // 6: "Grafito" (Graphite, a light grey).
    NoteColor("Grafito", Color(0xFFE1E3E8), Color(0xFF2E3038))
)

/**
 * Gives the background colour for a note with colour number [index], taking into account
 * whether the phone is in dark mode.
 *
 * It is `@Composable` because it reads Compose things (the current theme and dark mode),
 * which can only be read from inside a composable function. If dark mode changes, the screens
 * that use it recompose and get the new colour.
 *
 * @param index the note's colour number (position in `NotePalette`).
 * @return the colour to paint the note with.
 */
@Composable
fun noteBackground(index: Int): Color {
    // "No colour": use the theme's normal surface colour.
    if (index == 0) return MaterialTheme.colorScheme.surface
    // Get the colour at that position; if the number is out of range (e.g. a colour that no
    // longer exists), `getOrElse` falls back to the first one.
    val c = NotePalette.getOrElse(index) { NotePalette[0] }
    // Night version in dark mode, day version otherwise.
    return if (isSystemInDarkTheme()) c.dark else c.light
}
