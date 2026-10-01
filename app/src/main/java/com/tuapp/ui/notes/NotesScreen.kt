// ============================================================================================
// FILE: NotesScreen.kt
// --------------------------------------------------------------------------------------------
// What is it for?
//   It draws the "Notas" (Notes) section of the home screen, in the style of Google Keep: a
//   search bar, a grid of note cards of different heights (a "staggered" grid), and a "+"
//   button to write a new note. Each card shows the title, the first lines of the poem, its
//   type (poem / song), how many audios it has and whether it is pinned.
//
// What is it connected to?
//   - ui/home/HomeScreen.kt shows it when the "Notas" tab is selected.
//   - NotesViewModel.kt gives it the notes, the search text and the audio counts.
//   - ui/components/SearchField.kt: the search bar.
//   - ui/theme/ (VerseStyle, noteBackground): text styles and card colours.
//   - `openNote` (from MainActivity's navigation) opens the editor (ui/editor/).
//
// JETPACK COMPOSE IN A NUTSHELL:
//   - `@Composable` marks a function that describes a piece of screen; calling it places it.
//   - When Compose "state" it reads changes, the function runs again ("recomposition") and
//     the screen updates by itself.
//   - `Modifier` is a chain of settings: size, padding, clicks...
//   - `Scaffold` is Material 3's page skeleton with slots (bars, floating button, content).
//   - Lazy lists/grids (`LazyVerticalStaggeredGrid`) only build the items visible on screen,
//     so they stay fast with many notes.
// ============================================================================================
package com.tuapp.ui.notes

// A thin border line (for white cards).
import androidx.compose.foundation.BorderStroke
// Layout pieces: spacing, `Box`, `Column`, `Row`, `Spacer`, sizes and padding.
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
// The staggered grid: columns where each item keeps its own height (like a brick wall of
// notes), as in Google Keep.
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
// Icons: add (+), push-pin (pinned note), headphones (has audios).
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Headphones
// Material 3: floating button, icon, theme, page skeleton, `Surface` (a coloured, shaped,
// optionally clickable card) and text.
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
// Lets us write `val x by someState` to read state directly.
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// Text styling: italic, alignment, and "…" when text is too long.
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
// Turns a StateFlow into Compose state that respects the app's lifecycle.
import androidx.lifecycle.compose.collectAsStateWithLifecycle
// Gets (or creates) this screen's ViewModel.
import androidx.lifecycle.viewmodel.compose.viewModel
// Our own classes.
import com.tuapp.data.Note
import com.tuapp.ui.components.SearchField
import com.tuapp.ui.theme.VerseStyle
import com.tuapp.ui.theme.noteBackground

/**
 * The Notes section: search bar + grid of note cards + "+" button.
 *
 * @param openNote opens a note in the editor given its id; 0 means "a new note".
 * @param vm the ViewModel. The default value asks Android for this screen's
 *   `NotesViewModel`, built with its `Factory` the first time and reused afterwards.
 */
@Composable
fun NotesScreen(
    // 0 = new note
    openNote: (Long) -> Unit,
    vm: NotesViewModel = viewModel(factory = NotesViewModel.Factory)
) {
    // Read the ViewModel's streams as Compose state: when they change, this function runs
    // again and the screen updates. `by` lets us use `notes` directly as its value.
    // `collectAsStateWithLifecycle()` also stops listening while the app is in the background.
    // - `notes`: the list (filtered by the search), or null while loading.
    val notes by vm.notes.collectAsStateWithLifecycle()
    // - `query`: the search text.
    val query by vm.query.collectAsStateWithLifecycle()
    // - `audioCounts`: how many audios each note has, by note id.
    val audioCounts by vm.audioCounts.collectAsStateWithLifecycle()

    // Page skeleton with a floating "+" button that opens a new note (id 0).
    // "Nueva nota" = "New note" (read by screen readers).
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { openNote(0L) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Icon(Icons.Filled.Add, contentDescription = "Nueva nota") }
        }
    ) { padding ->
        // Main content, respecting the space the Scaffold reserves (`padding`).
        Column(Modifier.padding(padding).fillMaxSize()) {
            // The search bar. `vm::search` passes the ViewModel's `search` function as the
            // callback. "Buscar en tus notas" = "Search your notes".
            SearchField(query, vm::search, "Buscar en tus notas")
            // Local copy so Kotlin can check for null safely below.
            val list = notes
            // `when` picks the first true branch:
            when {
                // Still loading: show nothing (`Unit` = "nothing").
                list == null -> Unit
                // No notes (or none match): show the empty-state message.
                list.isEmpty() -> EmptyState(query)
                // Otherwise: the staggered grid.
                else -> LazyVerticalStaggeredGrid(
                    // As many columns as fit, each at least 160 dp wide.
                    columns = StaggeredGridCells.Adaptive(160.dp),
                    // Space around the grid; 96 dp at the bottom so the "+" button does not
                    // cover the last cards.
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
                    // 10 dp gaps between cards, vertically and horizontally.
                    verticalItemSpacing = 10.dp,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // One card per note, identified by its id. Tapping a card opens that note.
                    // `audioCounts[note.id] ?: 0` reads the count from the dictionary, or 0 if
                    // the note has no entry (`?:` = "if null, use this instead").
                    items(list, key = { it.id }) { note ->
                        NoteCard(note, audioCounts[note.id] ?: 0, onClick = { openNote(note.id) })
                    }
                }
            }
        }
    }
}


