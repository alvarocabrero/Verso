// ============================================================================================
// FILE: AudiosScreen.kt
// --------------------------------------------------------------------------------------------
// What is it for?
//   It draws the "Audios" section of the home screen: a search field, the list of audios, a
//   "+" button to record or import an audio, the dialogs to rename / delete / link an audio,
//   and the recording panel that slides up from the bottom while recording.
//
// What role does it play in the app?
//   ui/home/HomeScreen.kt shows this screen when the user picks the "Audios" tab at the
//   bottom. All the decisions (search, play, record, save...) are made by AudiosViewModel.kt;
//   this file only DRAWS and passes on the user's taps.
//
// What is it connected to?
//   - AudiosViewModel.kt: the data and the actions.
//   - AudioComponents.kt: `AudioRow`, `RenameDialog`, `ConfirmDialog`, `LinkDialog`.
//   - ui/components/SearchField.kt: the search bar.
//   - ui/theme/ (VerseStyle): text styles.
//   - audio/AudioFormat.kt: formats the recording time.
//
// JETPACK COMPOSE REMINDER (more detail at the top of AudioComponents.kt):
//   - A `@Composable` function describes a piece of screen. Calling one inside another
//     places it on screen.
//   - When "state" it reads changes, Compose calls it again ("recomposition") and the screen
//     updates by itself.
//   - `remember { ... }` keeps a value between those repeated calls.
//   - `Modifier` sets size, padding, clicks, etc.
//   - `Scaffold`: a ready-made page skeleton from Material 3 with "slots" for the usual
//     parts of a screen: top bar, bottom bar, floating button, pop-up messages ("snackbars")
//     and the main content. You give it each part and it places them in the right spot.
//
// PERMISSIONS (Android concept):
//   Some things, like using the microphone, need the user's explicit permission. The app
//   declares it in AndroidManifest.xml (RECORD_AUDIO) and, the first time, must ASK the user
//   with a system pop-up. The `record()` function below checks this.
// ============================================================================================
package com.tuapp.ui.audios

// ---- Imports ----
// Android permissions: `Manifest.permission.RECORD_AUDIO` is the microphone permission;
// `PackageManager.PERMISSION_GRANTED` is the value meaning "the user said yes".
import android.Manifest
import android.content.pm.PackageManager
// "Activity result" tools: they let us open a system screen (file picker, permission pop-up)
// and receive its answer in a lambda.
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
// Layout pieces: `Box` (things stacked on top of each other), `Column`, `Row`, `Spacer`
// (empty space), sizes and paddings.
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
// Scrolling list that only builds the visible rows.
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
// Icons: add (+), microphone, three dots (more options), pause, paper clip (attach file).
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.outlined.AttachFile
// Material 3 components: buttons, drop-down menus, floating button, divider line, progress
// bar, bottom sheet, Scaffold, snackbar (short message at the bottom), text...
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
// Compose runtime: composable functions, "effects" (code that runs on appearing/changing),
// state and remember.
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// "Local" values: things Compose makes available anywhere in the screen tree, such as the
// Android `Context` or the focus manager.
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
// Units: `dp` for sizes, `sp` for font sizes (like dp, but also follows the user's font size
// setting).
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Helper to check permissions on any Android version.
import androidx.core.content.ContextCompat
// Lifecycle: tells us when the app goes to the background, comes back, etc.
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
// Turns a StateFlow into Compose state (explained below).
import androidx.lifecycle.compose.collectAsStateWithLifecycle
// `viewModel(...)`: gets (or creates the first time) the ViewModel for this screen.
import androidx.lifecycle.viewmodel.compose.viewModel
// Our own classes.
import com.tuapp.audio.AudioFormat
import com.tuapp.data.Audio
import com.tuapp.ui.components.SearchField
import com.tuapp.ui.theme.VerseStyle

/**
 * Home section with the audio clips. [openNote] opens a linked note in the editor.
 *
 * @param openNote function to call with a note id to open that note in the editor (it comes
 *   from the navigation in MainActivity, through HomeScreen).
 * @param vm the ViewModel. The default value `viewModel(factory = ...)` asks Android for this
 *   screen's `AudiosViewModel`, creating it with its `Factory` the first time and giving back
 *   the same one afterwards (so it survives screen rotations).
 */
