// =================================================================================================
// FILE: MetricaTest.kt  (automatic tests for counting the syllables of a verse)
// -------------------------------------------------------------------------------------------------
// WHAT IS THIS FILE FOR?
// It checks that `Metrica` (analisis/Metrica.kt) counts the *sílabas métricas* (metrical
// syllables: the syllables of a line of poetry as it is read aloud, which are not always the
// same as the grammar syllables) and finds the *metro* (the length, in syllables, that the
// poem's lines follow).
//
// Metrics terms used below (in Spanish, as in the engine):
//   - *sinalefa*: merging the vowel at the end of a word with the vowel at the start of the
//     next word into one syllable ("no hay" is read "no‿hay", one syllable). In the expected
//     results, the sign "‿" marks a *sinalefa*.
//   - *dialefa*: the opposite, NOT merging those vowels (they stay in two syllables).
//   - *ley del acento final*: the rule that the last word changes the count: if it is *aguda*
//     (stressed on the last syllable) you add 1; if it is *esdrújula* (stressed on the
//     third-to-last) you subtract 1; if it is *llana* (second-to-last) nothing changes.
//   - *octosílabo* (8 syllables), *endecasílabo* (11), *alejandrino* (14): common line lengths.
//
// Because a *sinalefa* can be made or not, a line can have several possible counts. The engine
// gives the smallest (`minimo`, with every possible *sinalefa*) and the largest (`maximo`,
// with none), and `silabasPara(metro)` splits the line so it fits a given length (or gives
// null if it can't).
//
// WHAT IS A TEST? Code that runs part of the app with a known input and compares the result
// with the right answer; if they differ, the test "fails". Developers run tests to be sure a
// change did not break anything. They are not part of the installed app.
//
// WHAT IS JUNIT? The usual library to write and run tests:
//   - `@Test` marks a function as a test; JUnit runs each one.
//   - `assertEquals(expected, actual)` fails the test if the two values are different. An
//     optional first argument is a message shown when it fails.
//   - `assertNull(value)` fails the test if the value is not `null` (null = "nothing").
// =================================================================================================

// Same package as the engine, so the tests can use its code directly.
package com.tuapp.analisis

// JUnit tools (explained above).
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Tests for `Metrica`. JUnit runs every function marked with `@Test`. */
class MetricaTest {

    /**
     * Helper used by most tests. For the line [verso] it checks three things:
     *  1. the smallest possible count is [min],
     *  2. the largest possible count is [max],
     *  3. when the line is split to fit [metro] syllables, the syllables joined with "-" give
     *     exactly [silabas].
     *
     * - `Metrica.Verso(verso)` analyses the line.
     * - `"$verso (mín)"`: a "string template": `$verso` is replaced by the line's text. It is
     *   the message shown if the check fails.
     * - `v.silabasPara(metro)` may return null (when the line can't fit that length). The `!!`
     *   says "I am sure it is not null here"; if it were null, the test would crash (and so
     *   fail), which is what we want.
     */
    private fun check(verso: String, min: Int, max: Int, metro: Int, silabas: String) {
        val v = Metrica.Verso(verso)
        assertEquals("$verso (mín)", min, v.minimo)
        assertEquals("$verso (máx)", max, v.maximo)
        assertEquals(verso, silabas, v.silabasPara(metro)!!.joinToString("-"))
    }

    /**
     * Lines with no possible *sinalefa* (no vowel meets another vowel between words): the
     * smallest and largest counts are the same. Lorca (8) and Bécquer (11).
     */
    @Test fun sinSinalefas() {
        check("Verde que te quiero verde", 8, 8, 8, "Ver-de-que-te-quie-ro-ver-de")
        check("Volverán las oscuras golondrinas", 11, 11, 11,
            "Vol-ve-rán-las-os-cu-ras-go-lon-dri-nas")
    }

