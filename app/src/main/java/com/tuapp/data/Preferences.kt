// =================================================================================================
// FILE: Preferences.kt
// -------------------------------------------------------------------------------------------------
// WHAT IS THIS FILE FOR?
// It keeps the app's settings (the options the user can switch on or off):
//   - seseo in rhymes, show the analysis, colour the rhymes, and which home tab is open.
//
// The settings are saved in "SharedPreferences": a small key-value store that Android keeps
// on the phone for each app (like a tiny notebook of "name -> value"). So they survive when
// the app is closed.
//
// They are also "Compose state": when a setting changes, every screen that reads it is
// redrawn by itself (and the analysis runs again with the new option).
//
// Created once in VersoApp.kt (`preferences`) and used by the settings menu, the home screen
// and the editor.
// =================================================================================================

package com.tuapp.data

// `Context`: Android's "context", needed to open the SharedPreferences.
import android.content.Context
// Compose state tools. `mutableStateOf` creates a value that Compose watches; `getValue` and
// `setValue` let us use it with `by` (explained below).
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * App settings, stored in SharedPreferences. They are Compose state, so the UI and the
 * analysis react when they change.
 *
 * `class Preferences(context: Context)`: to create it you give it a Context. Here `context`
 * has no `val`, so it is only a constructor parameter (used once below), not a property.
 */
class Preferences(context: Context) {

    // Open (or create) the app's settings store called "preferencias".
    // `Context.MODE_PRIVATE` = only this app can read it. `sp` is short for SharedPreferences.
    private val sp = context.getSharedPreferences("preferencias", Context.MODE_PRIVATE)

    // ---------------------------------------------------------------------------------------------
    // HOW THE SETTINGS BELOW WORK (same pattern for all four):
    //   var seseo by mutableStateOf(sp.getBoolean(SESEO, false)); private set
    // - `var` = a property that can change.
    // - `mutableStateOf(start)` = a Compose "state" box holding a value. When the value
    //   changes, Compose redraws ("recomposes") every screen that read it.
    // - `by` = "delegation": reading or writing `seseo` really reads or writes the value inside
    //   that box, so we can use `seseo` like a normal variable.
    // - `sp.getBoolean(SESEO, false)` = the start value: what is saved under the key SESEO, or
    //   `false` if nothing was ever saved.
    // - `; private set` = anyone can READ it, but only this class can CHANGE it. Other code must
    //   use the `updateX` functions, which also save the new value.
    // ---------------------------------------------------------------------------------------------

    /**
     * Seseo in rhymes (s = z = c before e/i), for Latin American or Andalusian accents.
     * When true, words like "casa" and "caza" count as a full rhyme. `Boolean` = true/false.
     */
    var seseo by mutableStateOf(sp.getBoolean(SESEO, false)); private set

    /** Show syllables, rhymes and literary devices in the editor. On (`true`) by default. */
    var showAnalysis by mutableStateOf(sp.getBoolean(SHOW_ANALYSIS, true)); private set

    /** Colour each rhyme group in the text (highlighter style). Off by default. */
    var colorRhymes by mutableStateOf(sp.getBoolean(COLOR_RHYMES, false)); private set

    /** Home screen section: 0 = notes, 1 = audios. `getInt` reads a whole number. */
    var homeTab by mutableStateOf(sp.getInt(HOME_TAB, 0)); private set

    // ---------------------------------------------------------------------------------------------
    // The `updateX` functions change a setting in two steps:
    //   1. Change the Compose state (the screens update at once).
    //   2. Save it on the phone: `sp.edit()` opens an editor, `putInt`/`putBoolean` writes the
    //      value under its key, and `apply()` saves it in the background.
    // Two steps on one line are separated by `;`.
    // (They are called `updateX` and not `setX` because `setX` would clash with the hidden
    // setter function that Kotlin creates for each `var`.)
    // ---------------------------------------------------------------------------------------------

    /** Changes and saves the open home tab (0 = notes, 1 = audios). */
    fun updateHomeTab(value: Int) { homeTab = value; sp.edit().putInt(HOME_TAB, value).apply() }

    /** Changes and saves the seseo option. */
    fun updateSeseo(value: Boolean) { seseo = value; sp.edit().putBoolean(SESEO, value).apply() }

    /** Changes and saves the "show analysis" option. */
    fun updateShowAnalysis(value: Boolean) {
        showAnalysis = value
        sp.edit().putBoolean(SHOW_ANALYSIS, value).apply()
    }

    /** Changes and saves the "colour rhymes" option. */
    fun updateColorRhymes(value: Boolean) {
        colorRhymes = value
        sp.edit().putBoolean(COLOR_RHYMES, value).apply()
    }

    // The keys (names) under which each setting is saved. They are kept from version 0.1.0
    // ("seseo", "mostrar_analisis" are Spanish) so existing settings survive an update:
    // never rename them.
    // - `companion object` = things that belong to the class itself, not to each object
    //   (like shared constants). Here it is `private`: only this class can see it.
    // - `const val` = a constant fixed when the app is built; it can never change.
    private companion object {
        const val SESEO = "seseo"
        const val SHOW_ANALYSIS = "mostrar_analisis"
        const val COLOR_RHYMES = "color_rhymes"
        const val HOME_TAB = "home_tab"
    }
}
