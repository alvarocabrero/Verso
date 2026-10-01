// =================================================================================================
// FILE: MainActivity.kt
// -------------------------------------------------------------------------------------------------
// WHAT IS THIS FILE FOR?
// In Android, an "Activity" is a window of the app that the system opens. Verso has only one
// activity, `MainActivity`, which opens when you tap the app icon. All the screens are drawn
// inside it with Jetpack Compose (the modern way to build Android screens: you describe the
// screen with Kotlin functions).
//
// This file does two things:
//   1. `MainActivity`: prepares the window and tells it "draw the Verso theme and, inside it,
//      the navigation".
//   2. `AppNavigation`: defines the app's "routes" (screens) and how to go from one to another:
//        - "home": the start screen (`HomeScreen`, in ui/home/), with the Notes and Audios
//          sections.
//        - "editor/{id}": the editor of one note (`EditorScreen`, in ui/editor/). The `{id}`
//          is the number of the note to open (0 means "new note").
//
// The look (colours, fonts) comes from `VersoTheme`, in ui/theme/.
// AndroidManifest.xml is the file that declares this activity as the one opened at launch.
// =================================================================================================

// The package ("logical folder") this file belongs to.
package com.tuapp

// `Bundle`: a "parcel" of data that Android uses to save and restore a window's state
// (for example, when you rotate the phone).
import android.os.Bundle
// `ComponentActivity`: Android's base class for an activity that works with Compose.
import androidx.activity.ComponentActivity
// `setContent`: puts Compose content inside the activity.
import androidx.activity.compose.setContent
// `enableEdgeToEdge`: makes the app draw from edge to edge (also under the system bars: the
// clock bar at the top and the navigation bar at the bottom).
import androidx.activity.enableEdgeToEdge
// `Composable`: the annotation that marks functions that draw UI in Compose.
import androidx.compose.runtime.Composable
// Pieces of the Compose navigation library (explained in `AppNavigation`).
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
// The app's own screens and theme.
import com.tuapp.ui.editor.EditorScreen
import com.tuapp.ui.home.HomeScreen
import com.tuapp.ui.theme.VersoTheme

/**
 * The app's only window (activity).
 *
 * - `class MainActivity : ComponentActivity()` means MainActivity inherits from
 *   ComponentActivity: it is an Android activity and gets all its basic behaviour.
 * - Android creates this object by itself when the app opens; nobody creates it by hand.
 */
class MainActivity : ComponentActivity() {
    /**
     * `onCreate` is a function that Android calls automatically when it creates the window.
     * It is the place to prepare everything that will be shown.
     *
     * - `fun` declares a function (a named block of code that can be run).
     * - `override` says this function "replaces" one that already exists in the parent class
     *   (ComponentActivity), to add our own behaviour.
     * - `savedInstanceState: Bundle?` is the parameter it receives: its name, a colon and its
     *   type. The `?` after the type means it can be `null` (that is, "nothing there"). It is
     *   null the first time the window opens, and has data when the window is being re-created
     *   (for example, after rotating the screen).
     * - It returns nothing (Kotlin calls that `Unit`, and you don't need to write it).
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        // `super.onCreate(...)` first runs the original version from the parent class. Android
        // requires this: it gets the activity ready before we add our own things.
        super.onCreate(savedInstanceState)
        // Turn on "edge to edge" mode so the content uses the whole screen.
        enableEdgeToEdge()
        // `setContent { ... }` says "from now on, this window shows what this Compose block
        // describes". The code between the braces is a lambda: a piece of code handed to the
        // function so it can run it when needed.
        setContent {
            // Apply Verso's visual theme and, inside it, show the app's navigation.
            // Everything drawn inside `VersoTheme { ... }` uses its colours and fonts.
            VersoTheme { AppNavigation() }
        }
    }
}

/**
 * Defines the app's screens and how to move between them.
 *
 * - `@Composable` is an "annotation" (a label that starts with @) that marks this function as
 *   part of the Compose UI. A composable function does not return a value: it describes what
 *   to draw. Compose runs it again automatically when the data it uses changes; this is called
 *   "recomposition", and it is what keeps the screen up to date.
 * - `private` means it can only be used inside this file.
 * - By convention, composable function names start with a capital letter.
 */
@Composable
private fun AppNavigation() {
    // `rememberNavController()` creates (and remembers between recompositions) the "navigation
    // controller": the object that knows which screen we are on and lets us go to another one
    // or go back. "remember" in Compose means the value is kept when the function runs again,
    // instead of being created from scratch every time.
    val nav = rememberNavController()
    // `NavHost` is the "container" that shows the current screen for the current route. It
    // gets the controller and the start route ("home"). Arguments written as `name = value`
    // are "named arguments": they make clear what each value is.
    // Inside its braces we register the possible routes.
    NavHost(navController = nav, startDestination = "home") {
        // Route "home": the start screen.
        composable("home") {
            // Show `HomeScreen` and tell it what to do when the user opens a note:
            // `openNote = { id -> ... }` is a lambda that receives one parameter `id` (the part
            // before the arrow `->`) and runs the part after the arrow.
            // `nav.navigate("editor/$id")` goes to the editor route. The `$id` inside the text
            // is a "string template": it is replaced by the value of `id`. For example, if id
            // is 5, the route becomes "editor/5".
            HomeScreen(openNote = { id -> nav.navigate("editor/$id") })
        }
        // The editor route. `{id}` in the route is a slot filled with the note number.
        composable(
            route = "editor/{id}",
            // Declare that the "id" argument is a `Long` (a whole number, no decimals, that
            // can be very large). `listOf(...)` creates a list.
            // `navArgument("id") { type = ... }` configures that argument inside its lambda.
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) {
            // Show the editor. `onBack` is what happens when "back" is pressed in it:
            // `nav.navigateUp()` returns to the previous screen (the home screen).
            // The editor reads the `id` from the route by itself, through its ViewModel.
            EditorScreen(onBack = { nav.navigateUp() })
        }
    }
}
