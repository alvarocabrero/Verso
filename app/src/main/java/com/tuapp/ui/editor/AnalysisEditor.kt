// ===============================================================================================
// FILE: AnalysisEditor.kt
// -----------------------------------------------------------------------------------------------
// The `package` line says which "logical folder" this file belongs to. Everything declared here
// has the full name `com.tuapp.ui.editor.Something` (it matches the folder `ui/editor/`).
// ===============================================================================================
package com.tuapp.ui.editor

// `import` lines bring in names from other libraries or other files so they can be used here by
// their short name. Mostly Jetpack Compose pieces:
// - `androidx.compose.foundation.*`: layouts (Box, Row, Column), the basic text field
//   (`BasicTextField`), lazy lists, clicks, backgrounds, "bring into view" (auto-scrolling).
// - `androidx.compose.material3.*` and `material.icons.*`: Material style widgets and icons.
// - `androidx.compose.runtime.*`: state (`remember`, `mutableStateOf`, `rememberSaveable`) and
//   effects (`LaunchedEffect`).
// - `androidx.compose.ui.*`: `Modifier`, drawing (`drawBehind`, `DrawScope`, shapes, colours),
//   geometry (`Offset`, `Size`, `Rect`), text layout (`TextLayoutResult`), accessibility.
// - `com.tuapp.analisis.*`: the analysis engine (in Spanish): `AnalisisPoema` (full analysis),
//   `Metrica` (syllable counting / metre), `Recursos` (literary devices), `Rima` (rhyme).
// - `com.tuapp.ui.theme.*`: text styles for verses and the colours used for rhymes.
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tuapp.analisis.AnalisisPoema
import com.tuapp.analisis.Metrica
import com.tuapp.analisis.Recursos
import com.tuapp.analisis.Rima
import com.tuapp.ui.theme.VerseStyle
import com.tuapp.ui.theme.rhymeBackground
import androidx.compose.foundation.shape.RoundedCornerShape

// ===============================================================================================
// WHAT THIS FILE IS FOR
// -----------------------------------------------------------------------------------------------
// This file shows the poem analysis INSIDE the editor, while you write:
//   - `VerseEditor`: the big text field where you write the verses. On its right it has a
//     margin with, for each verse, its number of metrical syllables and its rhyme letter
//     (A, B, C...). It can also paint rhyme endings with highlighter colours, and mark the
//     words of a selected literary device (background, underline or dotted line).
//   - `VerseMargin`: that right-hand margin (syllables + rhyme letter).
//   - `drawBackground` / `drawHighlight`: the code that paints the marks behind the text.
//   - `rangeShifter`: keeps marks in the right place while the analysis is catching up.
//   - `AnalysisPanel`: the panel at the bottom ("Endecasílabo · ABBA · 2 recursos") that opens
//     into the seseo switch, a rhyme-colour legend and the list of literary devices.
//   - `DeviceRow`: one row of that list.
//   - `summary`, `verseNumbers`, `RhymeLegend`: small helpers for the panel.
//
// HOW IT CONNECTS TO OTHER FILES
//   - `EditorScreen.kt` uses `VerseEditor`, `AnalysisPanel` and `highlightFor`.
//   - `DeviceInfo.kt` has `DeviceInfoDialog`, opened by the (i) button of `DeviceRow`.
//   - `EditorViewModel.kt` computes the analysis (`AnalisisPoema.Resultado`) that arrives here.
//   - `analisis/` is the analysis engine (names in Spanish). The key pieces used here:
//     `AnalisisPoema.Resultado` (the full result: `texto` = analysed text, `lineas` = lines,
//     `recursos` = devices, `metro` = dominant metre, `esquema` = rhyme scheme like "ABBA"),
//     `AnalisisPoema.Linea` (one line: `inicio`/`fin` = start/end position in the text,
//     `silabas` = syllable count, `encaja` = "fits the metre", `rima` = its rhyme),
//     `Recursos.Recurso` (a device: `tipo` = type, `lineas` = line numbers, `evidencia` =
//     evidence text, `clara` = clear or only possible).
//   - `ui/theme/RhymeColors.kt`: `rhymeBackground(group, type)`, the colour of each rhyme group.
//
// KEY COMPOSE IDEAS USED HERE
// * `@Composable` function: describes a piece of screen. Compose re-runs it ("recomposition")
//   whenever state it read has changed.
// * State: `mutableStateOf(x)` is a watched box; `remember { ... }` keeps it between runs;
//   `rememberSaveable` also keeps it when the phone rotates.
// * `Modifier`: a chain of adjustments applied to an element (size, padding, drawing, clicks...),
//   in order from left to right.
// * `LaunchedEffect(keys) { ... }`: runs a coroutine (work that may wait) when the keys change.
// * `TextLayoutResult`: after Compose lays out a text, it gives this object. It is a "map" of
//   the text on screen: which visual line each character is on, and where (in pixels) each line
//   starts, ends and has its baseline (the invisible line letters sit on). We use it to put the
//   margin marks and the highlights exactly next to or under the right words.
// * `drawBehind { ... }`: a modifier that lets us paint freely (rectangles, lines) BEHIND an
//   element's content, like drawing on the paper before the ink.
//
// A FEW WORDS ON POSITIONS
//   Text positions are counted in characters from the start of the text: the first character is
//   0, the second 1, etc. (an "offset"). A range like `3..7` means "characters 3 to 7, both
//   included". Screen positions are in pixels; `dp` is a size unit that looks the same on every
//   screen, and `toPx()` converts dp into pixels.
// ===============================================================================================

