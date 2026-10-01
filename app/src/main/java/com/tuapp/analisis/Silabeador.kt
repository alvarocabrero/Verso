// =================================================================================================
// FILE: Silabeador.kt  (the "syllable splitter")
// -------------------------------------------------------------------------------------------------
// `package` names the "logical folder" this file belongs to. All code that starts with
// `package com.tuapp.analisis` is part of the same group (the analysis engine) and can use each
// other without any import. Think of it as a family surname shared by several files.
// =================================================================================================
package com.tuapp.analisis

// =================================================================================================
// WHAT IS THIS FILE FOR?
//
// It is the most basic piece of Verso's metrical analysis engine. Its job is to take ONE single
// word (for example "murciélago") and:
//   1. split it into syllables: mur-cié-la-go;
//   2. say which syllable is the stressed one (the one that sounds loudest): "cié";
//   3. classify the word by where the stress falls: *aguda*, *llana*, *esdrújula* or
//      *sobresdrújula* (see below).
//
// ROLE IN THE APP
// When you write a poem in the editor, the app has to count syllables and find rhymes. All of
// that starts here: without knowing where the syllables are, you cannot measure a line of verse
// or know where its rhyme begins.
//
// RELATION WITH OTHER FILES
//   - Metrica.kt  uses `silabear`, `analizar` and `palabrasDe` to count the syllables of a line.
//   - Rima.kt     uses `silabear`, `silabaTonica` and `palabrasDe` to find the rhyming ending
//                 (from the stressed vowel to the end of the word).
//   - Recursos.kt uses `palabrasDe` to cut lines into words.
//   - AnalisisPoema.kt uses it indirectly, through the three files above.
//
// SPANISH LANGUAGE CONCEPTS YOU NEED
//   - *Sílaba ortográfica* (grammatical syllable): the syllable of a word on its own, as taught
//     at school (ca-sa, ár-bol). This file ONLY computes these. The *sílabas métricas* (the
//     ones that count in poetry, with vowels merged across words) are computed in Metrica.kt.
//   - *Vocales fuertes* (strong or open vowels): a, e, o.
//     *Vocales débiles* (weak or closed vowels): i, u.
//   - *Diptongo* (diphthong): two vowels side by side said in ONE syllable (ai-re, ciu-dad).
//   - *Triptongo* (triphthong): three vowels in one syllable (buey, U-ru-guay).
//   - *Hiato* (hiatus): two vowels side by side that go in DIFFERENT syllables (po-e-ta, rí-o).
//   - *Dígrafo* (digraph): two letters that make one single sound (ch, ll, rr, and "qu"/"gu"
//     before e/i, where the u is silent).
//   - *Aguda*: stress on the last syllable (can-CIÓN). *Llana*: on the second to last (CA-sa).
//     *Esdrújula*: on the third to last (PÁ-ja-ro).
//     *Sobresdrújula*: even earlier (DÍ-ga-me-lo).
//
// IT DOES NOT DEPEND ON ANDROID
// This is "pure" Kotlin: it does not use screens or anything from the phone. That is why it can
// be tested with JUnit (a program that runs automatic tests) on a normal computer, with no
// phone emulator.
// =================================================================================================

/**
 * Spelling-based syllable splitter for Spanish.
 *
 * - Splits words into grammatical syllables (handles *diptongos*, *triptongos*, *hiatos*,
 *   the digraphs ch/ll/rr, silent "qu"/"gu", "y" used as a vowel, and consonant pairs that
 *   never split).
 * - Finds the stressed syllable and the stress type (*aguda*, *llana*, *esdrújula*...).
 *
 * It does not depend on Android: it can be tested with plain JUnit.
 *
 * KOTLIN SYNTAX: `object`
 * An `object` is a class that has exactly ONE copy in the whole program (other languages call
 * this a "singleton"). You do not create it with `Silabeador()`; you use it directly by its
 * name: `Silabeador.silabear("casa")`. Think of it as a toolbox that is always there, ready to
 * use, without having to build it first.
 */
object Silabeador {

