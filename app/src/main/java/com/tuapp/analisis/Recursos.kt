// =================================================================================================
// FILE: Recursos.kt  (literary device detector)
// -------------------------------------------------------------------------------------------------
// `package` names the "logical folder" this file belongs to: the analysis engine. All files with
// the same `package` line can use each other directly, with no `import`.
// =================================================================================================
package com.tuapp.analisis

// =================================================================================================
// WHAT IS THIS FILE FOR?
//
// It reads a poem and finds *recursos literarios* (literary devices): patterns of sound,
// repetition and structure that poets use on purpose, such as repeating the same word at the
// start of several lines (*anáfora*) or repeating a consonant sound (*aliteración*).
//
// ROLE IN THE APP
// In the editor, the analysis panel lists the devices found ("Anáfora: «verde»", "Aliteración:
// sonido «s» ×6"...). When you tap one, the editor highlights the words involved. The list comes
// from `Recursos.detectar`; the highlighting is computed in AnalisisPoema.kt from the data here.
//
// RELATION WITH OTHER FILES
//   - Silabeador.kt: `palabrasDe` cuts each line into words.
//   - Rima.kt: `terminacion` finds internal rhymes; `fonetica` turns words into sounds for the
//     alliteration detector.
//   - AnalisisPoema.kt: calls `detectar` and turns each result into ranges of characters to
//     highlight.
//
// THE DEVICES IT DETECTS (names in Spanish, as used by the app)
//   - *Aliteración* (alliteration): the same consonant SOUND repeated much more than usual in
//     one or two lines. "bajo el ala aleve del leve abanico" (b/v and l).
//   - *Anáfora* (anaphora): several lines in a row START with the same word(s).
//   - *Epífora* (epiphora): several lines in a row END with the same word(s).
//   - *Anadiplosis*: a line ends with the same word the next line starts with.
//   - *Epanadiplosis*: a line starts and ends with the same word.
//   - *Geminación* (geminatio): the same word twice in a row ("¡agua, agua!").
//   - *Polisíndeton* (polysyndeton): many conjunctions ("y", "ni", "o"...) used on purpose.
//   - *Asíndeton* (asyndeton): a list of items with commas and NO conjunction.
//   - *Paralelismo* (parallelism): lines in a row with the same grammatical shape.
//   - *Estribillo* (refrain): a whole line repeated in several places of the text.
//   - *Rima interna* (internal rhyme): a word INSIDE a line rhymes with another word nearby.
//
// WHAT IT CANNOT DETECT
// Devices that depend on MEANING (metaphor, simile, personification, hyperbole...) cannot be
// found with simple rules; that would need a language model (an AI).
//
// TWO IDEAS USED EVERYWHERE BELOW
//   - STANZAS (*estrofas*): most detectors work inside each stanza, that is, each block of lines
//     with no empty line between them. An empty line breaks a run: an *anáfora* does not jump
//     from one stanza to the next. The refrain is the exception: it is searched in the whole text.
//   - UNSTRESSED FILLER WORDS (*átonos*): to avoid false alarms, repetitions of little words
//     like "el", "la", "de", "y" are usually ignored (almost every line has them).
// =================================================================================================

/**
 * Detector of pattern-based literary devices (sound, repetition, structure). Devices based on
 * meaning (metaphor, simile, personification...) cannot be detected with rules: that needs a
 * language model.
 *
 * KOTLIN SYNTAX: `object`
 * An `object` is a class with exactly ONE copy in the whole program (a "singleton"). It is used
 * directly by its name: `Recursos.detectar(texto)`.
 */
object Recursos {

    /**
     * The kinds of device. Each one carries two texts shown to the user in the app (in
     * Spanish): a display name ([nombre]) and a short description ([descripcion]).
     *
     * KOTLIN SYNTAX: `enum class` with values
     * An `enum class` is a type with a fixed list of options. Here each option also carries
     * data: `enum class Tipo(val nombre: String, val descripcion: String)` says every option
     * has a name and a description, and each option below fills them in, e.g.
     * `ANAFORA("Anáfora", "...")`. The options are separated by commas.
     * Every option also has an automatic number, `ordinal` (0 for the first, 1 for the
     * second...), used later to sort results.
     */
    enum class Tipo(val nombre: String, val descripcion: String) {
        ALITERACION("Aliteración", "Repetición de un mismo sonido consonántico"),
        ANAFORA("Anáfora", "Varios versos empiezan con las mismas palabras"),
        EPIFORA("Epífora", "Varios versos terminan con las mismas palabras"),
        ANADIPLOSIS("Anadiplosis", "Un verso empieza con la palabra con que acaba el anterior"),
        EPANADIPLOSIS("Epanadiplosis", "El verso empieza y termina con la misma palabra"),
        GEMINACION("Geminación", "Una palabra se repite seguida"),
        POLISINDETON("Polisíndeton", "Uso repetido de conjunciones"),
        ASINDETON("Asíndeton", "Enumeración sin conjunciones"),
        PARALELISMO("Paralelismo", "Versos consecutivos con la misma estructura"),
        ESTRIBILLO("Estribillo", "Verso que se repite a lo largo del texto"),
        RIMA_INTERNA("Rima interna", "Una palabra dentro del verso rima con otra del mismo verso o con el final de un verso vecino")
    }

