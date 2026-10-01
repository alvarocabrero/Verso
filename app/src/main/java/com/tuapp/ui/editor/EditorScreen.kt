// ===============================================================================================
// FILE: EditorScreen.kt
// -----------------------------------------------------------------------------------------------
// The `package` line says which "logical folder" this file belongs to. Everything declared here
// has the full name `com.tuapp.ui.editor.Something` (it matches the folder `ui/editor/`).
// ===============================================================================================
package com.tuapp.ui.editor

// `import` lines bring in names from other libraries or other files so they can be used here by
// their short name (for example `Text(...)` instead of `androidx.compose.material3.Text(...)`).
// Almost all of them are Jetpack Compose building blocks:
// - `androidx.compose.foundation.*`: basic pieces (layouts like Row/Column/Box, scrolling,
//   backgrounds, borders, clicks, lazy lists).
// - `androidx.compose.material3.*`: ready-made widgets in Google's "Material 3" style (top bar,
//   buttons, chips, text fields, bottom sheet).
// - `androidx.compose.material.icons.*`: icon images (back arrow, pin, palette, bin...).
// - `androidx.compose.runtime.*`: state and effects (`remember`, `mutableStateOf`,
//   `LaunchedEffect`).
// - `androidx.compose.ui.*`: `Modifier`, colours, measuring, accessibility (semantics), units.
// - `androidx.lifecycle.*`: getting the ViewModel and reading Flows safely from the screen.
// - `com.tuapp.*`: this app's own pieces (audio rows and dialogs, colours, text styles).
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuapp.audio.AudioFormat
import com.tuapp.data.Audio
import com.tuapp.data.NoteType
import com.tuapp.ui.audios.AudioRow
import com.tuapp.ui.audios.LinkDialog
import com.tuapp.ui.theme.NotePalette
import com.tuapp.ui.theme.VerseStyle
import com.tuapp.ui.theme.noteBackground

// ===============================================================================================
// WHAT THIS FILE IS FOR
// -----------------------------------------------------------------------------------------------
// This is the note editor SCREEN: what you see when you open or create a poem or a song. From
// top to bottom it has:
//   - a top bar: back arrow, "#" (show/hide analysis), brush (colour rhymes), pin, palette
//     (note colour) and bin (delete);
//   - an optional row of colour circles (when the palette button is on);
//   - the "Poema" / "Canción" (poem / song) chips and an "Audios" chip;
//   - the title field and, below it, the verse field with its analysis margin;
//   - the analysis panel at the bottom (summary, seseo switch, list of literary devices).
// It also contains the bottom sheet that lists the note's audios.
//
// HOW IT CONNECTS TO OTHER FILES
//   - `EditorViewModel.kt`: holds the data and does the work (autosave, analysis). This screen
//     only reads its values and calls its functions.
//   - `AnalysisEditor.kt`: `VerseEditor` (the verse field with margin and highlights),
//     `AnalysisPanel` (the bottom panel) and `highlightFor` (how to mark a device).
//   - `ui/audios/`: `AudioRow` (one audio with its play button) and `LinkDialog` (choose audios).
//   - `ui/theme/`: `NotePalette` / `noteBackground` (note colours) and `VerseStyle` (text styles).
//   - `MainActivity.kt`: opens this screen for the route "editor/{id}".
//
// JETPACK COMPOSE IN A FEW WORDS (read this first)
// * A `@Composable` function describes a piece of screen. It does not "return" the screen: it
//   "emits" UI elements (texts, buttons...) by calling other composables inside it.
// * Recomposition: when a piece of STATE that a composable read changes, Compose runs that
//   composable again to update the screen. So these functions can run many times per second;
//   they must be quick and should only describe the UI.
// * State: `mutableStateOf(x)` is a box with a value that Compose watches. Reading it inside a
//   composable "subscribes" to it; changing it triggers recomposition.
// * `remember { ... }`: keeps a value between recompositions. Without it, a `mutableStateOf`
//   would be created again (and reset) every time the function runs.
// * `Modifier`: a chain of instructions that adjust an element: size, padding (inner space),
//   background, border, click behaviour, scrolling... Order matters: they apply from left to
//   right, like layers.
// * Layouts: `Column` stacks children vertically, `Row` horizontally, `Box` on top of each other.
// * `LaunchedEffect(key) { ... }`: runs a block of "side-effect" work (like scrolling with an
//   animation) in a coroutine tied to the screen. It runs once when it first appears, and again
//   every time `key` changes.
// ===============================================================================================

