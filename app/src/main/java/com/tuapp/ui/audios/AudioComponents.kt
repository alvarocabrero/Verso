package com.tuapp.ui.audios

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.tuapp.audio.AudioFormat
import com.tuapp.audio.PlaybackState
import com.tuapp.data.Audio
import com.tuapp.data.AudioNoteLink

/**
 * One audio: play/pause button, name, duration and date, a seek bar while it is
 * loaded, the notes it is linked to (if given) and a [trailing] slot for a menu
 * or an unlink button. All UI text is in Spanish.
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
    val colors = MaterialTheme.colorScheme
    val loaded = playback.audioId == audio.id
    val playing = loaded && playback.playing
    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = onTogglePlay) {
                Icon(
                    if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "Pausar" else "Reproducir"
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .then(if (onNameClick != null) Modifier.clickable(onClick = onNameClick) else Modifier)
            ) {
                Text(audio.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val time = if (loaded) "${AudioFormat.duration(playback.positionMs)} / ${AudioFormat.duration(audio.durationMs)}"
                           else AudioFormat.duration(audio.durationMs)
                Text(
                    "$time · ${AudioFormat.date(audio.created)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
            trailing()
        }
        if (loaded && playback.durationMs > 0) {
            Slider(
                value = playback.positionMs.toFloat().coerceIn(0f, playback.durationMs.toFloat()),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..playback.durationMs.toFloat(),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        if (notes.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = 52.dp)
            ) {
                notes.forEach { n ->
                    AssistChip(
                        onClick = { onOpenNote?.invoke(n.noteId) },
                        label = { Text(n.title.ifBlank { "Sin título" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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

/** Dialog to rename an audio. */
@Composable
fun RenameDialog(current: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    // The current name comes selected, so typing replaces it
    var field by remember { mutableStateOf(TextFieldValue(current, TextRange(0, current.length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val text = field.text
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar nombre") },
        text = {
            OutlinedTextField(
                value = field,
                onValueChange = { field = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/** Yes/no confirmation. */
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
 * A checkbox list to choose which items are linked (notes for an audio, or
 * audios for a note). [onConfirm] receives the final selection.
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
    var chosen by remember { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (items.isEmpty()) Text(empty)
            else LazyColumn(Modifier.heightIn(max = 400.dp)) {
                items(items, key = key) { item ->
                    val id = key(item)
                    val checked = id in chosen
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { chosen = if (checked) chosen - id else chosen + id }
                    ) {
                        Checkbox(checked = checked, onCheckedChange = { chosen = if (it) chosen + id else chosen - id })
                        Text(label(item), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(chosen) }, enabled = items.isNotEmpty()) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
