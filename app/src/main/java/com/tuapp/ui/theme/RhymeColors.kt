// ============================================================================================
// FILE: RhymeColors.kt
// --------------------------------------------------------------------------------------------
// What is it for?
//   In the editor, lines that rhyme with each other are marked with the same colour, like
//   with a highlighter pen: rhyme group A in one colour, group B in another, and so on. This
//   file chooses those colours.
//
// What is it connected to?
//   - The analysis engine (com.tuapp.analisis, in Spanish) finds the rhymes; its class `Rima`
//     ("rhyme") says which group a line belongs to and the rhyme type (`Rima.Tipo`):
//       * `CONSONANTE`: *rima consonante* (full rhyme: vowels AND consonants match from the
//         stressed vowel on, e.g. "canción / corazón").
//       * `ASONANTE`: *rima asonante* (assonant rhyme: only the vowels match, e.g.
//         "luna / duda").
//   - The editor (ui/editor/) uses `rhymeColor` and `rhymeBackground` to paint the rhyme
//     letters and the highlighted endings.
// ============================================================================================
package com.tuapp.ui.theme

// Tells whether the phone is in dark mode.
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
// The engine's rhyme class (Spanish name).
import com.tuapp.analisis.Rima

/**
 * Colours for rhyme groups (A, B, C…), from the Okabe–Ito palette, a set of colours designed
 * so they stay distinguishable for colour-blind readers. From the ninth group on they repeat.
 *
 * `private` = only usable inside this file. Each colour is `Color(0xFFRRGGBB)`: `FF` is full
 * opacity and the next pairs are red, green and blue in hexadecimal (base 16).
 */
private val RhymePalette = listOf(
    // orange
    Color(0xFFE69F00),
    // sky blue
    Color(0xFF56B4E9),
    // bluish green
    Color(0xFF009E73),
    // yellow
    Color(0xFFF0E442),
    // blue
    Color(0xFF0072B2),
    // vermilion (a bright orange-red)
    Color(0xFFD55E00),
    // reddish purple
    Color(0xFFCC79A7),
    // olive
    Color(0xFF999933)
)

/**
 * The colour for rhyme group number [group] (0 = A, 1 = B...).
 *
 * `group.mod(RhymePalette.size)` is the remainder of dividing by 8 (always 0 or more), so
 * group 8 gets colour 0 again, group 9 colour 1, and so on: the colours repeat in a cycle.
 * Syntax: `fun ... = expression` is a one-line function.
 */
fun rhymeColor(group: Int): Color = RhymePalette[group.mod(RhymePalette.size)]

/**
 * Highlighter background for a rhyme ending: full rhymes stronger, assonant ones softer. A
 * little stronger in dark mode, where the tint is less visible.
 *
 * @param group the rhyme group number.
 * @param type the rhyme type: `CONSONANTE` (full rhyme) or `ASONANTE` (assonant rhyme).
 * @return the group colour made partly transparent.
 */
@Composable
fun rhymeBackground(group: Int, type: Rima.Tipo): Color {
    val dark = isSystemInDarkTheme()
    // "Alpha" is opacity: 0 = invisible, 1 = solid. `.34f` means 0.34 (a `Float` number).
    // `when (type) { ... }` picks a branch depending on the value of `type` (like a switch).
    val alpha = when (type) {
        Rima.Tipo.CONSONANTE -> if (dark) .34f else .26f
        Rima.Tipo.ASONANTE -> if (dark) .17f else .12f
    }
    // A copy of the group colour with that opacity.
    return rhymeColor(group).copy(alpha = alpha)
}
