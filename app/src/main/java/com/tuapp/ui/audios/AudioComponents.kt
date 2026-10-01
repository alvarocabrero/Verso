// ============================================================================================
// FILE: AudioComponents.kt
// --------------------------------------------------------------------------------------------
// What is it for?
//   It holds reusable pieces of screen ("components") for audios:
//     - `AudioRow`: one row of the audio list (play/pause button, name, duration, date, seek
//       bar, linked notes).
//     - `RenameDialog`: a pop-up window to rename an audio.
//     - `ConfirmDialog`: a pop-up "are you sure?" window with yes/no buttons.
//     - `LinkDialog`: a pop-up window with a checkbox list, to choose which notes an audio is
//       linked to (or which audios a note is linked to).
//
// What is it connected to?
//   - ui/audios/AudiosScreen.kt uses all of them in the Audios section.
//   - The note editor (ui/editor/) also uses `AudioRow`, `ConfirmDialog` and `LinkDialog` to
//     show and link the audios of a note.
//   - audio/AudioFormat.kt (texts for durations and dates) and audio/AudioPlayer.kt
//     (`PlaybackState`, what is playing).
//
// JETPACK COMPOSE, THE BASICS (read this first):
//   Jetpack Compose is Android's modern way to build screens. Instead of drawing a screen
//   by hand, you write functions that DESCRIBE what the screen should look like for the
//   current data, and Compose takes care of drawing it.
//   - `@Composable`: a label (an "annotation") put on a function. It means "this function
//     describes a piece of user interface". A composable function does not return anything
//     visible in the code; calling it inside another composable "places" that piece on the
//     screen. For example, calling `Text("Hola")` puts a text there.
//   - Composables are nested like boxes inside boxes: a `Column` (things stacked top to
//     bottom) can contain a `Row` (things side by side), which contains a `Text`, etc.
//   - RECOMPOSITION: when the data a composable reads changes, Compose calls that function
//     again and updates only what changed on screen. You never "change a label by hand": you
//     change the data, and the screen follows. It is like a spreadsheet: change a cell and
//     the formulas that use it update themselves.
//   - STATE: data that, when it changes, causes a recomposition. It is created with
//     `mutableStateOf(value)`.
//   - `remember { ... }`: since a composable function is called again and again, a normal
//     variable inside it would be reset every time. `remember` keeps a value between those
//     calls (it "remembers" it while that piece stays on screen).
//   - `Modifier`: a chain of settings for a piece of screen: size, padding (inner margin),
//     click behaviour, etc. They are joined with dots: `Modifier.fillMaxWidth().padding(8.dp)`.
//     The order matters (each one wraps the next).
//   - `dp`: "density-independent pixels", a size unit that looks the same size on any
//     screen. `12.dp` means 12 of those units.
//   - Lambdas as parameters: many composables receive functions like `onClick = { ... }`:
//     code that runs when something happens (a "callback"). The type `() -> Unit` means "a
//     function that receives nothing and returns nothing" (`Unit` = "nothing useful").
//     `(Long) -> Unit` receives a `Long` number and returns nothing.
//   - Material 3 (`androidx.compose.material3`): Google's ready-made design kit: buttons,
//     dialogs, text fields, sliders... with a coherent look.
// ============================================================================================
package com.tuapp.ui.audios

// ---- Imports ----
// Layout pieces from Compose "foundation": `Column` (vertical stack), `Row` (horizontal
// line), `FlowRow` (a row that wraps onto new lines when full), `Arrangement` (spacing),
// sizes and padding modifiers, `clickable` (makes something react to taps).
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
// `LazyColumn`: a scrolling vertical list that only builds the rows visible on screen
// ("lazy"), so it stays fast with many items. `items` adds rows from a list.
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
// Keyboard settings for text fields.
import androidx.compose.foundation.text.KeyboardOptions
// Ready-made icons (notes, pause, play).
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
// Material 3 components: dialogs, chips (small rounded buttons), checkboxes, buttons, text...
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
// Compose runtime: `@Composable`, state and `remember`.
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// Alignment, Modifier, text input helpers, focus (which field receives the keyboard).
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
// Our own classes.
import com.tuapp.audio.AudioFormat
import com.tuapp.audio.PlaybackState
import com.tuapp.data.Audio
import com.tuapp.data.AudioNoteLink

