// =================================================================================================
// FILE: SilabeadorTest.kt  (automatic tests for the syllable splitter)
// -------------------------------------------------------------------------------------------------
// WHAT IS THIS FILE FOR?
// It checks, automatically, that `Silabeador` (analisis/Silabeador.kt) splits Spanish words
// into syllables correctly, finds the stressed syllable and extracts the words of a line.
// `Silabeador` is the base of the whole analysis engine: counting the syllables of a verse
// (Metrica.kt) and finding rhymes (Rima.kt) both depend on it.
//
// WHAT IS A TEST? A small piece of code that runs part of the app with a known input and
// compares the result with the expected answer. If they differ, the test "fails" and tells
// you which case broke. Tests are not part of the app the user installs: developers run them
// (for example with `gradlew testDebugUnitTest`) to be sure that a change did not break
// something that used to work.
//
// WHAT IS JUNIT? The most common library to write and run tests in Java and Kotlin. It gives:
//   - `@Test`: an annotation (a label starting with @) that marks a function as a test. JUnit
//     finds every function with this label and runs it, one by one.
//   - `assertEquals(expected, actual)`: checks that two values are equal. If they are not, the
//     test fails and shows both values. It can take an optional first argument: a message to
//     show when it fails (useful to know WHICH case broke).
//   - A test passes if it ends without any failed check.
//
// Spanish terms used below (the engine keeps its names in Spanish):
//   - *sílaba*: syllable. *silabear*: to split into syllables.
//   - *diptongo*: two vowels in the same syllable ("ciu-dad"). *triptongo*: three ("buey").
//   - *hiato*: two vowels next to each other but in different syllables ("rí-o").
//   - *aguda*, *llana*, *esdrújula*, *sobresdrújula*: word stressed on the last, second-to-last,
//     third-to-last or an earlier syllable.
// =================================================================================================

// The package of the tests is the same as the engine's, so they can see its code.
package com.tuapp.analisis

// `.*` at the end of an import brings ALL the values of `TipoAcentual` (AGUDA, LLANA, ...), so
// we can write `AGUDA` instead of `Silabeador.TipoAcentual.AGUDA`.
import com.tuapp.analisis.Silabeador.TipoAcentual.*
// JUnit tools (explained at the top of the file).
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for `Silabeador`. JUnit creates an object of this class and runs each `@Test`
 * function in it.
 */
class SilabeadorTest {

    /**
     * Helper used by most tests: splits [palabra] (word) into syllables, joins them with "-"
     * and checks that the result equals [esperado] (expected).
     *
     * - `private fun`: a function only visible inside this class.
     * - `= assertEquals(...)`: the whole body is this one call.
     * - The first argument (`palabra`) is the message shown if it fails, so we know which word
     *   was wrong.
     * - `Silabeador.silabear(palabra)` returns a list of syllables, for example
     *   ["ca", "sa"]; `.joinToString("-")` turns it into the text "ca-sa".
     */
    private fun check(palabra: String, esperado: String) =
        assertEquals(palabra, esperado, Silabeador.silabear(palabra).joinToString("-"))

    // In the tests below, `@Test fun name() { ... }` is written on one line: the label and the
    // function. Several `check(...)` calls on one line are separated by `;`.

    /** Simple words: a consonant between two vowels starts the next syllable ("e-xa-men"). */
    @Test fun basicas() {
        check("casa", "ca-sa"); check("sol", "sol"); check("examen", "e-xa-men")
    }

    /**
     * *Dígrafos*: pairs of letters that make one sound ("rr", "ch", "gu" before e/i). They must
     * never be split: "pe-rro", not "per-ro".
     */
    @Test fun digrafos() {
        check("perro", "pe-rro"); check("chocolate", "cho-co-la-te"); check("guerra", "gue-rra")
    }

    /**
     * Silent "u" (in "que", "gui", it is not pronounced, so it is not a vowel of its own) and
     * *diéresis* "ü" (the dots mean the "u" IS pronounced: "pin-güi-no", and "güéis" is one
     * syllable).
     */
    @Test fun uMudaYDieresis() {
        check("queso", "que-so"); check("guitarra", "gui-ta-rra")
        check("pingüino", "pin-güi-no"); check("averigüéis", "a-ve-ri-güéis")
    }