    /**
     * One device found in the text.
     *
     * Boxes:
     * - [tipo]: which device it is.
     * - [lineas]: the line numbers involved (0 = first line of the text).
     * - [evidencia]: a short text to show the user, e.g. «temprano» or sonido «s» ×6.
     * - [palabras]: the words to highlight (may be empty). `= emptyList()` is a default value:
     *   if the creator does not give it, it is an empty list.
     * - [intensidad]: only for *aliteración*; how many times the sound goes over its normal
     *   frequency in Spanish (e.g. 4.7 means "4.7 times more than usual"). `Double` is a number
     *   with decimals; `Double?` means it may also be null ("nothing"), and `= null` makes null
     *   the default.
     * - [clara] ("clear"): tells a "clear" alliteration from a "possible" one.
     *
     * KOTLIN SYNTAX:
     * - `data class`: a class made only to hold data, like a form with boxes; `val` = read-only.
     * - The braces after the parentheses hold extra members of the class.
     * - `val clara: Boolean get() = ...` is a COMPUTED property: it has no stored value; every
     *   time someone reads `clara`, the expression after `get() =` is worked out again.
     */
    data class Recurso(
        val tipo: Tipo,
        val lineas: List<Int>,
        val evidencia: String,
        val palabras: List<String> = emptyList(),
        val intensidad: Double? = null
    ) {
        // Clear if it has no intensity (every device other than alliteration) or if the
        // intensity reaches the "clear" threshold. `||` = "or".
        val clara: Boolean get() = intensidad == null || intensidad >= UMBRAL_CLARA
    }

    /**
     * From this intensity on, an alliteration is considered clear, not just possible.
     * `const val` = a fixed constant. It is public, so the editor can use it too.
     */
    const val UMBRAL_CLARA = 4.5

    // Spanish conjunctions that join items: "y"/"e" (and), "ni" (nor), "o"/"u" (or).
    // `setOf(...)` creates a set (a collection with no repeats, quick to ask "is it in here?").
    private val CONJUNCIONES = setOf("y", "e", "ni", "o", "u")
    // Articles ("the", "a"...) plus the pronouns "le"/"les".
    private val ARTICULOS = setOf("el", "la", "lo", "los", "las", "un", "una", "unos", "unas", "le", "les")
    // All the unstressed filler words: articles + conjunctions + prepositions ("de", "en",
    // "con"...), unstressed pronouns ("se", "me", "te"...) and possessives ("mi", "tu", "su"...).
    // The `+` between sets joins them.
    private val ATONOS = ARTICULOS + CONJUNCIONES + setOf(
        "al", "del", "de", "a", "en", "con", "por", "sin", "que", "se", "me", "te",
        "nos", "os", "mi", "tu", "su", "mis", "tus", "sus"
    )

    /**
     * Finds all devices in a text given as one single string.
     *
     * It just cuts the text into lines (`texto.lines()`) and calls the other `detectar` below.
     * Kotlin allows two functions with the same name if they receive different types (this is
     * called "overloading"): this one takes a `String`, the next one a `List<String>`.
     * `seseo: Boolean = false`: optional parameter, false by default (see Rima.kt: whether
     * s, z and c before e/i sound the same).
     */
    fun detectar(texto: String, seseo: Boolean = false): List<Recurso> = detectar(texto.lines(), seseo)

    /**
     * Finds all devices in a text given as a list of lines. This is the main entry point.
     *
     * Receives: [lineas], the lines of the text; [seseo].
     * Returns: the list of devices, sorted by first line and, within a line, by device type.
     *
     * STEPS:
     * 1. Turn every line into its list of lowercase words (`pal`).
     * 2. Find the stanzas (`bloques`).
     * 3. For each stanza, run the detectors that look at SEVERAL lines: *anáfora* and
     *    *polisíndeton* at the start, *epífora*, *anadiplosis*, *paralelismo*, *aliteración*
     *    and *rima interna*.
     * 4. For each non-empty line, run the detectors that look at ONE line: *epanadiplosis*,
     *    *geminación*, *polisíndeton* inside the line and *asíndeton*.
     * 5. Look for refrains (*estribillos*) in the whole text.
     * 6. Sort everything.
     */
    fun detectar(lineas: List<String>, seseo: Boolean = false): List<Recurso> {
        // Step 1. A list of lists: for each line, its words in lowercase.
        // `map { l -> ... }` names the lambda's item `l` instead of the default `it`.
        val pal = lineas.map { l -> Silabeador.palabrasDe(l).map { it.lowercase() } }
        // Step 2.
        val bloques = bloques(pal)
        // The results. `mutableListOf` = a list that can grow.
        val out = mutableListOf<Recurso>()

        // Step 3. `out += list` adds every item of the list to `out`.
        bloques.forEach { b ->
            out += anaforaYPolisindeton(b, pal)
            out += epifora(b, pal)
            out += anadiplosis(b, pal)
            out += paralelismo(b, pal)
            out += aliteraciones(b, lineas, seseo)
            out += rimasInternas(b, lineas, seseo)
        }
        // Step 4. `forEachIndexed { i, w -> ... }` gives each line's words `w` with its
        // position `i`.
        pal.forEachIndexed { i, w ->
            // Skip empty lines. `return@forEachIndexed` is a LABELED return: it only ends the
            // current turn of the lambda (like "continue with the next line"), not the whole
            // function.
            if (w.isEmpty()) return@forEachIndexed
            // Each of these may return a device or null. `?.let { out += it }` means "if it is
            // not null, add it to the results".
            epanadiplosis(i, w)?.let { out += it }
            geminacion(i, w)?.let { out += it }
            polisindetonEnVerso(i, w)?.let { out += it }
            asindeton(i, lineas[i])?.let { out += it }
        }
        // Step 5.
        out += estribillos(pal)
        // Step 6. `compareBy({ A }, { B })` sorts by A, and on a tie by B: first by the first
        // line involved, then by the device's position in the Tipo list (`ordinal`).
        return out.sortedWith(compareBy({ it.lineas.first() }, { it.tipo.ordinal }))
    }