/**
 * One audio: play/pause button, name, duration and date, a seek bar while it is loaded, the
 * notes it is linked to (if given) and a [trailing] slot for a menu or an unlink button. All
 * UI text is in Spanish.
 *
 * `@OptIn(ExperimentalLayoutApi::class)`: accepts using `FlowRow`, which is still marked
 * experimental. `@Composable`: this function describes a piece of screen (see the top of the
 * file).
 *
 * @param audio the audio to show.
 * @param playback what the app-wide player is doing now (to know if THIS audio is loaded).
 * @param onTogglePlay what to do when the play/pause button is tapped.
 * @param onSeek what to do when the user drags the seek bar; receives the position in ms.
 * @param modifier extra settings from the caller (convention: every component accepts one,
 *   and the default `Modifier` means "no extra settings").
 * @param onNameClick what to do when the name is tapped, or null if the name is not
 *   tappable. Syntax: `(() -> Unit)?` is "a function, or null".
 * @param notes the notes this audio is linked to (empty list by default).
 * @param onOpenNote what to do when a linked note is tapped; receives the note id.
 * @param trailing a "slot": a piece of screen chosen by the caller, drawn at the right end of
 *   the row (for example a "more options" menu). By default it is empty (`{}`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AudioRow(
    audio: Audio,
    playback: PlaybackState,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onNameClick: (() -> Unit)? = null,
    notes: List<AudioNoteLink> = emptyList(),
    onOpenNote: ((Long) -> Unit)? = null,
    trailing: @Composable () -> Unit = {}
) {
    // The theme's colour set (see ui/theme/Theme.kt).
    val colors = MaterialTheme.colorScheme
    // Is THIS audio the one loaded in the player?
    val loaded = playback.audioId == audio.id
    // Is it loaded AND sounding right now? (`&&` means "and")
    val playing = loaded && playback.playing
    // The whole row: a vertical stack, full width, with some padding around.
    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        // Top line: button + texts + trailing slot, centred vertically.
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Round button with a soft background. Its content (in the braces) is the icon:
            // "pause" if playing, "play" if not. `contentDescription` is the text read aloud
            // by screen readers for blind users ("Pausar" = Pause, "Reproducir" = Play).
            FilledTonalIconButton(onClick = onTogglePlay) {
                Icon(
                    if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "Pausar" else "Reproducir"
                )
            }
            // The texts column. `weight(1f)` makes it take all the free width between the
            // button and the trailing slot. `then(...)` adds the `clickable` modifier only if
            // `onNameClick` was given; otherwise it adds an empty `Modifier` (nothing).
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .then(if (onNameClick != null) Modifier.clickable(onClick = onNameClick) else Modifier)
            ) {
                // The audio name: at most 2 lines; if longer, it ends in "…" (Ellipsis).
                Text(audio.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                // The time text: if loaded, "position / total" (e.g. "0:42 / 2:31"); if not,
                // only the total length.
                val time = if (loaded) "${AudioFormat.duration(playback.positionMs)} / ${AudioFormat.duration(audio.durationMs)}"
                           else AudioFormat.duration(audio.durationMs)
                // Second line: time and creation date, smaller and in a softer colour.
                // `$time` inside the text is a string template: it inserts the value.
                Text(
                    "$time · ${AudioFormat.date(audio.created)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
            // Draw whatever the caller put in the trailing slot.
            trailing()
        }
        // Seek bar, only while this audio is loaded and its length is known.
        if (loaded && playback.durationMs > 0) {
            // `Slider`: a bar with a handle you can drag.
            Slider(
                // Where the handle is: the current position, kept between 0 and the length.
                value = playback.positionMs.toFloat().coerceIn(0f, playback.durationMs.toFloat()),
                // When the user drags it, tell the caller the new position (`it`) in ms.
                onValueChange = { onSeek(it.toLong()) },
                // The bar goes from 0 to the audio's length (`..` makes a range).
                valueRange = 0f..playback.durationMs.toFloat(),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        // Linked notes, only if there are any.
        if (notes.isNotEmpty()) {
            // A row of chips that wraps onto more lines if needed, 6 dp apart. It is pushed
            // 52 dp to the right so it lines up with the texts, not the button.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = 52.dp)
            ) {
                // One chip per note. `n` is the name given to each item in this lambda.
                notes.forEach { n ->
                    AssistChip(
                        // On tap, open the note if the caller allowed it. Syntax:
                        // `onOpenNote?.invoke(...)` calls the function only if it is not null.
                        onClick = { onOpenNote?.invoke(n.noteId) },
                        // The chip text: the note title, or "Sin título" ("Untitled") if the
                        // title is blank; one line at most.
                        label = { Text(n.title.ifBlank { "Sin título" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        // A small notes icon before the text. `contentDescription = null`
                        // means it is only decoration (screen readers skip it).
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Outlined.Notes, contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize))
                        }
                    )
                }
            }
        }
    }
}

/**
 * Dialog (pop-up window) to rename an audio. Title "Cambiar nombre" ("Rename"), buttons
 * "Guardar" ("Save") and "Cancelar" ("Cancel").
 *
 * @param current the current name, shown in the text field.
 * @param onConfirm called with the new name when the user taps "Guardar".
 * @param onDismiss called when the user cancels or taps outside the dialog.
 */
