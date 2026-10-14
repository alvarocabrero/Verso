// =================================================================================================
// FILE: RhymeColorsTest.kt  (automatic tests for the rhyme colours)
// -------------------------------------------------------------------------------------------------
// WHAT IS THIS FILE FOR?
// It checks that two DIFFERENT rhymes never get colours that look alike in the editor. The
// rhyme colours (ui/theme/RhymeColors.kt) are shown as soft tints over the note's paper, so
// the test does the same: it blends each colour with the background (light and dark mode, at
// the strength used for full and for assonant rhymes) and measures how different every pair
// looks with CIEDE2000, the standard formula for colour difference ("ΔE"):
//   - around 1 is the smallest difference a person can notice side by side;
//   - around 10 or more, two small patches are clearly different colours.
//
// WHAT IS A TEST? Code that runs part of the app with a known input and compares the result
// with the right answer; if they differ, the test "fails". `@Test` marks each test function,
// and `assertTrue(message, condition)` fails (showing the message) if the condition is false.
// =================================================================================================
package com.tuapp.ui.theme

import androidx.compose.ui.graphics.Color
import com.tuapp.analisis.Rima
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class RhymeColorsTest {

    // The note backgrounds without a colour, from Theme.kt (light paper and night paper).
    private val lightPaper = Color(0xFFF6F7F9)
    private val nightPaper = Color(0xFF15171D)

    /**
     * Any two rhyme colours, as they really look in every situation, differ by at least these
     * amounts. Full rhymes are stronger tints, so they can be held to a higher minimum than the
     * softer assonant ones. (The previous palette scored about 5 and 2: its two blues were
     * easy to mix up.)
     */
    @Test fun dosRimasDistintasNuncaSeParecen() {
        for (dark in listOf(false, true)) {
            val paper = if (dark) nightPaper else lightPaper
            for ((type, minimum) in listOf(Rima.Tipo.CONSONANTE to 15.0, Rima.Tipo.ASONANTE to 9.0)) {
                val alpha = rhymeAlpha(type, dark)
                // Every pair (i, j) with i before j, so each pair is checked once.
                for (i in RhymePalette.indices) for (j in i + 1 until RhymePalette.size) {
                    val d = deltaE(blend(RhymePalette[i], alpha, paper), blend(RhymePalette[j], alpha, paper))
                    assertTrue(
                        "groups $i and $j look too alike (ΔE ${"%.1f".format(d)}, dark=$dark, $type)",
                        d >= minimum
                    )
                }
            }
        }
    }

    /**
     * Consecutive groups (A and B, B and C...) usually sit next to each other in a poem, so
     * they must be even more different.
     */
    @Test fun gruposSeguidosMuyDistintos() {
        for (dark in listOf(false, true)) {
            val paper = if (dark) nightPaper else lightPaper
            val alpha = rhymeAlpha(Rima.Tipo.CONSONANTE, dark)
            // `zipWithNext()` gives each colour with the next one: (0,1), (1,2)...
            RhymePalette.zipWithNext().forEach { (a, b) ->
                assertTrue(deltaE(blend(a, alpha, paper), blend(b, alpha, paper)) >= 25.0)
            }
        }
    }

    /**
     * After the 8 colours run out, groups reuse them but are underlined, so they still look
     * different from the group whose colour they share.
     */
    @Test fun losGruposQueRepitenColorVanSubrayados() {
        assertFalse(rhymeUnderlined(0))
        assertFalse(rhymeUnderlined(RhymePalette.size - 1))
        assertTrue(rhymeUnderlined(RhymePalette.size))
    }

    // ---------- Colour maths ----------

    /** The colour seen when [c] is painted with opacity [alpha] over [paper], as RGB 0-255. */
    private fun blend(c: Color, alpha: Float, paper: Color): DoubleArray = doubleArrayOf(
        (c.red * alpha + paper.red * (1 - alpha)) * 255.0,
        (c.green * alpha + paper.green * (1 - alpha)) * 255.0,
        (c.blue * alpha + paper.blue * (1 - alpha)) * 255.0
    )

    /** sRGB (0-255) to CIE Lab, the colour space where distances match what people see. */
    private fun lab(rgb: DoubleArray): DoubleArray {
        fun lin(v: Double): Double { val c = v / 255; return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4) }
        fun f(t: Double) = if (t > 0.008856) cbrt(t) else 7.787 * t + 16.0 / 116
        val r = lin(rgb[0]); val g = lin(rgb[1]); val b = lin(rgb[2])
        val x = (r * 0.4124 + g * 0.3576 + b * 0.1805) / 0.95047
        val y = r * 0.2126 + g * 0.7152 + b * 0.0722
        val z = (r * 0.0193 + g * 0.1192 + b * 0.9505) / 1.08883
        return doubleArrayOf(116 * f(y) - 16, 500 * (f(x) - f(y)), 200 * (f(y) - f(z)))
    }

    /** CIEDE2000 colour difference between two RGB colours (the standard formula). */
    private fun deltaE(rgb1: DoubleArray, rgb2: DoubleArray): Double {
        val (l1, a1, b1) = lab(rgb1).toList()
        val (l2, a2, b2) = lab(rgb2).toList()
        fun rad(d: Double) = Math.toRadians(d)
        val cb = (hypot(a1, b1) + hypot(a2, b2)) / 2
        val g = 0.5 * (1 - sqrt(cb.pow(7) / (cb.pow(7) + 25.0.pow(7))))
        val a1p = (1 + g) * a1; val a2p = (1 + g) * a2
        val c1p = hypot(a1p, b1); val c2p = hypot(a2p, b2)
        val h1p = (Math.toDegrees(atan2(b1, a1p)) + 360) % 360
        val h2p = (Math.toDegrees(atan2(b2, a2p)) + 360) % 360
        val dLp = l2 - l1; val dCp = c2p - c1p
        var dhp = h2p - h1p
        if (c1p * c2p == 0.0) dhp = 0.0 else if (dhp > 180) dhp -= 360 else if (dhp < -180) dhp += 360
        val dHp = 2 * sqrt(c1p * c2p) * sin(rad(dhp / 2))
        val lbp = (l1 + l2) / 2; val cbp = (c1p + c2p) / 2
        var hbp = h1p + h2p
        if (c1p * c2p != 0.0) { if (abs(h1p - h2p) > 180) hbp += if (hbp < 360) 360 else -360; hbp /= 2 }
        val t = 1 - 0.17 * cos(rad(hbp - 30)) + 0.24 * cos(rad(2 * hbp)) + 0.32 * cos(rad(3 * hbp + 6)) - 0.20 * cos(rad(4 * hbp - 63))
        val dTh = 30 * exp(-((hbp - 275) / 25).pow(2))
        val rc = 2 * sqrt(cbp.pow(7) / (cbp.pow(7) + 25.0.pow(7)))
        val sl = 1 + 0.015 * (lbp - 50).pow(2) / sqrt(20 + (lbp - 50).pow(2))
        val sc = 1 + 0.045 * cbp; val sh = 1 + 0.015 * cbp * t; val rt = -sin(rad(2 * dTh)) * rc
        return sqrt((dLp / sl).pow(2) + (dCp / sc).pow(2) + (dHp / sh).pow(2) + rt * (dCp / sc) * (dHp / sh))
    }
}