    // ---------- Helpers ----------

    /**
     * Groups of consecutive non-empty lines (stanzas).
     *
     * Receives: [pal], the words of every line. Returns: a list of stanzas, each one being the
     * list of its line positions. For lines [A, B, (empty), C] it gives [[0, 1], [3]].
     */
    private fun bloques(pal: List<List<String>>): List<List<Int>> {
        // All the stanzas found so far.
        val res = mutableListOf<MutableList<Int>>()
        // The stanza being built right now. `var` because we swap it for a new one.
        var actual = mutableListOf<Int>()
        pal.forEachIndexed { i, w ->
            // Empty line: close the current stanza (if it has lines) and start a new one.
            if (w.isEmpty()) { if (actual.isNotEmpty()) res += actual; actual = mutableListOf() }
            // Line with words: add it to the current stanza.
            else actual += i
        }
        // Do not forget the last stanza (there may be no empty line after it).
        if (actual.isNotEmpty()) res += actual
        return res
    }

    /**
     * Walks through runs of consecutive lines where [clave] ("key") gives the same result.
     *
     * For example, with `clave` = "first word of the line", it finds groups of lines in a row
     * that start with the same word, and calls [accion] ("action") with each group of 2 or more.
     *
     * KOTLIN SYNTAX: FUNCTION TYPES
     * - `clave: (Int) -> String?` means `clave` is itself a FUNCTION: it receives an `Int` (a
     *   line position) and returns a `String?` (a text, or null).
     * - `accion: (List<Int>) -> Unit` is a function that receives a list of positions and
     *   returns `Unit`, which means "nothing useful" (it just does something).
     * Passing functions as parameters lets the same run-finding logic be reused by several
     * detectors, each one giving its own key and action.
     */
    private fun rachas(b: List<Int>, clave: (Int) -> String?, accion: (List<Int>) -> Unit) {
        var i = 0
        while (i < b.size) {
            // The key of the line where the run starts.
            val k = clave(b[i])
            // `j` moves forward while the next line has the same key. A null key never makes a
            // run.
            var j = i
            while (k != null && j + 1 < b.size && clave(b[j + 1]) == k) j++
            // If the run has 2 or more lines, run the action on it. `subList(i, j + 1)` = the
            // positions from i to j, both included.
            if (j > i) accion(b.subList(i, j + 1))
            // Continue after the run.
            i = j + 1
        }
    }

    /**
     * The words that ALL the given lines share at the start. For ["verde", "que", "te"] and
     * ["verde", "viento"] it gives ["verde"].
     *
     * `ls.all { ... }` is true when the lambda is true for every item. We keep increasing `k`
     * while every line has a word at position k and it equals the first line's word there.
     * Then `take(k)` returns the first k words.
     */
    private fun prefijoComun(ls: List<List<String>>): List<String> {
        var k = 0
        while (ls.all { it.size > k && it[k] == ls[0][k] }) k++
        return ls[0].take(k)
    }

    /**
     * True if all the lines in [run] have exactly the same words. Used to skip them: identical
     * lines are a refrain, not an *anáfora* or *epífora*. Short `=` form: returns the result.
     */
    private fun todasIguales(run: List<Int>, pal: List<List<String>>) = run.all { pal[it] == pal[run[0]] }

    // ---------- Repetition ----------

    /**
     * *Anáfora* and *polisíndeton* at the start of lines, in one stanza [b].
     *
     * - *Anáfora*: 2 or more lines in a row start with the same word(s). If what they share is
     *   only ONE filler word ("la... / la..."), at least 3 lines are needed (2 is too common).
     *   The shared words are highlighted.
     * - *Polisíndeton* (at the start): the same, but the shared first word is a conjunction
     *   ("Y... / Y... / Y..."). It is reported as polysyndeton instead of anaphora.
     */
    private fun anaforaYPolisindeton(b: List<Int>, pal: List<List<String>>): List<Recurso> {
        val res = mutableListOf<Recurso>()
        // Key = first word of the line (`firstOrNull` gives null for a line with no words).
        // The last lambda is written OUTSIDE the parentheses: in Kotlin, when the last
        // parameter of a function is a function, it can go after the `)`. `run` is the list of
        // line positions of each run.
        rachas(b, { pal[it].firstOrNull() }) { run ->
            // Identical lines: a refrain, not an anaphora. `return@rachas` ends only this call
            // of the lambda (moves on to the next run).
            if (todasIguales(run, pal)) return@rachas
            // The words they share at the start, and the first of them.
            val pref = prefijoComun(run.map { pal[it] })
            val w = pref[0]
            when {
                // Starts with a conjunction: polysyndeton. The evidence text reads, e.g.,
                // «y» al inicio de 3 versos ("«y» at the start of 3 lines"). `${run.size}`
                // inserts the number of lines into the text.
                w in CONJUNCIONES ->
                    res += Recurso(Tipo.POLISINDETON, run, "«$w» al inicio de ${run.size} versos", listOf(w))
                // "la… / la…" is too common. Only one filler word shared by fewer than 3 lines:
                // do nothing (`Unit` = "nothing").
                pref.size == 1 && w in ATONOS && run.size < 3 -> Unit
                // Otherwise: anaphora. The evidence is the shared words joined with spaces.
                else -> res += Recurso(Tipo.ANAFORA, run, "«${pref.joinToString(" ")}»", pref)
            }
        }
        return res
    }