/**
 * How a selected literary device is marked on the text.
 *
 * `enum class`: a type with a fixed list of possible values (an "enumeration"), like the options
 * in a menu. Here there are three:
 * - `BACKGROUND`: a soft coloured rectangle behind the words (most devices);
 * - `UNDERLINE`: a solid line under the words (a clear alliteration);
 * - `DOTTED`: a dashed line under the words (a possible alliteration).
 * (*Aliteración* = alliteration: the same consonant sound repeated in nearby words.)
 */
enum class HighlightStyle { BACKGROUND, UNDERLINE, DOTTED }

/**
 * What to highlight for a device: the character ranges to mark and the style to use.
 *
 * `data class`: a class made just to hold data; Kotlin adds comparing, copying (`copy`) and
 * printing for free. `val` = read-only field.
 * - `ranges: List<IntRange>`: a list of ranges of positions in the text (for example `[3..7,
 *   20..25]`). `List<...>` is a generic type: "a list of" whatever is in `< >`.
 * - `style: HighlightStyle`: one of the three styles above.
 */
data class Highlight(val ranges: List<IntRange>, val style: HighlightStyle)

/**
 * Translates a device (`Recursos.Recurso`) into a [Highlight]: which ranges and which style.
 *
 * - `fun highlightFor(...) = Highlight(...)`: a function whose body is a single expression after
 *   `=`; it returns that value.
 * - `AnalisisPoema.rangos(analysis, device)`: asks the engine for the character ranges of the
 *   words involved in this device (*rangos* = ranges).
 * - `when { condition -> value ... }`: checks the conditions in order and gives the value of the
 *   first true one (like a chain of if / else if / else):
 *   - not an alliteration (`!=` = "is not equal to") -> background;
 *   - a clear alliteration (`device.clara`) -> solid underline;
 *   - otherwise (a possible alliteration) -> dotted line.
 *
 * @param analysis the full analysis result.
 * @param device the device the person selected.
 * @return the ranges and style to draw.
 */
fun highlightFor(analysis: AnalisisPoema.Resultado, device: Recursos.Recurso) = Highlight(
    AnalisisPoema.rangos(analysis, device),
    when {
        device.tipo != Recursos.Tipo.ALITERACION -> HighlightStyle.BACKGROUND
        device.clara -> HighlightStyle.UNDERLINE
        else -> HighlightStyle.DOTTED
    }
)

// Space above the text inside the verse field. It is shared by the field and the margin so their
// rows line up exactly. `private val` = a fixed value only visible in this file.
private val TEXT_TOP_PADDING = 8.dp
// Width of the right-hand margin with syllables and rhyme letters. The field leaves the same
// amount of empty space on its right so the text never runs under the margin.
private val MARGIN_WIDTH = 56.dp

