package com.tuapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Note colours: paper and faded-ink tones, each with a night version. [name] is shown in the UI. */
data class NoteColor(val name: String, val light: Color, val dark: Color)

val NotePalette = listOf(
    NoteColor("Sin color", Color.Unspecified, Color.Unspecified),
    NoteColor("Salvia", Color(0xFFE2ECDD), Color(0xFF28362A)),
    NoteColor("Lavanda", Color(0xFFE7E2F3), Color(0xFF322D44)),
    NoteColor("Cielo", Color(0xFFDDEAF4), Color(0xFF233441)),
    NoteColor("Trigo", Color(0xFFF3ECCF), Color(0xFF3A3524)),
    NoteColor("Rosa", Color(0xFFF4DFE3), Color(0xFF42272D)),
    NoteColor("Grafito", Color(0xFFE1E3E8), Color(0xFF2E3038))
)

@Composable
fun noteBackground(index: Int): Color {
    if (index == 0) return MaterialTheme.colorScheme.surface
    val c = NotePalette.getOrElse(index) { NotePalette[0] }
    return if (isSystemInDarkTheme()) c.dark else c.light
}
