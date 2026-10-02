// ============================================================================================
// FILE: HomeScreen.kt
// --------------------------------------------------------------------------------------------
// What is it for?
//   It is the app's main screen (the one you see when you open it). At the bottom there is a
//   navigation bar with two tabs, "Notas" (Notes) and "Audios"; above it, the chosen section
//   is shown: NotesScreen or AudiosScreen. The app remembers the last tab you used, even after
//   closing it.
//
// What is it connected to?
//   - MainActivity.kt: its navigation shows HomeScreen on the "home" route and gives it the
//     `openNote` function (which navigates to the editor).
//   - ui/notes/NotesScreen.kt and ui/audios/AudiosScreen.kt: the two sections.
//   - data/Preferences (through VersoApp): stores which tab is selected.
//
// JETPACK COMPOSE IN A NUTSHELL:
//   - A function marked `@Composable` describes a piece of screen. Calling one inside another
//     places it there (like nesting boxes).
//   - When data it reads changes (Compose "state"), Compose calls the function again
//     ("recomposition") and updates the screen by itself; you never change the screen by
//     hand.
//   - `Modifier` is a chain of settings for size, padding, clicks...
//   - `Scaffold` is Material 3's page skeleton with slots for a top bar, bottom bar,
//     floating button and the main content; it places each part where it belongs.
// ============================================================================================
package com.tuapp.ui.home

// Layout: `Box` (a container where children are stacked on top of each other), padding and
// "window insets" (the space taken by system bars, explained below).
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
// Icons: notes (filled and outlined versions) and headphones (filled and outlined).
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.outlined.Headphones
// Material 3: icon, bottom navigation bar and its items, page skeleton, text.
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
// Gives access to the Android `Context` from inside a composable.
import androidx.compose.ui.platform.LocalContext
// Controls which element has the keyboard focus.
import androidx.compose.ui.platform.LocalFocusManager
// The screen's lifecycle (resumed, stopped...) and a way to observe its events.
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
// Our own classes.
import com.tuapp.VersoApp
import com.tuapp.ui.audios.AudiosScreen
import com.tuapp.ui.notes.NotesScreen

/**
 * Home: two sections, notes and audios, switched from the bottom bar. The last one is
 * remembered.
 *
 * @param openNote function that opens a note in the editor given its id (0 = new note). This
 *   screen does not use it itself; it passes it on to the two sections.
 */
@Composable
fun HomeScreen(openNote: (Long) -> Unit) {
    // Get the app's preferences (small saved settings). Steps: `LocalContext.current` is the
    // Android Context; `.applicationContext` is the whole app's context; `as VersoApp` treats
    // it as our own Application class, which holds `preferences`.
    val preferences = (LocalContext.current.applicationContext as VersoApp).preferences
    // The selected tab: 0 = Notes, 1 = Audios. `homeTab` is Compose state inside
    // Preferences, so when it changes this screen recomposes and shows the other section.
    val tab = preferences.homeTab

    // When this screen comes back to the front (after closing the editor, or when the app
    // returns from the background), Android would hand the keyboard focus to the first text
    // field it finds: the search bar. The keyboard would then open by itself and cover the
    // "+" button. So every time the screen is "resumed" we take the focus away again.
    //   - `LocalFocusManager.current` controls which element has the focus.
    //   - `LocalLifecycleOwner.current.lifecycle` is this screen's lifecycle (created, started,
    //     resumed, stopped...).
    //   - `DisposableEffect` runs its block when the screen appears and its `onDispose` block
    //     when it goes away; here it registers an observer and removes it at the end.
    val focusManager = LocalFocusManager.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) focusManager.clearFocus(force = true)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    Scaffold(
        // The bottom bar with the two tabs.
        bottomBar = {
            NavigationBar {
                // "Notas" tab. It is highlighted when `tab == 0`; tapping it saves tab 0.
                // The icon is filled when selected and outlined when not. The `null` is the
                // screen-reader description, not needed because the label already says it.
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { preferences.updateHomeTab(0) },
                    icon = { Icon(if (tab == 0) Icons.AutoMirrored.Filled.Notes else Icons.AutoMirrored.Outlined.Notes, null) },
                    label = { Text("Notas") }
                )
                // "Audios" tab, the same idea with the headphones icon and tab 1.
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { preferences.updateHomeTab(1) },
                    icon = { Icon(if (tab == 1) Icons.Filled.Headphones else Icons.Outlined.Headphones, null) },
                    label = { Text("Audios") }
                )
            }
        }
    ) { padding ->
        // The main content. `padding` is the space the Scaffold keeps for the bottom bar.
        // `consumeWindowInsets(padding)` tells the inner screens "this space is already taken
        // care of", so they do not add it a second time (each section has its own Scaffold).
        // `focusable()` makes this container able to hold the focus. When the editor closes,
        // Compose gives the focus to the first focusable element of the screen; without this
        // it would be the search bar, and the keyboard would open by itself.
        Box(Modifier.padding(padding).consumeWindowInsets(padding).focusable()) {
            // Show the section for the selected tab.
            if (tab == 1) AudiosScreen(openNote = openNote) else NotesScreen(openNote = openNote)
        }
    }
}
