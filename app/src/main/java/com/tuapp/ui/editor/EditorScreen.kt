package com.tuapp.ui.editor

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onBack: () -> Unit,
    vm: EditorViewModel = viewModel(factory = EditorViewModel.Factory)
) {
    var showColors by remember { mutableStateOf(false) }
    var showAudios by remember { mutableStateOf(false) }
    val noteAudios by vm.noteAudios.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = noteBackground(vm.color),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    val show = vm.preferences.showAnalysis
                    IconButton(onClick = { vm.preferences.updateShowAnalysis(!show) }) {
                        Icon(
                            if (show) Icons.Filled.Numbers else Icons.Outlined.Numbers,
                            contentDescription = if (show) "Ocultar análisis" else "Mostrar análisis",
                            tint = if (show) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    if (show) {
                        val colored = vm.preferences.colorRhymes
                        IconButton(onClick = { vm.preferences.updateColorRhymes(!colored) }) {
                            Icon(
                                if (colored) Icons.Filled.Brush else Icons.Outlined.Brush,
                                contentDescription = if (colored) "Quitar colores de rima" else "Colorear rimas",
                                tint = if (colored) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                    }
                    IconButton(onClick = vm::togglePinned) {
                        Icon(
                            if (vm.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (vm.pinned) "Desfijar" else "Fijar"
                        )
                    }
                    IconButton(onClick = { showColors = !showColors }) {
                        Icon(Icons.Outlined.Palette, contentDescription = "Cambiar color")
                    }
                    IconButton(onClick = { vm.delete(); onBack() }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Borrar nota")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        val showAnalysis = vm.preferences.showAnalysis
        val analysis = if (showAnalysis) vm.analysis else null
        val selected = vm.selectedDevice
        val highlight = remember(analysis, selected) {
            if (analysis != null && selected != null) highlightFor(analysis, selected) else null
        }
        val scroll = rememberScrollState()
        val density = LocalDensity.current
        var versesY by remember { mutableFloatStateOf(0f) }
        var versesLayout by remember { mutableStateOf<TextLayoutResult?>(null) }

        // When a device is selected, scroll its first verse into view
        LaunchedEffect(selected) {
            val l = versesLayout ?: return@LaunchedEffect
            val start = highlight?.ranges?.firstOrNull()?.first ?: return@LaunchedEffect
            if (start > l.layoutInput.text.length) return@LaunchedEffect
            val y = versesY + l.getLineTop(l.getLineForOffset(start)) - with(density) { 48.dp.toPx() }
            scroll.animateScrollTo(y.toInt().coerceAtLeast(0))
        }

        Column(
            Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .fillMaxSize()
        ) {
            AnimatedVisibility(visible = showColors) {
                ColorPicker(selected = vm.color, onSelect = vm::updateColor)
            }
            if (vm.loading) return@Column

            Column(Modifier.weight(1f).verticalScroll(scroll)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    NoteType.entries.forEach { t ->
                        FilterChip(
                            selected = vm.type == t,
                            onClick = { vm.updateType(t) },
                            label = { Text(t.label) }
                        )
                    }
                    val audioCount = noteAudios.size
                    AssistChip(
                        onClick = { showAudios = true },
                        label = { Text(if (audioCount == 0) "Audios" else if (audioCount == 1) "1 audio" else "$audioCount audios") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Headphones, contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize))
                        }
                    )
                }

                TextField(
                    value = vm.title,
                    onValueChange = vm::updateTitle,
                    placeholder = { Text("Título", style = VerseStyle.title) },
                    textStyle = VerseStyle.title,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = transparentFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                VerseEditor(
                    value = vm.content,
                    onValueChange = vm::updateContent,
                    placeholder = if (vm.type == NoteType.SONG) "Escribe aquí tu letra…" else "Escribe aquí tus versos…",
                    analysis = analysis,
                    highlight = highlight,
                    showMargin = showAnalysis,
                    colorRhymes = showAnalysis && vm.preferences.colorRhymes,
                    onLayout = { versesLayout = it },
                    modifier = Modifier.onGloballyPositioned { versesY = it.positionInParent().y }
                )
            }

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
    if (showAudios) NoteAudiosSheet(vm, noteAudios, onDismiss = { showAudios = false })
}

@Composable
private fun ColorPicker(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        NotePalette.forEachIndexed { i, c ->
            val isSelected = i == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(noteBackground(i))           // the same background the note will have
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    )
                    .clickable { onSelect(i) }
                    .semantics { contentDescription = c.name }
            ) {
                if (isSelected) Icon(
                    Icons.Filled.Check, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun transparentFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent
)

/** The note's audios: play them, unlink them, or link more. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteAudiosSheet(vm: EditorViewModel, audios: List<Audio>, onDismiss: () -> Unit) {
    val playback by vm.player.state.collectAsStateWithLifecycle()
    val all by vm.allAudios.collectAsStateWithLifecycle()
    var linking by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(
                "Audios de la nota",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            if (audios.isEmpty()) {
                Text(
                    "Esta nota no tiene audios. Vincula grabaciones o archivos de la sección Audios.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            } else {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(audios, key = { it.id }) { audio ->
                        AudioRow(
                            audio = audio,
                            playback = playback,
                            onTogglePlay = { vm.togglePlay(audio) },
                            onSeek = vm::seek,
                            trailing = {
                                IconButton(onClick = { vm.unlinkAudio(audio) }) {
                                    Icon(Icons.Outlined.LinkOff, contentDescription = "Desvincular «${audio.name}»")
                                }
                            }
                        )
                    }
                }
            }
            TextButton(
                onClick = { linking = true },
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Outlined.Link, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Vincular audios")
            }
        }
    }
    if (linking) LinkDialog(
        title = "Vincular audios",
        empty = "Todavía no tienes audios. Grábalos o añádelos desde la sección Audios.",
        items = all,
        key = { it.id },
        label = { "${it.name} · ${AudioFormat.duration(it.durationMs)}" },
        selected = audios.map { it.id }.toSet(),
        onConfirm = { vm.setAudioLinks(it); linking = false },
        onDismiss = { linking = false }
    )
}
