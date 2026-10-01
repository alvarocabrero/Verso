// =================================================================================================
// FILE: AnalisisPoema.kt  (the full analysis of a poem)
// -------------------------------------------------------------------------------------------------
// `package` names the "logical folder" this file belongs to: the analysis engine. All files with
// the same `package` line can use each other directly, with no `import`.
// =================================================================================================
package com.tuapp.analisis

// =================================================================================================
// WHAT IS THIS FILE FOR?
//
// It is the "front door" of the analysis engine: the one function the editor calls. It takes the
// whole text of a poem and, in one go, gathers everything the screen needs:
//   - the dominant meter (for example 11, *endecasílabo*);
//   - for each line: where it starts and ends in the text, how many syllables to show, whether
//     it fits the meter, and its rhyme letter;
//   - the literary devices found;
//   - the rhyme scheme as text ("ABBA ABBA").
// It also turns each literary device and each rhyme into RANGES OF CHARACTERS (for example
// "characters 12 to 17"), so the editor knows exactly which letters to highlight or colour.
//
// ROLE IN THE APP
// While you type, the editor's ViewModel (EditorViewModel, in ui/editor) calls
// `AnalisisPoema.analizar` in the background, and the screen draws the numbers, letters and
// highlights from the `Resultado` it gets back.
//
// RELATION WITH OTHER FILES
//   - Metrica.kt: measures every line and picks the dominant meter.
//   - Rima.kt: gives the rhyme letter of each line and the rhyming endings.
//   - Recursos.kt: finds the literary devices.
//   - ui/editor (outside the engine): uses `analizar`, `rangos` and `tramosDeRima`.
//
// A NOTE ON "POSITIONS" IN A TEXT
// A text is a row of characters numbered from 0. In "ab\ncd" the characters are a=0, b=1, the
// line break=2, c=3, d=4. A RANGE such as 3..4 means "from character 3 to character 4, both
// included". The editor uses these numbers to know where to paint.
// =================================================================================================

/**
 * Full analysis of a text to show it in the editor: syllables of each line fitted to the
 * dominant meter, rhyme letter and literary devices. It also turns each device into ranges of
 * characters so it can be highlighted.
 *
 * KOTLIN SYNTAX: `object`
 * An `object` is a class with exactly ONE copy in the whole program (a "singleton"). It is used
 * directly by its name: `AnalisisPoema.analizar(texto)`.
 */
object AnalisisPoema {

    /**
     * One line of the text. [inicio]/[fin]: its position in the full text.
     * [silabas]: the count to show (null for lines without words); if the line admits the
     * dominant meter, it is that meter; if not, the closest value in its range, and [encaja] is
     * false.
     *
     * Boxes:
     * - [inicio] ("start"): position of the line's first character in the whole text.
     * - [fin] ("end"): position of the line break that ends it (or of the end of the text).
     * - [silabas] ("syllables"): number to show next to the line, or null. `Int?` = "a whole
     *   number, or nothing".
     * - [encaja] ("fits"): true if the line can measure the dominant meter. The editor shows the
     *   number in red when it is false.
     * - [rima]: the line's rhyme letter and ending (see Rima.kt), or null for lines without
     *   words.
     *
     * KOTLIN SYNTAX: `data class`
     * A class made only to hold data, like a form with boxes. Each `val name: Type` is a
     * read-only box.
     */
    data class Linea(
        val inicio: Int,
        val fin: Int,
        val silabas: Int?,
        val encaja: Boolean,
        val rima: Rima.RimaVerso?
    )

    /**
     * Everything the analysis produces for a text.
     *
     * Boxes:
     * - [texto]: the exact text that was analysed. The editor compares it with what is on screen
     *   now: since the analysis runs in the background, the user may have typed more in the
     *   meantime, and then the highlights would point at the wrong letters.
     * - [seseo]: whether *seseo* was used (s, z and c before e/i treated as the same sound).
     * - [metro]: the dominant meter, or null if there are no lines with words.
     * - [lineas]: one [Linea] per line of the text, including empty ones.
     * - [recursos]: the literary devices (same as `Recursos.detectar`).
     * - [esquema]: the rhyme scheme as text (computed, see below).
     */
    data class Resultado(
        val texto: String,
        val seseo: Boolean,
        val metro: Int?,
        val lineas: List<Linea>,
        val recursos: List<Recursos.Recurso>
    ) {
        /**
         * "ABBA ABBA": one letter per line, a space between stanzas.
         *
         * KOTLIN SYNTAX: a property with `get()` has no stored value; the code after `get() =`
         * runs every time someone reads it.
         *
         * HOW IT WORKS:
         * 1. `joinToString("") { l -> ... }` glues together, with nothing in between, one piece
         *    per line: its letter as text, or a space if it has no rhyme info (empty line).
         *    `l.rima?.letra?.toString()`: the `?.` ("safe call") goes on only if the value is not
         *    null; `?: " "` ("Elvis operator") gives a space when the result is null.
         * 2. `.trim()` removes spaces at the very start and end.
         * 3. `.replace(Regex(" +"), " ")` turns any run of several spaces into a single one (the
         *    Regex ` +` means "one or more spaces"), so two empty lines still give one gap.
         */
        val esquema: String
            get() = lineas.joinToString("") { l -> l.rima?.letra?.toString() ?: " " }
                .trim().replace(Regex(" +"), " ")
    }

