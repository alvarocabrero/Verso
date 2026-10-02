# Verso

**English** · [Español](README.es.md)

**An Android notebook for writing poetry and song lyrics in Spanish.**

Verso works like a simple Google Keep (notes in a grid, colours, pinning, search), with one
distinctive feature: **it analyses what you write as you write it**. It counts the metrical
syllables of each line, detects the rhyme scheme and points out literary devices such as
alliteration, anaphora or parallelism. It also keeps **voice memos and demos**: record
them in the app or attach audio files, and link them to the notes they belong to.

The app's interface and analysis are in Spanish, and it is designed for Spanish verse.

<p align="center">
  <img src="docs/screenshots/home-notes.png" width="200" alt="Notes section: cards with colours and audio counters">
  <img src="docs/screenshots/rhyme-colours.png" width="200" alt="Editor with syllables, rhyme letters and coloured rhymes">
  <img src="docs/screenshots/device-highlighted.png" width="200" alt="A literary device (anadiplosis) highlighted in the text">
  <img src="docs/screenshots/audios.png" width="200" alt="Audios section: playing an audio linked to a note">
</p>
<p align="center">
  <img src="docs/screenshots/device-info.png" width="200" alt="Explanation of a literary device with a classic example">
  <img src="docs/screenshots/alliteration-possible-dark.png" width="200" alt="Possible alliteration with a dotted underline, dark mode">
  <img src="docs/screenshots/note-audios.png" width="200" alt="A note's linked audios, playing one">
  <img src="docs/screenshots/recording.png" width="200" alt="Recording an audio">
</p>

## Features

**Notes**
- Poems and songs as notes, with a title, a type (Poem / Song) and one of seven paper
  colours (each with a dark variant).
- Pin notes to the top, search by title and content, staggered grid.
- Autosave: there is no save button, and a note left empty is discarded.

**Audios**
- A second section, switched from the bottom bar: **Notas** / **Audios**.
- **Record** ideas in the app (with pause) or **attach** audio files from the phone (mp3,
  m4a, wav, ogg, flac…); imported files are copied into the app.
- Play them in place with a seek bar, **rename** and delete them.
- **Link** audios and notes, many to many: a song can have several demos and a demo can
  belong to several songs. The editor shows a note's audios; note cards show 🎧 and a count.
- Everything stays on the phone: audios are not uploaded anywhere.

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
  epanadiplosis, geminatio, polysyndeton, asyndeton, parallelism, refrain and internal rhyme.
  Tapping one highlights its words and scrolls to it.
- An **ⓘ button** next to each device explains what it is, with a classic example, where it
  was found and, for alliterations, why it counts as clear or possible.
- An always-visible summary: *«Endecasílabo · ABBA ABBA · 3 recursos»* (hendecasyllable,
  rhyme scheme, number of devices).
- **Rhyme colouring** (brush button): each rhyme group gets its own highlighter colour on the
  rhyming ending, softer for assonant rhymes; internal rhymes take their group's colour.
- The whole analysis can be hidden with the **#** button in the top bar.

## Download

The latest signed build is in
[Releases](https://github.com/alvarocabrero/Verso/releases/latest). Download the `.apk` on
your phone and allow installing apps from that source when Android asks.

What changed in each version is in the [changelog](CHANGELOG.md).

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
[docs/development.md](docs/development.md).

## Project layout

```
app/src/main/java/com/tuapp/
  VersoApp.kt          Application: hand-rolled dependency container (no Hilt)
  MainActivity.kt      Navigation: home (notes / audios) and editor
  data/                Room (Note, Audio, NoteAudio, DAOs, VersoDatabase), repositories and preferences
  audio/               Recorder, player and audio formatting helpers
  ui/home/             Home screen with the bottom bar
  ui/notes/            Notes section: search and card grid
  ui/audios/           Audios section: list, player, recording, import, links
  ui/editor/           Editor with autosave and real-time analysis
  ui/theme/            "Ink on paper" theme, verse text styles and note palette
  analisis/            Analysis engine in pure Kotlin (no Android dependencies)
app/src/test/java/com/tuapp/analisis/   JUnit tests for the engine (62)
app/src/test/java/com/tuapp/audio/      JUnit tests for the audio helpers (5)
docs/                  Technical documentation (docs/es/: Spanish copy)
```

## Documentation

| Document | Contents |
|---|---|
| [Architecture](docs/architecture.md) | Layers, data flow, autosave, threading, navigation, audios (recording, import, links) |
| [Analysis engine](docs/analysis-engine.md) | Syllabification, metre, rhyme and literary devices: rules, algorithms and API |
| [Editor](docs/editor.md) | How the analysis is shown in the UI: margin, highlighting, panel |
| [Development](docs/development.md) | Environment, build, tests, emulator, releases, conventions, troubleshooting |
| [Changelog](CHANGELOG.md) | What changed in each version |

A Spanish copy of every document is in [docs/es/](docs/es/).

## Known limitations

- It does not detect *diéresis* or *sinéresis* (splitting or merging vowels within a
  word), nor the caesura of alexandrines.
- There is one dominant metre per poem: in polymetric poems, lines of a different length
  are shown in red.
- Assonant internal rhymes are only looked for near a line ending, and some will be
  coincidences; there are no near rhymes.
- Semantic devices (metaphor, simile, personification…) are not detected; that would
  need a language model.

## Roadmap

- Recording in the background (with the screen locked), and recording straight from a note.
- Tags, and a trash bin with undo.
- Export and share (text, image).
- Metre per stanza for polymetric poems.
- Optional detection of semantic devices with a language model.

## Conventions

- App code, file and folder names are in English.
- The analysis engine (`analisis/`) keeps its Spanish names, since they are Spanish
  metrics terms (*sílaba*, *sinalefa*, *rima asonante*). It does not depend on Android,
  and every change to it comes with tests.
- Everything the user sees in the app is in Spanish.

## License

Verso is free software, released under the [GNU General Public License v3.0](LICENSE).
You can use, study, share and modify it; any distributed version, modified or not,
must stay under the same license and include its source code.