/**
 * The whole note editor screen.
 *
 * SYNTAX AND CONCEPTS:
 * - `@OptIn(ExperimentalMaterial3Api::class)`: "I accept using parts of Material 3 marked as
 *   experimental" (here `TopAppBar` and `ModalBottomSheet`). Without it the compiler complains.
 * - `@Composable`: this function describes UI (see the notes at the top of the file).
 * - `fun EditorScreen(...)`: declares the function. It is public (no `private`), because
 *   `MainActivity.kt` calls it.
 *
 * @param onBack a function (lambda) to call to leave the editor and go back to the list. Its
 *   type `() -> Unit` means "takes nothing, returns nothing".
 * @param vm the screen's ViewModel. `= viewModel(factory = EditorViewModel.Factory)` is a
 *   default value: if the caller does not pass one, Android gives the ViewModel for this screen
 *   (creating it with the factory the first time, and reusing it after a rotation).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onBack: () -> Unit,
    vm: EditorViewModel = viewModel(factory = EditorViewModel.Factory)
) {
    // Whether the row of colour circles is open. `var x by remember { mutableStateOf(false) }`:
    // - `mutableStateOf(false)`: a state box starting at `false`;
    // - `remember { ... }`: keep that same box across recompositions (do not reset it);
    // - `by`: read and write `showColors` directly instead of `box.value`;
    // - `var`: it can change (`val` would be read-only).
    var showColors by remember { mutableStateOf(false) }
    // Whether the audios bottom sheet is open.
    var showAudios by remember { mutableStateOf(false) }
    // The list of audios linked to this note. `vm.noteAudios` is a StateFlow (a stream of values
    // over time). `collectAsStateWithLifecycle()` turns it into Compose state, so the screen
    // redraws when the list changes, and it stops listening while the app is in the background.
    // `val ... by` reads the current value directly.
    val noteAudios by vm.noteAudios.collectAsStateWithLifecycle()

    // `Scaffold` is the standard page frame of Material Design: it places a top bar, the content
    // and other standard parts in the right spots.
    Scaffold(
        // Page background: the note's colour (`noteBackground` picks the light or dark version).
        containerColor = noteBackground(vm.color),
        // The top bar. The braces `{ ... }` are a lambda: a piece of UI passed as a parameter.
        topBar = {
            TopAppBar(
                // No title text in the bar (`{}` = an empty piece of UI).
                title = {},
                // The icon on the left: a back arrow that calls `onBack`.
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        // `contentDescription` is what screen readers say aloud ("Volver" =
                        // "Back"), for blind or low-vision users. User-facing text is Spanish.
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                // The icons on the right side of the bar.
                actions = {
                    // Is the analysis currently shown? (A saved setting.)
                    val show = vm.preferences.showAnalysis
                    // "#" button: toggles the analysis. `!show` = the opposite of `show`.
                    IconButton(onClick = { vm.preferences.updateShowAnalysis(!show) }) {
                        // `if (show) a else b` is used as a value: a filled icon when on, an
                        // outlined one when off. The description and the tint (colour) change
                        // too: main theme colour when on, normal content colour when off.
                        // `LocalContentColor.current` = "the colour content normally has here".
                        Icon(
                            if (show) Icons.Filled.Numbers else Icons.Outlined.Numbers,
                            contentDescription = if (show) "Ocultar análisis" else "Mostrar análisis",
                            tint = if (show) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    // The brush button only appears when the analysis is shown.
                    if (show) {
                        // Is "colour the rhymes" switched on?
                        val colored = vm.preferences.colorRhymes
                        // Brush button: toggles rhyme colouring (remembered between sessions).
                        IconButton(onClick = { vm.preferences.updateColorRhymes(!colored) }) {
                            Icon(
                                if (colored) Icons.Filled.Brush else Icons.Outlined.Brush,
                                contentDescription = if (colored) "Quitar colores de rima" else "Colorear rimas",
                                tint = if (colored) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                    }
                    // Pin button. `vm::togglePinned` is a "function reference": it passes the
                    // function itself (to be called later on click), without calling it now.
                    IconButton(onClick = vm::togglePinned) {
                        // Filled pin if pinned; the description says what a tap will do
                        // ("Desfijar" = "Unpin", "Fijar" = "Pin").
                        Icon(
                            if (vm.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (vm.pinned) "Desfijar" else "Fijar"
                        )
                    }
                    // Palette button: opens or closes the row of colour circles.
                    IconButton(onClick = { showColors = !showColors }) {
                        Icon(Icons.Outlined.Palette, contentDescription = "Cambiar color")
                    }
                    // Bin button: delete the note, then leave the screen. `;` separates the two
                    // actions written on one line.
                    IconButton(onClick = { vm.delete(); onBack() }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Borrar nota")
                    }
                },
                // Transparent bar, so the note's colour shows behind it.
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    // The last lambda can go outside the brackets (a Kotlin rule called "trailing lambda").
    // This is the page content. `padding ->` names the lambda's parameter: the space that the
    // Scaffold asks us to leave free (under the top bar, above the system bars).
    ) { padding ->
        // Whether the analysis is shown (setting).
        val showAnalysis = vm.preferences.showAnalysis
        // The analysis to use: the latest result if the analysis is shown, otherwise `null`.
        val analysis = if (showAnalysis) vm.analysis else null
        // The literary device the person selected in the panel (or `null`).
        val selected = vm.selectedDevice
        // How to highlight the selected device in the text (which character ranges, which
        // style). `remember(analysis, selected) { ... }` keeps the result and only recomputes it
        // when `analysis` or `selected` change (they are the "keys"). If either is null, there
        // is nothing to highlight.
        val highlight = remember(analysis, selected) {
            if (analysis != null && selected != null) highlightFor(analysis, selected) else null
        }
        // The scroll position of the page (title + verses scroll together as one sheet).
        val scroll = rememberScrollState()
        // Screen density, used to convert `dp` (size units) into real pixels.
        val density = LocalDensity.current
        // Where the verse field starts inside the scrolling column, in pixels (measured below).
        // `mutableFloatStateOf(0f)`: a state box for a decimal number (`0f` = 0 as a Float).
        var versesY by remember { mutableFloatStateOf(0f) }
        // The latest `TextLayoutResult` of the verse field (sent up by `VerseEditor`).
        // A `TextLayoutResult` is Compose's "map" of how the text was laid out: which visual
        // line each character is on and at which pixel positions. `<TextLayoutResult?>` says
        // the box holds "a layout or null" (null until the field has been drawn once).
        var versesLayout by remember { mutableStateOf<TextLayoutResult?>(null) }

        // When a device is selected, scroll its first verse into view.
        // `LaunchedEffect(selected)` runs this block in a coroutine each time `selected`
        // changes (and cancels the previous run if it was still going).
        LaunchedEffect(selected) {
            // `?: return@LaunchedEffect`: if there is no layout yet, stop here ("return from the
            // LaunchedEffect block"; the `@` label says which block to leave).
            val l = versesLayout ?: return@LaunchedEffect
            // The first character of the first highlighted range. The chain of `?.` stops
            // safely at any null (no highlight, or no ranges); in that case, stop here too.
            // `firstOrNull()` = the first item, or null if the list is empty. `.first` on a
            // range is its starting position.
            val start = highlight?.ranges?.firstOrNull()?.first ?: return@LaunchedEffect
            // Safety check: the position must exist in the text that was laid out.
            if (start > l.layoutInput.text.length) return@LaunchedEffect
            // Pixel height to scroll to: where the field starts + the top of the visual line
            // holding `start`, minus 48 dp of room above it. `getLineForOffset(start)` gives the
            // visual line number of that character, `getLineTop(line)` its top in pixels.
            // `with(density) { 48.dp.toPx() }` converts 48 dp into pixels for this screen.
            val y = versesY + l.getLineTop(l.getLineForOffset(start)) - with(density) { 48.dp.toPx() }
            // Scroll smoothly there. `toInt()` = to a whole number; `coerceAtLeast(0)` = never
            // below 0 (you cannot scroll above the top).
            scroll.animateScrollTo(y.toInt().coerceAtLeast(0))
        }

        // The main column of the page.
        Column(
            Modifier
                // Leave the space the Scaffold asked for (top bar, system bars)...
                .padding(padding)
                // ...and tell the inner parts that this space is already taken care of, so the
                // keyboard padding below is not added twice.
                .consumeWindowInsets(padding)
                // Leave room for the on-screen keyboard (IME = "input method editor"), so the
                // text you type is not hidden behind it.
                .imePadding()
                // Take all the available space.
                .fillMaxSize()
        ) {
            // The colour circles slide in and out with an animation when `showColors` changes.
            AnimatedVisibility(visible = showColors) {
                // `onSelect = vm::updateColor`: tapping a circle calls `vm.updateColor(index)`.
                ColorPicker(selected = vm.color, onSelect = vm::updateColor)
            }
            // While an existing note is still loading, show nothing more (stop this Column
            // block here). This prevents showing empty fields that would then jump.
            if (vm.loading) return@Column

            // The scrolling part: it takes all the remaining height (`weight(1f)`) and scrolls
            // vertically with the `scroll` state created above. Title and verses are inside ONE
            // scroll, so they move together like a sheet of paper.
            Column(Modifier.weight(1f).verticalScroll(scroll)) {
                // Row of chips: "Poema" / "Canción", and the "Audios" chip. Chips are small
                // rounded buttons. `Arrangement.spacedBy(8.dp)` puts 8 dp between them, and the
                // modifier adds 16 dp of space on the left and right.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    // `NoteType.entries` is the list of all values of the `NoteType` enum (POEM,
                    // SONG). `.forEach { t -> ... }` makes one chip for each, naming it `t`.
                    NoteType.entries.forEach { t ->
                        // A FilterChip looks "selected" when `selected` is true. `==` compares.
                        FilterChip(
                            selected = vm.type == t,
                            onClick = { vm.updateType(t) },
                            // `t.label` is the Spanish name ("Poema" / "Canción").
                            label = { Text(t.label) }
                        )
                    }
                    // How many audios the note has.
                    val audioCount = noteAudios.size
                    // Chip that opens the audios sheet. Its text is "Audios", "1 audio" or
                    // "N audios". `"$audioCount audios"` is a string template: `$audioCount` is
                    // replaced by the number.
                    AssistChip(
                        onClick = { showAudios = true },
                        label = { Text(if (audioCount == 0) "Audios" else if (audioCount == 1) "1 audio" else "$audioCount audios") },
                        // Headphones icon on the left. `contentDescription = null` because the
                        // chip's text already says what it is (screen readers skip the icon).
                        leadingIcon = {
                            Icon(Icons.Outlined.Headphones, contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize))
                        }
                    )
                }

                // The title field (a standard Material text field).
                TextField(
                    // What it shows: the ViewModel's title.
                    value = vm.title,
                    // What to do when the person types: `vm.updateTitle(newText)`, which also
                    // schedules the autosave.
                    onValueChange = vm::updateTitle,
                    // Grey hint shown while the title is empty ("Título" = "Title").
                    placeholder = { Text("Título", style = VerseStyle.title) },
                    textStyle = VerseStyle.title,
                    // At most 3 lines tall.
                    maxLines = 3,
                    // The keyboard starts sentences with a capital letter.
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    // No background or underline, so it looks like writing on paper.
                    colors = transparentFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                // The verse field with the analysis margin and highlights (in AnalysisEditor.kt).
                VerseEditor(
                    value = vm.content,
                    onValueChange = vm::updateContent,
                    // Hint text: "Write your lyrics here" for a song, "Write your verses
                    // here" for a poem.
                    placeholder = if (vm.type == NoteType.SONG) "Escribe aquí tu letra…" else "Escribe aquí tus versos…",
                    analysis = analysis,
                    highlight = highlight,
                    // Show the syllables / rhyme margin only when the analysis is on.
                    showMargin = showAnalysis,
                    // Colour the rhymes only if both settings are on (`&&` = "and").
                    colorRhymes = showAnalysis && vm.preferences.colorRhymes,
                    // Each time the field lays out its text, keep the layout here (used above to
                    // scroll to a device). `it` is the `TextLayoutResult` received.
                    onLayout = { versesLayout = it },
                    // `onGloballyPositioned` runs after the field is placed on screen;
                    // `it.positionInParent().y` is how far from the top of the scrolling column
                    // the field starts. Stored in `versesY` for the scroll calculation above.
                    modifier = Modifier.onGloballyPositioned { versesY = it.positionInParent().y }
                )
            }

            // The analysis panel at the bottom, outside the scroll (so it stays fixed), only if
            // the analysis is on. `vm::selectDevice` and `vm.preferences::updateSeseo` are
            // function references passed as callbacks.
            if (showAnalysis) AnalysisPanel(
                analysis = analysis,
                selected = selected,
                onSelect = vm::selectDevice,
                seseo = vm.preferences.seseo,
                onSeseoChange = vm.preferences::updateSeseo,
                colorRhymes = vm.preferences.colorRhymes
            )
        }
    }
    // The audios bottom sheet, when open. `onDismiss` closes it by setting the state to false
    // (in Compose, it disappears simply because this line stops calling it).
    if (showAudios) NoteAudiosSheet(vm, noteAudios, onDismiss = { showAudios = false })
}

/**
 * A horizontal row of coloured circles, one per note colour, to choose the note's colour.
 *
 * - `private`: only used in this file.
 * - `selected: Int`: the index of the current colour (it gets a tick and a thicker border).
 * - `onSelect: (Int) -> Unit`: a function to call with the chosen index. `(Int) -> Unit` means
 *   "a function that receives a whole number and returns nothing".
 */