@Composable
fun AudiosScreen(
    openNote: (Long) -> Unit,
    vm: AudiosViewModel = viewModel(factory = AudiosViewModel.Factory)
) {
    // The Android `Context`, needed below to check the microphone permission.
    // `.current` reads the value available at this point of the screen tree.
    val context = LocalContext.current
    // Read the ViewModel's data streams as Compose state.
    // `collectAsStateWithLifecycle()` subscribes to a StateFlow and gives a Compose state that
    // triggers a redraw on every new value; it also pauses listening while the app is in the
    // background, to save battery. `val x by ...` lets us use `x` directly as the value.
    // - `audios`: the (filtered) list, or null while loading.
    val audios by vm.audios.collectAsStateWithLifecycle()
    // - `query`: the search text.
    val query by vm.query.collectAsStateWithLifecycle()
    // - `links`: for each audio id, the notes it is linked to.
    val links by vm.links.collectAsStateWithLifecycle()
    // - `notes`: all the notes (for the link dialog).
    val notes by vm.notes.collectAsStateWithLifecycle()
    // - `playback`: what the player is doing now.
    val playback by vm.player.state.collectAsStateWithLifecycle()
    // The object that controls the snackbar (short message shown at the bottom).
    val snackbar = remember { SnackbarHostState() }

    // Local screen state (remembered between recompositions):
    // - `addMenu`: whether the "+" button's menu is open.
    var addMenu by remember { mutableStateOf(false) }
    // - `renaming` / `deleting` / `linking`: the audio being renamed / deleted / linked, or
    //   null when that dialog is closed. Setting one of them opens its dialog (see the end of
    //   this function).
    var renaming by remember { mutableStateOf<Audio?>(null) }
    var deleting by remember { mutableStateOf<Audio?>(null) }
    var linking by remember { mutableStateOf<Audio?>(null) }

    // Closing a dialog would otherwise hand the focus (and the keyboard) to the search bar.
    // `LaunchedEffect(a, b, c) { ... }` runs its block when this appears and again every time
    // one of its "keys" (a, b, c) changes. Here: when all three dialogs are closed, remove
    // the focus from everything so the keyboard does not pop up by itself.
    val focusManager = LocalFocusManager.current
    LaunchedEffect(renaming, deleting, linking) {
        if (renaming == null && deleting == null && linking == null) focusManager.clearFocus()
    }

    // Launcher for the system's "open document" picker. When the user picks a file, the
    // lambda receives its `uri` (or null if they cancelled) and we ask the ViewModel to
    // import it.
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.import(uri)
    }
    // Launcher for the system's permission pop-up. The lambda receives `granted` (true if the
    // user allowed the microphone); if so, recording starts.
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.startRecording()
    }
    // A small local function (declared inside another function): start recording, asking for
    // the microphone permission first if we do not have it yet.
    fun record() {
        // Do we already have the permission? (`==` compares two values)
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        // Yes -> record now. No -> show the permission pop-up (its answer goes to the
        // `micPermission` lambda above).
        if (granted) vm.startRecording() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    // When the ViewModel sets an import error, show it in a snackbar and then clear it.
    // `showSnackbar` is a `suspend` function (it waits until the message goes away), which is
    // why it must run inside a `LaunchedEffect` (effects run in a coroutine).
    LaunchedEffect(vm.importError) {
        vm.importError?.let { snackbar.showSnackbar(it); vm.clearImportError() }
    }

    // Leaving the app pauses the recording; it can be resumed or saved on return.
    // The "lifecycle" reports what happens to the screen: created, visible, hidden, etc.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // `DisposableEffect` is like `LaunchedEffect` but with a clean-up step: `onDispose { }`
    // runs when this piece leaves the screen. We use it to sign up to lifecycle events and to
    // sign off again later (otherwise we would keep listening forever).
    DisposableEffect(lifecycle) {
        // An observer that reacts to each event. `_` means "I ignore this parameter".
        // `ON_STOP` happens when the app is no longer visible (e.g. the user goes home).
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) vm.pauseRecording()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // The page skeleton: a snackbar area and a floating "+" button; the main content comes
    // in the last lambda.
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            // A `Box` so the drop-down menu appears anchored to the button.
            Box {
                // The round "+" button, in the theme's main colour. Tapping it opens the menu.
                // "Añadir audio" = "Add audio" (read by screen readers).
                FloatingActionButton(
                    onClick = { addMenu = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) { Icon(Icons.Filled.Add, contentDescription = "Añadir audio") }
                // The menu, visible while `addMenu` is true; tapping outside closes it.
                DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }) {
                    // "Grabar" (Record): close the menu and start recording.
                    DropdownMenuItem(
                        text = { Text("Grabar") },
                        leadingIcon = { Icon(Icons.Filled.Mic, contentDescription = null) },
                        onClick = { addMenu = false; record() }
                    )
                    // "Desde el dispositivo" (From the device): close the menu and open the
                    // file picker, showing only audio files (the text passed is the "any
                    // audio type" filter). `arrayOf(...)` creates an array (fixed-size list).
                    DropdownMenuItem(
                        text = { Text("Desde el dispositivo") },
                        leadingIcon = { Icon(Icons.Outlined.AttachFile, contentDescription = null) },
                        onClick = { addMenu = false; importLauncher.launch(arrayOf("audio/*")) }
                    )
                }
            }
        }
    ) { padding ->
        // Main content. `padding` is the space the Scaffold reserves for its other parts; we
        // apply it so nothing is hidden behind them.
        Column(Modifier.padding(padding).fillMaxSize()) {
            // The search bar. `vm::search` is a reference to the ViewModel's `search`
            // function, passed as the "text changed" callback. "Buscar en tus audios" =
            // "Search your audios".
            SearchField(query, vm::search, "Buscar en tus audios")
            // Copy into a local `val` so Kotlin knows it will not change in between (needed
            // for the null checks in `when` below).
            val list = audios
            when {
                // Still loading: show nothing (`Unit` = "do nothing").
                list == null -> Unit
                // No audios (or none match the search): show the empty message.
                list.isEmpty() -> EmptyAudios(query)
                // Otherwise, the scrolling list, with 96 dp of extra space at the bottom so
                // the last row is not covered by the "+" button.
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    // One row per audio; `key = { it.id }` identifies each row by its id.
                    items(list, key = { it.id }) { audio ->
                        AudioRow(
                            audio = audio,
                            playback = playback,
                            onTogglePlay = { vm.togglePlay(audio) },
                            onSeek = vm::seek,
                            // Tapping the name opens the rename dialog.
                            onNameClick = { renaming = audio },
                            // The notes linked to this audio (empty list if none).
                            notes = links[audio.id].orEmpty(),
                            onOpenNote = openNote,
                            // The "more options" menu at the right end of the row.
                            trailing = {
                                AudioMenu(
                                    onRename = { renaming = audio },
                                    onLink = { linking = audio },
                                    onDelete = { deleting = audio }
                                )
                            }
                        )
                        // A thin separator line between rows, starting 64 dp from the left.
                        HorizontalDivider(Modifier.padding(start = 64.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }

    // ---- Dialogs: each one is drawn only while its variable is not null ----
    // Syntax: `renaming?.let { a -> ... }` runs the block only if `renaming` is not null,
    // naming the audio `a` inside.
    // Rename dialog: on confirm, rename and close (`renaming = null`).
    renaming?.let { a ->
        RenameDialog(a.name, onConfirm = { vm.rename(a, it); renaming = null }, onDismiss = { renaming = null })
    }
    // Delete confirmation. Texts: "Delete «name»?", "The audio will be deleted and will no
    // longer be linked to its notes. The notes are not deleted.", button "Borrar" (Delete).
    deleting?.let { a ->
        ConfirmDialog(
            title = "¿Borrar «${a.name}»?",
            message = "Se borrará el audio y dejará de estar vinculado a sus notas. Las notas no se borran.",
            confirm = "Borrar",
            onConfirm = { vm.delete(a); deleting = null },
            onDismiss = { deleting = null }
        )
    }
    // Link dialog: "Vincular a notas" (Link to notes); if there are no notes, "You have no
    // notes yet.".
    linking?.let { a ->
        LinkDialog(
            title = "Vincular a notas",
            empty = "Todavía no tienes notas.",
            items = notes,
            // Each note is identified by its id...
            key = { it.id },
            // ...and shown by its title; if the title is blank, by the first non-blank line
            // of its content; if there is none, "Sin título" (Untitled).
            // `lineSequence()` splits the text into lines; `firstOrNull { ... }` finds the
            // first one matching the condition, or null.
            label = { it.title.ifBlank { it.content.lineSequence().firstOrNull { l -> l.isNotBlank() } ?: "Sin título" } },
            // Ticked at the start: the notes already linked to this audio.
            selected = links[a.id].orEmpty().map { it.noteId }.toSet(),
            onConfirm = { vm.setLinks(a, it); linking = null },
            onDismiss = { linking = null }
        )
    }
    // While recording, show the recording panel.
    if (vm.recording) RecordSheet(vm)
}

/**
 * The "more options" menu (three vertical dots) on each audio row: "Cambiar nombre"
 * (Rename), "Vincular a notas" (Link to notes), "Borrar" (Delete).
 *
 * `private` means it can only be used inside this file.
 *
 * @param onRename called when "Rename" is chosen.
 * @param onLink called when "Link to notes" is chosen.
 * @param onDelete called when "Delete" is chosen.
 */
@Composable
private fun AudioMenu(onRename: () -> Unit, onLink: () -> Unit, onDelete: () -> Unit) {
    // Whether the menu is open.
    var open by remember { mutableStateOf(false) }
    Box {
        // The three-dots button; "Más opciones" = "More options".
        IconButton(onClick = { open = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones") }
        // Each option first closes the menu, then calls the matching callback.
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Cambiar nombre") }, onClick = { open = false; onRename() })
            DropdownMenuItem(text = { Text("Vincular a notas") }, onClick = { open = false; onLink() })
            DropdownMenuItem(text = { Text("Borrar") }, onClick = { open = false; onDelete() })
        }
    }
}

/**
 * What is shown when the list is empty.
 * - No search: "Aún no hay audios" ("No audios yet") plus a hint: "Tap + to record an idea or
 *   add an audio from your phone."
 * - With a search: "Ningún audio se llama «query»" ("No audio is called «query»").
 *
 * @param query the current search text.
 */
@Composable
private fun EmptyAudios(query: String) {
    // A box filling the space, with 32 dp padding, whose content is centred.
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Main message, in the app's "empty" text style (see ui/theme/).
            Text(
                if (query.isBlank()) "Aún no hay audios" else "Ningún audio se llama «$query»",
                style = VerseStyle.empty,
                textAlign = TextAlign.Center
            )
            // The hint only when not searching.
            if (query.isBlank()) {
                // 8 dp of empty vertical space.
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

/**
 * Recording panel: elapsed time, input level, pause/resume, discard and save.
 *
 * It is a "modal bottom sheet": a panel that slides up from the bottom of the screen and
 * blocks the rest of the screen while it is open.
 *
 * @param vm the ViewModel, from which it reads the recording state and calls the actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordSheet(vm: AudiosViewModel) {
    // The sheet's state: fully open (no half-open position), and `confirmValueChange = {
    // false }` refuses any attempt to close it by dragging it down.
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { false })
    ModalBottomSheet(
        // Closes only with Save or Discard: tapping outside does nothing (empty lambda).
        onDismissRequest = {},
        sheetState = sheet,
        // No drag handle at the top (it cannot be dragged anyway).
        dragHandle = null
    ) {
        // Contents stacked and centred, with padding, and extra padding at the bottom so the
        // system's navigation bar does not cover the buttons.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding()
        ) {
            // Status: "En pausa" (Paused) in a soft colour, or "Grabando…" (Recording…) in
            // the error colour (red), as a warning that the microphone is on.
            Text(
                if (vm.recordingPaused) "En pausa" else "Grabando…",
                style = MaterialTheme.typography.titleMedium,
                color = if (vm.recordingPaused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(8.dp))
            // Big timer. Because `vm.elapsedMs` is Compose state updated every 100 ms, this
            // text recomposes and the time ticks forward on its own.
            Text(AudioFormat.duration(vm.elapsedMs), fontSize = 44.sp, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(16.dp))
            // Level meter: a horizontal bar filled from 0 to 1 with the microphone loudness.
            LinearProgressIndicator(
                progress = { vm.level },
                modifier = Modifier.fillMaxWidth().height(6.dp)
            )
            Spacer(Modifier.height(24.dp))
            // The three buttons side by side, 12 dp apart.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                // "Descartar" (Discard): throw the recording away.
                TextButton(onClick = vm::discardRecording) { Text("Descartar") }
                // Pause / continue button. Its icon and text change with the state:
                // paused -> microphone + "Continuar" (Continue); recording -> pause + "Pausa".
                OutlinedButton(onClick = { if (vm.recordingPaused) vm.resumeRecording() else vm.pauseRecording() }) {
                    Icon(if (vm.recordingPaused) Icons.Filled.Mic else Icons.Filled.Pause, contentDescription = null)
                    Spacer(Modifier.padding(start = 6.dp))
                    Text(if (vm.recordingPaused) "Continuar" else "Pausa")
                }
                // "Guardar" (Save): stop and save the recording.
                Button(onClick = vm::saveRecording) { Text("Guardar") }
            }
        }
    }
}