    /**
     * The four kinds of word depending on where the stress falls.
     *
     * KOTLIN SYNTAX: `enum class`
     * An `enum class` (enumeration) is a type that can only take a fixed set of values, written
     * inside the braces. Here a word can only be AGUDA, LLANA, ESDRUJULA or SOBRESDRUJULA;
     * nothing else is possible. It is like a drop-down menu with closed options.
     *
     * - AGUDA: the stress is on the last syllable (0 syllables after it). E.g. can-CIÓN.
     * - LLANA: the stress is on the second to last (1 syllable after it). E.g. CA-sa.
     * - ESDRUJULA: the stress is on the third to last (2 syllables after it). E.g. PÁ-ja-ro.
     * - SOBRESDRUJULA: 3 or more syllables after the stress. E.g. DÍ-ga-me-lo.
     */
    enum class TipoAcentual { AGUDA, LLANA, ESDRUJULA, SOBRESDRUJULA }

    /**
     * The result of analysing one word: the full "report" about that word.
     *
     * KOTLIN SYNTAX: `data class`
     * A `data class` is a class made only to hold data, like a form with boxes to fill in.
     * Kotlin adds some tools to it for free: comparing two of them (two forms with the same data
     * count as "equal"), printing them in a readable way, copying them, etc.
     *
     * What goes inside the parentheses are its boxes (called "properties"):
     * - `val` means the box is read-only: once the form is created, its value cannot change
     *   (the opposite is `var`, which can be changed later).
     * - `texto: String` means the box is called `texto` and its type is `String` (text).
     * - `List<String>` is a list of texts. What goes between `<` and `>` says what type the
     *   items of the list are (this is called a "generic" type).
     * - `Int` is a whole number (integer).
     *
     * Boxes:
     * - [texto]: the word exactly as it arrived ("Murciélago").
     * - [silabas]: its syllables (["Mur", "cié", "la", "go"]).
     * - [tonica]: the index (position, counting from 0) of the stressed syllable. In
     *   programming, lists are numbered starting at 0: the first syllable is 0, the second is
     *   1, and so on. For "murciélago" it is 1 ("cié").
     * - [tipo]: *aguda*, *llana*, *esdrújula* or *sobresdrújula*.
     */
    data class Palabra(
        val texto: String,
        val silabas: List<String>,
        // index (starting at 0) of the stressed syllable
        val tonica: Int,
        val tipo: TipoAcentual
    )

    // ---------- Letter classification ----------
    //
    // These constants are "lists of letters" written as text. They let us quickly ask
    // "is this letter a strong vowel?" by checking whether it appears inside the text.
    //
    // KOTLIN SYNTAX:
    // - `private` means it can only be used inside this `object`. It is an inner detail that
    //   the rest of the app does not need to see (like the gears inside a watch).
    // - `const val` is a constant: a fixed value, known before the program runs, that never
    //   changes. By convention constants are written in CAPITAL LETTERS.
    // - Texts are written between double quotes: "aeoáéó".

    // Strong (open) vowels, with and without accent mark: a, e, o.
    private const val FUERTES = "aeoáéó"
    // Weak vowels WITH an accent mark (í, ú). They are special: the accent mark makes them
    // stressed, and that causes a *hiato* with the vowel next to them (rí-o, ma-íz).
    private const val DEBILES_TILDADAS = "íú"
    // Weak vowels without accent mark (i, u) and ü with diaeresis (as in "pingüino").
    private const val DEBILES = "iuü"
    // All the vowels together. A `+` between texts glues them one after the other.
    private const val VOCALES = FUERTES + DEBILES_TILDADAS + DEBILES
    // Vowels with an accent mark: if a syllable contains one, that syllable is the stressed one.
    private const val TILDES = "áéíóú"

    /**
     * Consonant pairs that are never split (pr, bl, tr...).
     *
     * In Spanish some pairs of consonants always stay together in the same syllable because
     * they are said in one go: a-BRA-zo (not ab-ra-zo), ha-BLAR, in-FLA-mar. They are p, b, c,
     * g, f, k, t, d followed by "r", and p, b, c, g, f, k followed by "l". ("tl" is NOT treated
     * as inseparable here: at-le-ta; neither is "dl".)
     *
     * HOW THIS SET IS BUILT (step by step):
     * - `Set<String>` is a set of texts (like a list, but with no repeats and designed to answer
     *   "is this inside?" quickly).
     * - `"pbcgfktd".map { "${it}r" }` goes through each letter of the text "pbcgfktd" and, for
     *   each one, builds a new text with that letter followed by "r": "pr", "br", "cr"...
     *   · The code between braces `{ ... }` is a LAMBDA: a small piece of code passed to
     *     another function as an argument, like a "recipe" that `map` applies to every item.
     *   · `it` is the automatic name Kotlin gives to the current item inside a lambda that has
     *     only one parameter (here, each letter).
     *   · `"${it}r"` is a STRING TEMPLATE: inside the quotes, `${...}` is replaced by the value
     *     of what is inside. If `it` is 'p', the result is "pr".
     * - `.toSet()` turns the resulting list into a set.
     * - A `+` between two sets joins them into one.
     * - The value is computed only once, the first time the `object` is used.
     */
    private val INSEPARABLES: Set<String> =
        "pbcgfktd".map { "${it}r" }.toSet() + "pbcgfk".map { "${it}l" }.toSet()

