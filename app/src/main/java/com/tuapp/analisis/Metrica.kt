// =================================================================================================
// FILE: Metrica.kt  (the "meter" counter)
// -------------------------------------------------------------------------------------------------
// `package` names the "logical folder" this file belongs to: the analysis engine. All files with
// the same `package` line can use each other directly.
// =================================================================================================
package com.tuapp.analisis

// `import` brings a name from somewhere else so it can be used here by its short name. Without
// this line we would have to write `Silabeador.TipoAcentual.AGUDA` every time; with it, we can
// write just `TipoAcentual.AGUDA`. (TipoAcentual is the "stress type" defined in Silabeador.kt.)
import com.tuapp.analisis.Silabeador.TipoAcentual

// =================================================================================================
// WHAT IS THIS FILE FOR?
//
// It counts the METRICAL syllables of each line of a poem and decides which meter (line length)
// the poem uses. For example, it tells you that a sonnet is written in *endecasílabos*
// (11-syllable lines).
//
// ROLE IN THE APP
// In the editor, next to each line, Verso shows a number: how many syllables that line has.
// That number comes from here. It also decides the "dominant meter" of the poem, and marks the
// lines that do not fit it.
//
// RELATION WITH OTHER FILES
//   - Silabeador.kt: gives the grammatical syllables of each word and its stress type. This file
//     builds on that.
//   - AnalisisPoema.kt: calls `Verso`, `metroDominante` and uses `minimo`/`maximo`/`admite` to
//     decide what number to show next to each line.
//   - Rima.kt: the rhyme letters are written in lowercase when the meter is 8 or less; that
//     decision is made in AnalisisPoema.kt using the meter computed here.
//
// SPANISH METRICS: THE CONCEPTS
//   - *Sílaba métrica* (metrical syllable): the syllables you actually hear when reading a line
//     aloud. They are NOT always the same as the grammatical syllables, for two reasons:
//
//   1. *Sinalefa*: when a word ends in a vowel and the next one starts with a vowel, the two
//      vowels are said together, merged into ONE syllable. Example: "no hay" is "no‿hay"
//      (1 syllable, not 2). A silent "h" does not stop it: "se hace" = "se‿ha-ce".
//      The poet may choose NOT to merge them; that is called *dialefa* (keeping the vowels in
//      separate syllables). Because of this, a line does not have one fixed count but a RANGE.
//
//   2. *Ley del acento final* (final-stress rule): Spanish meters are measured as if every line
//      ended in a *llana* word (stress on the second to last syllable). So:
//        - if the last word is *aguda* (stress on the last syllable), ADD 1 syllable;
//        - if it is *esdrújula* or *sobresdrújula* (stress earlier), SUBTRACT 1;
//        - if it is *llana*, leave it as it is.
//
//   - *Metro* (meter): the number of metrical syllables a line should have. Each has a name:
//     *octosílabo* (8), *endecasílabo* (11), *alejandrino* (14)... Lines of 8 or fewer syllables
//     are called *arte menor* ("minor art"); 9 or more, *arte mayor* ("major art").
//
// WHAT THIS FILE DOES NOT HANDLE (known limits)
//   - *Diéresis* (splitting a diphthong into two syllables on purpose: sü-a-ve) and *sinéresis*
//     (merging a hiatus into one syllable: poe-ta).
//   - The two halves (*hemistiquios*) of the *alejandrino*.
//   - Poems that mix several meters: only one meter is chosen for the whole text.
// =================================================================================================

/**
 * Metrical analysis of Spanish verse.
 *
 * Since *sinalefa* is optional in poetry, a line does not have one single count but a
 * RANGE: [minimo] (all *sinalefas* applied) up to [maximo] (none applied).
 * Each *sinalefa* removes exactly one syllable, so every value in the range can be
 * reached. [metroDominante] picks the value that fits the most lines.
 *
 * KOTLIN SYNTAX: `object`
 * An `object` is a class with exactly ONE copy in the whole program (a "singleton"). You use
 * it directly by its name, for example `Metrica.metroDominante(...)`, without creating it.
 */