    /**
     * *Sinalefa* through a silent "h": "no hay" merges ("no‿hay"), because the "h" is not
     * pronounced. But "hie-" sounds like a consonant ("y"), so "la hierba" does NOT merge.
     */
    @Test fun sinalefaConH() {
        check("Caminante, no hay camino", 8, 9, 8, "Ca-mi-nan-te-no‿hay-ca-mi-no")
        // "hie-" does not make a *sinalefa* (it sounds like a consonant).
        check("la hierba verde", 5, 5, 5, "la-hier-ba-ver-de")
    }

    /**
     * *Ley del acento final* (the last word's stress changes the count).
     */
    @Test fun leyDelAcentoFinal() {
        // Ends in an *aguda* word ("an-DAR"): +1 syllable.
        check("se hace camino al andar", 8, 10, 8, "se‿ha-ce-ca-mi-no‿al-an-dar")
        // Ends in an *aguda* word ("e-CHAR"): +1 syllable.
        check("quiero yo mi suerte echar", 8, 9, 8, "quie-ro-yo-mi-suer-te‿e-char")
        // Ends in an *esdrújula* word ("mur-CIÉ-la-go"): −1 syllable.
        check("dame la mano, murciélago", 8, 8, 8, "da-me-la-ma-no-mur-cié-la-go")
    }

    /**
     * *Dialefa* on a stressed vowel: when the vowel at the end of a word is stressed, the
     * *sinalefa* is broken first, if that is needed to reach the length.
     */
    @Test fun dialefaEnVocalTonica() {
        // Garcilaso: "está‿en" is broken (the final vowel of "está" is stressed) and
        // "mi‿alma" is kept, so the line has 11 syllables.
        check("Escrito está en mi alma vuestro gesto", 10, 13, 11,
            "Es-cri-to‿es-tá-en-mi‿al-ma-vues-tro-ges-to")
        check("En tanto que de rosa y azucena", 10, 12, 11,
            "En-tan-to-que-de-ro-sa‿y-a-zu-ce-na")
    }

    /** An *alejandrino* (14 syllables), by Neruda. */
    @Test fun alejandrino() {
        check("Puedo escribir los versos más tristes esta noche", 14, 15, 14,
            "Pue-do‿es-cri-bir-los-ver-sos-más-tris-tes-es-ta-no-che")
    }

    /**
     * A final "y" that sounds like a consonant before a vowel ("soy un") does not make a
     * *sinalefa*, so the count is fixed at 8 (José Martí).
     */
    @Test fun yFinalNoHaceSinalefa() {
        check("Yo soy un hombre sincero", 8, 8, 8, "Yo-soy-un-hom-bre-sin-ce-ro")
    }

    /**
     * An 8-syllable line can't be read as 11: `silabasPara(11)` must give `null`.
     */
    @Test fun metroNoAdmitido() {
        assertNull(Metrica.Verso("Verde que te quiero verde").silabasPara(11))
    }

    /**
     * The *metro dominante* (the length that most lines fit) of four lines of a Garcilaso
     * sonnet must be 11, and its name must be "endecasílabo".
     *
     * - `"""..."""` is a multi-line text; `.trimIndent()` removes the common spaces at the
     *   start of each line.
     * - `Metrica.analizarTexto` analyses every line; `metroDominante` picks the length.
     * - `metro!!`: `metroDominante` may return null (the `?` type), and `!!` says "it is not
     *   null here".
     */
    @Test fun metroDominanteSoneto() {
        val versos = Metrica.analizarTexto(
            """
            Escrito está en mi alma vuestro gesto
            y cuanto yo escribir de vos deseo;
            vos sola lo escribistes, yo lo leo
            tan solo, que aun de vos me guardo en esto.
            """.trimIndent()
        )
        val metro = Metrica.metroDominante(versos)
        assertEquals(11, metro)
        assertEquals("endecasílabo", Metrica.nombreMetro(metro!!))
    }
}
