// =================================================================================================
// FILE: VersoApp.kt
// -------------------------------------------------------------------------------------------------
// WHAT IS THIS FILE FOR?
// This is the "application" itself: the object that Android creates ONE time when the app
// starts, before any screen. It lives as long as the app is in memory (even when you move
// from one screen to another).
//
// Here we build and keep the "shared" pieces that the whole app uses:
//   - the database (`VersoDatabase`, in data/VersoDatabase.kt),
//   - the notes repository (`NotesRepository`, in data/NotesRepository.kt),
//   - the settings (`Preferences`, in data/Preferences.kt),
//   - the audio repository (`AudioRepository`, in data/AudioRepository.kt),
//   - the single audio player (`AudioPlayer`, in audio/AudioPlayer.kt),
//   - and a "coroutine scope" (`appScope`) for jobs that must finish even if a screen is
//     closed (for example, saving the note when you leave the editor).
//
// This is sometimes called a "dependency container": one central place where the screens go
// to get what they need, instead of each screen creating its own copy. Some libraries do this
// automatically (for example Hilt), but here it is done "by hand" to keep the project small
// and simple.
//
// Android knows it must use THIS class as the application because the file
// AndroidManifest.xml names it (attribute `android:name`). The screens (their ViewModels)
// get it later through Android's `Context`.
// =================================================================================================

// `package` says which "logical folder" (package) this file belongs to. It keeps code
// organised and avoids name clashes: the full name of the class below is `com.tuapp.VersoApp`.
package com.tuapp

// Each `import` brings a class from another package, so we can use it here by its short name.
// `Application` is Android's base class for "the app".
import android.app.Application
// The app's own classes, defined in other files.
import com.tuapp.audio.AudioPlayer
import com.tuapp.data.AudioRepository
import com.tuapp.data.NotesRepository
import com.tuapp.data.Preferences
import com.tuapp.data.VersoDatabase
// Pieces of Kotlin's coroutine library (explained below, at `appScope`).
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * The app's entry point. It is also a simple dependency container
 * (no Hilt, to keep the project light while it grows).
 *
 * KOTLIN SYNTAX:
 * - `class VersoApp` declares a class: a "mould" that describes some data and behaviour.
 * - `: Application()` means "inherits from Application": VersoApp IS an Android Application
 *   and gets everything that Application already knows how to do. The brackets `()` call the
 *   parent class's constructor (they build it with no arguments).
 * - The braces `{ ... }` hold the body of the class: its properties and functions.
 *
 * Nobody in the code writes `VersoApp()`: Android itself creates this object when the app
 * starts.
 */
class VersoApp : Application() {

    /**
     * A coroutine scope that outlives the screens: used to save the note when leaving the
     * editor.
     *
     * WHAT IS A COROUTINE? A task that can run "in the background" and pause without freezing
     * the app (for example, writing to the database without blocking the screen).
     * A `CoroutineScope` is like a "box" that groups coroutines: if the box is cancelled, all
     * the tasks inside it are cancelled too. Each screen has its own box, which is cancelled
     * when the screen closes. This box, instead, lives as long as the app, so a task started
     * here will finish even if the screen that started it no longer exists.
     *
     * - `val` declares a read-only property: it is set once and cannot be set again
     *   (the opposite is `var`, which can be changed later).
     * - `SupervisorJob()`: if one task fails, the other tasks in the box are NOT cancelled.
     * - `Dispatchers.Default`: says on which "threads" (workers) the tasks run. Default is a
     *   group of threads meant for computing work, away from the main thread that draws the
     *   screen.
     * - The `+` joins both pieces into one configuration for the box.
     */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ---------------------------------------------------------------------------------------------
    // `by lazy { ... }` means "create it lazily": the code inside the braces does NOT run when
    // the app starts, but the first time someone uses the property. The result is then kept,
    // and the same object is returned every time after that. So the app starts faster and each
    // piece exists only once.
    //
    // The braces `{ ... }` are a "lambda": a piece of code passed around like a value, to be
    // run later. Its last value is what it "returns".
    //
    // `this` means the VersoApp object itself. An Application is also a `Context` (Android's
    // "context": the door to files, settings and system services), so we can pass `this` to
    // anything that needs a Context.
    // ---------------------------------------------------------------------------------------------

    /** The app's SQLite database (notes, audios and their links), built with Room. */
    val database by lazy { VersoDatabase.create(this) }

    /**
     * The notes repository: the door the screens use to read and save notes.
     * It receives the notes DAO (`database.noteDao()`), which is the part that talks to the
     * database. The dot `.` is used to reach something that belongs to an object.
     */
    val repository by lazy { NotesRepository(database.noteDao()) }

    /** App settings (seseo, show analysis, colour rhymes, home tab). */
    val preferences by lazy { Preferences(this) }

    /**
     * The audio repository. It needs the audio DAO (for the database) and the Context
     * (`this`) so it can store the sound files in the app's private storage.
     */
    val audioRepository by lazy { AudioRepository(database.audioDao(), this) }

    /** One player for the whole app, so only one audio sounds at a time. */
    val audioPlayer by lazy { AudioPlayer() }
}
