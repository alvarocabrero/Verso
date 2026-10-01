// =================================================================================================
// FILE: RimaTest.kt  (automatic tests for rhyme detection)
// -------------------------------------------------------------------------------------------------
// WHAT IS THIS FILE FOR?
// It checks that `Rima` (analisis/Rima.kt) finds where the rhyme of a line starts, decides if
// two lines rhyme (and how), and builds the *esquema de rima* (rhyme scheme) of a poem.
//
// Metrics terms used below (in Spanish, as in the engine):
//   - *terminación*: the ending of a line from its last stressed vowel ("cielo" -> "elo").
//     That is the part that must match for two lines to rhyme.
//   - *rima consonante*: full rhyme; ALL the sounds of the ending match ("cielo" / "suelo").
//   - *rima asonante*: only the VOWELS of the ending match ("cántaro" / "pájaro": a...o).
//   - *seseo*: pronouncing "s", "z" and "c" (before e/i) the same, as in Latin America or
//     Andalusia. With *seseo*, "casa" and "caza" sound the same.
//   - *esquema de rima*: one letter per line. Lines with the same letter rhyme together; "-"
//     is a line that rhymes with no other; a space is an empty line (a break between
//     *estrofas*, i.e. stanzas). Capital letters are used for *arte mayor* (long lines, 9
//     or more syllables) and small letters for *arte menor* (8 or fewer).
//   - *romance*: a traditional Spanish poem of 8-syllable lines where only the even lines
//     rhyme, in *asonante* ("-a-a").
//   - *aguda* / *llana* / *esdrújula*: word stressed on the last / second-to-last /
//     third-to-last syllable.
//
// WHAT IS A TEST? Code that runs part of the app with a known input and compares the result
// with the right answer; if they differ, the test "fails". Developers run tests to be sure a
// change did not break anything. They are not part of the installed app.
//
// WHAT IS JUNIT? The usual library to write and run tests:
//   - `@Test` marks a function as a test; JUnit runs each one.
//   - `assertEquals(expected, actual)` fails the test if the two values are different.
//   - `assertNull(value)` fails the test if the value is not `null` (null = "nothing"; here it
//     means "they do not rhyme").
// =================================================================================================

// Same package as the engine, so the tests can use its code directly.
package com.tuapp.analisis

// The two kinds of rhyme, imported by name so we can write `ASONANTE` instead of
// `Rima.Tipo.ASONANTE`.
import com.tuapp.analisis.Rima.Tipo.ASONANTE
import com.tuapp.analisis.Rima.Tipo.CONSONANTE
// JUnit tools (explained above).
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Tests for `Rima`. JUnit runs every function marked with `@Test`. */
class RimaTest {

    /**
     * Short helper: compares two lines [a] and [b] and returns `CONSONANTE`, `ASONANTE` or
     * `null` (no rhyme). [seseo] is optional: `= false` is its default value, so it can be
     * left out.
     */
    private fun rima(a: String, b: String, seseo: Boolean = false) = Rima.comparar(a, b, seseo)

    /**
     * The *terminación* (ending from the last stressed vowel) of a few lines.
     * `Rima.terminacion(...)` may return null, so `!!` says "it is not null here" before we
     * read `.texto` (the written ending) or `.asonante` (just its vowels).
     */
    @Test fun terminaciones() {
        assertEquals("elo", Rima.terminacion("mirando al cielo")!!.texto)
        // "cuida": in the *diptongo* "ui" the stress falls on the "i", so the ending is "ida".
        assertEquals("ida", Rima.terminacion("nadie la cuida")!!.texto)
        assertEquals("ántaro", Rima.terminacion("agua del cántaro")!!.texto)
        // *Esdrújula* word: the vowels that count are the stressed one (á) and the last one (o),
        // so the *asonante* key is "ao" (the middle "a" is skipped).
        assertEquals("ao", Rima.terminacion("agua del cántaro")!!.asonante)
    }

    /**
     * Pairs with *rima consonante* (all sounds match). Note that the comparison is by SOUND:
     * "guerra" / "tierra" (the "u" in "gue" is silent), "gente" / "fuente" (the "ue" diphthong
     * counts from its stressed "e"), "hoy" / "voy" (the "h" is silent).
     */
    @Test fun consonantes() {
        assertEquals(CONSONANTE, rima("cielo", "suelo"))
        assertEquals(CONSONANTE, rima("guerra", "tierra"))
        assertEquals(CONSONANTE, rima("fuego", "juego"))
        assertEquals(CONSONANTE, rima("gente", "fuente"))
        assertEquals(CONSONANTE, rima("hoy", "voy"))
        assertEquals(CONSONANTE, rima("amor", "flor"))
    }

