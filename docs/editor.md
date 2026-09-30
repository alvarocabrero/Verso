# The editor and real-time analysis

[Español](es/editor.md)

This document explains how the analysis is displayed while typing. The computation itself
is in [analysis-engine.md](analysis-engine.md); the note's lifecycle, in
[architecture.md](architecture.md).

<p align="center">
  <img src="screenshots/editor-margin.png" width="220" alt="Syllable and rhyme margin">
  <img src="screenshots/alliteration-clear.png" width="220" alt="Clear alliteration, underlined">
  <img src="screenshots/alliteration-possible-dark.png" width="220" alt="Possible alliteration, dotted, dark mode">
</p>

## What the writer sees

```
 ←                      #   📌  🎨  🗑
 [Poema] [Canción]
 Soneto de repente
 Un soneto me manda hacer Violante,        11  A
 que en mi vida me he visto en tanto
 aprieto;                                  11  B    ← wrapped line: mark on its last row
 catorce versos dicen que es soneto;       11  B
 burla burlando van los tres delante.      11  A
 ───────────────────────────────────────────────
 Endecasílabo · ABBA · 2 recursos          ˄        ← panel (expands)
```

- **Number**: the line's metrical syllables fitted to the dominant metre. Grey if the line
  admits it, red (`colorScheme.error`) if not.
- **Letter**: rhyme group, in the primary colour. A faint `·` if the line rhymes with no
  other. Lowercase for *arte menor* (short lines).
- **Panel**: summary *metre · scheme · devices*. Tapping it expands the seseo setting and
  the list of devices (type, *clara/posible* for alliterations, lines and evidence). Tapping
  a device highlights it and scrolls to its first line; tapping it again clears it.
- **# button** (top bar): shows or hides the whole analysis. Remembered across sessions.

All user-facing text is in Spanish.

## Components

All in `ui/editor/`.

| Composable / function | File | Role |
|---|---|---|
| `EditorScreen` | `EditorScreen.kt` | Layout: top bar, colour picker, type, title, verses, panel |
| `VerseEditor` | `AnalysisEditor.kt` | Verse `BasicTextField` with margin, highlight and cursor kept in view |
| `VerseMargin` | `AnalysisEditor.kt` | Syllables and rhyme letter aligned with each line |
| `drawHighlight` | `AnalysisEditor.kt` | Background, underline or dotted line under a device's words |
| `AnalysisPanel` / `DeviceRow` | `AnalysisEditor.kt` | Expandable summary, seseo and device list |
| `highlightFor` | `AnalysisEditor.kt` | Turns a `Recurso` into ranges and a highlight style |

## Why a single scroll

The title and the verses sit inside one `Column` with `verticalScroll`, and the verse field
**has no scroll of its own** (it grows with the text). This way:

- the margin can be placed using the field's `TextLayoutResult` coordinates, without
  tracking a second inner scroll;
- the title scrolls with the poem, like a sheet of paper.