/**
 * One note card in the grid.
 *
 * `private` = only usable inside this file.
 *
 * @param note the note to show.
 * @param audioCount how many audios are linked to it.
 * @param onClick what to do when the card is tapped (open the note).
 */
@Composable
private fun NoteCard(note: Note, audioCount: Int, onClick: () -> Unit) {
    // `Surface`: a tappable card with rounded corners (14 dp) and the note's background
    // colour. Notes with colour 0 (the default, no colour) get a thin border so they stand out
    // from the page; coloured notes do not need one (`null` = no border).
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = noteBackground(note.color),
        border = if (note.color == 0) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    ) {
        // The card's contents, stacked, with 14 dp of inner space.
        Column(Modifier.padding(14.dp)) {
            // The title, only if it has one: at most 2 lines, then "…", plus a small gap.
            if (note.title.isNotBlank()) {
                Text(note.title, style = VerseStyle.cardTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
            }
            // No title and no text at all.
            if (note.title.isBlank() && note.content.isBlank()) {
                // A note kept only for its audios: show "Sin texto" ("No text") in italics and
                // a softer colour. `.copy(...)` makes a copy of the style with italics added.
                Text(
                    "Sin texto",
                    style = VerseStyle.cardBody.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // The text of the note, if any.
            if (note.content.isNotBlank()) {
                // Line breaks are kept: a poem is not read as a paragraph.
                // Steps: `trim()` removes blank space at the start and end; `lines()` splits
                // into lines; `take(8)` keeps the first 8; `joinToString("\n")` joins them
                // back with line breaks ("\n" is the "new line" character).
                Text(
                    note.content.trim().lines().take(8).joinToString("\n"),
                    style = VerseStyle.cardBody,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(10.dp))
            // Bottom line of the card: type on the left, audio count and pin on the right.
            Row(verticalAlignment = Alignment.CenterVertically) {
                // The note type's label (e.g. "Poema" or "Canción", "Poem" / "Song").
                Text(
                    note.type.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // A flexible empty space that takes all the free width, pushing what follows
                // to the right end.
                Spacer(Modifier.weight(1f))
                // Headphones icon + number, only if the note has audios.
                if (audioCount > 0) {
                    // Screen-reader text: "1 audio" or "N audios". `$audioCount` inserts the
                    // number into the text (string template).
                    Icon(
                        Icons.Outlined.Headphones,
                        contentDescription = if (audioCount == 1) "1 audio" else "$audioCount audios",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        " $audioCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // If the pin icon follows, leave an 8 dp gap before it.
                    if (note.pinned) Spacer(Modifier.width(8.dp))
                }
                // Pin icon if the note is pinned (kept at the top of the list).
                // "Fijada" = "Pinned".
                if (note.pinned) Icon(
                    Icons.Filled.PushPin,
                    contentDescription = "Fijada",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * What is shown when there are no notes to display.
 * - No search: "La página está en blanco" ("The page is blank") and the hint "Toca + para
 *   escribir tu primer verso." ("Tap + to write your first verse.").
 * - With a search: "Ninguna nota contiene «query»" ("No note contains «query»").
 *
 * @param query the current search text.
 */
@Composable
private fun EmptyState(query: String) {
    // A box filling the space, with 32 dp padding, contents centred.
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (query.isBlank()) "La página está en blanco" else "Ninguna nota contiene «$query»",
                style = VerseStyle.empty,
                textAlign = TextAlign.Center
            )
            // The hint only when not searching.
            if (query.isBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Toca + para escribir tu primer verso.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
