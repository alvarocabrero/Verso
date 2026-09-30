package com.tuapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Indigo ink on cool paper: a notebook, not a task board.
private val Ink = Color(0xFF2E3A6E)
private val LightInk = Color(0xFFB9C3F0)
private val Paper = Color(0xFFF6F7F9)
private val NightPaper = Color(0xFF15171D)

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE2F7),
    onPrimaryContainer = Color(0xFF17204A),
    secondaryContainer = Color(0xFFE3E6F1),
    onSecondaryContainer = Color(0xFF1E2438),
    background = Paper,
    onBackground = Color(0xFF1B1D24),
    surface = Paper,
    onSurface = Color(0xFF1B1D24),
    surfaceVariant = Color(0xFFE7E9EF),
    onSurfaceVariant = Color(0xFF4A4E5C),
    outlineVariant = Color(0xFFD3D6E0)
)

private val DarkColors = darkColorScheme(
    primary = LightInk,
    onPrimary = Color(0xFF1A2350),
    primaryContainer = Color(0xFF34406F),
    onPrimaryContainer = Color(0xFFDDE2F7),
    secondaryContainer = Color(0xFF2E3242),
    onSecondaryContainer = Color(0xFFDFE2EE),
    background = NightPaper,
    onBackground = Color(0xFFE4E5EB),
    surface = NightPaper,
    onSurface = Color(0xFFE4E5EB),
    surfaceVariant = Color(0xFF262933),
    onSurfaceVariant = Color(0xFFB8BBC8),
    outlineVariant = Color(0xFF3A3E4B)
)

/**
 * Styles for the creative text. The UI uses the system sans serif;
 * verses use a serif with generous line height, to read line by line.
 */
object VerseStyle {
    val body = TextStyle(fontFamily = FontFamily.Serif, fontSize = 18.sp, lineHeight = 30.sp)
    val title = TextStyle(
        fontFamily = FontFamily.Serif, fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold
    )
    val cardTitle = TextStyle(
        fontFamily = FontFamily.Serif, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold
    )
    val cardBody = TextStyle(fontFamily = FontFamily.Serif, fontSize = 14.sp, lineHeight = 21.sp)
    val empty = TextStyle(
        fontFamily = FontFamily.Serif, fontSize = 22.sp, lineHeight = 30.sp, fontStyle = FontStyle.Italic
    )
}

@Composable
fun VersoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
