// =================================================================================================
// FILE: Rima.kt  (rhyme detection)
// -------------------------------------------------------------------------------------------------
// `package` names the "logical folder" this file belongs to: the analysis engine. All files with
// the same `package` line can use each other directly, with no `import`.
// =================================================================================================
package com.tuapp.analisis

// =================================================================================================
// WHAT IS THIS FILE FOR?
//
// It decides whether two lines rhyme, and what kind of rhyme they make. It also builds the
// "rhyme scheme" of a whole poem: the letters like ABBA ABBA that poets use to describe which
// lines rhyme with which (every line with the same letter rhymes with the others).
//
// ROLE IN THE APP
// In the editor, next to each line, Verso shows its rhyme letter (A, B, C...) and can colour the
// rhyming endings. The letters and the endings come from here.
//
// RELATION WITH OTHER FILES
//   - Silabeador.kt: gives the words of a line, their syllables and the stressed syllable.
//   - AnalisisPoema.kt: calls `esquema` to get the letter of every line, and `terminacion` to
//     know which characters to colour.
//   - Recursos.kt: uses `terminacion` and `fonetica` to find internal rhymes and to turn words
//     into sounds for alliteration.
//
// SPANISH RHYME: THE CONCEPTS
//   - In Spanish, rhyme starts at the LAST STRESSED VOWEL of the line and goes to the end.
//     For "cántaro" that is "ántaro"; for "amor" it is "or".
//   - *Rima consonante* (full rhyme): ALL the sounds from that vowel on are the same, vowels
//     and consonants: "cielo / suelo" (-elo), "canción / corazón" (-ón).
//   - *Rima asonante* (vowel rhyme): only the VOWELS are the same, the consonants may differ:
//     "cielo / lejos" (e-o). Only two vowels count: the stressed one and the one in the last
//     syllable. In a *diptongo* (two vowels in one syllable) the strong one (a, e, o) counts.
//     In the last, unstressed syllable, i sounds close enough to e, and u to o, so "fácil /
//     calle" (a-e) and "Venus / tenso" (e-o) are treated as rhyming.
//   - The comparison is made by SOUND, not by spelling:
//       · b and v sound the same in Spanish;
//       · h is silent;
//       · *yeísmo*: ll and y sound the same (most Spanish speakers);
//       · *seseo* (optional): s, z and c before e/i all sound like "s". This is how Spanish is
//         spoken in Latin America, the Canary Islands and parts of Andalusia, so lyrics written
//         there rhyme "casa / caza". The user can switch this on or off in the app.
// =================================================================================================

/**
 * Rhyme detection for Spanish.
 *
 * Rhyme starts at the last stressed vowel of the line:
 * - CONSONANTE (*rima consonante*): all sounds from there on match (cielo / suelo).
 * - ASONANTE (*rima asonante*): only the vowels match (cielo / lejos). Two vowels count: the
 *   stressed one and the one in the last syllable; in a *diptongo* the strong vowel counts;
 *   in the unstressed final position, i ≈ e and u ≈ o (fácil / calle, Venus / tenso).
 *
 * The *consonante* comparison is phonetic (by sound): b = v, silent h, *yeísmo* (ll = y),
 * and optionally *seseo* (s = z = c before e/i), useful for Latin American lyrics.
 *
 * KOTLIN SYNTAX: `object`
 * An `object` is a class with exactly ONE copy in the whole program (a "singleton"). It is
 * used directly by its name: `Rima.comparar("cielo", "suelo")`.
 */
object Rima {

    /**
     * The two kinds of rhyme.
     *
     * KOTLIN SYNTAX: `enum class`
     * A type with a fixed, closed list of possible values; here only CONSONANTE or ASONANTE.
     */
    enum class Tipo { CONSONANTE, ASONANTE }

    /**
     * The rhyming ending of a line, in three forms.
     *
     * KOTLIN SYNTAX: `data class`
     * A class made only to hold data, like a form with boxes. Each `val name: Type` is a
     * read-only box. `String` means text.
     *
     * Boxes:
     * - [palabra]: the last word of the line ("cántaro").
     * - [texto]: the written ending, from the stressed vowel to the end ("ántaro").
     * - [consonante]: the phonetic key for *rima consonante* ("antaro"): the ending turned into
     *   sounds (no accent marks, b = v, etc.). Two lines with the same key rhyme fully.
     * - [asonante]: the vowel key for *rima asonante* ("ao"). Two lines with the same key share
     *   their vowels.
     */
    data class Terminacion(
        val palabra: String,
        // written ending: "ántaro"
        val texto: String,
        // phonetic key: "antaro"
        val consonante: String,
        // vowels: "ao"
        val asonante: String
    )