The trade-off is that Compose does not keep the cursor in view on its own in this layout.
`VerseEditor` handles it by hand (see [Keeping the cursor in view](#keeping-the-cursor-in-view)).

The field has a minimum height of 320 dp so the page can be tapped anywhere even when
empty.

## Aligning the margin

Each `AnalisisPoema.Linea` knows where it starts and ends in the text (`inicio`, `fin`).
With the field's `TextLayoutResult`:

```kotlin
val row    = layout.getLineForOffset(line.fin)   // last visual row of the line
val top    = layout.getLineTop(row)
val height = layout.getLineBottom(row) - top
```

The margin is a 56 dp `Box` overlaid on the right of the field (which has the same end
padding). Each mark is a `Row` moved with `offset { IntOffset(0, topPadding + top) }` and
`height` tall, vertically centred. Using `fin` rather than `inicio` puts the mark of a
wrapped line on its last row, next to the rhyming word.

Each mark has a screen-reader description: *"11 sílabas, rima A"* or
*"9 sílabas, no encaja en el metro"*.

## When the analysis lags behind

The analysis arrives 300 ms after typing stops, so for a moment the result belongs to an
earlier version of the text. To avoid drawing marks in the wrong places:

| What | Shown when… | Why |
|---|---|---|
| Margin | the result has **the same number of lines** as the current text | Typing inside a line doesn't move lines; adding or removing lines does |
| Highlight | the analysed text is **identical** to the current one | Ranges are character offsets: any change shifts them |

In practice the margin doesn't flicker while typing within a line, and disappears for a
moment after pressing Enter.

## Highlighting

`highlightFor(analysis, device)` gets the ranges from `AnalisisPoema.rangos` and picks the
style:

| Device | Style |
|---|---|
| Clear alliteration (intensity ≥ 4.5) | `UNDERLINE`: a solid 2 dp line under the word |
| Possible alliteration | `DOTTED`: a dashed line (2 dp dash, 3 dp gap) |
| Any other device | `BACKGROUND`: a rounded rectangle in the primary colour at 16 % |

It is drawn in the field's own `Modifier.drawBehind`, **behind** the text. For each range
the visual rows it spans are computed and, on each, the x coordinates with
`getHorizontalPosition`; the underline sits 4 dp below the baseline. Everything uses the
theme's primary colour, so it works the same in light and dark mode.

If the selected device disappears after an edit (the new analysis doesn't contain it), the
selection is cleared automatically.

## Scrolling to a device

When a device is selected, `EditorScreen` scrolls the page to it:

```kotlin
LaunchedEffect(selected) {
    val start = highlight.ranges.first().first
    val y = versesY + layout.getLineTop(layout.getLineForOffset(start)) - 48.dp
    scroll.animateScrollTo(y)
}
```

`versesY` is the field's position inside the scrolling column (measured with
`onGloballyPositioned { it.positionInParent().y }`); `versesLayout` comes from the field
through the `onLayout` parameter.

## Keeping the cursor in view

`VerseEditor` keeps its own `TextFieldValue` to know where the cursor is (the ViewModel
only stores the `String`). If the text changes from outside (when the note loads), the
value is reset with the cursor at the end.

While the field has focus, every time the selection or the `TextLayoutResult` changes:

```kotlin
val cursor = layout.getCursorRect(selection.end)
bringIntoView.bringIntoView(cursor expanded by 32 dp above and below)
```

The `BringIntoViewRequester` sits in the modifier chain **after** the field's padding, so
its coordinates match the text's. The parent scroll responds by scrolling just enough for
the cursor to be visible above the keyboard (the column has `imePadding()`).

## Panel and keyboard

With the keyboard open, the expanded panel would leave almost no room for the verses. So
`AnalysisPanel` watches `WindowInsets.isImeVisible` and **collapses when the keyboard
appears**. It can be reopened with the keyboard visible; it only collapses at the moment
the keyboard shows up. The device list is at most 260 dp tall and scrolls on its own.

The expanded state is kept with `rememberSaveable` (it survives rotation).

## Seseo

The *Seseo (casa = caza)* chip in the panel toggles `Preferences.seseo`. Since it is Compose
state and part of the `snapshotFlow` key, the analysis is recomputed immediately. It
affects:

- full rhyme (*casa / caza*, *abrazo / paso*);
- alliteration (*s*, *z* and *c* before e/i become the same sound, with a base frequency
  of 9.1 %).

## Accessibility

- Top-bar icons have a `contentDescription` that follows their state
  (*Ocultar análisis / Mostrar análisis*, *Fijar / Desfijar*).
- Each margin mark is a single semantics node with its description.
- The panel announces *Abrir análisis / Cerrar análisis*.
- Visually, a line that doesn't fit is told apart only by the number's colour; the
  screen-reader description does say so ("no encaja en el metro").

## Ideas

- Metre per stanza, for polymetric poems.
- Show a line's syllabification when its number is tapped (`Verso.silabasPara` already
  returns the syllables with `‿`).
- A "doesn't fit" indicator that doesn't rely on colour alone.