/**
 * The verse field. With [analysis], it shows the syllables and rhyme letter of each line in the
 * right margin, lined up with the line's LAST visual row (a long verse can wrap over several
 * rows on screen, and the rhyme is at its end). With [colorRhymes], each rhyme ending gets a
 * highlighter background in its group's colour.
 *
 * While the analysis lags behind the text (it arrives 300 ms after you stop typing), the margin
 * stays if the number of lines has not changed, and highlighted ranges are shifted past the edit
 * (those touching the edited part are hidden until the new analysis arrives).
 *
 * Why the field has no scroll of its own: title and verses sit in ONE scrolling column (in
 * `EditorScreen`). That makes it easy to place the margin using the field's own coordinates,
 * but it means Compose does not keep the cursor visible by itself; this function does it by
 * hand (see the `LaunchedEffect` below).
 *
 * - `@OptIn(ExperimentalFoundationApi::class)`: accepts using an experimental feature
 *   (`BringIntoViewRequester`).
 *
 * @param value the current text (the note's verses).
 * @param onValueChange called with the new text whenever the person types. `(String) -> Unit` =
 *   "a function that receives a text and returns nothing".
 * @param placeholder grey hint text shown while the field is empty.
 * @param analysis the latest analysis, or `null` if there is none (or the analysis is off).
 * @param highlight what to highlight for the selected device, or `null`.
 * @param showMargin whether to reserve and show the syllables / rhyme margin.
 * @param colorRhymes whether to paint rhyme endings in colour.
 * @param onLayout called with each new `TextLayoutResult`, so the screen can scroll to a device.
 * @param modifier extra adjustments from the caller. `= Modifier` is a default: "no extras".
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VerseEditor(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    analysis: AnalisisPoema.Resultado?,
    highlight: Highlight?,
    showMargin: Boolean,
    colorRhymes: Boolean,
    onLayout: (TextLayoutResult) -> Unit,
    modifier: Modifier = Modifier
) {
    // The latest text layout of the field (null until it is first drawn). It is state, so
    // anything that reads it (the margin, the drawing) is updated when the layout changes.
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    // The theme's colour palette (light or dark).
    val colors = MaterialTheme.colorScheme
    // The analysis to use for the margin, only if it has the SAME number of lines as the
    // current text. `value.count { c -> c == '\n' } + 1` counts line breaks and adds one (3 line
    // breaks = 4 lines). `takeIf { ... }` gives the value if the condition is true, else null.
    // `?.` makes the whole thing null if `analysis` is null. Typing inside a verse does not move
    // the verses, so the margin stays; pressing Enter adds a line, so the margin hides for a
    // moment until the new analysis arrives (otherwise marks would sit next to the wrong verse).
    val margin = analysis?.takeIf { it.lineas.size == value.count { c -> c == '\n' } + 1 }

    // Ranges from the analysed text, moved to where they are in the current text.
    // `shift` is a function (built by `rangeShifter`) that converts a range in the old analysed
    // text into the same characters in the current text, or null if they were edited.
    // `remember(analysis?.texto, value) { ... }` only rebuilds it when the analysed text or the
    // current text change. `analysis?.let { ... }`: only if there is an analysis; `it` is it.
    val shift = remember(analysis?.texto, value) { analysis?.let { rangeShifter(it.texto, value) } }
    // The selected device's highlight, with its ranges shifted. Read it as: "if there is a
    // highlight `h` AND a shift function `s`, make a copy of `h` whose ranges are the old ones
    // passed through `s`". `mapNotNull(s)` applies `s` to each range and drops the nulls (ranges
    // that touch the edit). `h.copy(ranges = ...)` = the same Highlight with new ranges.
    val shiftedHighlight = highlight?.let { h -> shift?.let { s -> h.copy(ranges = h.ranges.mapNotNull(s)) } }
    // The rhyme "spans" to colour: the pieces of text that rhyme (only the rhyming ending, not the
    // whole word), each with its rhyme group and type (*consonante* = full rhyme, *asonante* =
    // vowel-only rhyme). Only computed if rhyme colouring is on and there is an analysis;
    // otherwise an empty list. Remembered until `analysis` or `colorRhymes` change.
    // (*tramosDeRima* = "rhyme spans".)
    val rhymeSpans = remember(analysis, colorRhymes) {
        if (colorRhymes && analysis != null) AnalisisPoema.tramosDeRima(analysis) else emptyList()
    }
    // For each rhyme span: shift its range to the current text and pair it with its fill colour.
    // `a to b` makes a pair `(range, colour)`. `shift?.invoke(span.rango)` calls the `shift`
    // function (if there is one) on the span's range; if the result is null (no shift function,
    // or the span touches the edit), `?.let` is skipped and `mapNotNull` drops it.
    val rhymeFills = rhymeSpans.mapNotNull { span ->
        shift?.invoke(span.rango)?.let { it to rhymeBackground(span.grupo, span.tipo) }
    }

    // Own TextFieldValue, so we know where the cursor is.
    // The ViewModel only keeps the text (`String`). A `TextFieldValue` is the text PLUS the
    // selection/cursor position (`TextRange`). We start with the cursor at the end of the text.
    var field by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    // If the text changed from outside (for example, when the note finishes loading), reset
    // the field to that text with the cursor at the end.
    if (field.text != value) field = TextFieldValue(value, TextRange(value.length))

    // The field has no scroll of its own: the cursor must be brought into view by hand.
    // `BringIntoViewRequester` is a helper that can ask the scrolling parent: "please scroll so
    // that this rectangle of me is visible".
    val bringIntoView = remember { BringIntoViewRequester() }
    // Whether the field has the keyboard focus (the person is writing in it).
    var focused by remember { mutableStateOf(false) }
    // 32 dp converted to pixels: extra room to keep visible above and below the cursor.
    // `with(LocalDensity.current) { ... }` lets us use `toPx()` with this screen's density.
    val slack = with(LocalDensity.current) { 32.dp.toPx() }
    // Every time the cursor moves, the layout changes, or the focus changes...
    LaunchedEffect(field.selection, layout, focused) {
        // ...stop if there is no layout yet (`?: return@LaunchedEffect` leaves this block)...
        val l = layout ?: return@LaunchedEffect
        // ...or if the field is not focused (no need to chase the cursor)...
        if (!focused) return@LaunchedEffect
        // ...find the cursor's rectangle on screen. `field.selection.end` is the cursor position;
        // `coerceIn(0, length)` keeps it within the text, just in case.
        val cursor = l.getCursorRect(field.selection.end.coerceIn(0, l.layoutInput.text.length))
        // ...and ask the parent to scroll so that the cursor, plus 32 dp above and below, is
        // visible (above the keyboard). `Rect(left, top, right, bottom)` is a rectangle.
        bringIntoView.bringIntoView(Rect(cursor.left, cursor.top - slack, cursor.right, cursor.bottom + slack))
    }

    // A `Box` stacks its children on top of each other: the text field, and on top of its right
    // edge, the margin.
    Box(modifier.fillMaxWidth()) {
        // `BasicTextField` is Compose's plain text field, with no built-in decoration. It gives
        // us full control over drawing.
        BasicTextField(
            // What it shows: our own `field` (text + cursor).
            value = field,
            // When the person types or moves the cursor, Compose calls this with the new value.
            onValueChange = { new ->
                // Keep the new text + cursor.
                field = new
                // If the TEXT really changed (not just the cursor), tell the caller, which tells
                // the ViewModel (it saves and re-analyses).
                if (new.text != value) onValueChange(new.text)
            },
            // Verse text style, in the theme's normal text colour.
            textStyle = VerseStyle.body.copy(color = colors.onSurface),
            // The blinking cursor is painted in the theme's main colour.
            cursorBrush = SolidColor(colors.primary),
            // The keyboard starts sentences with a capital letter.
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            // Each time the text is laid out: keep the layout here AND pass it up to the screen.
            onTextLayout = { layout = it; onLayout(it) },
            // `decorationBox` wraps the real text area (`inner`) with extra things: here the
            // grey hint text, shown behind the text area while the text is empty.
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, style = VerseStyle.body, color = colors.onSurfaceVariant)
                    // Draw the real text area (with the cursor).
                    inner()
                }
            },
            // The modifier chain (applied in order):
            modifier = Modifier
                // Full width...
                .fillMaxWidth()
                // ...at least 320 dp tall, so you can tap anywhere on the page even when empty...
                .heightIn(min = 320.dp)
                // ...remember whether it has the focus (updates `focused`)...
                .onFocusChanged { focused = it.hasFocus }
                // ...inner spacing: 16 dp left, 8 dp top, 24 dp bottom, and on the right either
                // the margin width (when the margin is shown) or 16 dp...
                .padding(
                    start = 16.dp, top = TEXT_TOP_PADDING, bottom = 24.dp,
                    end = if (showMargin) MARGIN_WIDTH else 16.dp
                )
                // ...hook up the "bring into view" helper. It comes AFTER the padding, so its
                // coordinates match the text layout's coordinates exactly...
                .bringIntoViewRequester(bringIntoView)
                // ...and paint behind the text (also after the padding, so the same
                // coordinates apply). This block runs on every redraw.
                .drawBehind {
                    // No layout yet: nothing to paint (`return@drawBehind` leaves this block).
                    val l = layout ?: return@drawBehind
                    // First, the rhyme colours. `forEach { (range, fill) -> ... }` goes through
                    // the pairs, "unpacking" each into `range` and `fill`.
                    rhymeFills.forEach { (range, fill) -> drawBackground(l, range, fill) }
                    // Then, on top, the selected device's highlight (if any), in the main colour.
                    if (shiftedHighlight != null) drawHighlight(l, shiftedHighlight, colors.primary)
                }
        )
        // Copy the state into a local `val` so Kotlin can be sure it will not change between the
        // null check and its use (a "smart cast" needs a stable value).
        val l = layout
        // Show the margin only if it is wanted, the analysis matches the text, and there is a
        // layout. `Modifier.align(Alignment.TopEnd)` puts it at the top-right corner of the Box.
        if (showMargin && margin != null && l != null) {
            VerseMargin(margin, l, colorRhymes, Modifier.align(Alignment.TopEnd))
        }
    }
}

/**
 * Maps a range of [old] to the same characters in [new], assuming a single contiguous edit
 * (what typing produces: you insert or delete in one place). Ranges before the edit stay, those
 * after it move by the length difference, and those overlapping it return null.
 *
 * How it works: compare the two texts from the start (common "prefix") and from the end
 * (common "suffix"). What is between them is the edited part. Example: old "casa azul", new
 * "casa muy azul": the prefix is "casa " and the suffix is "azul"; the edit inserted "muy ".
 *
 * - The return type `(IntRange) -> IntRange?` is a FUNCTION: "give me a range, I give you back a
 *   range or null". So `rangeShifter` builds and returns a small converter function.
 *
 * @param old the text the analysis was computed on.
 * @param new the text currently in the field.
 * @return a function that converts a range of [old] into a range of [new], or null.
 */