    /**
     * The rhyme information of one line inside a scheme.
     *
     * - [letra]: 'A', 'B'... (or 'a', 'b'... in lowercase), or '-' if the line rhymes with no
     *   other. `Char` means one single character.
     * - [tipo]: CONSONANTE or ASONANTE, or `null` if the line does not rhyme. The `?` after
     *   `Tipo` means the box may hold "nothing" (`null`).
     * - [terminacion]: the ending of this line (see [Terminacion]).
     */
    data class RimaVerso(val letra: Char, val tipo: Tipo?, val terminacion: Terminacion)

    // ---------- Stressed vowel inside a syllable ----------

    /**
     * Position of the vowel that carries the stress inside [silaba].
     *
     * We already know WHICH syllable is stressed (Silabeador tells us), but a syllable may have
     * several vowels ("cié", "cui", "buey"), and rhyme starts at one precise vowel. Rules:
     * 1. If a vowel has an accent mark, that one ("cié" gives the é).
     * 2. Otherwise, the first strong vowel: a, e or o ("bue" gives the e).
     * 3. If there are only weak vowels (i, u): the last one ("cui" in cui-da gives the i;
     *    "ciu" in ciu-dad gives the u). The silent u of "qu"/"gu" before e/i is skipped.
     *
     * Receives: [silaba], one syllable. Returns: the position (from 0) of that vowel inside the
     * syllable, or `null` if it has no vowel. `Int?` = "a whole number, or nothing".
     *
     * KOTLIN SYNTAX:
     * - `indexOfFirst { ... }` gives the position of the first letter for which the lambda
     *   (code between braces) is true, or −1 if there is none.
     * - `.let { ... }` runs the lambda with the value as `it`. Here: "if the position is 0 or
     *   more, return it from the whole function".
     */
    private fun indiceVocalTonica(silaba: String): Int? {
        val s = silaba.lowercase()
        // Rule 1: a vowel with an accent mark.
        s.indexOfFirst { it in "áéíóú" }.let { if (it >= 0) return it }
        // Rule 2: the first strong vowel.
        s.indexOfFirst { it in "aeo" }.let { if (it >= 0) return it }
        // Only weak vowels (cui-da, ciu-dad, muy): the last one counts,
        // skipping the silent u of qu/gu before e/i.
        // `var idx: Int? = null` = a changeable variable that may hold a number or nothing,
        // starting as nothing.
        var idx: Int? = null
        // Go through every letter `c` with its position `i`.
        s.forEachIndexed { i, c ->
            // Weak vowel (y counts too, as in "muy").
            if (c in "iuüy") {
                // Is this a silent u? It is when: it is a u, it is not the first letter, the
                // letter before is q or g, and the letter after is e, é, i or í.
                // - `s.getOrNull(i + 1)` gives the next letter or null if there is none.
                // - `?.let { it in "eéií" }` : if there is a next letter, check if it is e/i;
                //   if there is none, the whole thing is null. The `?.` is the "safe call": it
                //   only goes on when the value is not null.
                // - `== true` turns "true / false / null" into a plain yes or no (null = no).
                val uMuda = c == 'u' && i > 0 && s[i - 1] in "qg" && s.getOrNull(i + 1)?.let { it in "eéií" } == true
                // If it is not silent, remember it; later ones overwrite earlier ones, so at the
                // end `idx` holds the LAST weak vowel.
                if (!uMuda) idx = i
            }
        }
        return idx
    }

    /**
     * Removes the accent mark from a vowel ('á' becomes 'a'...), turns 'ü' into 'u' and 'y'
     * into 'i'. Any other character is returned unchanged. Used to compare vowels by sound.
     * The short `=` form returns the result of the `when` directly; `;` separates cases on the
     * same line.
     */
    private fun sinTilde(c: Char): Char = when (c) {
        'á' -> 'a'; 'é' -> 'e'; 'í' -> 'i'; 'ó' -> 'o'; 'ú', 'ü' -> 'u'; 'y' -> 'i'; else -> c
    }

    // ---------- Phonetic normalization ----------

