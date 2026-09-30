# Verso: an Android notes app for poetry and song lyrics

A Google Keep-style app for writing poems and songs in Spanish. What sets it apart is
text analysis: metrical syllables per line, rhymes and literary devices.

## Language

- App code (classes, functions, variables, comments, file and folder names) is in **English**.
- The **analysis engine** (`com.tuapp.analisis`, `app/src/main/java/com/tuapp/analisis/`
  and its tests) stays in **Spanish**: its names are Spanish metrics terms. Don't
  translate or rename it.
- Everything the user sees (UI text, analysis results, content descriptions) is in **Spanish**.
- Docs: `README.md` (English, shown by GitHub) and `README.es.md` (Spanish); `docs/` in
  English with a Spanish copy in `docs/es/` (same file names). Keep both languages in sync.
- `CHANGELOG.md` / `CHANGELOG.es.md`: add user-visible changes under "Unreleased"; on release,
  rename that section to the version and date.
- Repository: https://github.com/alvarocabrero/Verso (branch `main`).

## Status

- Builds with no warnings (`gradlew assembleDebug` / `assembleRelease`) and the 62 JVM
  tests pass (57 engine + 5 audio helpers;
  `gradlew testDebugUnitTest`). Tested on an emulator, not yet on a
  real phone.
- Release v0.1.0 is published on GitHub (signed APK, ~1.2 MB).
- Build environment on this PC: JDK 17 (Temurin, in `C:\Program Files\Eclipse Adoptium`;
  `java` is not on the PATH, set `JAVA_HOME`), Android SDK in `%LOCALAPPDATA%\Android\Sdk`
  (set in `local.properties`), Gradle wrapper 8.9, `gh` logged in as `alvarocabrero`.
- Dependency versions (AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01, Room 2.6.1)
  are from late 2024 and can be updated.

## Stack

Kotlin, Jetpack Compose (Material 3), Room with KSP, Navigation Compose, ViewModel. No
Hilt: dependencies are created by hand in `VersoApp`. minSdk 26, compileSdk/targetSdk 35,
Java 17. Code package `com.tuapp`, applicationId `com.tuapp.verso`.

## Structure

```
app/src/main/java/com/tuapp/
  VersoApp.kt            Application: database, repositories, preferences, audioPlayer, appScope
  MainActivity.kt        NavHost: "home" (Notes / Audios tabs) and "editor/{id}" (id 0 = new note)
  data/                  Note, Audio, NoteAudio (Room), DAOs, VersoDatabase (v2), repositories, Preferences
  audio/                 AudioRecorder, AudioPlayer, AudioFormat (pure helpers, tested)
  ui/theme/              Theme (indigo ink on cool paper), VerseStyle, NotePalette
  ui/home/               HomeScreen: bottom bar switching sections
  ui/notes/              Notes section: staggered grid + search
  ui/audios/             Audios section: list, player, record sheet, import, rename, link dialogs
  ui/components/         SearchField
  ui/editor/             Editor with autosave and real-time analysis
  analisis/              Analysis engine, pure Kotlin with no Android dependencies (Spanish)
app/src/test/java/com/tuapp/          JUnit tests: analisis/ (engine), audio/ (helpers)
docs/                    architecture, analysis-engine, editor, development (+ docs/es/)
```

## Data and editor

- `Note`: id, title, content, type (`NoteType.POEM` / `SONG`), color (index into
  `NotePalette`), pinned, created, modified.
- **Database compatibility**: the table (`notas`) and columns keep the Spanish names from
  v0.1.0 via `@ColumnInfo`, and `Converters` stores `NoteType` as `"POEMA"`/`"CANCION"`.
  Preference keys (`seseo`, `mostrar_analisis`) also keep their names. Never rename
  them; schema changes need a version bump and a Room `Migration`.
- List order: pinned first, then by modification date.
- Autosave in `EditorViewModel`: saves 600 ms after the last change and on exit
  (`onCleared`, using `appScope` because `viewModelScope` is already cancelled). A note
  left empty is deleted. A `Mutex` and `NonCancellable` prevent duplicate inserts.
- Don't name functions `setX` when there is a `var x … private set` (JVM clash); use `updateX`.

## Audios (0.2.0)

- DB version 2: tables `audios` and `note_audios` (many-to-many, FKs `ON DELETE CASCADE`),
  created by `MIGRATION_1_2`; schemas exported to `app/schemas/`. Files in
  `files/audios/` (imports are copied). A note with audios but no text is kept.
- Recording: `MediaRecorder` AAC/m4a; paused on `ON_STOP` (no background recording yet).
  Import via SAF `OpenDocument("audio/*")`. One app-wide `AudioPlayer` (`MediaPlayer`).
- Preference keys added: `color_rhymes`, `home_tab`.

