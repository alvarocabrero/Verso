# Changelog

**English** · [Español](CHANGELOG.es.md)

## Unreleased (0.2.0)

On `main`, not yet in a published release.

### Added
- **Audios section.** The home screen has a bottom bar to switch between *Notas* and
  *Audios* (the last one used is remembered).
  - Record in the app with pause and resume and a level meter. If the app goes to the
    background the recording is paused, not lost.
  - Attach audio files from the phone (mp3, m4a, wav, ogg, flac…); they are copied into the
    app, so they survive if the original is deleted.
  - Play in place with a seek bar, rename, and delete (with confirmation).
  - Link audios and notes, many to many, from both sides: the audio list shows each audio's
    notes, and the editor has an *Audios* chip to play, unlink and link. Note cards show 🎧
    and the number of audios. A note with audios but no text is kept.
  - Audios stay on the device.
- **ⓘ button on each literary device**: what it is, a classic example, where it was found
  and, for alliterations, why it counts as clear or possible.
- **Internal rhyme** as a new literary device (full/consonant rhymes only).
- **Rhyme colouring** (brush button): a highlighter colour per rhyme group on the rhyming
  ending, from a colour-blind-safe palette, softer for assonant rhymes, with a legend in the
  analysis panel. Remembered across sessions.

### Changed
- Source code and documentation translated to English. The analysis engine keeps its
  Spanish names, and the app itself stays in Spanish.
- Database version 2 (new tables for audios). Upgrading from 0.1.0 keeps all notes.

### Other
- Licensed under the [GNU GPL v3.0](LICENSE).

## 0.1.0 (2026-09-29)

First release, as a signed APK.

- Notes for poems and songs: title, type, seven paper colours, pinning, search, staggered
  grid and autosave.
- Real-time analysis in the editor: metrical syllables per line fitted to the dominant
  metre, rhyme scheme (consonant and assonant, optional seseo) and literary devices
  (alliteration, anaphora, epiphora, anadiplosis, epanadiplosis, geminatio, polysyndeton,
  asyndeton, parallelism, refrain), with highlighting and a summary panel.