    /**
     * *Epífora*: 2 or more lines in a row that END with the same word(s), in stanza [b].
     * A single filler word does not count. The shared final words are highlighted.
     *
     * Trick: to find the common ENDING, each line's words are reversed, the common START is
     * found with `prefijoComun`, and the result is reversed back.
     */
    private fun epifora(b: List<Int>, pal: List<List<String>>): List<Recurso> {
        val res = mutableListOf<Recurso>()
        // Key = last word of the line.
        rachas(b, { pal[it].lastOrNull() }) { run ->
            if (todasIguales(run, pal)) return@rachas
            // Common suffix (ending) of the lines.
            val suf = prefijoComun(run.map { pal[it].reversed() }).reversed()
            // Only one filler word in common: ignore.
            if (suf.size == 1 && suf[0] in ATONOS) return@rachas
            res += Recurso(Tipo.EPIFORA, run, "«${suf.joinToString(" ")}»", suf)
        }
        return res
    }

    /**
     * *Anadiplosis*: a line ends with the word the next line starts with (not a filler word).
     * Example: "...que de ti me aparta. / Aparta tu mirada...". Both words are highlighted.
     *
     * KOTLIN SYNTAX:
     * - `zipWithNext()` makes pairs of neighbours: [1, 2, 3] gives (1,2), (2,3).
     * - `{ (x, y) -> ... }` takes each pair apart into `x` and `y` ("destructuring").
     * - `mapNotNull` transforms each item and throws away the null results.
     */
    private fun anadiplosis(b: List<Int>, pal: List<List<String>>): List<Recurso> =
        b.zipWithNext().mapNotNull { (x, y) ->
            // Last word of line x.
            val w = pal[x].last()
            // Same as the first word of line y, and not a filler word: found one.
            if (w == pal[y].first() && w !in ATONOS) Recurso(Tipo.ANADIPLOSIS, listOf(x, y), "«$w»", listOf(w))
            else null
        }

    /**
     * *Epanadiplosis*: a line of 3 or more words that starts and ends with the same word (not a
     * filler word). Example: "Verde que te quiero verde". Returns the device, or null.
     *
     * Receives: [i], the line position; [w], its words.
     */
    private fun epanadiplosis(i: Int, w: List<String>): Recurso? =
        if (w.size >= 3 && w.first() == w.last() && w.first() !in ATONOS)
            Recurso(Tipo.EPANADIPLOSIS, listOf(i), "«${w.first()}»", listOf(w.first()))
        else null

    /**
     * *Geminación*: the same word (2 letters or more) twice in a row in one line.
     * Example: "¡Agua, agua!". Only the first repeated pair is reported.
     *
     * `firstOrNull { (a, b) -> ... }` finds the first pair of neighbours that are equal and at
     * least 2 letters long (to skip "y y" or "a a"). If found, `?.let { (a, _) -> ... }` builds
     * the device; `_` means "I do not need this part of the pair".
     */
    private fun geminacion(i: Int, w: List<String>): Recurso? =
        w.zipWithNext().firstOrNull { (a, b) -> a == b && a.length >= 2 }?.let { (a, _) ->
            Recurso(Tipo.GEMINACION, listOf(i), "«$a, $a»", listOf(a))
        }

    /**
     * *Estribillo* (refrain): the same line (2 words or more) appears 2 or more times anywhere
     * in the text. The whole lines are highlighted.
     *
     * STEPS:
     * 1. Keep the positions of lines with at least 2 words.
     * 2. Group them by their words joined into one text (so punctuation and capitals do not
     *    matter). `groupBy` gives a map: text → list of positions.
     * 3. Keep the groups with 2 or more lines.
     * 4. Turn each into a device. `(texto, idx)` takes each map entry apart into key and value.
     *    The evidence reads «line text» (×3).
     */
    private fun estribillos(pal: List<List<String>>): List<Recurso> =
        pal.indices.filter { pal[it].size >= 2 }
            .groupBy { pal[it].joinToString(" ") }
            .filter { it.value.size >= 2 }
            .map { (texto, idx) -> Recurso(Tipo.ESTRIBILLO, idx, "«$texto» (×${idx.size})") }

    // ---------- Conjunctions and lists ----------

    /**
     * *Polisíndeton* inside one line: 2 or more conjunctions (y, e, ni, o, u) in the same line.
     * Example: "y la luna y el mar y el viento". The conjunctions are highlighted (each
     * different one listed once, thanks to `distinct()`). The evidence reads, e.g.,
     * "3 conjunciones en el verso" ("3 conjunctions in the line").
     */
    private fun polisindetonEnVerso(i: Int, w: List<String>): Recurso? {
        // All the words of the line that are conjunctions.
        val conj = w.filter { it in CONJUNCIONES }
        return if (conj.size >= 2)
            Recurso(Tipo.POLISINDETON, listOf(i), "${conj.size} conjunciones en el verso", conj.distinct())
        else null
    }