object Metrica {

    /**
     * Unstressed words (*átonos*): one-syllable words with no stress of their own (articles,
     * prepositions, conjunctions, unstressed pronouns, possessives placed before the noun).
     *
     * Why this matters: when deciding which *sinalefa* to break first, we need to know if the
     * vowel at the joint is stressed. A one-syllable word like "mi" or "la" is not stressed, but
     * one like "sol" or "fue" is. The syllable splitter cannot tell them apart, so we keep this
     * list.
     *
     * KOTLIN SYNTAX:
     * - `private` = only usable inside this `object`.
     * - `val` = a value that cannot be reassigned.
     * - `setOf(...)` creates a set: a collection with no repeats, good for asking
     *   "is this word in here?".
     */
    private val ATONOS = setOf(
        "el", "la", "lo", "los", "las", "un", "una", "unos", "unas", "al", "del",
        "de", "a", "en", "con", "por", "sin", "so", "y", "e", "o", "u", "ni", "que",
        "se", "me", "te", "le", "les", "nos", "os", "mi", "tu", "su", "mis", "tus", "sus"
    )
    // All vowels, with and without accent mark, and ü. Used to ask "does this word start/end
    // with a vowel?". `const val` = a fixed constant known before the program runs.
    private const val VOCALES = "aeiouáéíóúü"

    /**
     * A possible *sinalefa* between word number [indice] and the next word.
     * [resistencia] ("resistance"): 2 if the final vowel is stressed (está‿en), 1 if the
     * first vowel of the next word is stressed (mi‿alma), 0 if neither is. The most resistant
     * ones are broken first when a line has to be stretched to fit a meter.
     *
     * Why: a *sinalefa* where one vowel is stressed sounds forced, so poets usually keep those
     * vowels apart (*dialefa*). A *sinalefa* between two unstressed vowels is the most natural
     * and is kept as long as possible.
     *
     * KOTLIN SYNTAX: `data class`
     * A class made only to hold data, like a small form. `val indice: Int` is a read-only box
     * called `indice` that holds a whole number.
     */
    data class Sinalefa(val indice: Int, val resistencia: Int)

    /**
     * One line of verse, fully measured.
     *
     * You create one by passing the text of the line: `Metrica.Verso("Verde que te quiero
     * verde")`. At that moment all its properties below are computed.
     *
     * KOTLIN SYNTAX: `class`
     * A normal `class` is a template for building objects. `class Verso(val texto: String)`
     * means: to build a Verso you must give it a text, and that text is kept in a read-only
     * property called `texto`. (Unlike `object`, you can create as many Versos as you want:
     * one per line of the poem.)
     *
     * Each `val` inside the braces is a property computed once, when the Verso is created, in
     * the order they are written.
     */
    class Verso(val texto: String) {
        /**
         * The words of the line, each one analysed by the Silabeador (syllables, stressed
         * syllable, stress type).
         *
         * `Silabeador.palabrasDe(texto)` gives the list of words;
         * `.map { Silabeador.analizar(it) }` turns each word (`it`) into its full analysis.
         * A lambda (code between braces) is a small "recipe" applied to each item; `it` is the
         * current item.
         */
        val palabras: List<Silabeador.Palabra> =
            Silabeador.palabrasDe(texto).map { Silabeador.analizar(it) }

