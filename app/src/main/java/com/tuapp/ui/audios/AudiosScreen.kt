package com.tuapp.ui.audios

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuapp.audio.AudioFormat
import com.tuapp.data.Audio
import com.tuapp.ui.components.SearchField
import com.tuapp.ui.theme.VerseStyle

/** Home section with the audio clips. [openNote] opens a linked note in the editor. */
@Composable
fun AudiosScreen(
    openNote: (Long) -> Unit,
    vm: AudiosViewModel = viewModel(factory = AudiosViewModel.Factory)
) {
    val context = LocalContext.current
    val audios by vm.audios.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val links by vm.links.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val playback by vm.player.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var addMenu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Audio?>(null) }
    var deleting by remember { mutableStateOf<Audio?>(null) }
    var linking by remember { mutableStateOf<Audio?>(null) }

    // Closing a dialog would otherwise hand the focus (and the keyboard) to the search bar
    val focusManager = LocalFocusManager.current
    LaunchedEffect(renaming, deleting, linking) {
        if (renaming == null && deleting == null && linking == null) focusManager.clearFocus()
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.import(uri)
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.startRecording()
    }
    fun record() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) vm.startRecording() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(vm.importError) {
        vm.importError?.let { snackbar.showSnackbar(it); vm.clearImportError() }
    }

    // Leaving the app pauses the recording; it can be resumed or saved on return
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) vm.pauseRecording()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { addMenu = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) { Icon(Icons.Filled.Add, contentDescription = "Añadir audio") }
                DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Grabar") },
                        leadingIcon = { Icon(Icons.Filled.Mic, contentDescription = null) },
                        onClick = { addMenu = false; record() }
                    )
                    DropdownMenuItem(
                        text = { Text("Desde el dispositivo") },
                        leadingIcon = { Icon(Icons.Outlined.AttachFile, contentDescription = null) },
                        onClick = { addMenu = false; importLauncher.launch(arrayOf("audio/*")) }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SearchField(query, vm::search, "Buscar en tus audios")
            val list = audios
            when {
                list == null -> Unit
                list.isEmpty() -> EmptyAudios(query)
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(list, key = { it.id }) { audio ->
                        AudioRow(
                            audio = audio,
                            playback = playback,
                            onTogglePlay = { vm.togglePlay(audio) },
                            onSeek = vm::seek,
                            onNameClick = { renaming = audio },
                            notes = links[audio.id].orEmpty(),
                            onOpenNote = openNote,
                            trailing = {
                                AudioMenu(
                                    onRename = { renaming = audio },
                                    onLink = { linking = audio },
                                    onDelete = { deleting = audio }
                                )
                            }
                        )
                        HorizontalDivider(Modifier.padding(start = 64.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }

    renaming?.let { a ->
        RenameDialog(a.name, onConfirm = { vm.rename(a, it); renaming = null }, onDismiss = { renaming = null })
    }
    deleting?.let { a ->
        ConfirmDialog(
            title = "¿Borrar «${a.name}»?",
            message = "Se borrará el audio y dejará de estar vinculado a sus notas. Las notas no se borran.",
            confirm = "Borrar",
            onConfirm = { vm.delete(a); deleting = null },
            onDismiss = { deleting = null }
        )
    }
    linking?.let { a ->
        LinkDialog(
            title = "Vincular a notas",
            empty = "Todavía no tienes notas.",
            items = notes,
            key = { it.id },
            label = { it.title.ifBlank { it.content.lineSequence().firstOrNull { l -> l.isNotBlank() } ?: "Sin título" } },
            selected = links[a.id].orEmpty().map { it.noteId }.toSet(),
            onConfirm = { vm.setLinks(a, it); linking = null },
            onDismiss = { linking = null }
        )
    }
    if (vm.recording) RecordSheet(vm)
}

@Composable
private fun AudioMenu(onRename: () -> Unit, onLink: () -> Unit, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Cambiar nombre") }, onClick = { open = false; onRename() })
            DropdownMenuItem(text = { Text("Vincular a notas") }, onClick = { open = false; onLink() })
            DropdownMenuItem(text = { Text("Borrar") }, onClick = { open = false; onDelete() })
        }
    }
}

@Composable
private fun EmptyAudios(query: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (query.isBlank()) "Aún no hay audios" else "Ningún audio se llama «$query»",
                style = VerseStyle.empty,
                textAlign = TextAlign.Center
            )
            if (query.isBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Toca + para grabar una idea o añadir un audio del móvil.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Recording panel: elapsed time, input level, pause/resume, discard and save. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordSheet(vm: AudiosViewModel) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { false })
    ModalBottomSheet(
        onDismissRequest = {},   // closes only with Save or Discard
        sheetState = sheet,
        dragHandle = null
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding()
        ) {
            Text(
                if (vm.recordingPaused) "En pausa" else "Grabando…",
                style = MaterialTheme.typography.titleMedium,
                color = if (vm.recordingPaused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(8.dp))
            Text(AudioFormat.duration(vm.elapsedMs), fontSize = 44.sp, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { vm.level },
                modifier = Modifier.fillMaxWidth().height(6.dp)
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = vm::discardRecording) { Text("Descartar") }
                OutlinedButton(onClick = { if (vm.recordingPaused) vm.resumeRecording() else vm.pauseRecording() }) {
                    Icon(if (vm.recordingPaused) Icons.Filled.Mic else Icons.Filled.Pause, contentDescription = null)
                    Spacer(Modifier.padding(start = 6.dp))
                    Text(if (vm.recordingPaused) "Continuar" else "Pausa")
                }
                Button(onClick = vm::saveRecording) { Text("Guardar") }
            }
        }
    }
}
