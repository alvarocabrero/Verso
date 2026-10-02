# Analysis engine

[Español](es/analysis-engine.md)

Package `com.tuapp.analisis`. Pure Kotlin with no Android dependencies, tested with JUnit
on the JVM (`./gradlew testDebugUnitTest`).

The engine models **Spanish** prosody, so its API (objects, functions, fields) uses the
Spanish terms of Spanish metrics. This document explains them in English.

| Object | Responsibility |
|---|---|
| [`Silabeador`](#silabeador-syllabifier) | Splits words into syllables and finds the stressed one |
| [`Metrica`](#metrica-metre) | Counts a line's metrical syllables and picks the poem's metre |
| [`Rima`](#rima-rhyme) | Rhyme endings, rhyme comparison and scheme (ABBA…) |
| [`Recursos`](#recursos-literary-devices) | Detects sound, repetition and structure devices |
| [`AnalisisPoema`](#analisispoema-the-full-analysis) | Puts it all together for the editor and computes what to highlight |

The examples in this document come from the tests, so they reflect what the code
actually does.

### Glossary

| Spanish term | Meaning |
|---|---|
| *sílaba* | syllable |
| *tónica* | stressed (syllable or vowel) |
| *aguda / llana / esdrújula / sobresdrújula* | stress on the last / second-to-last / third-to-last / earlier syllable |
| *diptongo / triptongo / hiato* | two or three vowels in one syllable / two adjacent vowels in separate syllables |
| *verso* | line of verse |
| *metro* | metre: syllables per line |
| *sinalefa* | merging the last vowel of a word with the first vowel of the next into one syllable |
| *dialefa* | breaking a possible *sinalefa* |
| *rima consonante / asonante* | full rhyme (all sounds) / assonant rhyme (vowels only) |
| *arte menor / mayor* | lines of up to 8 syllables / 9 or more |
| *seseo* | pronouncing *z* and soft *c* as *s* (Latin America, parts of Andalusia) |
| *yeísmo* | pronouncing *ll* as *y* |
| *estrofa* | stanza |
| *recurso (literario)* | literary device |

---

## Silabeador (syllabifier)

**Orthographic** (grammatical) syllabification: the syllables of a word on its own, without
merging across words (that is `Metrica`'s job).

### API

```kotlin
Silabeador.silabear("murciélago")          // [mur, cié, la, go]
Silabeador.silabaTonica("murciélago")      // 1  (index of the stressed syllable)
Silabeador.analizar("murciélago")          // Palabra(texto, silabas, tonica = 1, tipo = ESDRUJULA)
Silabeador.palabrasDe("¡Ay, qué dolor!")   // [Ay, qué, dolor]
```

- `TipoAcentual` (stress type): `AGUDA`, `LLANA`, `ESDRUJULA`, `SOBRESDRUJULA`, depending
  on how many syllables follow the stressed one (0, 1, 2, 3 or more).
- `palabrasDe` ("words of") extracts runs of letters (`\p{L}+`): it ignores punctuation
  **and digits**.
- Syllables keep the original capitalisation and accents.

### Algorithm

**1. Tokenising.** The word becomes a sequence of units, each a vowel or a consonant:

| Case | Treatment | Example |
|---|---|---|
| `ch`, `ll`, `rr` | A single consonant | pe-**rr**o, **ch**o-co-la-te |
| `qu`, `gu` before e/i | A single consonant; the u is silent | **qu**e-so, **gu**i-ta-rra |
| `gü` | The ü is a vowel | pin-**güi**-no |
| `y` not followed by a vowel | Vowel (like i) | ho**y**, re**y**, **y** |
| `y` followed by a vowel | Consonant | re-**y**es |

**2. Vowel nuclei.** Adjacent vowels share a nucleus (diphthong or triphthong) unless they
form a **hiatus**. Spanish vowels are *strong* (a, e, o) or *weak* (i, u):

| Combination | Result | Examples |
|---|---|---|
| Two different weak vowels | Diphthong | **ciu**-dad, **cui**-da-do, cons-**truí** |
| Two identical weak vowels | Hiatus | chi-i-ta |
| Accented weak vowel (í, ú) next to a strong one | Hiatus | rí-o, ma-íz, Ra-úl, o-í-do |
| Strong + strong | Hiatus | po-e-ta, le-er, a-é-re-o |
| Strong + unaccented weak | Diphthong | **ai**-re, **Eu**-ro-pa, **hue**-vo |
| Weak + strong + weak | Triphthong | **buey**, U-ru-**guay** |

An *h* between vowels does not split anything: bú-ho, a-hu-mar.

**3. Distributing consonants** between two nuclei:

| Consonants between vowels | Split | Examples |
|---|---|---|
| 0 or 1 | All go to the next syllable | ca-sa, e-xa-men |
| 2 | Between them, unless they are an inseparable cluster | ac-ción, in-fla-mar; a-**br**a-zo |
| 3 or more | The last two go to the next syllable if inseparable; otherwise only the last one | ins-**tr**u-men-to, obs-tá-cu-lo, trans-**pl**an-te |

Inseparable clusters: `pr br cr gr fr kr tr dr` and `pl bl cl gl fl kl`. `tl` is not:
at-le-ta.

**4. Stressed syllable.**
1. If a syllable has a written accent, it is the stressed one.
2. If the word has one syllable, that one.
3. If the word ends in a vowel, *n* or *s*, the second-to-last (*llana*); otherwise the
   last (*aguda*).

### Limitations

- Orthographic syllabification: dialectal pronunciations (e.g. *ti-a* vs *tia*) are not
  covered; in metrics they are treated as poetic licence.
- Foreign words and acronyms are syllabified with Spanish rules.
- Unaccented monosyllables count as stressed when computing the stress type; for
  *sinalefa*, `Metrica` uses its own list of unstressed words.

---

## Metrica (metre)

Counts **metrical** syllables, which differ from grammatical ones for two reasons:

- **Sinalefa**: a word's final vowel merges with the next word's initial vowel
  (*no‿hay*, *se‿ha-ce*).
- **Final-stress rule**: if the line ends in an *aguda* word, add one syllable; if it ends
  in an *esdrújula* (or *sobresdrújula*), subtract one.

### A range, not a number

*Sinalefa* is optional in practice (poets can break it: *dialefa*), so a line doesn't have
one count but a **range**:

- `maximo` = grammatical syllables + final adjustment (no *sinalefa*).
- `minimo` = `maximo` − number of possible *sinalefas* (all applied).

Since each *sinalefa* removes exactly one syllable, every value in the range is reachable.

```kotlin
val v = Metrica.Verso("Escrito está en mi alma vuestro gesto")
v.minimo                       // 10
v.maximo                       // 13
v.admite(11)                   // true
v.silabasPara(11)              // Es-cri-to‿es-tá-en-mi‿al-ma-vues-tro-ges-to
v.silabasPara(8)               // null: the line cannot measure 8
```

### When a *sinalefa* is possible

Between two consecutive words when:
- the first **ends in a vowel** (the final *y* of *rey*, *hoy*, *soy* doesn't count: it is
  a semi-consonant), and
- the second **starts with a vowel**, also after a silent *h* (*no‿hay*, *se‿ha-ce*),
  except *hie-* and *hue-* (*la hierba*, *el hueso*: there the h sounds like a consonant).
  The conjunction *y* counts as a vowel.

### Which *sinalefas* break first

When a line has to be lengthened to fit a metre, `silabasPara(metro)` first breaks the
most "resistant" *sinalefas*:

| Resistance | Situation | Example |
|---|---|---|
| 2 | The final vowel is **stressed** | *es-tá-en* (broken) |
| 1 | The next word's initial vowel is stressed | *mi-al-ma* |
| 0 | Neither is stressed | *mi‿al-ma* (kept) |

At equal resistance, those closer to the end of the line break first. To decide whether a
monosyllable is stressed there is a list of unstressed words: articles (*el, la, los, las,
lo, un, una…*), contractions (*al, del*), prepositions (*a, de, en, con, por, sin, so*),
conjunctions (*y, e, o, u, ni, que*), unstressed pronouns (*me, te, se, le, les, nos, os*)
and possessives before a noun (*mi, tu, su, mis, tus, sus*).

Applied *sinalefas* are marked with `‿` in the output.

### Dominant metre

```kotlin
val versos = Metrica.analizarTexto(text)    // one Verso per line (empty ones don't count)
val metro = Metrica.metroDominante(versos)  // 11
Metrica.nombreMetro(metro!!)                // "endecasílabo"
```

`metroDominante` tries every value between the lines' minimum and maximum and keeps the
one **admitted by the most lines**. Ties are broken by how common each metre is in the
tradition: 11, 8, 7, 14, 6, 5, 9, 12, 10, 13, 4, 3, 2.

Names (in Spanish): bisílabo (2), trisílabo, tetrasílabo, pentasílabo, hexasílabo,
heptasílabo, octosílabo, eneasílabo, decasílabo, endecasílabo, dodecasílabo,
tridecasílabo and alejandrino (14). Outside that range: "N sílabas".

### Examples (from the tests)

| Line | Range | Measured |
|---|---|---|
| Verde que te quiero verde | 8–8 | Ver-de-que-te-quie-ro-ver-de |
| Caminante, no hay camino | 8–9 | Ca-mi-nan-te-no‿hay-ca-mi-no |
| la hierba verde | 5–5 | la-hier-ba-ver-de |
| se hace camino al andar | 8–10 | se‿ha-ce-ca-mi-no‿al-an-dar (*aguda* +1) |
| dame la mano, murciélago | 8–8 | da-me-la-ma-no-mur-cié-la-go (*esdrújula* −1) |
| En tanto que de rosa y azucena | 10–12 | En-tan-to-que-de-ro-sa‿y-a-zu-ce-na (11) |
| Puedo escribir los versos más tristes esta noche | 14–15 | Pue-do‿es-cri-bir-… (14) |
| Yo soy un hombre sincero | 8–8 | Yo-soy-un-hom-bre-sin-ce-ro |

### Limitations

- No **diéresis** (*sü-a-ve*, splitting a diphthong) or **sinéresis** (*poe-ta* as one
  syllable).
- No **hemistichs** in alexandrines (7 + 7, with the final-stress rule applied to each half).
- One metre for the whole text: in polymetric poems, lines of a different length are
  marked as not fitting.

---

## Rima (rhyme)

Spanish rhyme starts at the line's **last stressed vowel**.

- **Consonante** (full rhyme): every sound matches from there (*cielo / suelo*).
- **Asonante** (assonant rhyme): only the vowels match (*cielo / lejos*).

### API

```kotlin
Rima.terminacion("agua del cántaro")
// Terminacion(palabra = "cántaro", texto = "ántaro", consonante = "antaro", asonante = "ao")

Rima.comparar("cielo", "suelo")                // CONSONANTE
Rima.comparar("casa", "caza")                  // ASONANTE
Rima.comparar("casa", "caza", seseo = true)    // CONSONANTE
Rima.comparar("casa", "perro")                 // null

val e = Rima.esquema(lines, seseo = false, minusculas = false)  // List<RimaVerso?>
Rima.esquemaComoTexto(e)                                        // "ABBA ABBA"
```

### Ending (*terminación*)

1. Take the line's last word and its stressed syllable.
2. Inside that syllable, the stressed vowel is: the accented one; otherwise the first
   strong vowel (a, e, o); if there are only weak ones, the last (*cui-da* → i,
   *ciu-dad* → u), skipping the silent u of *qu/gu*.
3. The written ending runs from that vowel to the end: *cántaro* → *ántaro*.

### Consonant key (phonetic)

The ending is normalised so that **sounds** are compared, not letters:

| Rule | Example |
|---|---|
| Silent h | *hoy* → *oi* |
| g before e/i sounds like j | *gente* → *jente* |
| qu before e/i → k; gu before e/i → g | *queso* → *keso*, *guerra* → *gerra* |
| ü → u | |
| c before e/i and z → θ (with *seseo* → s) | *caza* → *kaθa* / *kasa* |
| Any other c → k | |
| v → b | *b* and *v* sound the same |
| ll → y (*yeísmo*) | |
| y at the end or before a consonant → i | *hoy* → *oi* |
| Accents removed | *ántaro* → *antaro* |

So *guerra / tierra*, *gente / fuente*, *hoy / voy* and *fuego / juego* are full rhymes.

### Assonant key

- The stressed vowel + the vowel of the **last syllable** (in a diphthong, the strong one).
- In an unstressed last syllable, **i ≈ e** and **u ≈ o**: *fácil / calle*, *Venus / tenso*.
- If the word is *aguda*, the key is just the stressed vowel (*amor* → *o*).
- Examples: *cántaro / pájaro* (a-o), *sangre / hambre* (a-e). *mar / montaña* don't rhyme
  (*aguda* vs *llana*).

### Scheme

`esquema(lines)` returns one element per line: `null` for empty lines and
`RimaVerso(letra, tipo, terminacion)` (letter, type, ending) for the rest.

1. Lines with the **same consonant key** are grouped (groups of 2 or more).
2. The remaining lines join an existing group with the same **assonant** key; if there is
   none, they form a new group when there are 2 or more.
3. Groups are ordered by first appearance and get letters A, B, C… (a, b, c… with
   `minusculas = true`, used for *arte menor*).
4. Lines that rhyme with nothing get `-`.

Example: a *romance* (*-a-a-a*) or a sonnet (*ABBA ABBA*). In `esquemaComoTexto` empty lines
show as a space: *"AA AA"*.

---

## Recursos (literary devices)

```kotlin
val devices: List<Recursos.Recurso> = Recursos.detectar(text, seseo = false)
```

```kotlin
data class Recurso(
    val tipo: Tipo,                  // ALITERACION, ANAFORA, …
    val lineas: List<Int>,           // line indices (0 = the text's first line)
    val evidencia: String,           // short text to display: «temprano», sonido «s» ×6…
    val palabras: List<String>,      // words involved (may be empty)
    val intensidad: Double? = null   // alliterations only
) { val clara: Boolean }             // intensidad == null || intensidad >= 4.5
```

| `Tipo` | Device |
|---|---|
| `ALITERACION` | Alliteration |
| `ANAFORA` | Anaphora |
| `EPIFORA` | Epiphora (epistrophe) |
| `ANADIPLOSIS` | Anadiplosis |
| `EPANADIPLOSIS` | Epanadiplosis |
| `GEMINACION` | Geminatio (immediate repetition) |
| `POLISINDETON` | Polysyndeton |
| `ASINDETON` | Asyndeton |
| `PARALELISMO` | Parallelism |
| `ESTRIBILLO` | Refrain |
| `RIMA_INTERNA` | Internal rhyme |

Results are ordered by first line and then by type.

### Stanzas

Most detectors work **within each stanza** (runs of consecutive non-empty lines). An empty
line breaks runs: an anaphora does not jump from one stanza to the next. The refrain is
the exception: it is searched across the whole text.

### Unstressed words

To avoid a flood of false positives, repeated function words are ignored: articles
(*el, la, lo, los, las, un, una, unos, unas, le, les*), conjunctions (*y, e, ni, o, u*) and
other unstressed words (*al, del, de, a, en, con, por, sin, que, se, me, te, nos, os, mi,
tu, su, mis, tus, sus*).

### Repetition detectors

| Device | Condition | Highlighted words |
|---|---|---|
| **Anaphora** | 2+ consecutive lines starting the same way. If the shared start is a single unstressed word, 3+ lines are needed | The shared prefix |
| **Polysyndeton** (at the start) | Like anaphora, but the first word is a conjunction | The conjunction |
| **Polysyndeton** (in a line) | 2+ conjunctions (*y, e, ni, o, u*) in one line | The conjunctions |
| **Epiphora** | 2+ consecutive lines ending the same way (a single unstressed word doesn't count) | The shared suffix |
| **Anadiplosis** | A line ends with the word the next one starts with (not unstressed) | Last word of the first line and first of the second |
| **Epanadiplosis** | A line of 3+ words that starts and ends with the same word (not unstressed) | First and last word |
| **Geminatio** | The same word (2+ letters) twice in a row | The adjacent repetitions |
| **Refrain** | The same line (2+ words) appears 2+ times anywhere | The whole lines |

Fully identical lines don't count as anaphora or epiphora (that is a refrain).

### Internal rhyme

A word **inside** a line (not the last one) that rhymes with another word. All the words of
a stanza that share a rhyme form **one** device, with the words as evidence
(`«soneto» · «aprieto»`) and highlighted; `Recurso.rima` says which kind it is.

**Full rhymes** (`CONSONANTE`), using the same consonant key as end rhyme (so *seseo*
applies: *casa / caza*):
- an inner word with another inner word of the same line (*la **luna** sobre la **laguna**
  se dormía*);
- an inner word with the last word of **any line of the stanza** (*tu **corazón** es mi
  **canción***; *Un **soneto** me manda hacer Violante, / que en mi vida me he visto en
  tanto **aprieto***);
- an inner word with an inner word of the next line (*la noche **oscura** se cierra / con
  **amargura** en el alma*).

Two inner words only count if both have two syllables or more: short words (*es / tres*)
rhyme by chance all the time. A short inner word can still rhyme with a line ending.

**Assonant rhymes** (`ASONANTE`, shown as *Rima interna · asonante*). Vowel rhymes appear by
chance far more often, so the rules are stricter:
- only an inner word with the last word of **its own line or of the line before or after**
  (*la **casa blanca** de la **plaza***);
- only words stressed before the last syllable (a two-vowel key, *a-a*); words stressed on
  the last syllable have a one-vowel key (*volverán / colgar*) that matches too many words.

Always ignored: unstressed function words, one-letter full-rhyme endings, and the same word
repeated, also in its plural or a longer form (*verde / verdes*).

Even so, some assonant internal rhymes will be coincidences; that is why they are labelled
and coloured more softly.

### Structure detectors

| Device | Condition |
|---|---|
| **Asyndeton** | Split by commas or semicolons, the line gives 3+ segments of at most 3 words, the last one doesn't start with a conjunction, and they aren't all the same |
| **Parallelism** | Consecutive lines with the same number of words (4+), not identical, matching in at least 3 positions and in at least half of them. For the comparison, definite articles, indefinite articles and possessives each count as one class (*el mar* ≈ *la luna*) |

### Alliteration

This is the most elaborate detector, because every Spanish line repeats consonants: what
matters is that a sound appears **much more often than usual**.

**1. Sounds, not letters.** Each word goes through the same phonetic normalisation as
rhyme (*c/qu/k* = k, *b/v* = b, *ll/y* = y, *g/j* before e/i = j…). The trilled *r*
(word-initial, after n/l/s, or *rr*) is told apart from the tap *r*.

**2. Only consonants in the onset**, i.e. at the start of a syllable (followed by a vowel
or forming an inseparable cluster like *pr*, *bl*). Syllable-final consonants (the *s* of
*más*) are barely noticed in alliteration and skew the count.

**3. Expected frequency.** Each sound is compared with its normal onset frequency in
Spanish prose:

| Sound | Frequency | Sound | Frequency |
|---|---|---|---|
| t | 10.4 % | s | 6.2 % (9.1 % with *seseo*) |
| k (c/qu) | 10.0 % | θ (z/c) | 2.9 % |
| d | 9.5 % | rr | 2.4 % |
| r | 9.1 % | y/ll | 2.2 % |
| m | 8.6 % | g | 2.2 % |
| p | 8.4 % | f | 2.0 % |
| b/v | 7.7 % | ñ | 1.3 % |
| n | 7.5 % | j/g | 1.1 % |
| l | 7.5 % | ch | 0.7 % |

Other sounds: 1.5 %. **Intensity** = (occurrences / total onsets) ÷ expected frequency.

**4. Conditions** for accepting a sound in a window of lines:

- Intensity ≥ **3.5** (≥ **4.5** is a *clear* alliteration; below that, *possible*).
- At least 3 occurrences in one line, or 4 in a pair of lines.
- In at least 3 **different roots** (first 4 letters): *caminante / camino* count once.
- In two-line windows, the sound must appear **in both** lines.
- Articles count towards the total but not as occurrences.

**5. Windows and merging.** Each line is analysed on its own, and so is each pair of
consecutive lines in the stanza. Overlapping pairs with the same sound are merged into a
single alliteration (lines 1–2 and 2–3 → lines 1–3), and single lines already covered by a
wider alliteration of the same sound are dropped.

Examples from the tests:

| Text | Result |
|---|---|
| bajo el ala aleve del leve abanico | b/v ×4 (4.7×, **clear**) and l ×3 (3.6×, **possible**) |
| en el silencio sólo se escuchaba / un susurro de abejas que sonaba | s ×6 over 2 lines |
| mis manos buscan tu mirada muda / mientras la madrugada muere mansa | a single m alliteration over lines 1–2 |
| Volverán las oscuras golondrinas / en tu balcón sus nidos a colgar | nothing |
| Puedo escribir los versos más tristes esta noche | nothing |

The evidence shows how the sound is spelled: `sonido «b/v» ×4`, `sonido «z/c» ×3`,
`sonido «s» ×6 en 2 versos`.

### What it doesn't detect

**Semantic** devices (metaphor, simile, personification, hyperbole, antithesis…) can't be
reliably recognised with rules; that would need a language model. Near rhymes,
hyperbaton and onomatopoeia aren't detected either.

---

## AnalisisPoema: the full analysis

This is the entry point used by the editor. It does a single pass and returns everything
needed to draw the screen.

```kotlin
val r = AnalisisPoema.analizar(text, seseo = false)

r.texto        // the analysed text (so the UI knows if it lags behind)
r.metro        // dominant metre, or null if there are no lines
r.lineas       // one Linea per line of text, including empty ones
r.recursos     // same as Recursos.detectar
r.esquema      // "ABBA ABBA": a letter per line, a space between stanzas
```

```kotlin
data class Linea(
    val inicio: Int,           // offset of the line's first character in the text
    val fin: Int,              // offset of the line break (or the end of the text)
    val silabas: Int?,         // null for lines without words
    val encaja: Boolean,       // does the line admit the dominant metre?
    val rima: Rima.RimaVerso?  // null for lines without words
)
```

- **Syllables shown**: if the line admits the metre, the metre; otherwise the value in its
  range closest to the metre (and `encaja = false`). Without a metre, the minimum.
- **Lowercase** scheme letters if the metre is 8 syllables or fewer (*arte menor*).
- Lines are split on `\n` only.

### What to highlight for each device

`AnalisisPoema.rangos(r, device)` returns character ranges (`IntRange`) over `r.texto`:

| Device | What is highlighted in each line |
|---|---|
| Anaphora | The first *k* words (*k* = words in the shared prefix), not other occurrences of the word in the line |
| Epiphora | The last *k* words |
| Anadiplosis | The first line's last word and the second line's first word |
| Epanadiplosis | First and last word |
| Geminatio | Only the adjacent occurrences of the repeated word |
| Alliteration, polysyndeton | Every word in the line that is in `palabras` |
| Parallelism, asyndeton, refrain | The whole line, from its first to its last letter (without surrounding punctuation) |

Matching is case-insensitive.

### Rhyme colouring spans

`AnalisisPoema.tramosDeRima(r)` returns the pieces of text to colour when rhyme colouring
is on, as `TramoRima(rango, grupo, tipo)` (range, colour group, rhyme type):

- the **rhyming ending** (from the stressed vowel to the end of the word) of every line that
  rhymes with another, with the group of its letter (A = 0, B = 1…) and its type
  (`CONSONANTE` or `ASONANTE`). Unrhymed lines (`-`) are not coloured;
- the words of every **internal rhyme**, with their own type (assonant ones softer): with the
  group of the line whose end rhyme they share (*soneto* takes the colour of *aprieto*'s
  group) or, if there is none, a new group after the letters' groups.

Example (Lope's quatrain): *son**eto*** (internal, B), *Viol**ante*** (A),
*apri**eto*** (B), *v**ersos*** (internal assonant, B), *son**eto*** (B), *del**ante*** (A).

---

## Tests

`app/src/test/java/com/tuapp/analisis/`:

| Class | Tests | Coverage |
|---|---|---|
| `SilabeadorTest` | 9 | Basic syllabification, digraphs, silent u and diaeresis, diphthongs and triphthongs, hiatus, y, consonant clusters, stress type, word extraction |
| `MetricaTest` | 8 | *Sinalefa* (with h), final-stress rule, *dialefa* on a stressed vowel, alexandrine, final y, metre not admitted, dominant metre of a sonnet |
| `RimaTest` | 8 | Endings, full rhymes, assonant rhymes, non-rhymes, *seseo*, sonnet and *romance* schemes, assonant/consonant merging |
| `RecursosTest` | 24 | Each device type, alliteration in one and two lines, merging, intensity, internal rhymes (full and assonant), and no false positives in well-known lines |
| `AnalisisPoemaTest` | 13 | Metre and scheme of a quatrain, *arte menor*, a line that doesn't fit, offsets, empty text, highlight ranges, rhyme colouring spans |

To add a case, use real lines of verse (by well-known authors when possible) and add a
comment explaining why the result is the expected one.