    /**
     * Internal classification of a vowel, used to decide *diptongo* or *hiato*.
     * - FUERTE: a, e, o (with or without accent mark).
     * - DEBIL: i, u, ü without accent mark.
     * - DEBIL_TILDADA: í, ú.
     */
    private enum class Clase { FUERTE, DEBIL, DEBIL_TILDADA }

    /**
     * Tells which [Clase] the vowel [v] belongs to.
     *
     * KOTLIN SYNTAX:
     * - `fun` declares a function (a named block of code that can be called).
     * - `(v: Char)` means it receives one parameter called `v` of type `Char` (one character).
     * - `: Clase` after the parentheses is the type of what the function returns.
     * - `= when (v) { ... }`: instead of a body with braces and `return`, you can write `=`
     *   followed directly by the expression whose result is returned. It is a short form.
     * - `when` works like "depending on": it compares `v` with each case from top to bottom and
     *   takes the first one that matches. `in FUERTES` means "if `v` is inside the text
     *   FUERTES". `else` is the default case when none of the others matched.
     * - `->` separates the condition from the result of that case.
     *
     * Note: `v` is assumed to be a vowel; that is why anything that is not strong and not an
     * accented weak vowel is treated as weak (i, u, ü).
     */
    private fun clase(v: Char): Clase = when (v) {
        in FUERTES -> Clase.FUERTE
        in DEBILES_TILDADAS -> Clase.DEBIL_TILDADA
        else -> Clase.DEBIL
    }

    /**
     * Removes the accent mark (or diaeresis) from a weak vowel so we can compare "the same
     * vowel": 'í' becomes 'i'; 'ú' and 'ü' become 'u'; any other letter is returned unchanged.
     *
     * Single characters are written between single quotes ('í'), while texts use double quotes
     * ("í"). The `;` lets you write several `when` cases on the same line.
     * `'ú', 'ü' -> 'u'` means "if it is ú or ü, return u".
     */
    private fun base(v: Char): Char = when (v) {
        'í' -> 'i'; 'ú', 'ü' -> 'u'; else -> v
    }

    // ---------- Tokenization ----------
    //
    // "Tokenizing" means cutting something into its smallest meaningful pieces ("tokens").
    // Here we cut the word into "sound units": every vowel is one unit and every consonant
    // too, but digraphs (ch, ll, rr, qu, gu) count as ONE single consonant because they stand
    // for one sound. This makes the later syllable split much simpler.

    /**
     * A sound unit: one vowel or one consonant (which may be a digraph).
     * [original] keeps capital letters and accent marks; [norm] is the lowercase form used
     * for the analysis ("y" used as a vowel is normalized to "i").
     *
     * Boxes:
     * - [esVocal]: `true` if it is a vowel, `false` if it is a consonant. `Boolean` is the type
     *   of yes/no values.
     * - [original]: the letters as they were written, so the syllables come back with their
     *   original capitals and accent marks ("Mur", not "mur").
     * - [norm]: "normalized" lowercase version, used for comparisons.
     *
     * The whole declaration fits on one line because it has no body: only boxes.
     */
    private data class Unidad(val esVocal: Boolean, val original: String, val norm: String)

