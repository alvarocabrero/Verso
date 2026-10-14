// ============================================================================================
// FILE: RhymeColors.kt
// --------------------------------------------------------------------------------------------
// What is it for?
//   In the editor, lines that rhyme with each other are marked with the same colour, like
//   with a highlighter pen: rhyme group A in one colour, group B in another, and so on. This
//   file chooses those colours.
//
//   The colours are shown as soft tints on the paper, and two different rhymes must never
//   look alike. So the palette was chosen by measuring, not by eye: every pair of colours was
//   blended on the note background (light and dark, full and assonant strength) and compared
//   with CIEDE2000, the standard formula for "how different do two colours look". The test
//   RhymeColorsTest (app/src/test/.../ui/theme/) checks it stays that way.
//
// What is it connected to?
//   - The analysis engine (com.tuapp.analisis, in Spanish) finds the rhymes; its class `Rima`
//     ("rhyme") says which group a line belongs to and the rhyme type (`Rima.Tipo`):
//       * `CONSONANTE`: *rima consonante* (full rhyme: vowels AND consonants match from the
//         stressed vowel on, e.g. "canción / corazón").
//       * `ASONANTE`: *rima asonante* (assonant rhyme: only the vowels match, e.g.
//         "luna / duda").
//   - The editor (ui/editor/) uses `rhymeColor`, `rhymeBackground` and `rhymeUnderlined` to
//     paint the rhyme letters and the highlighted endings.
// ============================================================================================
package com.tuapp.ui.theme

// Tells whether the phone is in dark mode.
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
// The engine's rhyme class (Spanish name).
import com.tuapp.analisis.Rima

/**
 * Colours for rhyme groups (A, B, C…), in the order they are given out.
 *
 * They are the 8 colours whose tints differ the most from each other (a minimum CIEDE2000
 * distance of about 16 between any two, against about 5 for the previous palette, whose two
 * blues were easy to confuse). They are also ordered so that consecutive groups (A and B,
 * B and C...), which usually sit next to each other in a poem, are as different as possible.
 *
 * `internal` = usable anywhere in this app module (the tests need it), but not outside it.
 * Each colour is `Color(0xFFRRGGBB)`: `FF` is full opacity and the next pairs are red, green
 * and blue in hexadecimal (base 16).
 */
internal val RhymePalette = listOf(
    // green
    Color(0xFF00CC11),
    // magenta
    Color(0xFFFF00D4),
    // golden yellow
    Color(0xFFFFD500),
    // navy blue
    Color(0xFF000073),
    // aqua
    Color(0xFF00FFEA),
    // red
    Color(0xFFCC0011),
    // sky blue
    Color(0xFF0095FF),
    // orange
    Color(0xFFFF6A00)
)

/**
 * The colour for rhyme group number [group] (0 = A, 1 = B...).
 *
 * `group.mod(RhymePalette.size)` is the remainder of dividing by 8 (always 0 or more), so
 * group 8 gets colour 0 again, group 9 colour 1, and so on. To tell those groups apart from
 * the first eight, they are also underlined (see [rhymeUnderlined]).
 * Syntax: `fun ... = expression` is a one-line function.
 */
fun rhymeColor(group: Int): Color = RhymePalette[group.mod(RhymePalette.size)]

/**
 * True for groups that reuse a colour (the ninth group on, I and later). They are drawn with
 * an underline as well as the tint, so they never look the same as the group whose colour
 * they share.
 */
fun rhymeUnderlined(group: Int): Boolean = group >= RhymePalette.size

/**
 * Highlighter background for a rhyme ending: full rhymes stronger, assonant ones softer. A
 * little stronger in dark mode, where the tint is less visible.
 *
 * @param group the rhyme group number.
 * @param type the rhyme type: `CONSONANTE` (full rhyme) or `ASONANTE` (assonant rhyme).
 * @return the group colour made partly transparent.
 */
@Composable
fun rhymeBackground(group: Int, type: Rima.Tipo): Color =
    rhymeColor(group).copy(alpha = rhymeAlpha(type, isSystemInDarkTheme()))

/**
 * How opaque the tint is. "Alpha" is opacity: 0 = invisible, 1 = solid; `.32f` means 0.32 (a
 * `Float` number). `when (type) { ... }` picks a branch depending on the value of `type`
 * (like a switch). It is a separate, non-`@Composable` function so the tests can use it.
 */
internal fun rhymeAlpha(type: Rima.Tipo, dark: Boolean): Float = when (type) {
    Rima.Tipo.CONSONANTE -> if (dark) .42f else .32f
    Rima.Tipo.ASONANTE -> if (dark) .21f else .16f
}
