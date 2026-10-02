# Architecture

[Español](es/architecture.md)

Verso is a single-activity app built with Jetpack Compose. It is organised in three
layers, with dependencies flowing one way:

```
 ui (Compose + ViewModel)  ──►  data (Room, preferences)
          │
          └────────────────►  analisis (pure Kotlin)
```

- **`analisis`** (analysis engine) knows nothing about Android or the rest of the app. It
  takes text and returns data, so it can be tested with JUnit on the JVM, without an
  emulator. It is written in Spanish because it models Spanish prosody; see
  [analysis-engine.md](analysis-engine.md).
- **`data`** stores notes (Room) and settings (SharedPreferences).
- **`ui`** ties both together: ViewModels read and write `data` and call `analisis`.

## File index

| File | Role |
|---|---|
| `VersoApp.kt` | `Application`. Creates the database, repositories, preferences, the audio player and `appScope` |
| `MainActivity.kt` | The only activity. Applies the theme and defines navigation |
| `data/Note.kt` | Room entity `Note`, enum `NoteType` and its `Converters` |
| `data/NoteDao.kt` | Queries: observe all, search, get, insert, update, delete |
| `data/Audio.kt` | Room entities `Audio` and `NoteAudio` (note ↔ audio link), and query result classes |
| `data/AudioDao.kt` | Audio queries: list, search, rename, delete, link/unlink, audios of a note, counts |
| `data/AudioRepository.kt` | Audio files in `files/audios/`: new recording files, import from a `Uri`, duration, delete |
| `data/VersoDatabase.kt` | Room database `verso.db` (version 2) and `MIGRATION_1_2` |
| `data/NotesRepository.kt` | Facade over the DAO: `save()` inserts or updates |
| `data/Preferences.kt` | Settings as Compose state: seseo, show analysis, rhyme colouring, home tab |
| `audio/AudioRecorder.kt` | `MediaRecorder` wrapper: AAC/m4a, pause/resume, elapsed time, input level |
| `audio/AudioPlayer.kt` | App-wide `MediaPlayer` wrapper with a `StateFlow<PlaybackState>`; one audio at a time |
| `audio/AudioFormat.kt` | Pure helpers (JVM-tested): durations, dates, default recording names, file names |
| `ui/home/HomeScreen.kt` | Bottom bar with the Notes / Audios sections |
| `ui/audios/AudiosScreen.kt` | Audio list, add menu (record / from device), recording sheet, dialogs |
| `ui/audios/AudiosViewModel.kt` | Audio list and search, playback, recording, import, rename, delete, links |
| `ui/audios/AudioComponents.kt` | Shared `AudioRow` (player row), rename, confirm and link dialogs |
| `ui/components/SearchField.kt` | Search bar shared by both sections |
| `ui/notes/NotesViewModel.kt` | Debounced search and the note list as a `StateFlow` |
| `ui/notes/NotesScreen.kt` | Search bar, staggered grid, cards, empty state |
| `ui/editor/EditorViewModel.kt` | Note state, autosave and background analysis |
| `ui/editor/EditorScreen.kt` | Top bar, type, title, verses and panel |
| `ui/editor/AnalysisEditor.kt` | Verse field with margin and highlighting, and the analysis panel |
| `ui/theme/Theme.kt` | Light/dark colours and `VerseStyle` (verse typography) |
| `ui/theme/NoteColors.kt` | `NotePalette` (7 colours) and `noteBackground(index)` |
| `analisis/*.kt` | Analysis engine, see [analysis-engine.md](analysis-engine.md) |

## Dependency injection

There is no Hilt or Koin. `VersoApp` acts as the container:

```kotlin
class VersoApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val database by lazy { VersoDatabase.create(this) }
    val repository by lazy { NotesRepository(database.noteDao()) }
    val preferences by lazy { Preferences(this) }
    val audioRepository by lazy { AudioRepository(database.audioDao(), this) }
    val audioPlayer by lazy { AudioPlayer() }
}
```

Each ViewModel has a `Factory` (`viewModelFactory { initializer { … } }`) that takes what
it needs from `this[APPLICATION_KEY] as VersoApp`. To add a dependency, create it as a
`lazy` property in `VersoApp` and pass it in the `initializer` of the ViewModel that uses it.

## Navigation

`MainActivity` defines a `NavHost` with two routes:

| Route | Screen | Notes |
|---|---|---|
| `home` | `HomeScreen` (Notes / Audios) | Start destination; the section is remembered |
| `editor/{id}` | `EditorScreen` | `id` is a `Long`; `0` means a new note |

The editor reads `id` from its `SavedStateHandle`, and goes back with `navigateUp()`.
The activity uses `enableEdgeToEdge()` and `windowSoftInputMode="adjustResize"`; keyboard
insets are handled in Compose with `imePadding()`.

## Data model

```kotlin
@Entity(tableName = "notas")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "titulo") val title: String = "",
    @ColumnInfo(name = "contenido") val content: String = "",
    @ColumnInfo(name = "tipo") val type: NoteType = NoteType.POEM,   // POEM or SONG
    val color: Int = 0,                                              // index into NotePalette (0 = no colour)
    @ColumnInfo(name = "fijada") val pinned: Boolean = false,
    @ColumnInfo(name = "creada") val created: Long,                  // epoch ms
    @ColumnInfo(name = "modificada") val modified: Long              // epoch ms
)
```

- **The table and columns keep the Spanish names used by version 0.1.0**, and
  `Converters` stores `NoteType` as `"POEMA"` / `"CANCION"`, as 0.1.0 did. This way the
  code could be translated to English without a database migration: updating from 0.1.0
  keeps every note.
- List order: `fijada DESC, modificada DESC` (pinned first, then most recently modified).
- Search: `LIKE '%text%'` over title and content. SQLite's `LIKE` is case-insensitive for
  ASCII letters only, not for accented ones.