    /**
     * Analyses a whole text. This is the function the editor calls.
     *
     * Receives: [texto], the full text of the note; [seseo], optional (false by default; the
     * `= false` makes it a default value so callers can leave it out).
     * Returns: a [Resultado] with everything.
     *
     * STEPS:
     * 1. Split the text into lines, ONLY at "\n" (the line-break character).
     * 2. Measure every line (Metrica.Verso) and pick the dominant meter.
     * 3. Compute the rhyme scheme. Letters are lowercase if the meter is 8 or less (*arte
     *    menor*, short lines, are traditionally written abab; *arte mayor*, ABAB).
     * 4. For each line, build its [Linea]:
     *    - the syllables to show: none for a line without words; the minimum of its range if
     *      there is no meter; otherwise the meter "clamped" into its range (the meter itself if
     *      the line admits it, or the nearest end of the range if not);
     *    - its start and end positions, keeping a running count (`inicio`).
     * 5. Detect the literary devices and build the result.
     */
    fun analizar(texto: String, seseo: Boolean = false): Resultado {
        // Step 1.
        val lineas = texto.split("\n")
        // Step 2. `map { ... }` builds a Verso for each line (`it`).
        val versos = lineas.map { Metrica.Verso(it) }
        val metro = Metrica.metroDominante(versos)
        // Step 3. `minusculas = ...` passes the parameter by NAME, which makes clear which
        // option is being set. `metro != null && metro <= 8`: there is a meter AND it is 8 or
        // less.
        val rimas = Rima.esquema(lineas, seseo, minusculas = metro != null && metro <= 8)

        // Step 4. `inicio` is the position where the current line starts. It is `var` because
        // it moves forward after every line.
        var inicio = 0
        // `mapIndexed { i, l -> ... }` transforms each line `l` (with its position `i`) into
        // the value of the last expression in the lambda (here, `linea`).
        val res = lineas.mapIndexed { i, l ->
            val v = versos[i]
            // `when { ... }` with no value: the first true condition wins.
            val silabas = when {
                v.palabras.isEmpty() -> null
                metro == null -> v.minimo
                // `coerceIn(a, b)` keeps the number inside a..b: if it is smaller than a it
                // gives a, if bigger than b it gives b, otherwise the number itself.
                else -> metro.coerceIn(v.minimo, v.maximo)
            }
            // Parameters passed by name (`inicio = ...`) to make the code easier to read.
            val linea = Linea(
                inicio = inicio,
                // The end is the start plus the line's length: the position of its "\n".
                fin = inicio + l.length,
                silabas = silabas,
                // Fits if there is no meter (nothing to fit) or if the line admits it.
                encaja = metro == null || v.admite(metro),
                rima = if (v.palabras.isEmpty()) null else rimas[i]
            )
            // The next line starts after this one and its "\n" (that is the + 1).
            inicio += l.length + 1
            // The last expression of the lambda is its result.
            linea
        }
        // Step 5.
        return Resultado(texto, seseo, metro, res, Recursos.detectar(lineas, seseo))
    }

    // ---------- Highlighting ----------

    /**
     * A word together with where it is in the whole text.
     * `IntRange` is a range of whole numbers, written `a..b` (both ends included).
     * `private` = only usable inside this `object`.
     */
    private data class Palabra(val texto: String, val rango: IntRange)

    /**
     * The words of line number [linea] of [r], in lowercase, each with its range of characters
     * in the WHOLE text (not just in the line).
     *
     * HOW IT WORKS:
     * - `r.texto.substring(l.inicio, l.fin)` cuts out the line.
     * - `Regex("\\p{L}+").findAll(...)` finds every word in it (`\p{L}+` = one or more letters;
     *   the backslash is written twice inside Kotlin text).
     * - For each match, `it.range` is its position inside the LINE; adding `l.inicio` turns it
     *   into a position in the whole text. `.first` and `.last` are the two ends of a range.
     */
    private fun palabrasConPosicion(r: Resultado, linea: Int): List<Palabra> {
        val l = r.lineas[linea]
        return Regex("\\p{L}+").findAll(r.texto.substring(l.inicio, l.fin)).map {
            Palabra(it.value.lowercase(), (l.inicio + it.range.first)..(l.inicio + it.range.last))
        }.toList()
    }

