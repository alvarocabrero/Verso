// =================================================================================================
// FILE: AnalisisPoemaTest.kt  (automatic tests for the full analysis of a poem)
// -------------------------------------------------------------------------------------------------
// WHAT IS THIS FILE FOR?
// It checks `AnalisisPoema` (analisis/AnalisisPoema.kt), the part of the engine that puts
// everything together for the editor: for a whole text it finds the *metro* (line length in
// syllables), the syllables of each line, the *esquema de rima* (rhyme scheme), the literary
// devices, and the exact character positions to highlight or colour on screen.
//
// `AnalisisPoema.analizar(text)` returns a `Resultado` (result) with, among other things:
//   - `metro`: the *metro dominante* (the length most lines fit), or null if there is none,
//   - `lineas`: one `Linea` per line of text, with `silabas` (its count), `encaja` (true if
//     it fits the metro), `rima` and `inicio`/`fin` (where the line starts and ends in the
//     text, counted in characters from the beginning),
//   - `esquema`: the rhyme scheme as text ("ABBA"),
//   - `recursos`: the literary devices found.
//
// Metrics terms used below (in Spanish, as in the engine):
//   - *endecasílabo*: an 11-syllable line. *arte menor*: lines of 8 syllables or fewer (their
//     rhyme scheme uses small letters).
//   - *rima abrazada*: "embraced" rhyme, ABBA. *rima consonante*: all sounds of the ending
//     match. *rima asonante*: only the vowels match. *rima interna*: a rhyme inside a line.
//   - *verso*: a line of a poem. *estrofa*: a stanza (lines between empty lines).
//   - *anadiplosis*: a line starts with the word the previous line ended with.
//   - *anáfora*: several lines start with the same word.
//   - *asíndeton*: a list with no conjunctions. *geminación*: a word repeated right away.
//
// WHAT IS A TEST? Code that runs part of the app with a known input and compares the result
// with the right answer; if they differ, the test "fails". Developers run tests to be sure a
// change did not break anything. They are not part of the installed app.
//
// WHAT IS JUNIT? The usual library to write and run tests:
//   - `@Test` marks a function as a test; JUnit runs each one.
//   - `assertEquals(expected, actual)`: fails if the two values are different.
//   - `assertTrue(x)` / `assertFalse(x)`: fail if `x` is not true / not false.
//   - `assertNull(x)`: fails if `x` is not `null` (null = "nothing").
// =================================================================================================

// Same package as the engine, so the tests can use its code directly.
package com.tuapp.analisis

// All the device types (ANADIPLOSIS, ANAFORA...) by their short name, thanks to the final `.*`.
import com.tuapp.analisis.Recursos.Tipo.*
// JUnit tools (explained above).
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for `AnalisisPoema`. JUnit runs every function marked with `@Test`. */
class AnalisisPoemaTest {

    /**
     * First quatrain of Lope de Vega's sonnet "Un soneto me manda hacer Violante": four
     * *endecasílabos* with *rima abrazada* (ABBA).
     *
     * - `private val`: a read-only value only visible inside this class.
     * - `"""..."""` is a text that can span several lines. `.trimIndent()` removes the spaces
     *   that all lines share at the start (the ones used to indent the code).
     */
    private val lope = """
        Un soneto me manda hacer Violante,
        que en mi vida me he visto en tanto aprieto;
        catorce versos dicen que es soneto;
        burla burlando van los tres delante.
    """.trimIndent()

    /**
     * Four lines by Antonio Machado: *arte menor* (8 syllables), only lines 2 and 4 rhyme
     * ("pasar" / "mar").
     */
    private val machado = """
        Todo pasa y todo queda,
        pero lo nuestro es pasar,
        pasar haciendo caminos,
        caminos sobre la mar.
    """.trimIndent()

    /**
     * Helper: turns a list of character ranges into the pieces of text they cover, so the
     * tests can compare readable words instead of numbers.
     * - `IntRange` is a range of whole numbers, like `0..4` (from 0 to 4, both included).
     * - `rangos.map { r.texto.substring(it) }`: for each range (`it`), cut that piece out of
     *   the analysed text.
     */
    private fun textos(r: AnalisisPoema.Resultado, rangos: List<IntRange>) = rangos.map { r.texto.substring(it) }