    /**
     * Turns written Spanish into a simple "sound spelling", so that two endings that SOUND the
     * same end up as the same text. Examples: "hoy" becomes "oi", "gente" becomes "jente",
     * "queso" becomes "keso", "caza" becomes "kaθa" (or "kasa" with *seseo*).
     *
     * Receives: [t], a lowercase text; [seseo], whether s = z = c before e/i.
     * Returns: the normalized text.
     *
     * KOTLIN SYNTAX:
     * - `internal` means it can be used from any file of this same module (the app), but not
     *   from outside it. Recursos.kt uses it, so it cannot be `private`.
     * - `replace(a, b)` gives a copy of the text with every `a` changed to `b`.
     * - `Regex("g(?=[eéií])")` is a REGULAR EXPRESSION (a search pattern). `g(?=[eéií])` means
     *   "a g that is followed by e, é, i or í" (the part in `(?=...)` is only checked, not
     *   replaced). `[...]` means "any one of these letters".
     * - `(?![...])` means "NOT followed by any of these letters".
     *
     * THE STEPS, IN ORDER (order matters: each step works on the result of the previous one):
     * 1. "ch" is temporarily turned into "ç" so that the next step (removing h) does not break
     *    it. Then every h is removed (silent h).
     * 2. g before e/i sounds like j: "gente" becomes "jente".
     * 3. "qu" before e/i sounds like k: "queso" becomes "keso".
     * 4. "gu" before e/i sounds like a hard g: "guerra" becomes "gerra".
     * 5. ü becomes u ("pingüino").
     * 6. c before e/i, and every z, become θ (the "th" sound of Spain) or s (with *seseo*).
     *    Every remaining c (before a, o, u or a consonant) sounds like k.
     * 7. v becomes b (they sound the same); ll becomes y (*yeísmo*).
     * 8. A y that is NOT followed by a vowel sounds like i: "hoy" becomes "oi".
     * 9. "ç" goes back to "ch", and accent marks are removed from every letter (the y that is
     *    still left is a consonant, so it is kept as y).
     */
    internal fun fonetica(t: String, seseo: Boolean): String {
        // The symbol used for the z sound: "s" with *seseo*, "θ" without it.
        val z = if (seseo) "s" else "θ"
        // Step 1. `var` because `s` will be replaced again and again below.
        var s = t.replace("ch", "ç").replace("h", "")
        // Step 2: gente becomes jente.
        s = s.replace(Regex("g(?=[eéií])"), "j")
        // Step 3: queso becomes keso.
        s = s.replace(Regex("qu(?=[eéií])"), "k")
        // Step 4: guerra becomes gerra.
        s = s.replace(Regex("gu(?=[eéií])"), "g")
        // Step 5.
        s = s.replace("ü", "u")
        // Step 6: soft c and z, then hard c.
        s = s.replace(Regex("c(?=[eéií])"), z).replace("z", z).replace("c", "k")
        // Step 7: b = v, ll = y.
        s = s.replace("v", "b").replace("ll", "y")
        // Step 8: hoy becomes oi.
        s = s.replace(Regex("y(?![aeiouáéíóú])"), "i")
        // Step 9. `.map { ... }` turns every character; here the y is kept and every other
        // letter loses its accent mark. `joinToString("")` glues the characters back into one
        // text with nothing between them.
        return s.replace("ç", "ch").map { if (it == 'y') 'y' else sinTilde(it) }.joinToString("")
    }

    // ---------- Public API ----------
    //
    // "Public API" = functions the rest of the app can call. They have no `private`, so they
    // are visible from outside (in Kotlin, with no modifier, everything is public).