    /** Pairs with *rima asonante* (only the vowels match). */
    @Test fun asonantes() {
        assertEquals(ASONANTE, rima("cántaro", "pájaro"))
        // "b" and "v" sound the same, but "br" is not "b", so it is only *asonante*.
        assertEquals(ASONANTE, rima("vive", "libre"))
        // An unstressed final "i" counts as "e": "fácil" (a-i) rhymes with "calle" (a-e).
        assertEquals(ASONANTE, rima("fácil", "calle"))
        // An unstressed final "u" counts as "o": "Venus" (e-u) rhymes with "tenso" (e-o).
        assertEquals(ASONANTE, rima("Venus", "tenso"))
        assertEquals(ASONANTE, rima("sangre", "hambre"))
    }

    /** Pairs that do not rhyme: the result must be `null`. */
    @Test fun noRiman() {
        assertNull(rima("casa", "perro"))
        // "mar" is *aguda* (ending "ar") and "montaña" is *llana* (ending "aña"): the vowel
        // patterns ("a" and "aa") differ, so no rhyme.
        assertNull(rima("mar", "montaña"))
    }

    /**
     * *Seseo*: without it, "casa" / "caza" is only *asonante* ("s" and "z" sound different in
     * Spain). With `seseo = true` they sound the same, so it becomes *consonante*.
     */
    @Test fun seseo() {
        assertEquals(ASONANTE, rima("casa", "caza"))
        assertEquals(CONSONANTE, rima("casa", "caza", seseo = true))
    }

    /**
     * Helper: builds the rhyme scheme of the given lines and returns it as text ("ABBA").
     *
     * - `vararg lineas: String` means the function accepts ANY number of text arguments,
     *   which arrive together as `lineas` (like a list).
     * - `minusculas` (small letters) is a named option, false by default; when true the
     *   scheme uses small letters (*arte menor*).
     * - `lineas.toList()` turns the arguments into a normal list.
     */
    private fun esquema(vararg lineas: String, minusculas: Boolean = false) =
        Rima.esquemaComoTexto(Rima.esquema(lineas.toList(), minusculas = minusculas))

    /**
     * A Garcilaso quatrain with *rima abrazada* ("embraced" rhyme): lines 1 and 4 rhyme
     * ("gesto" / "esto") and lines 2 and 3 rhyme ("deseo" / "leo"): "ABBA".
     */
    @Test fun sonetoABBA() {
        assertEquals("ABBA", esquema(
            "Escrito está en mi alma vuestro gesto",
            "y cuanto yo escribir de vos deseo;",
            "vos sola lo escribistes, yo lo leo",
            "tan solo, que aun de vos me guardo en esto."
        ))
    }

    /**
     * *Romances*: odd lines are free ("-") and even lines rhyme.
     * 1. A traditional romance in small letters (`minusculas = true`): "-a-a"
     *    ("calor" / "flor").
     * 2. Lorca, in capital letters: "-A-A" ("mar" / "montaña" alone would not rhyme, but
     *    "ramas" / "montaña" share the vowels a-a, so line 4 rhymes with line 2 in
     *    *asonante*). `lorca[3]` is the 4th line (lists start counting at 0), and its rhyme
     *    type must be `ASONANTE`.
     */
    @Test fun romances() {
        assertEquals("-a-a", esquema(
            "Que por mayo era, por mayo,",
            "cuando hace la calor,",
            "cuando los trigos encañan",
            "y están los campos en flor",
            minusculas = true
        ))
        val lorca = Rima.esquema(listOf(
            "Verde que te quiero verde.",
            "Verde viento. Verdes ramas.",
            "El barco sobre la mar",
            "y el caballo en la montaña."
        ))
        assertEquals("-A-A", Rima.esquemaComoTexto(lorca))
        assertEquals(ASONANTE, lorca[3]!!.tipo)
    }

    /**
     * A line with only an *asonante* match joins an existing *consonante* group.
     * "amor", "flor", "dolor" rhyme fully (group A); "sol" only shares the vowel "o", so it
     * joins group A as *asonante*. The empty line "" shows as a space: "AA AA".
     * `r[3]` ("dolor") must be `CONSONANTE` and `r[4]` ("sol") `ASONANTE`.
     */
    @Test fun asonanteSeUneAGrupoConsonante() {
        val r = Rima.esquema(listOf("amor", "flor", "", "dolor", "sol"))
        assertEquals("AA AA", Rima.esquemaComoTexto(r))
        assertEquals(CONSONANTE, r[3]!!.tipo)
        assertEquals(ASONANTE, r[4]!!.tipo)
    }
}