    /**
     * Lope's quatrain: the metro must be 11, every line must have 11 syllables and fit
     * (`all { it.encaja }` is true if EVERY line fits), and the scheme must be "ABBA".
     * `r.lineas.map { it.silabas }` makes the list of the syllable counts of the lines.
     */
    @Test fun endecasilabosConRimaAbrazada() {
        val r = AnalisisPoema.analizar(lope)
        assertEquals(11, r.metro)
        assertEquals(listOf(11, 11, 11, 11), r.lineas.map { it.silabas })
        assertTrue(r.lineas.all { it.encaja })
        assertEquals("ABBA", r.esquema)
    }

    /**
     * Machado's lines: the metro is 8, so the scheme uses small letters (*arte menor*):
     * "-a-a" (lines 1 and 3 do not rhyme).
     */
    @Test fun arteMenorEnMinusculas() {
        val r = AnalisisPoema.analizar(machado)
        assertEquals(8, r.metro)
        assertEquals("-a-a", r.esquema)
    }

    /**
     * Adding a short line ("soneto", 3 syllables) at the end of Lope's quatrain:
     * the metro is still 11, but the last line does not fit (`encaja` is false) and shows its
     * own count, 3. `"$lope\nsoneto"` is a string template: `$lope` is replaced by the poem,
     * and `\n` means "new line". `.last()` gives the last element of the list.
     */
    @Test fun versoQueNoEncaja() {
        val r = AnalisisPoema.analizar("$lope\nsoneto")
        assertEquals(11, r.metro)
        assertFalse(r.lineas.last().encaja)
        assertEquals(3, r.lineas.last().silabas)
    }

    /**
     * Positions and stanzas, with the text "hola", an empty line, and "adiós":
     * - Line positions (`inicio..fin`): "hola" goes 0..4, the empty line 5..5, "adiós" 6..11.
     *   (`..` makes a range from the first number to the second.)
     * - The empty line (`lineas[1]`, the second one: lists count from 0) has no syllables and
     *   no rhyme (both `null`).
     * - Scheme "- -": two lines that rhyme with nothing ("-"), with a space for the empty line
     *   that separates the two stanzas.
     */
    @Test fun posicionesYEstrofas() {
        val r = AnalisisPoema.analizar("hola\n\nadiós")
        assertEquals(listOf(0..4, 5..5, 6..11), r.lineas.map { it.inicio..it.fin })
        assertNull(r.lineas[1].silabas)
        assertNull(r.lineas[1].rima)
        assertEquals("- -", r.esquema)
    }

    /** An empty text: no metro, empty scheme, and no devices (`isEmpty()` = no elements). */
    @Test fun textoVacio() {
        val r = AnalisisPoema.analizar("")
        assertNull(r.metro)
        assertEquals("", r.esquema)
        assertTrue(r.recursos.isEmpty())
    }

    /**
     * Highlighting an *anadiplosis* in Machado's lines: `AnalisisPoema.rangos(...)` gives the
     * character ranges to highlight, and they must cover the two "pasar" (end of line 2 and
     * start of line 3). `.first { ... }` takes the first device of that type.
     */
    @Test fun resaltaAnadiplosis() {
        val r = AnalisisPoema.analizar(machado)
        val recurso = r.recursos.first { it.tipo == ANADIPLOSIS }
        assertEquals(listOf("pasar", "pasar"), textos(r, AnalisisPoema.rangos(r, recurso)))
    }

    /**
     * Highlighting an *anáfora*: only the "te" at the START of each line is highlighted, not
     * the second "te" in the middle of each line. The ranges start at positions 0, 21 and 43
     * (the first character of each line). `it.first` is the first number of a range.
     */
    @Test fun resaltaAnaforaSoloAlInicio() {
        val r = AnalisisPoema.analizar("te busco y te llamo,\nte pienso y te sueño,\nte quiero y te nombro")
        val recurso = r.recursos.first { it.tipo == ANAFORA }
        val rangos = AnalisisPoema.rangos(r, recurso)
        assertEquals(listOf("te", "te", "te"), textos(r, rangos))
        assertEquals(listOf(0, 21, 43), rangos.map { it.first })
    }