    /**
     * *Diptongos* and *triptongos*: vowels that stay together in one syllable ("ciu", "ai",
     * "uey"). Also: a final "y" acts as a vowel ("muy", "buey"), and a "h" between vowels does
     * not break a diphthong ("hue-vo"). "construí" keeps "uí" together.
     */
    @Test fun diptongosYTriptongos() {
        check("ciudad", "ciu-dad"); check("cuidado", "cui-da-do"); check("aire", "ai-re")
        check("Europa", "Eu-ro-pa"); check("huevo", "hue-vo"); check("construí", "cons-truí")
        check("buey", "buey"); check("Uruguay", "U-ru-guay"); check("muy", "muy")
    }

    /**
     * *Hiatos*: vowels that go in separate syllables. This happens with two strong vowels
     * (a, e, o: "po-e-ta") or with a stressed weak vowel (í, ú: "rí-o", "ma-íz"), also when a
     * silent "h" is in between ("bú-ho").
     */
    @Test fun hiatos() {
        check("río", "rí-o"); check("maíz", "ma-íz"); check("poeta", "po-e-ta")
        check("leer", "le-er"); check("aéreo", "a-é-re-o"); check("oído", "o-í-do")
        check("Raúl", "Ra-úl"); check("chiita", "chi-i-ta"); check("búho", "bú-ho")
    }

    /**
     * The letter "y": before a vowel it is a consonant ("re-yes"); at the end of a word or on
     * its own it is a vowel ("hoy", "y").
     */
    @Test fun yGriega() {
        check("reyes", "re-yes"); check("hoy", "hoy"); check("y", "y")
    }

    /**
     * Groups of consonants: some pairs stay together at the start of a syllable ("br", "fl",
     * "pl", "tr"), others are split ("c-c" in "ac-ción"). "tl" is split ("at-le-ta"), as in
     * Spain. "bs"/"ns" before another consonant stay in the previous syllable ("obs-", "ins-").
     */
    @Test fun gruposConsonanticos() {
        check("abrazo", "a-bra-zo"); check("acción", "ac-ción"); check("inflamar", "in-fla-mar")
        check("obstáculo", "obs-tá-cu-lo"); check("instrumento", "ins-tru-men-to")
        check("transplante", "trans-plan-te"); check("atleta", "at-le-ta")
        check("ahumar", "a-hu-mar")
    }

    /**
     * Stress type (*tipo acentual*) of each word, as returned by `Silabeador.analizar(word).tipo`:
     * *aguda* (stress on the last syllable: "co-ra-ZÓN", "re-LOJ", "SOL"),
     * *llana* (second-to-last: "CA-sa", "ÁR-bol", "e-XA-men"),
     * *esdrújula* (third-to-last: "mur-CIÉ-la-go"),
     * *sobresdrújula* (earlier: "DÍ-ga-me-lo").
     * Each line checks one word: `assertEquals(expected, actual)`.
     */
    @Test fun tipoAcentual() {
        assertEquals(AGUDA, Silabeador.analizar("corazón").tipo)
        assertEquals(AGUDA, Silabeador.analizar("reloj").tipo)
        assertEquals(AGUDA, Silabeador.analizar("sol").tipo)
        assertEquals(LLANA, Silabeador.analizar("casa").tipo)
        assertEquals(LLANA, Silabeador.analizar("árbol").tipo)
        assertEquals(LLANA, Silabeador.analizar("examen").tipo)
        assertEquals(ESDRUJULA, Silabeador.analizar("murciélago").tipo)
        assertEquals(SOBRESDRUJULA, Silabeador.analizar("dígamelo").tipo)
    }

    /**
     * `palabrasDe` extracts the words of a line, dropping punctuation (the final "." here).
     * `listOf(...)` creates the expected list of words.
     */
    @Test fun palabrasDeVerso() {
        assertEquals(
            listOf("Verde", "que", "te", "quiero", "verde"),
            Silabeador.palabrasDe("Verde que te quiero verde.")
        )
    }
}