private fun rangeShifter(old: String, new: String): (IntRange) -> IntRange? {
    // Same text: every range stays where it is. `{ it }` is a function that returns its input.
    if (old == new) return { it }
    // The common prefix and suffix cannot be longer than the shorter text.
    val maxCommon = minOf(old.length, new.length)
    // Count how many characters at the START are equal. `old[i]` is the character at position
    // `i`. `prefix++` adds 1. The `while` loop repeats while its condition is true.
    var prefix = 0
    while (prefix < maxCommon && old[prefix] == new[prefix]) prefix++
    // Count how many characters at the END are equal, without overlapping the prefix.
    // `old.length - 1 - suffix` walks backwards from the last character.
    var suffix = 0
    while (suffix < maxCommon - prefix && old[old.length - 1 - suffix] == new[new.length - 1 - suffix]) suffix++
    // How much longer (positive) or shorter (negative) the new text is.
    val delta = new.length - old.length
    // Where the edited part ends in the OLD text (everything from here on is the common suffix).
    val editEnd = old.length - suffix
    // Return the converter function. `r` is the range to convert.
    return { r ->
        when {
            // Entirely before the edit (`r.last` = its last character): unchanged.
            r.last < prefix -> r
            // Entirely after the edit (`r.first` = its first character): moved by `delta`.
            // `a..b` builds a new range from `a` to `b`, both included.
            r.first >= editEnd -> (r.first + delta)..(r.last + delta)
            // It touches the edited part: we cannot know where it is now, so hide it (null).
            else -> null
        }
    }
}

