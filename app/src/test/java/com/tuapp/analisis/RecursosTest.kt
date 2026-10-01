// =================================================================================================
// FILE: RecursosTest.kt  (automatic tests for literary device detection)
// -------------------------------------------------------------------------------------------------
// WHAT IS THIS FILE FOR?
// It checks that `Recursos` (analisis/Recursos.kt) finds the *recursos literarios* (literary
// devices: patterns of repetition and sound that poets use) in a list of lines, and that it
// does NOT find them where there are none ("false positives").
//
// `Recursos.detectar(lines)` returns a list of `Recurso` objects. Each one has:
//   - `tipo`: which device it is (one of the values of `Recursos.Tipo`),
//   - `lineas`: the positions of the lines involved (counting from 0: the first line is 0),
//   - `palabras`: the words to highlight,
//   - `evidencia`: a short text that explains what was found (shown to the user),
//   - `clara`: for *aliteración*, true if it is "clear", false if it is only "possible".
//
// The devices (Spanish names, as in the engine):
//   - *anáfora*: several lines START with the same word(s).
//   - *epífora*: several lines END with the same word(s).
//   - *anadiplosis*: a line starts with the word the previous line ended with.
//   - *epanadiplosis*: a line starts and ends with the same word.
//   - *geminación*: a word is repeated right after itself ("palabras, palabras").
//   - *polisíndeton*: many conjunctions ("y ... y ... y").
//   - *asíndeton*: a list of items with NO conjunctions ("acude, corre, vuela").
//   - *paralelismo*: lines next to each other with the same grammar structure.
//   - *estribillo*: a refrain, a line repeated along the text.
//   - *aliteración*: the same consonant sound repeated more than normal in Spanish.
//   - *rima interna*: a word INSIDE a line rhymes (full rhyme) with another word of the same
//     line or with the end of a neighbour line.
//   - *verso*: a line of a poem. *estrofa*: a stanza (a group of lines between empty lines).
//
// WHAT IS A TEST? Code that runs part of the app with a known input and compares the result
// with the right answer; if they differ, the test "fails". Developers run tests to be sure a
// change did not break anything. They are not part of the installed app.
//
// WHAT IS JUNIT? The usual library to write and run tests:
//   - `@Test` marks a function as a test; JUnit runs each one.
//   - `assertEquals(expected, actual)` fails the test if the two values are different.
//   - `assertTrue(condition)` fails the test if the condition is false.
// =================================================================================================

// Same package as the engine, so the tests can use its code directly.
package com.tuapp.analisis

// `Tipo` (the kinds of device) by its short name, and all its values (ANAFORA, EPIFORA...)
// thanks to the final `.*`, so we can write `ANAFORA` instead of `Recursos.Tipo.ANAFORA`.
import com.tuapp.analisis.Recursos.Tipo
import com.tuapp.analisis.Recursos.Tipo.*
// JUnit tools (explained above).
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for `Recursos`. JUnit runs every function marked with `@Test`. */
class RecursosTest {

    /**
     * Helper: detects the devices in the given lines and returns ONLY their types, in order.
     *
     * - `vararg lineas: String`: the function accepts any number of texts, which arrive
     *   together as `lineas`. `lineas.toList()` turns them into a normal list.
     * - `.map { it.tipo }`: `map` builds a new list by applying the code in braces to each
     *   element; `it` is the automatic name of the current element. So we keep each `tipo`.
     * - `List<Tipo>`: a list of device types.
     */
    private fun tipos(vararg lineas: String): List<Tipo> =
        Recursos.detectar(lineas.toList()).map { it.tipo }

    /**
     * Helper: detects the devices and returns the FIRST one of the given [tipo].
     * `.first { condition }` gives the first element for which the condition is true (and
     * fails the test with an error if there is none). `==` checks equality.
     */
    private fun primero(tipo: Tipo, vararg lineas: String) =
        Recursos.detectar(lineas.toList()).first { it.tipo == tipo }

    /**
     * *Anáfora* (Miguel Hernández): the three lines start with "temprano". The device must
     * cover lines 0, 1 and 2 and highlight the word "temprano" (upper/lower case ignored).
     */
    @Test fun anafora() {
        val r = primero(ANAFORA,
            "Temprano levantó la muerte el vuelo,",
            "temprano madrugó la madrugada,",
            "temprano estás rodando por el suelo.")
        assertEquals(listOf(0, 1, 2), r.lineas)
        assertEquals(listOf("temprano"), r.palabras)
    }

    /**
     * Three devices at once: the lines start the same ("te": *anáfora*), end the same
     * ("en la noche": *epífora*) and have the same structure (*paralelismo*). The types must
     * come in that order. Then, for just two lines, the *epífora* must highlight the three
     * shared final words.
     */
    @Test fun anaforaEpiforaYParalelismo() {
        val t = tipos("te busco en la noche,", "te pienso en la noche,", "te sueño en la noche")
        assertEquals(listOf(ANAFORA, EPIFORA, PARALELISMO), t)
        assertEquals(listOf("en", "la", "noche"),
            primero(EPIFORA, "te busco en la noche,", "te pienso en la noche,").palabras)
    }