- Version 2, with the schema exported to `app/schemas/`. **Any change to the entities needs a
  version bump and a
  `Migration`** (see `MIGRATION_1_2`); otherwise the app will crash on existing installs. See
  [development.md](development.md#changing-the-data-model).

### Audios (version 2, 0.2.0)

```kotlin
@Entity(tableName = "audios")
data class Audio(id: Long, name: String, fileName: String, durationMs: Long, created: Long)

@Entity(tableName = "note_audios", primaryKeys = ["note_id", "audio_id"], /* FKs, CASCADE */)
data class NoteAudio(noteId: Long, audioId: Long)
```

- **Many to many**: a note can have several audios and an audio several notes. Both
  foreign keys use `ON DELETE CASCADE`, so deleting a note or an audio removes its links
  (never the other side).
- `MIGRATION_1_2` only creates the two new tables; the notes table is untouched. It was
  checked by installing 0.2.0 over 0.1.0 with notes.
- Files live in the app's private storage (`files/audios/<uuid>.<ext>`); the table stores
  the file name. Imported files are **copied**, so they survive if the original is deleted.
  Deleting an audio deletes its file.

## Notes screen

`NotesViewModel` exposes `query: StateFlow<String>` and `notes: StateFlow<List<Note>?>`:

```kotlin
_query
    .debounce { if (it.isEmpty()) 0L else 200L }   // clearing the search is instant
    .flatMapLatest { repo.notes(it) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
```

`null` means "loading" and stops the empty state from flashing on launch. Cards show at
most 8 lines, keep line breaks, and only have a border when the note has no colour.

## Editor and autosave

`EditorViewModel` holds the note as Compose state (`mutableStateOf`) and persists it with
no action from the user:

1. Every change (`updateTitle`, `updateContent`, `updateType`, `updateColor`,
   `togglePinned`) sets `hasChanges` and schedules a save **600 ms** later, cancelling
   the previous one.
2. `save()` inserts the note if it is new (keeping the `id` Room returns) or updates it.
   It never saves empty notes (blank title and content).
3. When the screen is left, `onCleared()` does a final save. `viewModelScope` is already
   cancelled at that point, so it uses `appScope`, which lives as long as the app.
   If the note ended up empty and already existed, it is deleted (Keep's behaviour).
4. `delete()` marks the note as deleted (so `onCleared` does not bring it back) and
   removes it in `appScope`.

Two details prevent duplicate notes:
- A `Mutex` serialises every save and delete.
- The insert runs in `withContext(NonCancellable)`: if the scheduled save is cancelled
  while Room is inserting, the new `id` is not lost, and the next save updates instead of
  inserting again.

## Audios

<p align="center">
  <img src="screenshots/audios.png" width="200" alt="Audios section">
  <img src="screenshots/recording.png" width="200" alt="Recording">
  <img src="screenshots/note-audios.png" width="200" alt="A note's audios">
</p>

**Recording** (`AudioRecorder`, driven by `AudiosViewModel`): AAC in an `.m4a` file, mono,
44.1 kHz, 128 kbps (about 1 MB per minute). The recording sheet polls the elapsed time and
the input level every 100 ms. When the app goes to the background (`ON_STOP`) the recording
is **paused**, not lost; it can be resumed or saved on return. If the screen is destroyed
while recording, what was recorded is saved. Recordings get a default name
(`AudioFormat.recordingName`: "Grabación 30 sep 21:22"). The `RECORD_AUDIO` permission is
requested the first time.

**Importing**: the Storage Access Framework picker (`OpenDocument`, `audio/*`), then
`AudioRepository.import` copies the file, takes the display name without extension as the
audio's name ("maqueta_final.mp3" → "maqueta final") and reads the duration with
`MediaMetadataRetriever`. Files Android can't read as audio are rejected with a message.

**Playback**: a single `AudioPlayer` in `VersoApp`, shared by the audio list and the
editor, so starting one audio stops the other. It exposes `PlaybackState` (audio id,
playing, position, duration) as a `StateFlow`, updating the position every 200 ms.

**Linking**:
- From the audio list: ⋮ → *Vincular a notas* (checkbox list); each row shows its notes as
  chips that open them.
- From the editor: the *Audios* chip opens a sheet with the note's audios (play, unlink)
  and *Vincular audios*. Linking on a new note saves it first so it has an id.
- A note with no text but with audios is **kept** (it is not "empty"); its card says
  *Sin texto*. Note cards show 🎧 and the number of audios.

## Background analysis

The analysis is recomputed in the ViewModel from Compose state:

```kotlin
snapshotFlow { Triple(content, preferences.seseo, preferences.showAnalysis) }
    .filter { it.third }                 // if the analysis is hidden, it isn't computed
    .debounce(300)                       // wait until typing stops
    .mapLatest { (text, seseo) ->        // cancel the previous run if new text arrives
        withContext(Dispatchers.Default) { AnalisisPoema.analizar(text, seseo) }
    }
    .collect { r -> analysis = r; /* keep the selected device if it still exists */ }
```

The result (`AnalisisPoema.Resultado`) includes the text it was computed for. The UI uses
it to know whether it lags behind what is on screen (see [editor.md](editor.md)).

## Preferences

`Preferences` wraps a `SharedPreferences` file named `preferencias` and exposes each
setting as Compose state, so changing one redraws the UI and triggers the analysis:

| Key | Property | Default | Effect |
|---|---|---|---|
| `seseo` | `seseo` | `false` | s = z = c(e,i) in rhymes and alliterations |
| `mostrar_analisis` | `showAnalysis` | `true` | Margin, panel and computing the analysis |
| `color_rhymes` | `colorRhymes` | `false` | Rhyme colouring in the editor |
| `home_tab` | `homeTab` | `0` | Home section: 0 notes, 1 audios |

The keys keep their 0.1.0 names. Settings are global, not per note.

## Threads

| Work | Where it runs |
|---|---|
| Room queries | Room's executor (`suspend` and Room `Flow`s) |
| Text analysis | `Dispatchers.Default`, with a 300 ms debounce |
| Save on exit | `appScope` (`Dispatchers.Default`) |
| Drawing the margin and highlight | Main thread, from the already computed result |
| Importing and deleting audio files | `Dispatchers.IO` |
| Playback position, recording meter | Main thread, polled every 200 / 100 ms |

## Theme and design

"Ink notebook": indigo on cool paper in light mode (`#2E3A6E` on `#F6F7F9`) and light ink
on night paper in dark mode. Verses use a serif (`FontFamily.Serif`, 18 sp with 30 sp
line height); the UI uses the system sans serif. Cards are flat, without shadows. The note
palette has seven entries (Sin color, Salvia, Lavanda, Cielo, Trigo, Rosa, Grafito, shown
in Spanish in the UI), each with a dark version; `noteBackground(i)` picks one for the
current theme.

## Language

The app's code is in English, except for the analysis engine (`analisis/`), whose names
come from Spanish metrics terminology. Everything the user sees (UI text, the
analysis results) is in Spanish.
