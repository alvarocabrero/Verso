package com.tuapp.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * App settings, stored in SharedPreferences. They are Compose state so that
 * the UI and the analysis react when they change.
 */
class Preferences(context: Context) {

    private val sp = context.getSharedPreferences("preferencias", Context.MODE_PRIVATE)

    /** Seseo in rhymes (s = z = c before e/i), for Latin American or Andalusian accents. */
    var seseo by mutableStateOf(sp.getBoolean(SESEO, false)); private set

    /** Syllables, rhymes and literary devices in the editor. */
    var showAnalysis by mutableStateOf(sp.getBoolean(SHOW_ANALYSIS, true)); private set

    /** Colour each rhyme group in the text (highlighter style). */
    var colorRhymes by mutableStateOf(sp.getBoolean(COLOR_RHYMES, false)); private set

    fun updateSeseo(value: Boolean) { seseo = value; sp.edit().putBoolean(SESEO, value).apply() }

    fun updateShowAnalysis(value: Boolean) {
        showAnalysis = value
        sp.edit().putBoolean(SHOW_ANALYSIS, value).apply()
    }

    fun updateColorRhymes(value: Boolean) {
        colorRhymes = value
        sp.edit().putBoolean(COLOR_RHYMES, value).apply()
    }

    // Keys kept from version 0.1.0 so existing settings survive
    private companion object {
        const val SESEO = "seseo"
        const val SHOW_ANALYSIS = "mostrar_analisis"
        const val COLOR_RHYMES = "color_rhymes"
    }
}