    /**
     * *Asíndeton*: a list with no conjunction. The line, cut at commas or semicolons, gives 3
     * or more pieces of 3 words or fewer; the last piece does not start with a conjunction
     * (otherwise it is a normal list: "A, B y C"); and the pieces are not all the same (that
     * would be a *geminación*). Example: "Acude, corre, vuela". The whole line is highlighted.
     * The evidence reads, e.g., "3 elementos sin conjunción" ("3 items with no conjunction").
     */
    private fun asindeton(i: Int, linea: String): Recurso? {
        // Cut the line at every "," or ";" (the Regex `[,;]` means "a comma or a semicolon"),
        // turn each piece into its lowercase words, and drop empty pieces.
        val segs = linea.split(Regex("[,;]")).map { Silabeador.palabrasDe(it).map { w -> w.lowercase() } }
            .filter { it.isNotEmpty() }
        // All the conditions must be true (`&&` = "and").
        // The last one: "palabras, palabras, palabras" is a *geminación*, not an asyndeton.
        val ok = segs.size >= 3 &&
                segs.all { it.size <= 3 } &&
                segs.last().first() !in CONJUNCIONES &&
                segs.distinct().size > 1
        return if (ok) Recurso(Tipo.ASINDETON, listOf(i), "${segs.size} elementos sin conjunción") else null
    }

    // ---------- Internal rhyme ----------

    /**
     * Full rhymes (*rima consonante*) that involve a word INSIDE a line (not its last word):
     * - with another inner word of the same line ("la luna en la laguna"),
     * - with the last word of the same line ("mi corazón es tu canción"),
     * - with the last word of the line before or after (in the same stanza).
     * Only full rhyme counts: vowel rhyme (*asonante*) shows up by chance in almost every line.
     * Filler words, the same word repeated, and endings of a single letter are ignored.
     *
     * Each pair is reported once, with both words as evidence («soneto» · «aprieto») and both
     * highlighted.
     *
     * Receives: [b], the stanza (line positions); [lineas], all the lines; [seseo].
     */
    private fun rimasInternas(b: List<Int>, lineas: List<String>, seseo: Boolean): List<Recurso> {
        // A tiny helper class used only here: a word and its full-rhyme key (or null if the
        // word should be ignored).
        class Pal(val texto: String, val clave: String?)

        // A LOCAL FUNCTION (a function defined inside another function, only usable here).
        // It turns line `li` into its list of `Pal`. The key is:
        // - null for filler words;
        // - otherwise the full-rhyme key from Rima.kt, but only if it is 2 letters or longer.
        //   `takeIf { ... }` keeps the value if the condition is true, and gives null if not.
        //   The `?.` chain stops at the first null.
        fun palabras(li: Int) = Silabeador.palabrasDe(lineas[li]).map { p ->
            val clave = if (p.lowercase() in ATONOS) null
                        else Rima.terminacion(p, seseo)?.consonante?.takeIf { it.length >= 2 }
            Pal(p, clave)
        }

        // A map from each line position of the stanza to its words (computed once).
        // `associateWith { ... }` builds a map: each item → the lambda's result for it.
        val porLinea = b.associateWith { palabras(it) }
        val res = mutableListOf<Recurso>()
        // Pairs already reported, so none is reported twice. Each pair is stored as a SET of
        // two texts "line:word", so (A, B) and (B, A) count as the same pair.
        val vistos = mutableSetOf<Set<String>>()

        // Local function: records one rhyme between word `a` and word `c`, in the lines given.
        fun anadir(lineasRecurso: List<Int>, a: Pal, c: Pal) {
            // The same word twice ("luna ... luna") is repetition, not rhyme.
            if (a.texto.lowercase() == c.texto.lowercase()) return
            val par = setOf("${lineasRecurso.first()}:${a.texto.lowercase()}", "${lineasRecurso.last()}:${c.texto.lowercase()}")
            // `add` returns false if the pair was already in the set: then skip it.
            if (!vistos.add(par)) return
            res += Recurso(Tipo.RIMA_INTERNA, lineasRecurso, "«${a.texto}» · «${c.texto}»", listOf(a.texto, c.texto))
        }

        // Go through each line of the stanza: `k` = its place inside the stanza, `li` = its
        // position in the whole text.
        b.forEachIndexed { k, li ->
            // `getValue` reads the map and is sure the key exists.
            val ps = porLinea.getValue(li)
            // A line with fewer than 2 words has no "inside".
            if (ps.size < 2) return@forEachIndexed
            // Inner words = all but the last (`dropLast(1)`), and only those with a key.
            val interiores = ps.dropLast(1).filter { it.clave != null }
            // The last word of the line.
            val final = ps.last()

            // Inside the same line: inner with inner, inner with final.
            interiores.forEachIndexed { i, a ->
                // Compare `a` with every inner word AFTER it (`drop(i + 1)`), so each pair is
                // checked only once.
                interiores.drop(i + 1).filter { it.clave == a.clave }.forEach { c -> anadir(listOf(li), a, c) }
                // Compare `a` with the last word of the line.
                if (final.clave != null && final.clave == a.clave) anadir(listOf(li), a, final)
            }
            // With the last word of the neighbouring lines in the same stanza.
            // `listOfNotNull(x, y)` makes a list with x and y, leaving out any that is null
            // (the first line has no previous one, the last has no next one).
            listOfNotNull(b.getOrNull(k - 1), b.getOrNull(k + 1)).forEach { otro ->
                // Last word of the other line (or skip if it has none).
                val finalOtro = porLinea.getValue(otro).lastOrNull() ?: return@forEach
                if (finalOtro.clave == null) return@forEach
                // Every inner word of this line that rhymes with it. The two line numbers are
                // sorted so the device always lists the earlier line first.
                interiores.filter { it.clave == finalOtro.clave }.forEach { a ->
                    anadir(listOf(li, otro).sorted(), a, finalOtro)
                }
            }
        }
        return res
    }