        /**
         * Every place in the line where a *sinalefa* is possible, with its resistance.
         *
         * HOW IT IS COMPUTED, STEP BY STEP:
         * 1. `(0 until n)` is a RANGE of numbers from 0 up to n, NOT including n. Here n is
         *    (number of words − 1): the positions of each word that has a next word.
         *    `.coerceAtLeast(0)` makes sure n is never negative (a line with no words would give
         *    −1).
         * 2. `.filter { ... }` keeps only the positions where the word ends in a vowel AND the
         *    next one starts with a vowel (the two conditions for a *sinalefa*). `&&` = "and".
         * 3. `.map { j -> ... }` turns each kept position `j` into a `Sinalefa`, working out its
         *    resistance. Writing `j ->` gives the lambda's item a name instead of `it`.
         */
        val sinalefas: List<Sinalefa> = (0 until (palabras.size - 1).coerceAtLeast(0))
            .filter { terminaEnVocal(palabras[it].texto) && empiezaConVocal(palabras[it + 1].texto) }
            .map { j ->
                // The word before the joint (a) and the word after it (b).
                val a = palabras[j]
                val b = palabras[j + 1]
                // Is the LAST vowel of `a` stressed?
                // - If `a` has one syllable: it is stressed unless it is in the ATONOS list
                //   (`!in` = "is not in").
                // - If it has more: its last vowel is stressed only if the word is *aguda*.
                // In Kotlin `if (...) X else Y` is an expression that gives back X or Y.
                val finalTonico = if (a.silabas.size == 1) a.texto.lowercase() !in ATONOS
                                  else a.tipo == TipoAcentual.AGUDA
                // Is the FIRST vowel of `b` stressed?
                // - One syllable: stressed unless it is in ATONOS.
                // - Several: only if its stressed syllable is the first one (position 0).
                val inicioTonico = if (b.silabas.size == 1) b.texto.lowercase() !in ATONOS
                                   else b.tonica == 0
                // Resistance: 2 if the end of `a` is stressed, else 1 if the start of `b` is,
                // else 0.
                Sinalefa(j, if (finalTonico) 2 else if (inicioTonico) 1 else 0)
            }

        /**
         * *Ley del acento final* (final-stress rule): *aguda* +1, *llana* 0,
         * *esdrújula* or *sobresdrújula* −1.
         *
         * KOTLIN SYNTAX:
         * - `palabras.lastOrNull()` gives the last word, or `null` ("nothing") if the list is
         *   empty.
         * - `?.` is the "safe call": `x?.tipo` means "if x is not null, take its `tipo`;
         *   if x is null, the result is null too" (instead of crashing).
         * - `when (...)` compares the value with each case; `else` covers everything else,
         *   including null and *llana*.
         */
        val ajusteFinal: Int = when (palabras.lastOrNull()?.tipo) {
            TipoAcentual.AGUDA -> 1
            TipoAcentual.ESDRUJULA, TipoAcentual.SOBRESDRUJULA -> -1
            else -> 0
        }

        // Sum of the grammatical syllables of all words. `sumOf { ... }` adds up the value the
        // lambda gives for each item (here, how many syllables each word has).
        val silabasGramaticales: Int = palabras.sumOf { it.silabas.size }
        // The top of the range: no *sinalefa* at all, plus the final-stress adjustment. A line
        // with no words measures 0.
        val maximo: Int = if (palabras.isEmpty()) 0 else silabasGramaticales + ajusteFinal
        // The bottom of the range: every possible *sinalefa* applied (each one removes 1).
        val minimo: Int = maximo - sinalefas.size

        /**
         * Can this line measure [metro] syllables? True if the number is inside the range.
         *
         * KOTLIN SYNTAX: `minimo..maximo` is a RANGE that INCLUDES both ends; `in` checks if a
         * number is inside it. The function uses the short `=` form: the result of the
         * expression is what it returns (here, true or false).
         */
        fun admite(metro: Int) = metro in minimo..maximo