/**
 * The right-hand margin: for each verse, its syllable count and rhyme letter, lined up with the
 * verse's last visual row in the text field.
 *
 * @param analysis the analysis (already checked to have the same number of lines as the text).
 * @param layout the field's text layout, to know where each verse is on screen.
 * @param colorRhymes whether to give the rhyme letter its group's background colour.
 * @param modifier where to place the margin (the caller puts it at the top-right).
 */
@Composable
private fun VerseMargin(
    analysis: AnalisisPoema.Resultado,
    layout: TextLayoutResult,
    colorRhymes: Boolean,
    modifier: Modifier
) {
    // Screen density, to convert between dp and pixels.
    val density = LocalDensity.current
    val colors = MaterialTheme.colorScheme
    // The field's top padding in whole pixels (the layout's coordinates start below it).
    val topPadding = with(density) { TEXT_TOP_PADDING.roundToPx() }
    // Length of the text that was laid out (to avoid asking for positions beyond it).
    val length = layout.layoutInput.text.length

    // A column-shaped Box, as wide as the margin. Each mark inside is placed by hand at the
    // right height with `offset`.
    Box(modifier.width(MARGIN_WIDTH)) {
        // One mark per line of the analysis.
        analysis.lineas.forEach { line ->
            // Lines with no words (blank lines) have no syllable count (`null`): skip them.
            // `return@forEach` skips to the next line (like "continue" in other languages).
            val syllables = line.silabas ?: return@forEach
            // Safety check: the line's end must exist in the laid-out text.
            if (line.fin > length) return@forEach
            // The visual row holding the verse's LAST character (`fin` = end). Using the end, a
            // verse that wraps over several rows gets its mark on the last one, next to the
            // rhyming word.
            val row = layout.getLineForOffset(line.fin)
            // The top of that row, in pixels.
            val top = layout.getLineTop(row)
            // The row's height (bottom minus top), converted from pixels to dp.
            val height = with(density) { (layout.getLineBottom(row) - top).toDp() }
            // The rhyme letter (A, B, a, b...), or null if there is no rhyme information.
            val letter = line.rima?.letra
            // Accessibility description, in Spanish like the rest of the UI. `buildString` builds
            // a text piece by piece with `append`. Examples: "11 sílabas, rima A" ("11
            // syllables, rhyme A") or "9 sílabas, no encaja en el metro" ("does not fit the
            // metre"). A letter '-' means "no rhyme". `'-'` in single quotes is one character.
            val description = buildString {
                append("$syllables sílabas")
                if (!line.encaja) append(", no encaja en el metro")
                if (letter != null && letter != '-') append(", rima $letter")
            }
            // One mark: a row with the number and the letter, aligned to the right and centred
            // vertically within the verse's row.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    // Move it down to the verse's row. `offset { IntOffset(x, y) }` shifts it by
                    // whole pixels; `top.toInt()` turns the decimal pixel value into a whole one.
                    .offset { IntOffset(0, topPadding + top.toInt()) }
                    .width(MARGIN_WIDTH)
                    // As tall as the verse's row, so the vertical centring matches the text.
                    .height(height)
                    .padding(end = 12.dp)
                    // For screen readers, replace the inner texts with the single description
                    // above (one clear sentence instead of "11" and "A" read separately).
                    .clearAndSetSemantics { contentDescription = description }
            ) {
                // The syllable count: grey if the verse fits the dominant metre, red
                // (`colors.error`) if it does not.
                Text(
                    "$syllables",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (line.encaja) colors.onSurfaceVariant else colors.error,
                    textAlign = TextAlign.End
                )
                // Does this verse rhyme with another one?
                val rhymes = letter != null && letter != '-'
                // The rhyme letter in bold and in the main colour, or a faint "·" if it does not
                // rhyme.
                Text(
                    if (rhymes) "$letter" else "·",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (rhymes) colors.primary else colors.outlineVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .width(18.dp)
                        // `.then(...)` adds another modifier to the chain. Here: if rhyme
                        // colouring is on and the verse rhymes, add a rounded background in the
                        // group's colour; otherwise add nothing (`Modifier` alone = no change).
                        // The group number comes from the letter: 'a' - 'a' = 0, 'b' - 'a' = 1...
                        // (subtracting characters gives the distance between them).
                        // `lowercaseChar()` makes 'A' and 'a' the same. `letter!!` means "I am
                        // sure this is not null" (the `!!` operator; it would crash if it were
                        // null, but `rhymes` already checked it).
                        .then(
                            if (colorRhymes && rhymes) Modifier.background(
                                rhymeBackground(letter!!.lowercaseChar() - 'a', Rima.Tipo.CONSONANTE),
                                RoundedCornerShape(4.dp)
                            ) else Modifier
                        )
                )
            }
        }
    }
}