    /**
     * Turns a word into its list of [Unidad] (units).
     *
     * Receives: [palabra], the text of one word ("Guitarra").
     * Returns: a list of units; for "guitarra": gu | i | t | a | rr | a.
     *
     * Rules it applies (in this order; the first one that fits wins):
     * 1. "ch", "ll", "rr" become one single consonant.
     * 2. "qu" or "gu" before e/i become one single consonant (the u is silent: que-so,
     *    gui-ta-rra). If the u has a diaeresis ("güi" in pingüino) this rule does NOT apply,
     *    because "ü" is not "u": the ü will then be treated as a vowel.
     * 3. "y" is a vowel (it sounds like i) when NO vowel follows it: rey, hoy, "y" (the word
     *    "and"). When a vowel follows, it is a consonant: re-yes, ya.
     * 4. Any other letter is a vowel if it is in VOCALES, and a consonant otherwise. The
     *    letter "h" lands here as a consonant even though it is silent. That still gives the
     *    right result: a single consonant between two vowels always goes to the next syllable
     *    (bú-ho, a-hu-mar).
     */
    private fun tokenizar(palabra: String): List<Unidad> {
        // Lowercase copy of the word, so comparisons do not care about capital letters.
        // `val` = a variable that cannot be reassigned (its value stays fixed).
        val l = palabra.lowercase()
        // Empty list we will fill up. `mutableListOf` creates a list that CAN be changed (you
        // can add items to it); `<Unidad>` says what kind of things it will hold.
        val out = mutableListOf<Unidad>()
        // Position of the letter we are looking at. It is `var` because it will change.
        var i = 0
        // `while` loop: repeats the block as long as the condition is true.
        // `l.length` is the number of letters. While we have not gone past the last one, go on.
        while (i < l.length) {
            // Current letter. `l[i]` takes the character at position `i`.
            val c = l[i]
            // Next letter, or `null` if there is none (we are at the end).
            // `null` is the value for "nothing / not there". `getOrNull` returns the character
            // if it exists and `null` if the position is outside the text, instead of crashing.
            // So the type of `n` is `Char?`: the `?` means "a character... or nothing".
            val n = l.getOrNull(i + 1)
            // Letter two positions ahead, or `null`.
            val nn = l.getOrNull(i + 2)
            // The current two letters together ("ch", "qu"...), or an empty text if only one
            // letter is left. In Kotlin `if (...) A else B` is an expression: it gives back A or
            // B depending on the condition. `substring(i, i + 2)` cuts the text from position i
            // up to i+2 (not including i+2), so two letters.
            val par = if (i + 1 < l.length) l.substring(i, i + 2) else ""

            // `when` with nothing in parentheses: each condition is checked from top to bottom
            // and the block of the first true one is run.
            when {
                // Case 1: digraphs ch, ll, rr. `||` means "or".
                // Adds a consonant unit (esVocal = false) with the two original letters and moves
                // forward 2 positions. `out += x` adds x at the end of the list.
                // The `;` lets you write two instructions on the same line.
                par == "ch" || par == "ll" || par == "rr" -> {
                    out += Unidad(false, palabra.substring(i, i + 2), par); i += 2
                }
                // "qu" and "gu" before e/i: the u is silent.
                // Full condition: the letter is q or g (`&&` means "and also"), the next one is
                // u, a third letter exists (`nn != null`, "is not nothing") and that third letter
                // is e, é, i or í. Then "qu"/"gu" form one single consonant.
                (c == 'q' || c == 'g') && n == 'u' && nn != null && nn in "eéií" -> {
                    out += Unidad(false, palabra.substring(i, i + 2), par); i += 2
                }
                // "y" is a vowel when no vowel follows it (rey, hoy, y).
                c == 'y' -> {
                    // It is a vowel if there is no next letter (`n == null`) or if the next
                    // letter is not a vowel (`!in` = "is not inside").
                    val esVocal = n == null || n !in VOCALES
                    // If it is a vowel, its normalized form is "i" (same sound); if not, "y".
                    // `palabra[i].toString()` turns the original character into text.
                    // `i++` adds 1 to `i` (moves one position forward).
                    out += Unidad(esVocal, palabra[i].toString(), if (esVocal) "i" else "y"); i++
                }
                // Any other letter: vowel if it is in VOCALES, consonant otherwise.
                else -> {
                    out += Unidad(c in VOCALES, palabra[i].toString(), c.toString()); i++
                }
            }
        }
        // Give back the list of units we built.
        return out
    }

    // ---------- Diphthong / hiatus ----------