        /**
         * Syllables to show on screen, fitted to the requested [metro].
         * Applied *sinalefas* are marked with "‿". Returns null if the line cannot measure
         * that. By default (no [metro] given) it applies every *sinalefa*.
         *
         * Receives: [metro], the target number of syllables. Its default value is `minimo`.
         * Returns: a list of syllables such as [Es, cri, to‿es, tá, en, mi‿al, ma, ...], or
         * `null`. The `?` after `List<String>` means the result may be null ("nothing").
         *
         * ALGORITHM:
         * 1. We start from the minimum (all *sinalefas* applied). To reach `metro` we must
         *    break (metro − minimo) of them.
         * 2. Sort the *sinalefas* by resistance, highest first; on a tie, the one closest to the
         *    end of the line first. Take the first (metro − minimo): those are broken.
         * 3. The rest stay "active" (applied).
         * 4. Build the output: go word by word; if the joint before this word is active, glue
         *    the first syllable of this word to the last syllable of the previous one with "‿";
         *    otherwise just add its syllables.
         */
        fun silabasPara(metro: Int = minimo): List<String>? {
            // No words, or the meter is outside the range: impossible. `!` = "not".
            if (palabras.isEmpty() || !admite(metro)) return null
            // Step 2: the set of positions whose *sinalefa* is broken.
            // - `sortedWith(...)` sorts with a custom rule:
            //   `compareByDescending<Sinalefa> { it.resistencia }` = highest resistance first;
            //   `.thenByDescending { it.indice }` = on a tie, highest position (latest) first.
            //   `<Sinalefa>` tells Kotlin what type of items are being compared (a generic).
            // - `.take(n)` keeps the first n items.
            // - `.map { it.indice }` keeps only their positions; `.toSet()` makes it a set.
            val rotas = sinalefas
                .sortedWith(compareByDescending<Sinalefa> { it.resistencia }.thenByDescending { it.indice })
                .take(metro - minimo)
                .map { it.indice }.toSet()
            // Step 3: all positions minus the broken ones = the active *sinalefas*.
            // The `-` between two sets removes the items of the second from the first.
            val activas = sinalefas.map { it.indice }.toSet() - rotas

            // Step 4: build the list of syllables.
            val out = mutableListOf<String>()
            // `forEachIndexed { j, p -> ... }` gives each word `p` together with its position `j`.
            palabras.forEachIndexed { j, p ->
                // Is there an active *sinalefa* between the previous word (j − 1) and this one?
                if ((j - 1) in activas) {
                    // Replace the last syllable written so far with: itself + "‿" + the first
                    // syllable of this word. `out.lastIndex` is the position of the last item.
                    out[out.lastIndex] = out.last() + "‿" + p.silabas.first()
                    // Then add the remaining syllables of this word. `drop(1)` = all but the
                    // first one.
                    out += p.silabas.drop(1)
                // No *sinalefa*: add all the syllables of the word as they are.
                } else out += p.silabas
            }
            return out
        }
    }

    /**
     * Does the word end in a vowel (so it can take part in a *sinalefa*)?
     *
     * The word "y" ("and") on its own counts as a vowel. But a "y" at the END of a longer word
     * ("rey", "hoy", "soy") is NOT a vowel here: it sounds like a half-consonant and does not
     * merge with the next word. Since "y" is not in VOCALES, `l.last() in VOCALES` is false for
     * those words.
     */
    private fun terminaEnVocal(palabra: String): Boolean {
        val l = palabra.lowercase()
        // "rey", "hoy": the final y does not make a *sinalefa*.
        return l == "y" || l.last() in VOCALES
    }

    /**
     * Does the word start with a vowel (so it can take part in a *sinalefa*)?
     *
     * - The word "y" ("and") counts as a vowel.
     * - A silent "h" at the start is skipped: "hace", "hay" start with a vowel sound.
     * - BUT "hie-" and "hue-" (hierba, hielo, hueso, huevo) do NOT: in those words the start
     *   sounds like a consonant (like "y" or "w"), so there is no *sinalefa*: "la hierba".
     */
    private fun empiezaConVocal(palabra: String): Boolean {
        // `var` because we may change it (remove the "h").
        var l = palabra.lowercase()
        if (l == "y") return true
        if (l.startsWith("h")) {
            // `drop(1)` removes the first letter (the h).
            l = l.drop(1)
            // hielo, hueso: consonant-like sound, no *sinalefa*.
            if (l.startsWith("ie") || l.startsWith("ue")) return false
        }
        // True if something is left and its first letter is a vowel.
        return l.isNotEmpty() && l[0] in VOCALES
    }

