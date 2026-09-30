package com.tuapp

import android.app.Application
import com.tuapp.audio.AudioPlayer
import com.tuapp.data.AudioRepository
import com.tuapp.data.NotesRepository
import com.tuapp.data.Preferences
import com.tuapp.data.VersoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * App entry point. Also acts as a simple dependency container
 * (no Hilt, to keep the project light while it grows).
 */
class VersoApp : Application() {

    /** Scope that outlives screens: used to save when leaving the editor. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { VersoDatabase.create(this) }
    val repository by lazy { NotesRepository(database.noteDao()) }
    val preferences by lazy { Preferences(this) }
    val audioRepository by lazy { AudioRepository(database.audioDao(), this) }

    /** One player for the whole app, so only one audio sounds at a time. */
    val audioPlayer by lazy { AudioPlayer() }
}
