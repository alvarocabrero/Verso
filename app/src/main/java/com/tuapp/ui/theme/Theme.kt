// ============================================================================================
// FILE: Theme.kt
// --------------------------------------------------------------------------------------------
// What is it for?
//   It defines the app's visual look: the colour sets for light mode and dark mode, and the
//   text styles used for poems (a serif font, like in a printed book). The idea is "indigo
//   ink on cool paper": a notebook, not a task board.
//
// What role does it play in the app?
//   MainActivity.kt wraps the whole app in `VersoTheme { ... }`. From then on, any screen can
//   read the colours with `MaterialTheme.colorScheme.xxx` (for example
//   `MaterialTheme.colorScheme.primary`) and the poem text styles with `VerseStyle.xxx`.
//
// What is a "colour scheme"? (Material 3 concept)
//   Material 3 gives each colour a ROLE instead of using colours directly:
//     - `primary`: the main brand colour (buttons, "+" button).
//     - `onPrimary`: the colour of text/icons drawn ON TOP of `primary`.
//     - `background` / `surface`: page and card backgrounds; `onBackground` / `onSurface`:
//       text on them.
//     - `surfaceVariant` / `onSurfaceVariant`: a slightly different background (search bar)
//       and softer text (dates, hints).
//     - `primaryContainer`, `secondaryContainer`: softer tinted backgrounds for highlighted
//       elements, with their `on...` text colours.
//     - `outlineVariant`: thin lines and borders.
//   Screens ask for the role ("primary"), so changing the scheme changes the whole app.
// ============================================================================================
package com.tuapp.ui.theme

// Tells whether the phone is in dark mode.
import androidx.compose.foundation.isSystemInDarkTheme
// Material 3 theme and helpers to build light and dark colour schemes.
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
// `Color`: a colour value.
import androidx.compose.ui.graphics.Color
// Text styling: style object, font family, italic, boldness.
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
// `sp`: unit for font sizes (like `dp`, but it also follows the user's font size setting).
import androidx.compose.ui.unit.sp

// Indigo ink on cool paper: a notebook, not a task board.
// The four base colours. `private val` = constants only visible in this file. Colours are
// `Color(0xFFRRGGBB)`: `FF` is full opacity, then red, green and blue in hexadecimal.
// - `Ink`: dark indigo, the main colour in light mode.
private val Ink = Color(0xFF2E3A6E)
// - `LightInk`: pale indigo, the main colour in dark mode (dark ink would not be visible).
private val LightInk = Color(0xFFB9C3F0)
// - `Paper`: a very light, slightly cool grey for the day background.
private val Paper = Color(0xFFF6F7F9)
// - `NightPaper`: an almost-black blue-grey for the night background.
private val NightPaper = Color(0xFF15171D)

// Colour scheme for light (day) mode. Each line gives a colour to a role (see the top of the
// file). Roles not listed keep Material's default values.
private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE2F7),
    onPrimaryContainer = Color(0xFF17204A),
    secondaryContainer = Color(0xFFE3E6F1),
    onSecondaryContainer = Color(0xFF1E2438),
    background = Paper,
    onBackground = Color(0xFF1B1D24),
    surface = Paper,
    onSurface = Color(0xFF1B1D24),
    surfaceVariant = Color(0xFFE7E9EF),
    onSurfaceVariant = Color(0xFF4A4E5C),
    outlineVariant = Color(0xFFD3D6E0)
)

// Colour scheme for dark (night) mode: the same roles, with dark backgrounds and light text.
private val DarkColors = darkColorScheme(
    primary = LightInk,
    onPrimary = Color(0xFF1A2350),
    primaryContainer = Color(0xFF34406F),
    onPrimaryContainer = Color(0xFFDDE2F7),
    secondaryContainer = Color(0xFF2E3242),
    onSecondaryContainer = Color(0xFFDFE2EE),
    background = NightPaper,
    onBackground = Color(0xFFE4E5EB),
    surface = NightPaper,
    onSurface = Color(0xFFE4E5EB),
    surfaceVariant = Color(0xFF262933),
    onSurfaceVariant = Color(0xFFB8BBC8),
    outlineVariant = Color(0xFF3A3E4B)
)

/**
 * Styles for the creative text. The UI (buttons, menus) uses the system's sans-serif font;
 * verses use a serif font with generous line height, to read line by line.
 *
 * Syntax: `object` declares a single, ready-made object (no need to create it); you use it as
 * `VerseStyle.body`, `VerseStyle.title`, etc.
 * `lineHeight` is the distance from one line to the next; `fontWeight` is boldness
 * (`SemiBold` = a bit bold).
 */
object VerseStyle {
    // The poem text in the editor: 18 sp, lines 30 sp apart.
    val body = TextStyle(fontFamily = FontFamily.Serif, fontSize = 18.sp, lineHeight = 30.sp)
    // The poem title in the editor: large and semi-bold.
    val title = TextStyle(
        fontFamily = FontFamily.Serif, fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold
    )
    // The title on a note card in the grid (NotesScreen).
    val cardTitle = TextStyle(
        fontFamily = FontFamily.Serif, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold
    )
    // The text preview on a note card.
    val cardBody = TextStyle(fontFamily = FontFamily.Serif, fontSize = 14.sp, lineHeight = 21.sp)
    // The big italic message shown when a list is empty ("La página está en blanco").
    val empty = TextStyle(
        fontFamily = FontFamily.Serif, fontSize = 22.sp, lineHeight = 30.sp, fontStyle = FontStyle.Italic
    )
}

/**
 * The app's theme. Everything drawn inside it uses Verso's colours.
 *
 * It picks the dark scheme if the phone is in dark mode, the light one otherwise, and gives
 * it to Material's `MaterialTheme`. If the user switches dark mode, Compose recomposes and the
 * whole app changes colour.
 *
 * @param content the screens to draw inside the theme. Its type `@Composable () -> Unit` means
 *   "a piece of screen" (a composable function with no parameters). It is usually written as a
 *   trailing lambda: `VersoTheme { ...screens... }`.
 */
@Composable
fun VersoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