    /**
     * The rhyming ending of a line.
     *
     * Receives: [verso], one line of text; [seseo], optional, `false` by default (the
     * `= false` gives the parameter a default value, so callers may leave it out).
     * Returns: a [Terminacion], or `null` if the line has no words (or no vowel).
     *
     * Example: "agua del cántaro" gives palabra = "cántaro", texto = "ántaro",
     * consonante = "antaro", asonante = "ao".
     *
     * ALGORITHM:
     * 1. Take the last word of the line and split it into syllables.
     * 2. Find its stressed syllable, and the stressed vowel inside that syllable.
     * 3. The written ending goes from that vowel to the end of the word.
     * 4. The vowel key (*asonante*):
     *    - if the word is *aguda* (the stressed syllable is the last one), only the stressed
     *      vowel ("amor" gives "o");
     *    - otherwise, the stressed vowel + the main vowel of the last syllable, with i turned
     *      into e and u into o ("fácil" gives "ae", same as "calle").
     * 5. The full-rhyme key (*consonante*) is the written ending passed through `fonetica`.
     */
    fun terminacion(verso: String, seseo: Boolean = false): Terminacion? {
        // Step 1. `?:` is the "Elvis operator": `x ?: y` means "x, but if x is null, then y".
        // Here: if there is no last word, `return null` leaves the function at once.
        val palabra = Silabeador.palabrasDe(verso).lastOrNull() ?: return null
        val silabas = Silabeador.silabear(palabra)
        // Step 2: `t` = position of the stressed syllable.
        val t = Silabeador.silabaTonica(palabra, silabas)
        // `iv` = position of the stressed vowel inside that syllable (or leave if none).
        val iv = indiceVocalTonica(silabas[t]) ?: return null

        // Step 3. Where does the ending start inside the whole word? Add up the lengths of the
        // syllables before the stressed one (`take(t)` = the first t syllables; `sumOf` adds up
        // their lengths), then add the vowel's position inside the stressed syllable.
        val inicio = silabas.take(t).sumOf { it.length } + iv
        // Cut the word from there to the end. `substring(inicio)` = from `inicio` to the end.
        val texto = palabra.lowercase().substring(inicio)

        // Step 4. The stressed vowel without accent mark (first letter of the ending).
        val tonica = sinTilde(texto[0])
        // `"$tonica"` is a string template: `$name` inside quotes is replaced by its value.
        val asonante = if (t == silabas.lastIndex) "$tonica" else {
            // Not *aguda*: look at the last syllable.
            val ult = silabas.last()
            // Its main vowel, without accent mark (or null if it has none). `?.let` only runs
            // when the position is not null.
            val f = indiceVocalTonica(ult)?.let { sinTilde(ult.lowercase()[it]) }
            // In the unstressed final syllable, i counts as e and u counts as o.
            val eq = when (f) { 'i' -> 'e'; 'u' -> 'o'; else -> f }
            // Stressed vowel + final vowel. `${eq ?: ""}`: if `eq` is null, put nothing.
            "$tonica${eq ?: ""}"
        }
        // Step 5, and build the result.
        return Terminacion(palabra, texto, fonetica(texto, seseo), asonante)
    }

    /**
     * Do two lines rhyme? Returns the kind of rhyme, or null.
     *
     * Receives: two lines [a] and [b], and [seseo]. Returns: CONSONANTE if their full-rhyme
     * keys are equal; otherwise ASONANTE if their vowel keys are equal; otherwise `null`.
     * Examples: "cielo"/"suelo" CONSONANTE; "casa"/"caza" ASONANTE (CONSONANTE with *seseo*);
     * "casa"/"perro" null.
     */
    fun comparar(a: String, b: String, seseo: Boolean = false): Tipo? {
        // If either line has no ending, they cannot rhyme: return null right away.
        val ta = terminacion(a, seseo) ?: return null
        val tb = terminacion(b, seseo) ?: return null
        // `when { ... }` with no value: the first true condition wins.
        return when {
            ta.consonante == tb.consonante -> Tipo.CONSONANTE
            ta.asonante == tb.asonante -> Tipo.ASONANTE
            else -> null
        }
    }