    /**
     * Does the vowel [actual] break the current nucleus (is there a *hiato*)?
     *
     * A "nucleus" is the group of vowels at the centre of a syllable (one vowel, a *diptongo*
     * or a *triptongo*). While going through the word we collect consecutive vowels into the
     * same nucleus; before adding a new one we ask this function whether it must go into a
     * separate syllable (*hiato*, returns `true`) or stay in the same one (*diptongo*, `false`).
     *
     * Receives:
     * - [nucleo]: the vowels that already form the current nucleus (at least one).
     * - [actual]: the vowel we want to add.
     * Returns: `true` if there is a *hiato* (a new syllable must start), `false` otherwise.
     *
     * Spanish rules it applies:
     * - Two different weak vowels (i+u, u+i): *diptongo*. ciu-dad, cui-da-do, cons-truí.
     * - Two equal weak vowels (i+i): *hiato*. chi-i-ta.
     * - Accented weak vowel next to a strong one: *hiato*. rí-o, ma-íz, Ra-úl.
     * - Two strong vowels: *hiato*. po-e-ta, le-er.
     * - Strong + unaccented weak (or the other way round): *diptongo*. ai-re, hue-vo.
     * - Weak + strong + weak: *triptongo*. buey, U-ru-guay (this follows from the rules above).
     */
    private fun esHiato(nucleo: List<Unidad>, actual: Unidad): Boolean {
        // Last vowel of the current nucleus. `.last()` = last item; `.norm[0]` = its first
        // normalized character.
        val prev = nucleo.last().norm[0]
        // The vowel we want to add.
        val cur = actual.norm[0]
        // Class (strong / weak / accented weak) of each one.
        val p = clase(prev)
        val c = clase(cur)

        // Two weak vowels: *diptongo* (ciudad, construí) unless they are equal (chiita).
        // `!=` means "different from". If neither of them is strong...
        if (p != Clase.FUERTE && c != Clase.FUERTE) {
            // ...and both carry an accent mark (very rare), it is a *hiato*.
            if (p == Clase.DEBIL_TILDADA && c == Clase.DEBIL_TILDADA) return true
            // Otherwise it is a *hiato* only when they are the same vowel (i-i, u-u), ignoring
            // accent marks. `return` ends the function and gives back that value.
            return base(prev) == base(cur)
        }
        // Accented weak vowel next to a strong one: *hiato* (río, maíz).
        if (p == Clase.DEBIL_TILDADA || c == Clase.DEBIL_TILDADA) return true
        // Two strong vowels in the same nucleus: *hiato* (poeta, leer).
        // `nucleo.any { ... }` asks "is there ANY vowel in the nucleus that meets this?".
        // The whole nucleus is checked (not only the previous vowel): in "buey" the "e" gets in,
        // but if another strong vowel came later it would not fit (two strong vowels never share
        // a syllable).
        if (c == Clase.FUERTE && nucleo.any { clase(it.norm[0]) == Clase.FUERTE }) return true
        // In every other case: *diptongo* or *triptongo*, no *hiato*.
        return false
    }

    // ---------- Public API ----------
    //
    // "Public API" = the functions the rest of the app can use. They have no `private`, so
    // they can be seen from outside (in Kotlin, with no modifier, everything is public).