    // ---------- Whole poem ----------

    /**
     * Most common meters in the Spanish tradition, most common first. Used to break ties: if
     * two meters fit the same number of lines, the one that appears earlier here wins.
     * `listOf(...)` creates a read-only list.
     */
    private val PREFERENCIA = listOf(11, 8, 7, 14, 6, 5, 9, 12, 10, 13, 4, 3, 2)

    /**
     * The meter that fits the most lines (null if there are no lines).
     *
     * Receives: [versos], all the lines of the poem (already measured).
     * Returns: the winning number of syllables, or `null`. `Int?` means "a whole number, or
     * nothing".
     *
     * ALGORITHM:
     * 1. Ignore empty lines.
     * 2. Candidate meters: every number from the smallest minimum to the biggest maximum of all
     *    the lines.
     * 3. For each candidate, count how many lines admit it (can measure it).
     * 4. The candidate admitted by the most lines wins; on a tie, the most traditional one
     *    (earliest in PREFERENCIA).
     */
    fun metroDominante(versos: List<Verso>): Int? {
        // Step 1. `filter` keeps only the lines that have at least one word.
        val validos = versos.filter { it.palabras.isNotEmpty() }
        if (validos.isEmpty()) return null
        // Step 2. `minOf { ... }` / `maxOf { ... }` find the smallest / biggest value the lambda
        // gives across all items. `a..b` is the range of whole numbers from a to b.
        val candidatos = (validos.minOf { it.minimo }..validos.maxOf { it.maximo })
        // Steps 3 and 4. `maxWithOrNull(rule)` returns the "largest" item according to the
        // rule (or null if there are none). The rule:
        // - `compareBy<Int> { m -> ... }`: first, by how many lines admit meter `m`
        //   (`count { ... }` counts the items for which the lambda is true);
        // - `.thenByDescending { ... }`: on a tie, by position in PREFERENCIA, where a SMALLER
        //   position must win, so it is compared in descending order. `indexOf` gives −1 if the
        //   meter is not in the list; `.let { if (it < 0) 99 else it }` turns that −1 into 99
        //   (the least preferred). `let` runs the lambda with the value as `it` and returns
        //   what the lambda gives.
        return candidatos.maxWithOrNull(
            compareBy<Int> { m -> validos.count { it.admite(m) } }
                .thenByDescending { m -> PREFERENCIA.indexOf(m).let { if (it < 0) 99 else it } }
        )
    }

    /**
     * Measures every line of a text. Returns one [Verso] per line, including empty lines
     * (empty lines separate stanzas; they are ignored later when choosing the meter).
     * `lines()` splits the text at each line break.
     */
    fun analizarTexto(texto: String): List<Verso> =
        // Empty lines separate stanzas.
        texto.lines().map { Verso(it) }

    /**
     * The traditional Spanish name of a meter: 8 is "octosílabo", 11 "endecasílabo", 14
     * "alejandrino"... For any other number it returns, for example, "16 sílabas".
     * `"$n sílabas"` is a string template: `$n` is replaced by the value of `n`.
     */
    fun nombreMetro(n: Int): String = when (n) {
        2 -> "bisílabo"; 3 -> "trisílabo"; 4 -> "tetrasílabo"; 5 -> "pentasílabo"
        6 -> "hexasílabo"; 7 -> "heptasílabo"; 8 -> "octosílabo"; 9 -> "eneasílabo"
        10 -> "decasílabo"; 11 -> "endecasílabo"; 12 -> "dodecasílabo"
        13 -> "tridecasílabo"; 14 -> "alejandrino"
        else -> "$n sílabas"
    }
}