@Composable
private fun ColorPicker(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        // 10 dp between circles.
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            // The row can be swiped sideways if the circles do not fit on the screen.
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // `forEachIndexed { i, c -> ... }`: go through the palette, getting both the position
        // `i` (0, 1, 2...) and the colour entry `c`.
        NotePalette.forEachIndexed { i, c ->
            // Is this the colour currently chosen?
            val isSelected = i == selected
            // One circle. `Box` can hold a child centred inside it (the tick).
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    // 36 x 36 dp...
                    .size(36.dp)
                    // ...cut into a circle...
                    .clip(CircleShape)
                    // ...filled with the same background the note will have with this colour...
                    .background(noteBackground(i))
                    // ...with a border: thicker and in the main colour if selected, thin and
                    // faint otherwise. `if` / `else` split over two lines is still one value.
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    )
                    // ...that chooses this colour when tapped...
                    .clickable { onSelect(i) }
                    // ...and that screen readers announce by the colour's name.
                    // `semantics { ... }` sets accessibility information.
                    .semantics { contentDescription = c.name }
            ) {
                // A tick inside the selected circle.
                if (isSelected) Icon(
                    Icons.Filled.Check, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Colours for a `TextField` with no background and no underline, so the title looks like it is
 * written straight onto the note. `= TextFieldDefaults.colors(...)` is a one-expression body:
 * the function returns that value. `Color.Transparent` = fully see-through.
 * It is `@Composable` because reading the theme's default colours needs Compose.
 */
@Composable
private fun transparentFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent
)

/**
 * The note's audios, in a panel that slides up from the bottom ("bottom sheet"): play them,
 * unlink them, or link more.
 *
 * @param vm the editor's ViewModel (for playing, unlinking and linking).
 * @param audios the audios linked to the note right now. `List<Audio>` = a list of `Audio`.
 * @param onDismiss what to call to close the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteAudiosSheet(vm: EditorViewModel, audios: List<Audio>, onDismiss: () -> Unit) {
    // The player's state (which audio is playing, position...), as Compose state.
    val playback by vm.player.state.collectAsStateWithLifecycle()
    // Every audio in the app, for the link dialog.
    val all by vm.allAudios.collectAsStateWithLifecycle()
    // Whether the "link audios" dialog is open.
    var linking by remember { mutableStateOf(false) }

    // The sheet itself. Swiping it down or tapping outside calls `onDismiss`.
    ModalBottomSheet(onDismissRequest = onDismiss) {
        // `navigationBarsPadding()` leaves room for the phone's navigation bar at the bottom.
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)) {
            // Heading: "Audios de la nota" ("The note's audios").
            Text(
                "Audios de la nota",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            if (audios.isEmpty()) {
                // No audios yet: a short message explaining how to add some.
                Text(
                    "Esta nota no tiene audios. Vincula grabaciones o archivos de la sección Audios.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            } else {
                // A `LazyColumn` is a scrolling list that only builds the rows visible on screen
                // (efficient for long lists). At most 420 dp tall.
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    // One row per audio. `key = { it.id }` gives each row a stable identity (its
                    // id), so Compose keeps the right row state when the list changes.
                    items(audios, key = { it.id }) { audio ->
                        // A row with the audio's name, play button and progress (shared with the
                        // Audios section).
                        AudioRow(
                            audio = audio,
                            playback = playback,
                            onTogglePlay = { vm.togglePlay(audio) },
                            onSeek = vm::seek,
                            // Extra button at the end of the row: unlink this audio.
                            // `«${audio.name}»` inserts the audio's name into the description.
                            trailing = {
                                IconButton(onClick = { vm.unlinkAudio(audio) }) {
                                    Icon(Icons.Outlined.LinkOff, contentDescription = "Desvincular «${audio.name}»")
                                }
                            }
                        )
                    }
                }
            }
            // "Vincular audios" ("Link audios") button: opens the link dialog.
            TextButton(
                onClick = { linking = true },
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Outlined.Link, contentDescription = null)
                // An 8 dp gap between the icon and the text.
                Spacer(Modifier.width(8.dp))
                Text("Vincular audios")
            }
        }
    }
    // The dialog to choose which audios are linked (a list with checkboxes, from ui/audios/).
    if (linking) LinkDialog(
        title = "Vincular audios",
        // Message when the app has no audios at all.
        empty = "Todavía no tienes audios. Grábalos o añádelos desde la sección Audios.",
        // Every audio, to choose from.
        items = all,
        // How to identify each item: by its id.
        key = { it.id },
        // The text of each item: "name · duration" (for example "Idea 3 · 1:24").
        label = { "${it.name} · ${AudioFormat.duration(it.durationMs)}" },
        // Which ones start ticked: the ids of the audios already linked.
        selected = audios.map { it.id }.toSet(),
        // On "OK": link exactly the ticked ids (`it` is that set), then close the dialog.
        onConfirm = { vm.setAudioLinks(it); linking = false },
        // On cancel: just close.
        onDismiss = { linking = false }
    )
}