    /**
     * Splits a word into its grammatical syllables.
     *
     * Receives: [palabra], for example "instrumento".
     * Returns: the list of syllables, keeping capitals and accent marks:
     * ["ins", "tru", "men", "to"].
     *
     * ALGORITHM, STEP BY STEP:
     * 1. Tokenize the word (see `tokenizar`).
     * 2. Walk through the units, sorting them into two parallel lists:
     *    - `nucleos`: groups of vowels (each one will be the centre of a syllable).
     *    - `consonantes`: the groups of consonants BEFORE the first nucleus, BETWEEN each pair
     *      of nuclei, and AFTER the last one. There is always one more group than nuclei.
     *    Example "instrumento": consonants [] | nucleus i | [n,s,t,r] | nucleus u | [m] | e |
     *    [n,t] | o | [].
     * 3. Each nucleus is one syllable. The consonants at the start go to the first syllable
     *    and the ones at the end go to the last syllable.
     * 4. The consonants between two nuclei are shared out with these rules:
     *    - 0 or 1 consonant: all go to the next syllable (ca-sa, e-xa-men).
     *    - 2 consonants: one on each side (ac-ción), UNLESS they form an inseparable pair, in
     *      which case both go to the next syllable (a-bra-zo).
     *    - 3 or more: if the last two are inseparable, those two go to the next syllable
     *      (ins-tru-men-to); if not, only the last one does (obs-tá-cu-lo).
     */
    fun silabear(palabra: String): List<String> {
        // Step 1: the word turned into units.
        val t = tokenizar(palabra)
        // List of nuclei; each nucleus is itself a list of units (a list of lists).
        val nucleos = mutableListOf<List<Unidad>>()
        // List of consonant groups. It starts with one empty group already inside: the group of
        // consonants that may come before the first vowel.
        // (Groups: before, between and after the nuclei.)
        val consonantes = mutableListOf(mutableListOf<Unidad>())

        // Step 2: walk through the units one by one with the index `i`.
        var i = 0
        while (i < t.size) {
            // `!` means "not". If the unit is NOT a vowel (it is a consonant)...
            if (!t[i].esVocal) {
                // ...add it to the last consonant group (the "open" one) and move on.
                consonantes.last() += t[i]; i++
            } else {
                // It is a vowel: a new nucleus starts with it.
                val nuc = mutableListOf(t[i]); i++
                // While the next unit exists, is a vowel and does NOT make a *hiato* with the
                // nucleus, put it into the same nucleus (this is how diphthongs and triphthongs
                // are formed).
                while (i < t.size && t[i].esVocal && !esHiato(nuc, t[i])) {
                    nuc += t[i]; i++
                }
                // Store the finished nucleus...
                nucleos += nuc
                // ...and open a new consonant group for whatever comes after it.
                consonantes += mutableListOf<Unidad>()
            }
        }
        // If the word has no vowel at all (for example an acronym like "BBC"), it cannot be
        // split: the whole word is returned as one single "syllable".
        // `listOf(x)` creates a (read-only) list with a single item.
        if (nucleos.isEmpty()) return listOf(palabra)

        // Step 3: prepare one text "box" per syllable. A `StringBuilder` is a text you can keep
        // growing piece by piece (normal `String`s cannot be changed once made).
        // `MutableList(n) { ... }` creates a list of n items, each one made by the lambda
        // (here, an empty StringBuilder).
        val silabas = MutableList(nucleos.size) { StringBuilder() }
        // The starting consonants (before the first vowel) go to the first syllable.
        // `forEach { ... }` goes through each item and runs the lambda with it (`it`).
        // `append` adds text at the end of the StringBuilder.
        consonantes[0].forEach { silabas[0].append(it.original) }

        // Step 4: go through each nucleus by its position `k`.
        // `nucleos.indices` is the range of valid positions (0, 1, 2, ... up to the last).
        // `for (k in ...)` = "for each k in ...".
        for (k in nucleos.indices) {
            // Put the vowels of the nucleus into its syllable.
            nucleos[k].forEach { silabas[k].append(it.original) }
            // Consonants that come right after this nucleus (group k+1).
            val cs = consonantes[k + 1]

            // Final consonants: if this is the last nucleus, every consonant left is at the end
            // of the word and goes into this last syllable. `break` leaves the `for` loop.
            // `lastIndex` is the position of the last item.
            if (k == nucleos.lastIndex) {
                cs.forEach { silabas[k].append(it.original) }
                break
            }
            // `corte` ("cut") = how many of these consonants stay in the current syllable; the
            // rest move to the next one. It depends on how many consonants there are.
            val corte = when (cs.size) {
                // ca-sa: with 0 or 1 consonant, none stays; they all move to the next syllable.
                0, 1 -> 0
                // a-bra-zo / ac-ción: with 2, if together they form an inseparable pair (br),
                // none stays (0); if not (cc), the first one stays (1).
                2 -> if (cs[0].norm + cs[1].norm in INSEPARABLES) 0 else 1
                // ins-tru / obs-tá: with 3 or more, look at the last two (`cs.size - 2` is the
                // second to last position and `cs.last()` the last one). If they are
                // inseparable, both move; otherwise only the last one moves.
                else -> if (cs[cs.size - 2].norm + cs.last().norm in INSEPARABLES)
                    cs.size - 2 else cs.size - 1
            }
            // `subList(from, to)` takes a slice of the list (not including `to`).
            // The first `corte` consonants close the current syllable...
            cs.subList(0, corte).forEach { silabas[k].append(it.original) }
            // ...and the rest open the next syllable.
            cs.subList(corte, cs.size).forEach { silabas[k + 1].append(it.original) }
        }
        // Turn each StringBuilder into a normal text. `map` transforms every item of the list
        // with the lambda and returns the new list.
        return silabas.map { it.toString() }
    }

