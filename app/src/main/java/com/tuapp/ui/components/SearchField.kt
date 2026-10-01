// ============================================================================================
// FILE: SearchField.kt
// --------------------------------------------------------------------------------------------
// What is it for?
//   It draws the rounded search bar shown at the top of both home sections (Notes and
//   Audios): a magnifying-glass icon, the text you type, a grey hint when empty, and an "X"
//   button to clear the search.
//
// What is it connected to?
//   - ui/notes/NotesScreen.kt and ui/audios/AudiosScreen.kt use it.
//   - It does not search anything itself: it only shows the text and reports every change
//     through `onChange`. The ViewModels (NotesViewModel, AudiosViewModel) do the searching.
//
// JETPACK COMPOSE IN A NUTSHELL:
//   - `@Composable` marks a function that describes a piece of screen.
//   - This component is "stateless": it does not keep the text itself. It receives the
//     current text (`text`) and a function to call when it changes (`onChange`). The caller
//     stores the text and passes the new value back, which causes a "recomposition" (the
//     function is called again and the screen updates). This pattern is called "state
//     hoisting" (lifting the state up to the caller).
//   - `Modifier` sets size, padding, etc.
// ============================================================================================
package com.tuapp.ui.components

// Layout modifiers: full width and padding.
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
// A shape with rounded corners.
import androidx.compose.foundation.shape.RoundedCornerShape
// Icons: "X" (close) and magnifying glass (search).
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
// Material 3 components: icon, icon button, theme, text, text field and its default colours.
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
// `Color`: a colour; `Color.Transparent` is "no colour at all".
import androidx.compose.ui.graphics.Color
// `dp`: size unit that looks the same on every screen.
import androidx.compose.ui.unit.dp

/**
 * Rounded search bar used at the top of both home sections.
 *
 * @param text the text currently in the bar.
 * @param onChange called with the new text every time the user types or clears it.
 *   Its type `(String) -> Unit` means "a function that receives a text and returns nothing".
 * @param placeholder the grey hint shown while the bar is empty (e.g. "Buscar en tus notas",
 *   "Search your notes").
 */
@Composable
fun SearchField(text: String, onChange: (String) -> Unit, placeholder: String) {
    // Material 3's filled text field.
    TextField(
        // What it shows, and what to call when the user types.
        value = text,
        onValueChange = onChange,
        // The hint while empty.
        placeholder = { Text(placeholder) },
        // Magnifying glass on the left (decoration only, so no description for screen readers).
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        // On the right: an "X" button, only when there is some text. Tapping it sends an
        // empty text (""), which clears the search. "Borrar búsqueda" = "Clear search".
        trailingIcon = {
            if (text.isNotEmpty()) IconButton(onClick = { onChange("") }) {
                Icon(Icons.Filled.Close, contentDescription = "Borrar búsqueda")
            }
        },
        // A single line (pressing Enter does not add a new line).
        singleLine = true,
        // Fully rounded ends (pill shape).
        shape = RoundedCornerShape(28.dp),
        // Colours: the same soft background whether it has the cursor (focused) or not, and no
        // underline (the "indicator" line is made transparent).
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        // Full width, with 12 dp of space on the sides and 8 dp above and below.
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
    )
}
