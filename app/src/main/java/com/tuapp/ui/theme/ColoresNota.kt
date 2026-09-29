package com.tuapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Colores de nota: tonos de papel y tinta desvaída, con su versión nocturna. */
data class ColorNota(val nombre: String, val claro: Color, val oscuro: Color)

val PaletaNotas = listOf(
    ColorNota("Sin color", Color.Unspecified, Color.Unspecified),
    ColorNota("Salvia", Color(0xFFE2ECDD), Color(0xFF28362A)),
    ColorNota("Lavanda", Color(0xFFE7E2F3), Color(0xFF322D44)),
    ColorNota("Cielo", Color(0xFFDDEAF4), Color(0xFF233441)),
    ColorNota("Trigo", Color(0xFFF3ECCF), Color(0xFF3A3524)),
    ColorNota("Rosa", Color(0xFFF4DFE3), Color(0xFF42272D)),
    ColorNota("Grafito", Color(0xFFE1E3E8), Color(0xFF2E3038))
)

@Composable
fun fondoNota(indice: Int): Color {
    if (indice == 0) return MaterialTheme.colorScheme.surface
    val c = PaletaNotas.getOrElse(indice) { PaletaNotas[0] }
    return if (isSystemInDarkTheme()) c.oscuro else c.claro
}
