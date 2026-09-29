# Verso

[Español](README.md) · **English**

**An Android notebook for writing poetry and song lyrics in Spanish.**

Verso works like a simple Google Keep (notes in a grid, colours, pinning, search), with one
distinctive feature: **it analyses what you write as you write it**. It counts the metrical
syllables of each line, detects the rhyme scheme and points out literary devices such as
alliteration, anaphora or parallelism.

The app's interface and analysis are in Spanish, and it is designed for Spanish verse.

<p align="center">
  <img src="docs/capturas/editor-margen.png" width="240" alt="Editor with syllables and rhymes in the margin">
  <img src="docs/capturas/recurso-resaltado.png" width="240" alt="Anadiplosis highlighted in the text">
  <img src="docs/capturas/aliteracion-posible-oscuro.png" width="240" alt="Possible alliteration in dark mode">
</p>

## Features

**Notes**
- Poems and songs as notes, with a title, a type (Poem / Song) and one of seven paper
  colours (each with a dark variant).
- Pin notes to the top, search by title and content, staggered grid.
- Autosave: there is no save button, and a note left empty is discarded.

**Real-time analysis in the editor**
- **Metrical syllables** to the right of each line, fitted to the poem's dominant metre.
  This follows the rules of Spanish prosody: *sinalefa* (vowels merged across word
  boundaries) and the final-stress rule (+1 syllable for a line ending in an oxytone,
  −1 for a proparoxytone). Lines that don't fit are shown in red.
- **Rhyme**: rhyme letter in the margin (ABBA…), both *consonant* (full rhyme) and
  *assonant* (vowels only). Rhymes are compared by sound (b = v, silent h, *yeísmo*), with
  optional *seseo* (s = z = soft c) for Latin American or Andalusian accents. Letters are
  lowercase for short-line verse (*arte menor*, 8 syllables or fewer).
- **Literary devices**: alliteration (clear or possible), anaphora, epiphora, anadiplosis,
  epanadiplosis, geminatio, polysyndeton, asyndeton, parallelism and refrain.
  Tapping one highlights its words and scrolls to it.
- An always-visible summary: *«Endecasílabo · ABBA ABBA · 3 recursos»* (hendecasyllable,
  rhyme scheme, number of devices).
- The whole analysis can be hidden with the **#** button in the top bar.

## Download

The latest signed build is in
[Releases](https://github.com/alvarocabrero/Verso/releases/latest). Download the `.apk` on
your phone and allow installing apps from that source when Android asks.

## Requirements

| | |
|---|---|
| Android | 8.0 (API 26) or later |
| Build | JDK 17, Android SDK 35 |
| Language | Kotlin 2.0, Jetpack Compose (Material 3) |

## Getting started

```bash
git clone https://github.com/alvarocabrero/Verso.git
cd Verso
```

1. Create `local.properties` with your SDK path (Android Studio does this for you):
   ```properties
   sdk.dir=C:/Users/<you>/AppData/Local/Android/Sdk
   ```
2. Build and install on a connected phone or emulator:
   ```bash
   ./gradlew installDebug
   ```
3. Run the analysis engine's tests:
   ```bash
   ./gradlew testDebugUnitTest
   ```

The debug APK ends up in `app/build/outputs/apk/debug/app-debug.apk`.
The full environment guide (JDK, SDK, emulator on Windows) is in
[docs/desarrollo.md](docs/desarrollo.md) (Spanish).

## Project layout

```
app/src/main/java/com/tuapp/
  VersoApp.kt          Application: hand-rolled dependency container (no Hilt)
  MainActivity.kt      Navigation: note list and editor
  data/                Room (Nota, NotaDao, VersoDatabase), repository and preferences
  ui/notas/            Home screen: search and card grid
  ui/editor/           Editor with autosave and real-time analysis
  ui/theme/            "Ink on paper" theme, verse text styles and note palette
  analisis/            Analysis engine in pure Kotlin (no Android dependencies)
app/src/test/java/com/tuapp/analisis/   JUnit tests for the engine (48)
docs/                  Technical documentation
```

## Documentation

The technical documentation is written in Spanish:

| Document | Contents |
|---|---|
| [Arquitectura](docs/arquitectura.md) | Architecture: layers, data flow, autosave, threading, navigation |
| [Motor de análisis](docs/motor-de-analisis.md) | Analysis engine: syllabification, metre, rhyme and devices (rules, algorithms, API) |
| [Editor](docs/editor.md) | How the analysis is shown in the UI: margin, highlighting, panel |
| [Desarrollo](docs/desarrollo.md) | Development: environment, build, tests, emulator, releases, conventions, troubleshooting |

## Known limitations

- It does not detect *diéresis* or *sinéresis* (splitting or merging vowels within a
  word), nor the caesura of alexandrines.
- There is one dominant metre per poem: in polymetric poems, lines of a different length
  are shown in red.
- There are no internal rhymes or near rhymes.
- Semantic devices (metaphor, simile, personification…) are not detected; that would
  need a language model.

## Roadmap

- Tags, and a trash bin with undo.
- Export and share (text, image).
- Metre per stanza for polymetric poems.
- Optional detection of semantic devices with a language model.

## Conventions

All code, comments and UI text are in Spanish. The analysis engine does not depend on
Android, and every change to it comes with tests.
