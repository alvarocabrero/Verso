package com.tuapp.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Ajustes de la app, guardados en SharedPreferences. Son estado de Compose
 * para que la interfaz y el análisis reaccionen al cambiarlos.
 */
class Preferencias(context: Context) {

    private val sp = context.getSharedPreferences("preferencias", Context.MODE_PRIVATE)

    /** Seseo en las rimas (s = z = c ante e/i), para acento latinoamericano o andaluz. */
    var seseo by mutableStateOf(sp.getBoolean(SESEO, false)); private set

    /** Sílabas, rimas y recursos en el editor. */
    var mostrarAnalisis by mutableStateOf(sp.getBoolean(MOSTRAR_ANALISIS, true)); private set

    fun cambiarSeseo(v: Boolean) { seseo = v; sp.edit().putBoolean(SESEO, v).apply() }

    fun cambiarMostrarAnalisis(v: Boolean) {
        mostrarAnalisis = v
        sp.edit().putBoolean(MOSTRAR_ANALISIS, v).apply()
    }

    private companion object {
        const val SESEO = "seseo"
        const val MOSTRAR_ANALISIS = "mostrar_analisis"
    }
}
