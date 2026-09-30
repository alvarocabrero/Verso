package com.tuapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.tuapp.analisis.Rima

/**
 * Colours for rhyme groups (A, B, C…), from the Okabe–Ito palette so they stay
 * distinguishable for colour-blind readers. From the ninth group on they repeat.
 */
private val RhymePalette = listOf(
    Color(0xFFE69F00),   // orange
    Color(0xFF56B4E9),   // sky blue
    Color(0xFF009E73),   // bluish green
    Color(0xFFF0E442),   // yellow
    Color(0xFF0072B2),   // blue
    Color(0xFFD55E00),   // vermilion
    Color(0xFFCC79A7),   // reddish purple
    Color(0xFF999933)    // olive
)

fun rhymeColor(group: Int): Color = RhymePalette[group.mod(RhymePalette.size)]

/**
 * Highlighter background for a rhyme ending: full rhymes stronger, assonant
 * ones softer. A little stronger in dark mode, where the tint is less visible.
 */
@Composable
fun rhymeBackground(group: Int, type: Rima.Tipo): Color {
    val dark = isSystemInDarkTheme()
    val alpha = when (type) {
        Rima.Tipo.CONSONANTE -> if (dark) .34f else .26f
        Rima.Tipo.ASONANTE -> if (dark) .17f else .12f
    }
    return rhymeColor(group).copy(alpha = alpha)
}