    /**
     * Ranges of characters (in [Resultado.texto]) taken up by [recurso]: the specific words when
     * there are any and, if not, the whole lines (*paralelismo*, *asíndeton*, *estribillo*).
     *
     * Receives: [r], the analysis result; [recurso], one device from `r.recursos`.
     * Returns: a list of ranges to highlight. Comparisons ignore capital letters.
     *
     * WHAT IS HIGHLIGHTED IN EACH LINE, BY DEVICE:
     * - *Anáfora*: the first k words of the line (k = how many words the device lists), not
     *   other appearances of the same word later in the line.
     * - *Epífora*: the last k words.
     * - *Anadiplosis*: the last word of the first line and the first word of the second.
     * - *Epanadiplosis*: the first and the last word.
     * - *Geminación*: only the repeated word where it appears next to itself.
     * - Others with words (*aliteración*, *polisíndeton*, *rima interna*): every word of the line
     *   that is in the device's word list.
     * - Others without words (*paralelismo*, *asíndeton*, *estribillo*): the whole line, from its
     *   first letter to its last (punctuation at both ends left out).
     */
    fun rangos(r: Resultado, recurso: Recursos.Recurso): List<IntRange> {
        // The device's words in lowercase, as a set for quick "is it in here?" checks.
        val buscadas = recurso.palabras.map { it.lowercase() }.toSet()
        // Keep only line numbers that exist (`in r.lineas.indices`), then, for each one,
        // compute a list of ranges and join all the lists into one (`flatMapIndexed`).
        // `orden` is the line's place in the device (0 = first line of the device); `li` is the
        // line number.
        return recurso.lineas.filter { it in r.lineas.indices }.flatMapIndexed { orden, li ->
            val ps = palabrasConPosicion(r, li)
            // A line without words gives nothing. `return@flatMapIndexed x` ends this turn of
            // the lambda with the value x (a LABELED return), not the whole function.
            if (ps.isEmpty()) return@flatMapIndexed emptyList()
            // Number of words in the device.
            val k = recurso.palabras.size
            // Which words of the line to highlight, depending on the device type.
            val elegidas = when (recurso.tipo) {
                // `take(k)` = the first k items; `takeLast(k)` = the last k.
                Recursos.Tipo.ANAFORA -> ps.take(k)
                Recursos.Tipo.EPIFORA -> ps.takeLast(k)
                // First line of the device: its last word; second line: its first word.
                Recursos.Tipo.ANADIPLOSIS -> listOf(if (orden == 0) ps.last() else ps.first())
                Recursos.Tipo.EPANADIPLOSIS -> listOf(ps.first(), ps.last())
                // The positions `j` whose word is the searched one AND whose previous or next
                // word is the same word. `getOrNull` gives null at the edges of the line
                // instead of crashing, and `?.texto` goes on only if there is a word.
                Recursos.Tipo.GEMINACION -> ps.indices
                    .filter { j -> ps[j].texto in buscadas &&
                        (ps.getOrNull(j - 1)?.texto == ps[j].texto || ps.getOrNull(j + 1)?.texto == ps[j].texto) }
                    .map { ps[it] }
                // Every other device. With no words: the whole line, as one range from the
                // first letter of the first word to the last letter of the last word.
                // With words: every word of the line that is in the list.
                else -> if (buscadas.isEmpty()) return@flatMapIndexed listOf(ps.first().rango.first..ps.last().rango.last)
                        else ps.filter { it.texto in buscadas }
            }
            // Turn the chosen words into their ranges.
            elegidas.map { it.rango }
        }
    }

    // ---------- Rhyme colouring ----------

    /**
     * The rhyming ending (from the stressed vowel to the end of the word) with its colour
     * [grupo] and the [tipo] of rhyme.
     *
     * - [rango]: the characters to colour.
     * - [grupo] ("group"): a number that picks the colour: 0 for the lines with letter A, 1 for
     *   B, and so on. Internal rhymes that match no line ending get numbers after those.
     * - [tipo]: CONSONANTE or ASONANTE (the editor paints the vowel rhyme in a softer colour).
     */
    data class TramoRima(val rango: IntRange, val grupo: Int, val tipo: Rima.Tipo)