    // ---------- Structure ----------

    /**
     * Word "class" used to compare line structures for *paralelismo*: definite articles become
     * "ART", indefinite ones "IND", possessives "POS"; any other word stays as it is. This way
     * "el mar" and "la luna" share their first position (both ART).
     */
    private fun clase(w: String): String = when (w) {
        "el", "la", "lo", "los", "las" -> "ART"
        "un", "una", "unos", "unas" -> "IND"
        "mi", "tu", "su", "mis", "tus", "sus" -> "POS"
        else -> w
    }

    /**
     * Are lines [a] and [b] parallel? Yes when they have the same number of words (4 or more),
     * are not identical, and match (by `clase`) in at least 3 positions AND in at least half of
     * the positions.
     */
    private fun paralelos(a: List<String>, b: List<String>): Boolean {
        if (a.size != b.size || a.size < 4 || a == b) return false
        // How many positions match. `count { ... }` counts the positions where the lambda is
        // true.
        val coincidencias = a.indices.count { clase(a[it]) == clase(b[it]) }
        // `coincidencias * 2 >= a.size` is "at least half" written without decimals.
        return coincidencias >= 3 && coincidencias * 2 >= a.size
    }

    /**
     * *Paralelismo*: runs of consecutive lines, in stanza [b], where each pair of neighbours is
     * parallel. The whole lines are highlighted. The evidence reads, e.g.,
     * "3 versos con la misma estructura" ("3 lines with the same structure").
     */
    private fun paralelismo(b: List<Int>, pal: List<List<String>>): List<Recurso> {
        val res = mutableListOf<Recurso>()
        // The run being built.
        var run = mutableListOf<Int>()
        // `for ((x, y) in ...)` goes through each pair of neighbouring lines.
        for ((x, y) in b.zipWithNext()) {
            if (paralelos(pal[x], pal[y])) {
                // Starting a run: add the first line; then always add the second one.
                if (run.isEmpty()) run += x
                run += y
            } else {
                // The run is broken: save it (if there is one) and start again.
                if (run.isNotEmpty()) res += Recurso(Tipo.PARALELISMO, run, "${run.size} versos con la misma estructura")
                run = mutableListOf()
            }
        }
        // Save the last run if the stanza ended in the middle of one.
        if (run.isNotEmpty()) res += Recurso(Tipo.PARALELISMO, run, "${run.size} versos con la misma estructura")
        return res
    }

    // ---------- Alliteration ----------
    //
    // This is the most elaborate detector, because in Spanish ANY line repeats consonants. What
    // matters is that a sound appears MUCH MORE THAN NORMAL. The idea:
    //   1. Work with SOUNDS, not letters (c/qu/k are one sound; b/v are one sound...).
    //   2. Count only ONSET consonants: those at the START of a syllable (followed by a vowel, or
    //      forming an inseparable pair such as "pr", "bl"). Consonants at the end of a syllable
    //      (the s in "más") are barely heard in alliteration and would distort the count.
    //   3. Compare each sound's share with its normal share in Spanish prose. The ratio is the
    //      "intensity": 1 = normal, 4 = four times more than normal.
    //   4. Accept it only if it is strong enough, repeated enough, and spread over different
    //      words.

    /**
     * How often each consonant appears at the start of a syllable in ordinary Spanish prose
     * (as a share: .104 = 10.4 %). θ = z / ce / ci; R = the strong, rolled r (rr).
     *
     * `mapOf("t" to .104, ...)` builds a read-only map (a table of key → value pairs); `a to b`
     * makes one pair. `.104` is the decimal number 0.104.
     */
    private val FRECUENCIA_BASE = mapOf(
        "t" to .104, "k" to .100, "d" to .095, "r" to .091, "m" to .086, "p" to .084,
        "b" to .077, "n" to .075, "l" to .075, "s" to .062, "θ" to .029, "R" to .024,
        "y" to .022, "g" to .022, "f" to .020, "ñ" to .013, "j" to .011, "ch" to .007
    )
    /**
     * How each internal sound symbol is shown to the user: the sound "k" is shown as "c/qu",
     * "θ" as "z/c", and so on. Sounds not in this table are shown as they are.
     */
    private val GRAFIA = mapOf(
        "k" to "c/qu", "θ" to "z/c", "R" to "rr", "b" to "b/v", "y" to "y/ll", "j" to "j/g"
    )
    // Minimum intensity to report an alliteration at all ("possible" from here, "clear" from
    // UMBRAL_CLARA).
    private const val UMBRAL = 3.5
    // Consonant pairs that stay together at the start of a syllable, written in sound symbols
    // (k instead of c): "pr", "bl", "kr"...
    private val INSEPARABLES = setOf("pr", "br", "kr", "gr", "fr", "tr", "dr", "pl", "bl", "kl", "gl", "fl")