    /**
     * *Anadiplosis* (Antonio Machado): line 1 ends in "pasar" and line 2 starts with it; line 2
     * ends in "caminos" and line 3 starts with it. So there must be two: lines (1, 2) and
     * (2, 3). `.filter { ... }` keeps only the elements that match the condition.
     */
    @Test fun anadiplosis() {
        val r = Recursos.detectar(listOf(
            "Todo pasa y todo queda,",
            "pero lo nuestro es pasar,",
            "pasar haciendo caminos,",
            "caminos sobre la mar."
        )).filter { it.tipo == ANADIPLOSIS }
        assertEquals(listOf(listOf(1, 2), listOf(2, 3)), r.map { it.lineas })
    }

    /** *Epanadiplosis* (Lorca): the line starts and ends with "verde"; nothing else is found. */
    @Test fun epanadiplosis() {
        assertEquals(listOf(EPANADIPLOSIS), tipos("Verde que te quiero verde."))
    }

    /**
     * *Paralelismo* (Bécquer): "X son Y y van al Z" in both lines. Only that device is found.
     */
    @Test fun paralelismo() {
        assertEquals(listOf(PARALELISMO), tipos(
            "Los suspiros son aire y van al aire.",
            "Las lágrimas son agua y van al mar."))
    }

    /**
     * *Asíndeton*: a list separated by commas with no "y". *Polisíndeton*: many "y".
     */
    @Test fun asindetonYPolisindeton() {
        assertEquals(listOf(ASINDETON), tipos("Acude, corre, vuela,"))
        assertEquals(listOf(POLISINDETON), tipos("y el santo y la seña y la voz y la luz"))
    }

    /**
     * "Palabras, palabras, palabras" is *geminación* (the same word repeated), and must NOT
     * also count as *asíndeton*, even though it is a list with commas.
     * `GEMINACION in t` is true if the list `t` contains that value; `!in` is "not in".
     */
    @Test fun geminacionNoEsAsindeton() {
        val t = tipos("Palabras, palabras, palabras")
        assertTrue(GEMINACION in t)
        assertTrue(ASINDETON !in t)
    }

    /**
     * *Estribillo*: "Quédate conmigo esta noche" appears at line 0 and line 3 (line 2 is an
     * empty line between stanzas), so the refrain covers lines 0 and 3.
     */
    @Test fun estribillo() {
        val r = primero(ESTRIBILLO,
            "Quédate conmigo esta noche", "que el tiempo se nos va", "",
            "Quédate conmigo esta noche", "no me dejes nunca más")
        assertEquals(listOf(0, 3), r.lineas)
    }

    /**
     * *Aliteración* in Rubén Darío's line "bajo el ala aleve del leve abanico": the sound
     * "b/v" (same sound in Spanish) appears 4 times and "l" 3 times, so two *aliteraciones*
     * are found, each with its `evidencia` text. `.toSet()` turns the list into a "set" (a
     * group with no order), so the order of the two does not matter.
     * Then two more lines must also contain an *aliteración* ("s" and "m" sounds).
     * `.contains(x)` is true if the list has `x`.
     */
    @Test fun aliteracion() {
        val r = Recursos.detectar(listOf("bajo el ala aleve del leve abanico"))
            .filter { it.tipo == ALITERACION }
        assertEquals(setOf("sonido «b/v» ×4", "sonido «l» ×3"), r.map { it.evidencia }.toSet())
        assertTrue(tipos("en el silencio sólo se escuchaba").contains(ALITERACION))
        assertTrue(tipos("mi mamá me mima mucho").contains(ALITERACION))
    }

    /**
     * *Aliteración* spread over two lines. `r.size` is how many were found (must be 1); it
     * covers lines 0 and 1, and its text says the "s" sound appears 6 times in 2 lines.
     * `r[0]` is the first element of the list.
     */
    @Test fun aliteracionEntreVersos() {
        // Garcilaso: the "s" sound is spread across both lines.
        val r = Recursos.detectar(listOf(
            "en el silencio sólo se escuchaba",
            "un susurro de abejas que sonaba"
        )).filter { it.tipo == ALITERACION }
        assertEquals(1, r.size)
        assertEquals(listOf(0, 1), r[0].lineas)
        assertEquals("sonido «s» ×6 en 2 versos", r[0].evidencia)
    }

    /**
     * When each line has its own *aliteración* of the same sound ("m") and the pair has one
     * too, they are merged into ONE result that covers both lines, instead of three.
     */
    @Test fun versosSueltosSeFusionanEnUnaSola() {
        val r = Recursos.detectar(listOf(
            "mis manos buscan tu mirada muda",
            "mientras la madrugada muere mansa"
        )).filter { it.tipo == ALITERACION }
        // Only one, not three (line 1, line 2, and the pair).
        assertEquals(1, r.size)
        assertEquals(listOf(0, 1), r[0].lineas)
    }