## Analysis engine (`com.tuapp.analisis`, in Spanish)

**Silabeador**: `silabear(palabra)`, `silabaTonica()`, `analizar()` → syllables, index of
the stressed one and stress type (aguda/llana/esdrújula/sobresdrújula); `palabrasDe(verso)`.
Handles diphthongs, triphthongs, hiatus, ch/ll/rr, silent u in qu/gu, ü, final vocalic y
and inseparable consonant clusters.

**Metrica**: `Verso(texto)` gives a **range** `minimo..maximo`, because *sinalefa* is
optional (each one removes a syllable). Applies the final-stress rule (+1 aguda,
−1 esdrújula). `silabasPara(metro)` fits the line by breaking *sinalefas* on stressed
vowels first and marks applied ones with "‿". `metroDominante(versos)` picks the metre
that fits most lines. `nombreMetro(n)` → "endecasílabo", etc.

**Rima**: `terminacion(verso)` gives consonant keys (phonetic: b=v, silent h, yeísmo,
optional seseo) and assonant keys (stressed + final vowel; in an unstressed final
position i≈e, u≈o). `comparar(a, b)` and `esquema(lineas)` → ABBA letters, "-" for
unrhymed lines, `minusculas` for arte menor.

**Recursos**: `detectar(texto)` → list of `Recurso(tipo, lineas, evidencia, palabras,
intensidad)`. Detects alliteration, anaphora, epiphora, anadiplosis, epanadiplosis,
geminatio, polysyndeton, asyndeton, parallelism, refrain and internal rhyme. Alliteration counts only
syllable-onset consonants against their normal frequency in Spanish (threshold 3.5;
`clara` from 4.5), analyses each line and each pair of consecutive lines, and merges
overlaps.

**AnalisisPoema**: `analizar(texto, seseo)` combines dominant metre, syllables per line
(the metre if the line admits it; otherwise the closest value in its range and
`encaja = false`), rhyme and devices, with each line's offsets in the text.
`rangos(resultado, recurso)` gives the characters to highlight.

**Known limitations**: no diéresis/sinéresis or alexandrine hemistichs; no assonant internal rhymes or
near rhymes; no semantic devices (metaphor, simile…), which would need a language model;
one metre per poem.

## Analysis in the editor

- `EditorViewModel` recomputes it with `snapshotFlow` + `debounce(300)` + `mapLatest` on
  `Dispatchers.Default`. The result stores the analysed text: the margin is drawn while
  the line count is unchanged, the highlight only if the text matches exactly.
- `ui/editor/AnalysisEditor.kt`: `VerseEditor` (a `BasicTextField` inside the screen's
  scroll, without its own scroll, so the margin aligns with `TextLayoutResult`; syllables
  and letter on each line's last visual row, red if it doesn't fit; own `TextFieldValue`
  + `BringIntoViewRequester` keep the cursor in view), highlight drawn in `drawBehind`
  (background; clear alliteration solid underline, possible dotted) and `AnalysisPanel`
  (summary metre · scheme · device count, expandable list and seseo; collapses when the
  keyboard opens). Selecting a device scrolls to its first line.
- `data/Preferences` (SharedPreferences as Compose state): seseo, show/hide analysis and rhyme colouring
  (buttons in the top bar). Rhyme colouring: `AnalisisPoema.tramosDeRima` gives the spans,
  `ui/theme/RhymeColors.kt` the Okabe–Ito palette (assonant softer), and `rangeShifter`
  keeps highlights in place while the analysis lags behind.

## Design

Ink notebook: serif (`FontFamily.Serif`) with generous line height for verses, system
sans for the UI. Cards keep line breaks, are flat with no shadow (thin border only when
uncoloured). Seven note colours with a dark variant. UI text in plain Spanish.

## Releases

Signed with `%USERPROFILE%\.verso-firma\verso-release.jks`, read from `keystore.properties`
(gitignored). R8 + resource shrinking in release. Steps in `docs/development.md`
("Releasing a version"). Always test the release build, and install it over the previous
release to check notes survive. Release notes in English with a collapsible Spanish section.

## Emulator

AVD `Verso_API35` (Pixel 7, API 35), WHPX enabled. Useful: `adb shell settings put secure
stylus_handwriting_enabled 0` (Gboard stylus tutorial swallows `adb input`), keyevent 224
to wake a black screen, `MSYS_NO_PATHCONV=1` in Git Bash.

## Next steps

Background recording and recording from a note; tags, trash with undo, export/share, metre
per stanza, and optional semantic device
detection with a language model.

## Conventions

- Names in English for app code, Spanish in the analysis engine (see Language).
- The analysis engine must not depend on Android; every change to it comes with tests.
- If you change engine or editor behaviour, update the matching doc in both languages.