    /**
     * The onset consonants (at the start of a syllable, not at the end) of one word, as sound
     * symbols.
     *
     * Example: "trueno" gives [t, r, n]: "t" forms the inseparable pair "tr", "r" is followed
     * by the vowel "u", and "n" by the vowel "o". In general a consonant counts when the next
     * sound is a vowel or when it forms an inseparable pair with the next one. In "más" the
     * "s" is followed by nothing, so it does not count.
     *
     * STEPS:
     * 1. Turn the word into sounds with Rima.fonetica.
     * 2. Mark the strong r as "R": an r at the start of the word, an r after n, l or s
     *    ("honra", "alrededor", "Israel"), and "rr". (Regex: `^r` = "r at the very start";
     *    `(?<=[nls])r` = "an r that comes right after n, l or s".)
     * 3. Cut the sounds into tokens: "ch" is one token, every other character is one token.
     * 4. Keep each consonant token whose next token is a vowel or makes an inseparable pair.
     */
    private fun ataques(palabra: String, seseo: Boolean): List<String> {
        // Step 1.
        var f = Rima.fonetica(palabra.lowercase(), seseo)
        // Step 2.
        f = f.replace(Regex("^r"), "R").replace(Regex("(?<=[nls])r"), "R").replace("rr", "R")
        // Step 3.
        val toks = mutableListOf<String>()
        var i = 0
        while (i < f.length) {
            // `startsWith("ch", i)` = "does the text have 'ch' at position i?".
            if (f.startsWith("ch", i)) { toks += "ch"; i += 2 } else { toks += f[i].toString(); i++ }
        }
        // Step 4. For each token position `k`, give back the token if it is an onset
        // consonant, or null (and `mapNotNull` drops the nulls).
        return toks.indices.mapNotNull { k ->
            val t = toks[k]
            // The next token, or null at the end of the word.
            val sig = toks.getOrNull(k + 1)
            // A consonant: not a vowel, and a letter (`isLetter()`) or the θ symbol.
            val esConsonante = t !in listOf("a", "e", "i", "o", "u") && (t[0].isLetter() || t == "θ")
            // Onset: a next token exists and is a vowel, or together they form an inseparable
            // pair.
            if (esConsonante && sig != null && (sig in listOf("a", "e", "i", "o", "u") || t + sig in INSEPARABLES)) t
            else null
        }
    }

    /**
     * Counts of onset sounds in a group of lines (a "window" of one or two lines).
     *
     * KOTLIN SYNTAX: a `class` whose properties are set up inside the braces. `var total = 0`
     * is a changeable number starting at 0. `mutableMapOf<String, Int>()` is an empty,
     * changeable table from sound to number.
     *
     * - [total]: how many onset consonants there are in total (all sounds together).
     * - [cuenta]: for each sound, how many times it appears.
     * - [raices] ("roots"): for each sound, the different word roots (first 4 letters) where it
     *   appears; so "caminante" and "camino" count as one root.
     * - [ejemplos] ("examples"): for each sound, the words where it appears (to highlight them).
     * - [enLineas] ("in lines"): for each sound, the lines where it appears.
     */
    private class Estadisticas {
        var total = 0
        val cuenta = mutableMapOf<String, Int>()
        val raices = mutableMapOf<String, MutableSet<String>>()
        val ejemplos = mutableMapOf<String, MutableSet<String>>()
        val enLineas = mutableMapOf<String, MutableSet<Int>>()
    }

    /**
     * Fills an [Estadisticas] for the lines [idx].
     *
     * Articles count towards the TOTAL but not as appearances of a sound: "la" adds one "l" to
     * the total but is not counted as an "l" appearance (articles would inflate the count).
     */
    private fun estadisticas(idx: List<Int>, lineas: List<String>, seseo: Boolean): Estadisticas {
        val e = Estadisticas()
        // Two nested loops written on one line: for each line `li`, for each word `p` of it.
        for (li in idx) for (p in Silabeador.palabrasDe(lineas[li])) {
            // The onset sounds of this word.
            val a = ataques(p, seseo)
            e.total += a.size
            // Articles: counted in the total only. `continue` skips to the next word.
            if (p.lowercase() in ARTICULOS) continue
            for (t in a) {
                // Add 1 to this sound's count. `(e.cuenta[t] ?: 0)`: the current count, or 0 if
                // the sound was not in the table yet.
                e.cuenta[t] = (e.cuenta[t] ?: 0) + 1
                // `getOrPut(t) { mutableSetOf() }`: take the set for sound t, creating an empty
                // one first if there is none; then add to it with `+=`.
                // The root: the first 4 letters (caminante / camino = 1 root).
                e.raices.getOrPut(t) { mutableSetOf() } += p.lowercase().take(4)
                e.ejemplos.getOrPut(t) { mutableSetOf() } += p
                e.enLineas.getOrPut(t) { mutableSetOf() } += li
            }
        }
        return e
    }