    /**
     * A device with no specific words (*asíndeton*) highlights the whole line, without the
     * opening "¡" and the closing "!".
     */
    @Test fun resaltaVersoEnteroSinPalabras() {
        val r = AnalisisPoema.analizar("¡Ojos, boca, manos, pelo!")
        val recurso = r.recursos.first { it.tipo == ASINDETON }
        assertEquals(listOf("Ojos, boca, manos, pelo"), textos(r, AnalisisPoema.rangos(r, recurso)))
    }

    /**
     * Highlighting a *geminación* (Lorca): the three "verde" in a row are highlighted (the
     * repeated ones), not the first "Verde" at the start of the line.
     */
    @Test fun resaltaGeminacion() {
        val r = AnalisisPoema.analizar("Verde que te quiero verde, verde, verde")
        val recurso = r.recursos.first { it.tipo == GEMINACION }
        assertEquals(listOf("verde", "verde", "verde"), textos(r, AnalisisPoema.rangos(r, recurso)))
    }

    // ---------- Coloreado de rimas ----------
    // Rhyme colouring: the editor can paint each rhyme group in its own colour. These tests
    // check which pieces of text get coloured, with which group and which rhyme type.

    /**
     * Helper: analyses [texto] and returns, for each piece to colour, a `Triple` (a group of
     * three values): the coloured text, its colour group (0 = A, 1 = B...), and its rhyme
     * type (`Rima.Tipo`, consonante or asonante).
     * - `seseo: Boolean = false`: optional option, off by default.
     * - `List<Triple<String, Int, Rima.Tipo>>`: a list of triples of (text, number, type).
     * - `return` gives back the value; it is needed here because the body is a block `{ }`.
     */
    private fun tramos(texto: String, seseo: Boolean = false): List<Triple<String, Int, Rima.Tipo>> {
        val r = AnalisisPoema.analizar(texto, seseo)
        return AnalisisPoema.tramosDeRima(r).map { Triple(texto.substring(it.rango), it.grupo, it.tipo) }
    }

    /**
     * Lope's quatrain: the endings "ante" (group A = 0) and "eto" (group B = 1) are coloured,
     * all *consonante*. There is also a *rima interna*: "soneto" inside line 1 rhymes with
     * "aprieto" (group B), so it gets the B colour too. The pieces come in text order.
     * `val c = ...` is a short name so the list below is easier to read.
     */
    @Test fun tramosDeUnCuarteto() {
        val c = Rima.Tipo.CONSONANTE
        assertEquals(
            listOf(
                // «soneto», internal rhyme with «aprieto» (group B).
                Triple("eto", 1, c),
                // Violante (A).
                Triple("ante", 0, c),
                // aprieto (B).
                Triple("eto", 1, c),
                // soneto, end of line 3 (B).
                Triple("eto", 1, c),
                // delante (A).
                Triple("ante", 0, c)
            ),
            tramos(lope)
        )
    }

    /**
     * Only lines that rhyme are coloured.
     * `List(3) { ... }` creates a list of 3 elements, each made by the code in braces (here,
     * three equal triples).
     */
    @Test fun tramosSoloDeVersosQueRiman() {
        // "-a-a": the lines that rhyme with nothing are not coloured; the "pasar" at the
        // start of the third line is an internal rhyme with "mar" and takes its colour.
        // So: "ar" of "pasar" (line 2), "ar" of "pasar" (line 3), "ar" of "mar" (line 4).
        assertEquals(
            List(3) { Triple("ar", 0, Rima.Tipo.CONSONANTE) },
            tramos(machado)
        )
    }

    /**
     * *Rima asonante*: "cielo" and "lejos" share only the vowels e-o, so both endings are
     * coloured in group 0 with type ASONANTE.
     */
    @Test fun tramosAsonantes() {
        assertEquals(
            listOf(Triple("elo", 0, Rima.Tipo.ASONANTE), Triple("ejos", 0, Rima.Tipo.ASONANTE)),
            tramos("mirando al cielo\nse fue muy lejos")
        )
    }

    /**
     * An internal rhyme that does not match any line ending ("luna" / "laguna", both inside
     * the line) gets its own new colour group, here 0 because there are no other groups.
     */
    @Test fun rimaInternaSinFinalTieneGrupoPropio() {
        assertEquals(
            listOf(Triple("una", 0, Rima.Tipo.CONSONANTE), Triple("una", 0, Rima.Tipo.CONSONANTE)),
            tramos("la luna sobre la laguna se dormía")
        )
    }
}