/**
 * Highlighter-style rounded background behind the characters of [range] (used for rhyme colours).
 *
 * - `private fun DrawScope.drawBackground(...)`: an "extension function". Writing `DrawScope.`
 *   before the name adds this function to `DrawScope` (the drawing surface given by
 *   `drawBehind`), as if it were one of its own. Inside it we can call `DrawScope`'s own tools,
 *   like `drawRoundRect` and `toPx()`, directly.
 *
 * @param layout the text layout, to find where the characters are on screen.
 * @param range the characters to paint behind (both ends included).
 * @param fill the colour to paint.
 */
private fun DrawScope.drawBackground(layout: TextLayoutResult, range: IntRange, fill: Color) {
    // First character, and the position just AFTER the last one (`last + 1`), as layouts expect.
    val start = range.first
    val end = range.last + 1
    // Skip invalid ranges (negative, beyond the text, or empty). `||` means "or".
    if (start < 0 || end > layout.layoutInput.text.length || start >= end) return
    // The first and last visual rows the range covers (it may wrap over several).
    val first = layout.getLineForOffset(start)
    val last = layout.getLineForOffset(end - 1)
    // Paint one rectangle per visual row. `for (ln in first..last)` repeats for every row number
    // from `first` to `last`, both included.
    for (ln in first..last) {
        // Left edge: on the first row, where the range starts; on other rows, the row's left.
        // `getHorizontalPosition(offset, true)` = the x pixel of that character position.
        val x1 = if (ln == first) layout.getHorizontalPosition(start, true) else layout.getLineLeft(ln)
        // Right edge: on the last row, where the range ends; on other rows, the row's right.
        val x2 = if (ln == last) layout.getHorizontalPosition(end, true) else layout.getLineRight(ln)
        // Put them in order (min / max), just in case the text runs right-to-left.
        val left = minOf(x1, x2)
        val right = maxOf(x1, x2)
        // The row's baseline (the line letters sit on) and its height.
        val baseline = layout.getLineBaseline(ln)
        val lineHeight = layout.getLineBottom(ln) - layout.getLineTop(ln)
        // A rounded rectangle, a bit wider than the characters (2 dp extra each side), starting
        // at 62% of a line height above the baseline and 80% of a line height tall: it covers
        // the letters like a highlighter pen. `.62f` / `.8f` are decimal numbers (Float).
        // `Offset(x, y)` is a point, `Size(w, h)` a size, `CornerRadius` the corner roundness.
        drawRoundRect(
            fill,
            topLeft = Offset(left - 2.dp.toPx(), baseline - lineHeight * .62f),
            size = Size(right - left + 4.dp.toPx(), lineHeight * .8f),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
    }
}

/**
 * Draws the marks of the selected literary device under or behind its words: a soft background,
 * a solid underline or a dotted underline, depending on [h]'s style. It is also an extension
 * function of `DrawScope` (see `drawBackground` above).
 *
 * @param layout the text layout, to find where the characters are on screen.
 * @param h the ranges and the style.
 * @param color the colour to use (the theme's main colour, so it works in light and dark mode).
 */
private fun DrawScope.drawHighlight(layout: TextLayoutResult, h: Highlight, color: Color) {
    val length = layout.layoutInput.text.length
    // For each range to highlight...
    h.ranges.forEach { range ->
        val start = range.first
        val end = range.last + 1
        // Skip invalid ranges and go on with the next one.
        if (start < 0 || end > length || start >= end) return@forEach
        // Same row-by-row calculation as in `drawBackground`.
        val first = layout.getLineForOffset(start)
        val last = layout.getLineForOffset(end - 1)
        for (ln in first..last) {
            val x1 = if (ln == first) layout.getHorizontalPosition(start, true) else layout.getLineLeft(ln)
            val x2 = if (ln == last) layout.getHorizontalPosition(end, true) else layout.getLineRight(ln)
            val left = minOf(x1, x2)
            val right = maxOf(x1, x2)
            val baseline = layout.getLineBaseline(ln)
            // Choose the drawing by style. `when (value) { option -> ... }` compares the value
            // against each option.
            when (h.style) {
                // Soft background: the same rounded rectangle as the rhyme colours, in the main
                // colour at 16% opacity (`alpha` = how opaque; 0 invisible, 1 solid).
                HighlightStyle.BACKGROUND -> {
                    val lineHeight = layout.getLineBottom(ln) - layout.getLineTop(ln)
                    drawRoundRect(
                        color.copy(alpha = .16f),
                        topLeft = Offset(left - 2.dp.toPx(), baseline - lineHeight * .62f),
                        size = Size(right - left + 4.dp.toPx(), lineHeight * .8f),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
                // Underline (solid or dotted). Two options separated by a comma share this code.
                HighlightStyle.UNDERLINE, HighlightStyle.DOTTED -> {
                    // 4 dp below the baseline.
                    val y = baseline + 4.dp.toPx()
                    // A 2 dp thick line from left to right. For DOTTED, a "path effect" breaks it
                    // into dashes: 2 dp of line, 3 dp of gap, repeated. `floatArrayOf(a, b)` is a
                    // list of decimal numbers. For UNDERLINE, `null` = no effect (a solid line).
                    drawLine(
                        color, Offset(left, y), Offset(right, y),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = if (h.style == HighlightStyle.DOTTED)
                            PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 3.dp.toPx())) else null
                    )
                }
            }
        }
    }
}

/**
 * Bottom panel: a summary (metre and rhyme scheme) that opens into the list of detected literary
 * devices and the seseo setting. Tapping the summary opens / closes it. It closes by itself when
 * the keyboard appears, so the verses keep enough room.
 *
 * (*Seseo*: pronouncing s, z and soft c the same, as in Latin America and parts of Andalusia;
 * with it on, "casa" and "caza" rhyme.)
 *
 * - `@OptIn(ExperimentalLayoutApi::class)`: accepts an experimental feature (`isImeVisible`).
 *
 * @param analysis the latest analysis, or null.
 * @param selected the device currently selected, or null.
 * @param onSelect called when a device row is tapped.
 * @param seseo the current seseo setting.
 * @param onSeseoChange called with the new seseo value when the chip is tapped.
 * @param colorRhymes whether rhyme colouring is on (then a colour legend is shown).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalysisPanel(
    analysis: AnalisisPoema.Resultado?,
    selected: Recursos.Recurso?,
    onSelect: (Recursos.Recurso) -> Unit,
    seseo: Boolean,
    onSeseoChange: (Boolean) -> Unit,
    colorRhymes: Boolean
) {
    // Whether the panel is open. `rememberSaveable` is like `remember` but also survives the
    // screen being rebuilt (for example when the phone rotates).
    var expanded by rememberSaveable { mutableStateOf(false) }
    // With the keyboard open, the expanded panel would leave no room for the verses.
    // `WindowInsets.isImeVisible` is `true` while the on-screen keyboard is showing.
    val keyboard = WindowInsets.isImeVisible
    // Each time `keyboard` changes: if the keyboard has just appeared, close the panel. (You can
    // open it again with the keyboard visible; it only closes at the moment the keyboard shows.)
    LaunchedEffect(keyboard) { if (keyboard) expanded = false }
    val colors = MaterialTheme.colorScheme
    // The list of devices, or an empty list if there is no analysis. `orEmpty()` turns a null
    // list into an empty one.
    val devices = analysis?.recursos.orEmpty()

    // The whole panel: a column with a light background at 50% opacity.
    Column(Modifier.fillMaxWidth().background(colors.surfaceVariant.copy(alpha = .5f))) {
        // A thin line separating the panel from the verses.
        HorizontalDivider(color = colors.outlineVariant)
        // The summary row (always visible). Tapping it opens / closes the panel.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
        ) {
            // The summary text, on one line; if too long it ends with "…"
            // (`TextOverflow.Ellipsis`). `weight(1f)` makes it take all the space the arrow
            // icon leaves.
            Text(
                summary(analysis),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            // The arrow: pointing down when open, up when closed. The screen-reader description
            // says what a tap does ("Cerrar análisis" = "Close analysis", "Abrir" = "Open").
            Icon(
                if (expanded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                contentDescription = if (expanded) "Cerrar análisis" else "Abrir análisis",
                tint = colors.onSurfaceVariant
            )
        }
        // The expandable part, which slides in and out with an animation.
        AnimatedVisibility(visible = expanded) {
            Column {
                // A row with the seseo chip and, if rhymes are coloured, the colour legend.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    // Seseo on/off chip. Tapping it sends the opposite value (`!seseo`). The
                    // setting is part of what the ViewModel watches, so the analysis re-runs.
                    FilterChip(
                        selected = seseo,
                        onClick = { onSeseoChange(!seseo) },
                        label = { Text("Seseo (casa = caza)") }
                    )
                    if (colorRhymes && analysis != null) RhymeLegend(analysis)
                }
                if (devices.isEmpty()) {
                    // No devices: "No literary devices detected yet."
                    Text(
                        "No hay recursos detectados todavía.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    // The verse number of each line (blank lines do not count). `analysis!!`:
                    // "sure it is not null" (there are devices, so there is an analysis).
                    val numbers = verseNumbers(analysis!!)
                    // A scrolling list, at most 260 dp tall, with one row per device.
                    LazyColumn(Modifier.heightIn(max = 260.dp)) {
                        items(devices) { d ->
                            // `d == selected` tells the row whether it is the selected one.
                            // The last `{ onSelect(d) }` is the row's click action (trailing
                            // lambda: the last parameter written after the brackets).
                            DeviceRow(d, numbers, d == selected) { onSelect(d) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * One row of the device list: its name (and "clara"/"posible" for alliterations), where it is
 * ("verso 3", "versos 1–4" or "versos 1, 5, 9") with its evidence, and an (i) button that opens
 * the explanation dialog. The selected row has a coloured background.
 *
 * @param d the device.
 * @param numbers the verse number of each line (from `verseNumbers`); `Int?` = number or null.
 * @param selected whether this is the selected device.
 * @param onClick what to do when the row is tapped (select / unselect it).
 */
@Composable
private fun DeviceRow(d: Recursos.Recurso, numbers: List<Int?>, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    // Whether the info dialog is open.
    var showInfo by remember { mutableStateOf(false) }
    // Turn the device's line indexes into verse numbers (as the reader counts them).
    // `numbers.getOrNull(it)` = the item at that position, or null if out of range; nulls are
    // dropped by `mapNotNull`.
    val verses = d.lineas.mapNotNull { numbers.getOrNull(it) }
    // Describe where it is, in Spanish (*verso* = verse, *versos* = verses):
    val where = when {
        // No verses: nothing.
        verses.isEmpty() -> ""
        // One verse: "verso 3". `${verses[0]}` inserts the first item (lists start at 0).
        verses.size == 1 -> "verso ${verses[0]}"
        // Consecutive verses: "versos 1–4". `zipWithNext()` makes pairs of neighbours
        // ((1,2), (2,3), (3,4)); `all { (a, b) -> b == a + 1 }` checks every pair goes up by one.
        verses.zipWithNext().all { (a, b) -> b == a + 1 } -> "versos ${verses.first()}–${verses.last()}"
        // Otherwise: "versos 1, 5, 9". `joinToString(", ")` joins the numbers with commas.
        else -> "versos " + verses.joinToString(", ")
    }
    // A short extra label after the name of some devices; for the others, nothing.
    //   - alliterations: " · clara" (clear) or " · posible" (possible);
    //   - internal rhymes: " · asonante" when only the vowels match (a full rhyme gets none).
    // `when { ... }` picks the first branch whose condition is true.
    val degree = when {
        d.tipo == Recursos.Tipo.ALITERACION -> if (d.clara) " · clara" else " · posible"
        d.rima == Rima.Tipo.ASONANTE -> " · asonante"
        else -> ""
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            // Coloured background when selected, transparent otherwise.
            .background(if (selected) colors.secondaryContainer else Color.Transparent)
            // Tapping the row selects / unselects the device.
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
    ) {
        // Left part (takes all the free width): two lines of text.
        Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
            // First line: the device's name (+ degree), e.g. "Aliteración · clara".
            Text(d.tipo.nombre + degree, style = MaterialTheme.typography.titleSmall)
            // Second line: "where · evidence", leaving out the empty parts.
            Text(
                listOf(where, d.evidencia).filter { it.isNotEmpty() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
        // The (i) button: opens the explanation dialog. Its description reads "Qué es: <name>"
        // ("What it is: <name>").
        IconButton(onClick = { showInfo = true }) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = "Qué es: ${d.tipo.nombre}",
                tint = colors.onSurfaceVariant
            )
        }
    }
    // The dialog (in DeviceInfo.kt), while open. Its last parameter, the closing action, is
    // written as a trailing lambda: `{ showInfo = false }`.
    if (showInfo) DeviceInfoDialog(d, where) { showInfo = false }
}

/**
 * The panel header, shown in Spanish: "Endecasílabo · ABBA · 3 recursos" (metre name · rhyme
 * scheme · number of devices). (*Endecasílabo* = an 11-syllable verse.)
 *
 * @param a the analysis, or null.
 * @return the summary text, or an invitation to write ("Write some verses to analyse them") if
 *   there is no analysis or no dominant metre yet.
 */
private fun summary(a: AnalisisPoema.Resultado?): String {
    if (a == null || a.metro == null) return "Escribe unos versos para analizarlos"
    // The metre's name from the engine (e.g. "endecasílabo"), with its first letter capitalised.
    val metre = Metrica.nombreMetro(a.metro).replaceFirstChar { it.uppercase() }
    // How many devices, in words: "sin recursos" (none), "1 recurso", "N recursos".
    // `when (n) { 0 -> ...; 1 -> ...; else -> ... }` picks by the value of `n`.
    val n = a.recursos.size
    val devices = when (n) { 0 -> "sin recursos"; 1 -> "1 recurso"; else -> "$n recursos" }
    // Glue the three parts with " · ". `${a.esquema}` inserts the rhyme scheme ("ABBA ABBA").
    return "$metre · ${a.esquema} · $devices"
}

/**
 * The verse number (counting only lines with words) for each line of the text. Blank lines get
 * null. Example: lines ["uno", "", "dos"] give [1, null, 2].
 *
 * `++n` adds 1 to `n` and gives the NEW value. `map { ... }` builds a new list by transforming
 * each line.
 */
private fun verseNumbers(a: AnalisisPoema.Resultado): List<Int?> {
    var n = 0
    return a.lineas.map { if (it.silabas != null) ++n else null }
}

/**
 * The rhyme letters in use, each on its colour, as a legend for the coloured text.
 * It returns early (shows nothing) if no verse rhymes.
 */
@Composable
private fun RhymeLegend(analysis: AnalisisPoema.Resultado) {
    // For each line, take its rhyme only if it really rhymes (`tipo != null`), then its letter;
    // drop the nulls (`mapNotNull`) and the repeats (`distinct()`). Result: e.g. ['A', 'B'].
    val letters = analysis.lineas.mapNotNull { l -> l.rima?.takeIf { it.tipo != null }?.letra }.distinct()
    if (letters.isEmpty()) return
    // A row of small letters, 4 dp apart.
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        letters.forEach { letter ->
            // Each letter in bold, 20 dp wide, on a rounded background of its group's colour
            // (same colour calculation as in the margin: 'a' = group 0, 'b' = group 1...).
            Text(
                "$letter",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(20.dp)
                    .background(
                        rhymeBackground(letter.lowercaseChar() - 'a', Rima.Tipo.CONSONANTE),
                        RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}