    /**
     * Rhyme scheme of a text. Returns one item per line (null for empty lines). Letters are
     * given in order of appearance. Full rhymes (*consonante*) are grouped first; the lines left
     * alone are joined to an existing group by vowel rhyme (*asonante*), or form a new group.
     * [minusculas] ("lowercase"): use it for *arte menor* (lines of 8 syllables or fewer),
     * which by tradition is written with lowercase letters (abba instead of ABBA).
     *
     * Receives: [lineas], the lines of the text; [seseo]; [minusculas].
     * Returns: a list with one [RimaVerso] (or null) per line. `List<RimaVerso?>` = a list
     * whose items may be null.
     *
     * ALGORITHM, STEP BY STEP:
     * 1. Compute the ending of every non-empty line.
     * 2. Group lines that have the SAME full-rhyme key. Every group with 2 or more lines is a
     *    rhyme group of type CONSONANTE.
     * 3. Lines that are alone (no full-rhyme partner) are grouped by their vowel key:
     *    - if some existing group has that same vowel key, they join it as ASONANTE;
     *    - otherwise, if 2 or more lone lines share it, they form a new ASONANTE group.
     * 4. Sort the groups by their first line, and give them letters A, B, C... in that order.
     * 5. Lines in no group get '-'.
     * Example: a sonnet gives ABBA ABBA...; a *romance* (a traditional poem with vowel rhyme on
     * the even lines) gives -a-a-a.
     */
    fun esquema(lineas: List<String>, seseo: Boolean = false, minusculas: Boolean = false): List<RimaVerso?> {
        // Step 1. For each line: null if it is blank (only spaces or nothing), else its ending
        // (which may also be null if it has no words).
        val ts = lineas.map { if (it.isBlank()) null else terminacion(it, seseo) }
        // Positions of the lines that do have an ending. `ts.indices` = all valid positions.
        val indices = ts.indices.filter { ts[it] != null }

        // A small helper class defined INSIDE this function (only used here). A group has:
        // - `asonante`: its vowel key;
        // - `miembros` ("members"): a changeable map from line position to rhyme type.
        //   A MAP is a table of key → value pairs, like a dictionary; `MutableMap<Int, Tipo>`
        //   maps whole numbers (line positions) to rhyme types, and can be changed.
        class Grupo(val asonante: String, val miembros: MutableMap<Int, Tipo>)
        val grupos = mutableListOf<Grupo>()

        // Step 2. `groupBy { ... }` sorts the positions into a map: key = full-rhyme key,
        // value = list of positions with that key.
        // `ts[it]!!` : the `!!` operator says "I am sure this is not null, use it". (If it were
        // null the program would crash; here it is safe because `indices` only has non-null
        // positions.)
        val porConsonante = indices.groupBy { ts[it]!!.consonante }
        // `.values` = only the lists of positions. Keep those with 2 or more lines and create a
        // group for each one. `v.associateWith { Tipo.CONSONANTE }` builds a map where every
        // position in `v` points to CONSONANTE; `.toMutableMap()` makes it changeable.
        porConsonante.values.filter { it.size >= 2 }.forEach { v ->
            grupos += Grupo(ts[v[0]]!!.asonante, v.associateWith { Tipo.CONSONANTE }.toMutableMap())
        }
        // Step 3. Lone lines: those whose full-rhyme group has fewer than 2 members.
        val sueltos = indices.filter { porConsonante[ts[it]!!.consonante]!!.size < 2 }
        // Group them by vowel key. `forEach { (aso, v) -> ... }` takes each map entry apart
        // into its key (`aso`, the vowel key) and value (`v`, the positions). This is called
        // "destructuring".
        sueltos.groupBy { ts[it]!!.asonante }.forEach { (aso, v) ->
            // The first existing group with the same vowel key, or null if there is none.
            val destino = grupos.firstOrNull { it.asonante == aso }
            when {
                // Join that group, each one as ASONANTE. `destino.miembros[it] = x` puts an
                // entry into the map.
                destino != null -> v.forEach { destino.miembros[it] = Tipo.ASONANTE }
                // No group to join, but 2 or more lone lines share the vowels: new group.
                v.size >= 2 -> grupos += Grupo(aso, v.associateWith { Tipo.ASONANTE }.toMutableMap())
            }
        }
        // Step 4. Sort groups by their smallest (first) line position.
        // `g.miembros.keys` = the positions in that group; `.min()` = the smallest.
        grupos.sortBy { g -> g.miembros.keys.min() }

        // Step 5. Start with a list of the right size where every line with an ending gets
        // '-' (no rhyme) and every empty line gets null.
        // `MutableList<RimaVerso?>(n) { i -> ... }` creates n items using the lambda.
        // `ts[i]?.let { ... }` : if the ending exists, build the RimaVerso; if not, null.
        val resultado = MutableList<RimaVerso?>(lineas.size) { i -> ts[i]?.let { RimaVerso('-', null, it) } }
        // Now give each group its letter. `n` is the group's position (0, 1, 2...).
        grupos.forEachIndexed { n, g ->
            // `'A' + n` is the letter n places after A ('A' + 1 = 'B'). If lowercase was asked
            // for, it is turned into lowercase.
            val letra = ('A' + n).let { if (minusculas) it.lowercaseChar() else it }
            // Write the letter and rhyme type into each member line.
            g.miembros.forEach { (i, tipo) -> resultado[i] = RimaVerso(letra, tipo, ts[i]!!) }
        }
        return resultado
    }

    /**
     * Turns a scheme into text: "ABBA ABBA". Empty lines are shown as a space.
     *
     * `joinToString("") { ... }` glues all items together with nothing in between, using the
     * lambda to turn each item into text. `it?.letra?.toString() ?: " "` means: if the item is
     * not null, its letter as text; if it is null, a space.
     */
    fun esquemaComoTexto(esquema: List<RimaVerso?>): String =
        esquema.joinToString("") { it?.letra?.toString() ?: " " }
}