@Composable
fun RenameDialog(current: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    // The current name comes selected, so typing replaces it.
    // `TextFieldValue` holds the text AND the selection; `TextRange(0, current.length)` selects
    // from the first to the last character. `var ... by remember { mutableStateOf(...) }` is
    // the classic Compose recipe for a local, changeable state that survives recompositions.
    var field by remember { mutableStateOf(TextFieldValue(current, TextRange(0, current.length))) }
    // A "focus requester": a handle to ask for the keyboard focus on the text field.
    val focus = remember { FocusRequester() }
    // `LaunchedEffect(Unit) { ... }` runs a block ONCE when this piece first appears on screen
    // (the key `Unit` never changes, so it never runs again). Here: put the cursor in the
    // field, so the keyboard opens straight away.
    LaunchedEffect(Unit) { focus.requestFocus() }
    // The plain text currently typed.
    val text = field.text
    // Material's standard dialog. Its parts (title, text, buttons) are passed as small
    // composable lambdas.
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar nombre") },
        text = {
            // A text field with an outline. Each time the user types, `onValueChange` receives
            // the new value (`it`) and we store it in `field`, which triggers a recomposition
            // that shows the new text. A single line; the first letter of sentences in capitals.
            OutlinedTextField(
                value = field,
                onValueChange = { field = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )
        },
        // "Guardar" (Save) button: sends the text; disabled while the text is blank.
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text("Guardar") }
        },
        // "Cancelar" (Cancel) button: just closes.
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/**
 * Yes/no confirmation dialog (for example "Delete this audio?").
 *
 * @param title the dialog title.
 * @param message the explanation shown in the body.
 * @param confirm the label of the "yes" button (e.g. "Eliminar", "Delete").
 * @param onConfirm called when the user taps the "yes" button.
 * @param onDismiss called on "Cancelar" (Cancel) or when tapping outside.
 */
@Composable
fun ConfirmDialog(title: String, message: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/**
 * A checkbox list to choose which items are linked (notes for an audio, or audios for a
 * note). [onConfirm] receives the final selection.
 *
 * Syntax: `fun <T> LinkDialog(...)` is a GENERIC function. `T` is a placeholder for "some
 * type" chosen by the caller: it can be a list of notes or a list of audios, and the same code
 * works for both. Because the dialog does not know what a `T` is, the caller also gives:
 * - [key]: how to get the unique id (`Long`) of an item;
 * - [label]: how to get the text to show for an item.
 *
 * @param title the dialog title.
 * @param empty the text to show if there are no items at all.
 * @param items all the items that can be chosen.
 * @param selected the ids that are linked when the dialog opens.
 * @param onConfirm called with the set of chosen ids on "Guardar" (Save).
 * @param onDismiss called on "Cancelar" (Cancel) or when tapping outside.
 */
@Composable
fun <T> LinkDialog(
    title: String,
    empty: String,
    items: List<T>,
    key: (T) -> Long,
    label: (T) -> String,
    selected: Set<Long>,
    onConfirm: (Set<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    // The ids ticked so far, starting with the ones already linked. Changes are kept here
    // until the user taps "Guardar"; if they cancel, nothing is saved.
    var chosen by remember { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            // No items: show the "empty" message. Otherwise: a scrolling list at most 400 dp
            // tall.
            if (items.isEmpty()) Text(empty)
            else LazyColumn(Modifier.heightIn(max = 400.dp)) {
                // One row per item. Giving each row its `key` (the id) helps Compose keep
                // track of rows correctly when the list changes.
                items(items, key = key) { item ->
                    val id = key(item)
                    // Is this item ticked? (`in` asks whether the set contains the id)
                    val checked = id in chosen
                    // The whole row is tappable: tapping it adds the id to the set, or removes
                    // it if it was already there. Sets support `+` (add) and `-` (remove),
                    // which create a NEW set; assigning it to `chosen` triggers a redraw.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { chosen = if (checked) chosen - id else chosen + id }
                    ) {
                        // The checkbox itself does the same: `it` is true when it becomes
                        // ticked.
                        Checkbox(checked = checked, onCheckedChange = { chosen = if (it) chosen + id else chosen - id })
                        // The item's text, at most 2 lines.
                        Text(label(item), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        // "Guardar" (Save): sends the chosen ids; disabled when there are no items.
        confirmButton = {
            TextButton(onClick = { onConfirm(chosen) }, enabled = items.isNotEmpty()) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