    /**
     * Index of the stressed syllable, following the Spanish accent rules.
     *
     * Receives:
     * - [palabra]: the word.
     * - [silabas]: its syllables. It has a DEFAULT VALUE (`= silabear(palabra)`): if the caller
     *   does not pass them, they are computed automatically. If the caller already has them,
     *   it can pass them and the work is not repeated.
     * Returns: the position (from 0) of the stressed syllable.
     *
     * Rules (the Spanish spelling rules for accent marks, used in reverse):
     * 1. If a syllable has an accent mark, that one is stressed (can-CIÓN, PÁ-ja-ro).
     * 2. If the word has only one syllable, it is that one.
     * 3. If it ends in a vowel, "n" or "s" and has no accent mark, it is *llana*: the second to
     *    last (CA-sa, CAN-tan). If it ends in another consonant, it is *aguda*: the last one
     *    (a-MOR, re-LOJ). Note: a final "y" (rey) is not in "aeiouns", so "estoy" comes out as
     *    *aguda*, which is correct.
     */
    fun silabaTonica(palabra: String, silabas: List<String> = silabear(palabra)): Int {
        // Rule 1. `forEachIndexed { i, s -> ... }` goes through the list giving the lambda two
        // things: the position `i` and the item `s`. When a lambda receives several parameters,
        // they are named before the arrow `->`.
        // `s.lowercase().any { it in TILDES }` = "does any letter of the syllable carry an
        // accent mark?". If so, `return i` leaves the WHOLE `silabaTonica` function, returning i.
        silabas.forEachIndexed { i, s -> if (s.lowercase().any { it in TILDES }) return i }
        // Rule 2: one-syllable word.
        if (silabas.size == 1) return 0
        // Rule 3: look at the last letter of the word.
        val ultima = palabra.lowercase().last()
        // Second to last syllable (size − 2) if it ends in a vowel, n or s; otherwise the last.
        return if (ultima in "aeiouns") silabas.size - 2 else silabas.lastIndex
    }

    /**
     * Full analysis of one word: syllables, stressed syllable and stress type.
     *
     * Receives: [palabra] ("murciélago").
     * Returns: a [Palabra] with everything ("murciélago", [mur, cié, la, go], 1, ESDRUJULA).
     *
     * The type is found by counting how many syllables come AFTER the stressed one:
     * number of syllables − 1 − position of the stressed one.
     *   0 = *aguda*, 1 = *llana*, 2 = *esdrújula*, 3 or more = *sobresdrújula*.
     */
    fun analizar(palabra: String): Palabra {
        val silabas = silabear(palabra)
        // Pass the syllables we already have, so the word is not split twice.
        val tonica = silabaTonica(palabra, silabas)
        val tipo = when (silabas.size - 1 - tonica) {
            0 -> TipoAcentual.AGUDA
            1 -> TipoAcentual.LLANA
            2 -> TipoAcentual.ESDRUJULA
            else -> TipoAcentual.SOBRESDRUJULA
        }
        // Create the form with the four values. Kotlin has no `new` keyword: you just write the
        // class name and the values in parentheses.
        return Palabra(palabra, silabas, tonica, tipo)
    }

    /**
     * Gets the words of a line, ignoring punctuation.
     *
     * Receives: [verso], one line of text ("¡Ay, qué dolor!").
     * Returns: the list of words (["Ay", "qué", "dolor"]). Numbers and signs are ignored.
     *
     * HOW IT WORKS:
     * - `Regex(...)` creates a REGULAR EXPRESSION: a pattern used to search pieces of text.
     * - The pattern `\p{L}+` means "one or more letters in a row" (`\p{L}` = any letter of any
     *   language, including á, ñ, ü; `+` = one or more times). Inside a Kotlin text the
     *   backslash is written twice (`\\`) because a single backslash has a special meaning in
     *   strings.
     * - `findAll(verso)` finds every match in the line.
     * - `.map { it.value }` keeps the text of each match.
     * - `.toList()` turns the result into a normal list.
     * The function uses the short `=` form (no braces, no `return`), spread over two lines.
     */
    fun palabrasDe(verso: String): List<String> =
        Regex("\\p{L}+").findAll(verso).map { it.value }.toList()
}