    /**
     * Strength of an *aliteración*: it is "clear" (`clara` = true) when the sound appears at
     * least 4.5 times more than its normal frequency in Spanish; below that it is only
     * "possible".
     * - `.associateBy { it.evidencia }` builds a "map" (a lookup table) where each result is
     *   found by its `evidencia` text, so `r["sonido «l» ×3"]` gets that result.
     * - The lookup may give null, so `!!` says "it is there".
     * - `!` in front of a true/false value turns it around ("not").
     */
    @Test fun intensidad() {
        val r = Recursos.detectar(listOf("bajo el ala aleve del leve abanico"))
            .filter { it.tipo == ALITERACION }.associateBy { it.evidencia }
        // "b/v": 4.7 times the normal frequency, so it is clear.
        assertTrue(r["sonido «b/v» ×4"]!!.clara)
        // "l": 3.6 times the normal frequency, so it is only possible.
        assertTrue(!r["sonido «l» ×3"]!!.clara)
    }

    /**
     * Pairs of lines where NOTHING must be found (Neruda and Garcilaso): repeating a word like
     * "escribir" in two lines is not, by itself, a device. `emptyList<Tipo>()` is an empty
     * list of types (the `<Tipo>` says what kind of list it is).
     */
    @Test fun sinFalsosPositivosEntreVersos() {
        assertEquals(emptyList<Tipo>(), tipos(
            "Puedo escribir los versos más tristes esta noche",
            "Escribir, por ejemplo: la noche está estrellada"))
        assertEquals(emptyList<Tipo>(), tipos(
            "Escrito está en mi alma vuestro gesto",
            "y cuanto yo escribir de vos deseo"))
    }

    /**
     * Well-known lines (Bécquer, Machado, Neruda) where nothing must be found: protection
     * against the engine seeing devices everywhere.
     */
    @Test fun sinFalsosPositivos() {
        assertEquals(emptyList<Tipo>(), tipos(
            "Volverán las oscuras golondrinas",
            "en tu balcón sus nidos a colgar"))
        assertEquals(emptyList<Tipo>(), tipos("Caminante, no hay camino"))
        assertEquals(emptyList<Tipo>(), tipos("Puedo escribir los versos más tristes esta noche"))
    }

    // ---------- Rima interna ----------
    // Tests for *rima interna* (internal rhyme: a word inside a line rhymes fully with
    // another word of the same line or with the end of a neighbour line).

    /**
     * Helper: detects the devices in [lineas] (any number of texts) and keeps only the
     * *rima interna* ones.
     */
    private fun rimasInternas(vararg lineas: String) =
        Recursos.detectar(lineas.toList()).filter { it.tipo == RIMA_INTERNA }

    /**
     * "corazón" (inside the line) rhymes with "canción" (the end of the same line): one
     * result, in line 0, highlighting both words.
     */
    @Test fun rimaInternaConElFinalDelVerso() {
        val r = rimasInternas("tu corazón es mi canción")
        assertEquals(1, r.size)
        assertEquals(listOf(0), r[0].lineas)
        assertEquals(listOf("corazón", "canción"), r[0].palabras)
    }

    /** Two words that rhyme with each other, both inside the line. */
    @Test fun rimaInternaDentroDelVerso() {
        // luna / laguna: both are inside the line (neither is the last word).
        val r = rimasInternas("la luna sobre la laguna se dormía")
        assertEquals(listOf(listOf("luna", "laguna")), r.map { it.palabras })
    }

    /**
     * A word inside one line rhymes with the end of the next line. The `evidencia` text
     * shows the two words in Spanish quotes, joined by "·".
     */
    @Test fun rimaInternaConElVersoVecino() {
        // Lope: "soneto", inside the first line, rhymes with "aprieto", the end of the second.
        val r = rimasInternas(
            "Un soneto me manda hacer Violante,",
            "que en mi vida me he visto en tanto aprieto;"
        )
        assertEquals(1, r.size)
        assertEquals(listOf(0, 1), r[0].lineas)
        assertEquals("«soneto» · «aprieto»", r[0].evidencia)
    }

    /**
     * "casa" / "caza" is NOT a full rhyme without *seseo* (so nothing is found), but it IS
     * with `seseo = true` (then "s" and "z" sound the same). `.isEmpty()` is true when the
     * list has no elements.
     */
    @Test fun rimaInternaConSeseo() {
        assertTrue(rimasInternas("la casa junto a la caza").isEmpty())
        val r = Recursos.detectar(listOf("la casa junto a la caza"), seseo = true)
        assertEquals(listOf(RIMA_INTERNA), r.map { it.tipo })
    }

    /**
     * Cases that must NOT count as *rima interna*.
     */
    @Test fun rimaInternaNoCuentaRepeticionesNiAsonancias() {
        // The same word repeated is not a rhyme.
        assertTrue(rimasInternas("verde que te quiero verde").isEmpty())
        // Only *asonante* (vowels match, consonants don't): casa / plaza.
        assertTrue(rimasInternas("la casa blanca de la plaza").isEmpty())
        // No rhyming words at all.
        assertTrue(rimasInternas("el perro y el gato en la noche").isEmpty())
        // Different stanzas (an empty line between them): "corazón" / "canción" do not count.
        assertTrue(rimasInternas(
            "tu corazón late despacio", "", "y suena la canción").isEmpty())
    }
}