    /**
     * Range of the rhyming ending of [palabra], which takes up [rangoPalabra] in the text.
     *
     * The ending ("ántaro" in "cántaro") is always at the END of the word, so its range is the
     * last N characters of the word, where N is the ending's length: from (end − N + 1) to end.
     * Returns null if the word has no ending (`?: return null` leaves at once in that case).
     */
    private fun terminacionEn(palabra: String, rangoPalabra: IntRange, seseo: Boolean): IntRange? {
        val t = Rima.terminacion(palabra, seseo) ?: return null
        val fin = rangoPalabra.last
        return (fin - t.texto.length + 1)..fin
    }

    /**
     * Pieces of text to colour so the rhymes can be seen:
     * - the ending of each line that rhymes with another, with the group of its letter
     *   (A = 0, B = 1…) and its type (*consonante* or *asonante*). Lines that rhyme with nothing
     *   (letter '-') are not coloured;
     * - both words of each internal rhyme (*rima interna*): with the group of the line whose
     *   final rhyme they share or, if there is none, with a new group after the letter groups.
     *   They are always coloured as *consonante*.
     *
     * Example (a Lope de Vega quatrain): "son[eto]" (internal, B), "Viol[ante]" (A),
     * "apri[eto]" (B), "son[eto]" (B), "del[ante]" (A).
     *
     * Receives: [r], the analysis result. Returns: the pieces, sorted by position in the text.
     */
    fun tramosDeRima(r: Resultado): List<TramoRima> {
        val out = mutableListOf<TramoRima>()
        // A map from a full-rhyme key ("eto") to its colour group, so internal rhymes can reuse
        // the colour of the line ending they match.
        val grupoPorClave = mutableMapOf<String, Int>()

        // PART 1: line endings.
        r.lineas.forEachIndexed { i, l ->
            // Each of these lines leaves this turn of the loop (`return@forEachIndexed`) when
            // something is missing. `?:` gives the right side when the left side is null.
            val rima = l.rima ?: return@forEachIndexed
            // Line that rhymes with nothing: not coloured.
            val tipo = rima.tipo ?: return@forEachIndexed
            // Letter to number: subtracting two characters gives the distance between them, so
            // 'a' − 'a' = 0, 'b' − 'a' = 1... (`lowercaseChar()` handles both 'A' and 'a').
            val grupo = rima.letra.lowercaseChar() - 'a'
            // The last word of the line, with its position.
            val ultima = palabrasConPosicion(r, i).lastOrNull() ?: return@forEachIndexed
            val rango = terminacionEn(ultima.texto, ultima.rango, r.seseo) ?: return@forEachIndexed
            out += TramoRima(rango, grupo, tipo)
            // Remember this key's group, unless one is already remembered for it.
            grupoPorClave.putIfAbsent(rima.terminacion.consonante, grupo)
        }

        // PART 2: internal rhymes.
        // The first free group number: one more than the largest used (or 0 if none).
        // `maxOfOrNull { ... }` gives the largest value, or null for an empty list.
        var siguiente = (out.maxOfOrNull { it.grupo } ?: -1) + 1
        r.recursos.filter { it.tipo == Recursos.Tipo.RIMA_INTERNA }.forEach { recurso ->
            // The two words of the rhyme, each as a pair (text, range). `substring(range)` cuts
            // the text at that range; `a to b` makes a pair.
            val palabras = rangos(r, recurso).map { r.texto.substring(it) to it }
            // Their shared full-rhyme key: the first non-null key among the words.
            // `{ (p, _) -> ... }` takes each pair apart; `_` = "this part is not needed".
            val clave = palabras.firstNotNullOfOrNull { (p, _) -> Rima.terminacion(p, r.seseo)?.consonante }
                ?: return@forEach
            // The group of a line ending with the same key, or else a new group.
            // `getOrPut(key) { ... }` reads the map, and if the key is missing, stores and
            // returns the lambda's value. `siguiente++` gives the current value and THEN adds 1.
            val grupo = grupoPorClave.getOrPut(clave) { siguiente++ }
            palabras.forEach { (p, rango) ->
                val t = terminacionEn(p, rango, r.seseo) ?: return@forEach
                // Add it unless that exact piece is already coloured (e.g. it is also a line
                // ending). `none { ... }` = "no item meets this".
                if (out.none { it.rango == t }) out += TramoRima(t, grupo, Rima.Tipo.CONSONANTE)
            }
        }
        // Sort by where each piece starts in the text.
        return out.sortedBy { it.rango.first }
    }
}