    /**
     * A candidate alliteration: one sound in one window of lines.
     * - [sonido]: the sound symbol; [lineas]: the lines; [n]: how many times it appears;
     * - [intensidad]: how many times over the normal frequency; [ejemplos]: the words.
     */
    private class Candidato(
        val sonido: String, val lineas: List<Int>, val n: Int,
        val intensidad: Double, val ejemplos: Set<String>
    )

    /**
     * Alliterated sounds in the lines [idx]. In windows of several lines the sound must appear
     * in all of them, so that an alliteration packed into one line is not mistaken for one
     * spread over two.
     *
     * Receives: [idx], the lines of the window; [lineas]; [seseo]; [minimo], the minimum number
     * of appearances (3 for one line, 4 for a pair of lines).
     * Returns: the sounds that pass every test.
     *
     * THE TESTS (all must pass):
     * - appears at least [minimo] times;
     * - in at least 3 different word roots;
     * - intensity = (appearances ÷ total onsets) ÷ normal frequency, at least UMBRAL (3.5);
     * - appears in every line of the window.
     */
    private fun candidatos(idx: List<Int>, lineas: List<String>, seseo: Boolean, minimo: Int): List<Candidato> {
        val e = estadisticas(idx, lineas, seseo)
        // No consonants at all: nothing to find (and avoid dividing by zero).
        if (e.total == 0) return emptyList()
        // For each sound `t` and its count `n`...
        return e.cuenta.mapNotNull { (t, n) ->
            // Normal frequency of this sound. With *seseo*, "s" also covers z and soft c, so it
            // is more common (9.1 %). Sounds not in the table get 1.5 %.
            val base = if (seseo && t == "s") .091 else (FRECUENCIA_BASE[t] ?: .015)
            // `toDouble()` turns the whole number into a decimal so the division keeps decimals.
            val intensidad = n.toDouble() / e.total / base
            val ok = n >= minimo && e.raices[t]!!.size >= 3 && intensidad >= UMBRAL &&
                     e.enLineas[t]!!.size == idx.size
            if (ok) Candidato(t, idx, n, intensidad, e.ejemplos[t]!!) else null
        }
    }

    /**
     * Alliterations of one stanza [b]. Each line is analysed on its own, and each pair of
     * consecutive lines; overlapping pairs with the same sound are merged, and single lines
     * already covered by a wider alliteration of the same sound are dropped.
     *
     * STEPS:
     * 1. `individuales`: candidates in each single line (at least 3 appearances).
     * 2. `ventanas` ("windows"): candidates in each pair of neighbouring lines (at least 4).
     * 3. Merge: sort the pairs by sound and first line; when a pair has the same sound as the
     *    previous result and overlaps it (lines 1-2 and 2-3), join them into one (lines 1-3),
     *    recounting the sound over the joined lines and keeping the higher intensity.
     * 4. Drop the single-line results whose line and sound are already inside a merged one.
     * 5. Turn everything into [Recurso]s. The evidence reads, e.g., sonido «b/v» ×4
     *    ("sound b/v ×4"), adding " en 2 versos" ("in 2 lines") when it spans several lines.
     */
    private fun aliteraciones(b: List<Int>, lineas: List<String>, seseo: Boolean): List<Recurso> {
        // Step 1. `flatMap` is like `map` but joins all the resulting lists into one.
        val individuales = b.flatMap { candidatos(listOf(it), lineas, seseo, 3) }
        // Step 2.
        val ventanas = b.zipWithNext().flatMap { (x, y) -> candidatos(listOf(x, y), lineas, seseo, 4) }

        // Step 3.
        val fusionadas = mutableListOf<Candidato>()
        for (v in ventanas.sortedWith(compareBy({ it.sonido }, { it.lineas.first() }))) {
            // The last merged result so far (or null if none yet).
            val u = fusionadas.lastOrNull()
            // Same sound and overlapping lines: merge.
            if (u != null && u.sonido == v.sonido && u.lineas.last() >= v.lineas.first()) {
                // All lines of both, without repeats, in order.
                val union = (u.lineas + v.lineas).distinct().sorted()
                // Recount the sound over the joined lines.
                val n = estadisticas(union, lineas, seseo).cuenta[v.sonido] ?: 0
                // Replace the last result with the merged one. `maxOf(a, b)` = the larger one;
                // `u.ejemplos + v.ejemplos` joins both sets of words.
                fusionadas[fusionadas.lastIndex] =
                    Candidato(v.sonido, union, n, maxOf(u.intensidad, v.intensidad), u.ejemplos + v.ejemplos)
            // No overlap: this pair is a new result.
            } else fusionadas += v
        }

        // Step 4. Every (line, sound) pair covered by a merged result. `it to f.sonido` makes a
        // pair (line, sound); `toSet()` makes the lookup quick.
        val cubiertos = fusionadas.flatMap { f -> f.lineas.map { it to f.sonido } }.toSet()
        val sueltas = individuales.filter { (it.lineas[0] to it.sonido) !in cubiertos }

        // Step 5.
        return (fusionadas + sueltas).map { c ->
            val donde = if (c.lineas.size > 1) " en ${c.lineas.size} versos" else ""
            // `GRAFIA[c.sonido] ?: c.sonido`: the user-facing spelling, or the symbol itself.
            Recurso(Tipo.ALITERACION, c.lineas, "sonido «${GRAFIA[c.sonido] ?: c.sonido}» ×${c.n}$donde",
                c.ejemplos.toList(), c.intensidad)
        }
    }
}
