// ===============================================================================================
// FILE: DeviceInfo.kt
// -----------------------------------------------------------------------------------------------
// The `package` line below says which "logical folder" (package) this file belongs to. A package
// is like a family name for classes and functions: the full name of everything declared here is
// `com.tuapp.ui.editor.Something`. It matches the real folder
// `app/src/main/java/com/tuapp/ui/editor/`.
// ===============================================================================================
package com.tuapp.ui.editor

// `import` lines bring in pieces from other libraries or other files of the app, so they can be
// used here by their short name. For example, `import androidx.compose.material3.Text` lets us
// write `Text(...)` instead of `androidx.compose.material3.Text(...)`. Names starting with
// `androidx` come from Android / Jetpack Compose (Google's official library for drawing screens),
// names starting with `com.tuapp` come from this same app, and `java.util.Locale` comes from the
// standard Java library.
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.tuapp.analisis.Recursos
import com.tuapp.analisis.Recursos.Tipo
import com.tuapp.ui.theme.VerseStyle
import java.util.Locale

// ===============================================================================================
// WHAT THIS FILE IS FOR
// -----------------------------------------------------------------------------------------------
// When the editor analyses a poem, it finds "literary devices" (*aliteración*, *anáfora*,
// *paralelismo*, *estribillo*...). In the analysis panel at the bottom of the editor, each device
// has an (i) info button. Tapping it opens a dialog (a small floating window) that explains:
//   1. What the device is (a definition in plain words).
//   2. A classic example (Rubén Darío, Machado, Lorca, Bécquer, Lope, Miguel Hernández...).
//   3. Where it was found in the writer's own text, and the evidence.
//   4. For *aliteraciones* (repeated consonant sounds): how many times more often than normal
//      the sound appears, and why it counts as "clear" (*clara*) or only "possible" (*posible*).
//
// This file contains:
//   - `DeviceExplanation`: a "card" with definition, example and author.
//   - `EXPLANATIONS`: a dictionary that links each device type to its card.
//   - `DeviceInfoDialog`: the function that draws the dialog.
//   - `intensityText`: a helper that writes the sentence about an alliteration's intensity.
//
// HOW IT CONNECTS TO OTHER FILES
//   - `AnalysisEditor.kt` (same folder): it has `DeviceRow`, the row of each device in the
//     panel; its (i) button is what calls `DeviceInfoDialog`.
//   - `analisis/Recursos.kt` (the analysis engine, written in Spanish): it defines
//     `Recursos.Tipo` (the list of device types, each with a `nombre` = name and a
//     `descripcion` = short description), `Recursos.Recurso` (one device found in the text) and
//     the constant `UMBRAL_CLARA` (the threshold for a "clear" alliteration).
//   - `ui/theme/VerseStyle` (visual theme): the text style used to show verses.
//
// A WORD ON JETPACK COMPOSE
//   Jetpack Compose is the modern way to build screens on Android. Instead of drawing the screen
//   once and then changing it by hand, you write functions that DESCRIBE how the screen should
//   look for the current data. When the data changes, Compose calls those functions again and
//   updates only what is needed. This is called "recomposition". Think of a recipe: if the
//   ingredients change, you cook the dish again.
// ===============================================================================================

/**
 * A card with the explanation of one literary device. It is shown (in Spanish) in the info
 * dialog of the analysis panel.
 *
 * KOTLIN SYNTAX USED HERE:
 * - `private`: visibility. It means this can only be used inside THIS file. Nothing outside can
 *   see or use `DeviceExplanation`.
 * - `data class`: a "data class". A class is a mould for making objects; a `data class` is a
 *   mould made only to hold data (like an index card or a row in a table). Kotlin adds useful
 *   things for free: comparing two cards by their content, copying one while changing a field
 *   (`copy`), turning it into text, and so on.
 * - The part in brackets lists its properties (its "fields"):
 *   - `val` means "read-only value": once the card is created, it cannot change. (The opposite
 *     is `var`, which can change.)
 *   - `definition: String`: the field is called `definition` and its type is `String` (text).
 *   - `source: String? = null`: the `?` after the type means it can be "null", that is, it can
 *     have NO value. Some examples have no known author (they are made up), and then `source`
 *     is `null`. The `= null` is a default value: if no author is given when the card is
 *     created, it stays `null`.
 *
 * @property definition what the device is, in one or two sentences.
 * @property example some verses where the device can be seen (`\n` is a line break).
 * @property source the example's author, or `null` if the example is not by anyone in particular.
 */
private data class DeviceExplanation(
    val definition: String,
    val example: String,
    val source: String? = null
)

/**
 * A dictionary (in programming, a "map") that links each device type to its explanation card.
 * It works like an address book: you look up the device type and get its explanation.
 *
 * SYNTAX:
 * - `private val EXPLANATIONS = ...`: a constant that is private to this file. It is written in
 *   CAPITALS by convention, to show that it is a fixed value that never changes.
 * - `mapOf(key to value, key to value, ...)`: creates a map. The word `to` joins a key with its
 *   value, making a pair. Here the key is a `Tipo` (for example `Tipo.ALITERACION`) and the
 *   value is a `DeviceExplanation`.
 * - `DeviceExplanation("...", "...", "Rubén Darío")`: this creates a card, passing its three
 *   pieces of data in order (definition, example, author). If only two are passed, the author
 *   stays `null`.
 * - `"text " + "more text"`: `+` between texts glues them into one. It is used to split long
 *   sentences over several lines of code.
 * - `\n` inside a text means "line break": it separates the verses of an example.
 * - `Tipo` can be written like this, without `Recursos.` in front, thanks to the line
 *   `import com.tuapp.analisis.Recursos.Tipo` at the top.
 */
private val EXPLANATIONS = mapOf(
    // *Aliteración* (alliteration): a repeated consonant sound. Example by Rubén Darío.
    Tipo.ALITERACION to DeviceExplanation(
        "Repetición de un mismo sonido consonántico en palabras cercanas. Crea música " +
            "y a menudo imita lo que se describe: el susurro, el golpe, el viento.",
        "bajo el ala aleve del leve abanico",
        "Rubén Darío"
    ),
    // *Anáfora* (anaphora): the same word at the start of consecutive verses. Miguel Hernández.
    Tipo.ANAFORA to DeviceExplanation(
        "Repetición de una o varias palabras al principio de versos seguidos. Da ritmo " +
            "y va insistiendo en una idea.",
        "Temprano levantó la muerte el vuelo,\ntemprano madrugó la madrugada,\ntemprano estás rodando por el suelo.",
        "Miguel Hernández"
    ),
    // *Epífora* (epiphora): the same word at the end of consecutive verses. Made-up example.
    Tipo.EPIFORA to DeviceExplanation(
        "Repetición de una o varias palabras al final de versos seguidos. Es la anáfora " +
            "al revés: cierra cada verso con el mismo eco.",
        "te busco en la noche,\nte pienso en la noche,\nte sueño en la noche"
    ),
    // *Anadiplosis*: a verse starts with the word the previous one ended with. Antonio Machado.
    Tipo.ANADIPLOSIS to DeviceExplanation(
        "Un verso empieza con la misma palabra con la que termina el anterior, " +
            "encadenando las ideas.",
        "pero lo nuestro es pasar,\npasar haciendo caminos,\ncaminos sobre la mar.",
        "Antonio Machado"
    ),
    // *Epanadiplosis*: a verse starts and ends with the same word. Federico García Lorca.
    Tipo.EPANADIPLOSIS to DeviceExplanation(
        "Un verso empieza y termina con la misma palabra, que queda enmarcándolo.",
        "Verde que te quiero verde.",
        "Federico García Lorca"
    ),
    // *Geminación* (gemination): the same word repeated right away. Made-up example.
    Tipo.GEMINACION to DeviceExplanation(
        "Repetición seguida de una misma palabra. Intensifica una emoción o una imagen.",
        "Verde, verde, verde\nes el campo al amanecer."
    ),
    // *Polisíndeton* (polysyndeton): many conjunctions ("y", "ni", "o"). Made-up example.
    Tipo.POLISINDETON to DeviceExplanation(
        "Uso repetido de conjunciones (y, ni, o) donde no harían falta. Ralentiza el " +
            "ritmo y da sensación de acumulación.",
        "y el mar y la tierra y el cielo\ny todo lo que respira"
    ),
    // *Asíndeton* (asyndeton): a list with only commas, no conjunctions. Lope de Vega.
    Tipo.ASINDETON to DeviceExplanation(
        "Enumeración sin conjunciones, solo con comas. Acelera el ritmo y da viveza.",
        "Desmayarse, atreverse, estar furioso,\náspero, tierno, liberal, esquivo",
        "Lope de Vega"
    ),
    // *Paralelismo* (parallelism): consecutive verses with the same grammar. Bécquer.
    Tipo.PARALELISMO to DeviceExplanation(
        "Versos seguidos con la misma estructura gramatical. Crea simetría y hace " +
            "que el lector compare lo que dicen.",
        "Los suspiros son aire y van al aire.\nLas lágrimas son agua y van al mar.",
        "Gustavo Adolfo Bécquer"
    ),
    // *Rima interna* (internal rhyme): a rhyme inside a verse, or with the end of a verse of the
    // stanza. Vowel-only rhymes (*asonante*) are softer and may appear by chance.
    Tipo.RIMA_INTERNA to DeviceExplanation(
        "Rima entre una palabra del interior de un verso y otra del mismo verso o del " +
            "final de un verso de la estrofa. Añade ecos dentro de las líneas y es muy habitual " +
            "en letras de canciones y en el rap. Si solo coinciden las vocales (plata, ramas) " +
            "es asonante: es más suave y a veces aparece por casualidad.",
        "tu corazón es mi canción,\nla luna duerme en la laguna"
    ),
    // *Estribillo* (refrain): a verse or verses repeated through the text. Lorca.
    Tipo.ESTRIBILLO to DeviceExplanation(
        "Verso o grupo de versos que se repite a lo largo del poema o la canción. " +
            "Da unidad y es lo que más se recuerda.",
        "a las cinco de la tarde.",
        "Federico García Lorca"
    )
)

/**
 * Dialog that explains a detected literary device and why it was detected.
 *
 * It is opened from the (i) button of each device row in the analysis panel (see `DeviceRow` in
 * `AnalysisEditor.kt`). From top to bottom it shows: the definition, an example with its author,
 * the "En tu texto" ("In your text") section with where and what was found, and, for
 * alliterations, the intensity.
 *
 * SYNTAX AND CONCEPTS:
 * - `@Composable`: an "annotation" (a label starting with `@` that gives extra information to
 *   the compiler). It marks this function as a piece of Jetpack Compose user interface. A
 *   `@Composable` function does not return a value you use: instead it "emits" visual elements
 *   (texts, buttons...) to the screen. It can only be called from another `@Composable`
 *   function. By convention its name starts with a capital letter, like a component.
 * - `fun`: the keyword that declares a function (a named block of code you can call). There is
 *   no `private`, so it is public: other files can use it.
 * - Parameters (what it receives in brackets), each written `name: Type`:
 *   - `device: Recursos.Recurso`: the device that was found (its type, its verses, its evidence
 *     and, for alliterations, its intensity).
 *   - `where: String`: a ready-made text saying where it is, for example "versos 1–3".
 *   - `onDismiss: () -> Unit`: a function received as data (this is called a "lambda" or a
 *     "callback"). The type `() -> Unit` means "a function that takes nothing and returns
 *     nothing useful" (`Unit` means "nothing"). Whoever opens the dialog passes here what must
 *     be done to close it; the dialog calls it when the person taps "Entendido" ("Got it") or
 *     taps outside.
 * - It returns nothing: its effect is to show the dialog for as long as it keeps being called.
 *   (In Compose, a dialog is "closed" simply by no longer calling its function.)
 */
@Composable
fun DeviceInfoDialog(device: Recursos.Recurso, where: String, onDismiss: () -> Unit) {
    // Look up the card for this device's type in the EXPLANATIONS dictionary.
    // `EXPLANATIONS[key]` is how you read from a map. If there were no card for that type, the
    // result would be `null` (so `info` has the type "card or null").
    val info = EXPLANATIONS[device.tipo]
    // `MaterialTheme.colorScheme` is the colour palette of the app's current theme (it changes
    // between light and dark mode). It is kept in `colors` to type less later.
    val colors = MaterialTheme.colorScheme
    // `AlertDialog` is the standard dialog of Material Design (Google's visual style). It is
    // set up by passing named parameters (`name = value`).
    AlertDialog(
        // What to do if the person taps outside the dialog or presses "back": close it.
        onDismissRequest = onDismiss,
        // The confirm button. What goes between braces `{ ... }` is a lambda: a piece of UI that
        // the dialog draws in the right place. Here, a text button "Entendido" ("Got it") that,
        // when pressed (`onClick`), calls `onDismiss` to close the dialog.
        confirmButton = { TextButton(onClick = onDismiss) { Text("Entendido") } },
        // The dialog's title: the name of the device type ("Aliteración", "Anáfora"...).
        title = { Text(device.tipo.nombre) },
        // The body of the dialog.
        text = {
            // `Column` places its children one below the other.
            // `Modifier` is Compose's way to "decorate" or adjust an element: size, spacing,
            // background, whether it scrolls... Modifiers are chained with dots.
            // `verticalScroll(rememberScrollState())` lets the column scroll up and down if the
            // text does not fit. `rememberScrollState()` creates the scroll position and keeps
            // ("remembers") it between recompositions.
            Column(Modifier.verticalScroll(rememberScrollState())) {
                // First text: the definition.
                // `info?.definition` uses the `?.` operator ("safe call"): if `info` is `null`,
                // the result is `null` instead of a crash; otherwise it is the definition.
                // `?:` is the "Elvis" operator: "if the left side is null, use the right side".
                // So, if there is no card, the short description that comes with the analysis
                // engine itself (`device.tipo.descripcion`) is shown instead.
                Text(info?.definition ?: device.tipo.descripcion, style = MaterialTheme.typography.bodyMedium)

                // Only if there is a card (`info != null` means "info is not null") is the
                // example block shown. Inside this `if`, Kotlin already knows `info` is not null
                // and lets us write `info.example` directly (this is called a "smart cast").
                if (info != null) {
                    // `Spacer` is an empty gap; here 16 dp tall, to separate blocks.
                    // `dp` ("density-independent pixels") is Android's unit of size: it looks
                    // the same size on screens with more or fewer pixels.
                    Spacer(Modifier.height(16.dp))
                    // Label "Ejemplo" ("Example") in the theme's main colour.
                    Text("Ejemplo", style = MaterialTheme.typography.labelLarge, color = colors.primary)
                    Spacer(Modifier.height(4.dp))
                    // The example, in the verse style used on note cards, but in italics.
                    // `.copy(fontStyle = FontStyle.Italic)` makes a copy of the style changing
                    // only the font style to italic (every other setting stays the same).
                    Text(info.example, style = VerseStyle.cardBody.copy(fontStyle = FontStyle.Italic))
                    // If the example has an author, show it below, after a dash.
                    // `"— ${info.source}"` is a "string template": whatever is inside `${...}`
                    // is replaced by its value. If the author is "Lorca", the text is "— Lorca".
                    if (info.source != null) Text(
                        "— ${info.source}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }

                // "En tu texto" ("In your text") block: where the device was found in the poem.
                Spacer(Modifier.height(16.dp))
                Text("En tu texto", style = MaterialTheme.typography.labelLarge, color = colors.primary)
                Spacer(Modifier.height(4.dp))
                // Build a sentence like "Versos 1–3: temprano, temprano, temprano":
                // - `listOf(a, b)` makes a list with two texts: the place and the evidence.
                // - `where.replaceFirstChar { it.uppercase() }` capitalises the first letter of
                //   `where`. The braces `{ ... }` are a lambda, and `it` is the automatic name
                //   Kotlin gives to the single value the lambda receives (here, the first letter).
                // - `.filter { it.isNotEmpty() }` keeps only the texts that are not empty (in
                //   case there is no place or no evidence).
                // - `.joinToString(": ")` joins what is left, with ": " between them.
                Text(
                    listOf(where.replaceFirstChar { it.uppercase() }, device.evidencia)
                        .filter { it.isNotEmpty() }.joinToString(": "),
                    style = MaterialTheme.typography.bodyMedium
                )
                // `device.intensidad` (intensity) only has a value for alliterations (for other
                // devices it is `null`). `?.let { intensity -> ... }` means: "if it is not null,
                // run this block, calling its value `intensity`; if it is null, do nothing". It is
                // a short way to say "if it exists, use it".
                device.intensidad?.let { intensity ->
                    Spacer(Modifier.height(8.dp))
                    // Sentence explaining the intensity and whether the alliteration is clear.
                    Text(
                        intensityText(intensity, device.clara),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    )
}

/**
 * Writes the sentence (in Spanish) that explains an alliteration's intensity.
 *
 * The "intensity" is computed by the analysis engine: it is how many times more often than
 * normal in Spanish a sound appears in those verses. If it is 5.2, the sound appears 5.2 times
 * more than usual. From `Recursos.UMBRAL_CLARA` (4.5) upwards the alliteration counts as
 * "clear" (*clara*); below that, only "possible" (*posible*).
 *
 * SYNTAX:
 * - `private fun intensityText(...): String`: a function private to this file; the `: String`
 *   after the brackets says it RETURNS a text.
 * - `intensity: Double`: `Double` is a number with decimals.
 * - `clear: Boolean`: `Boolean` is a yes/no value (`true` or `false`).
 * - `return`: hands the result back to whoever called the function.
 *
 * @param intensity how many times more often than normal the sound appears.
 * @param clear `true` if the alliteration is clear, `false` if it is only possible.
 * @return the full sentence, in Spanish, ready to show.
 */
private fun intensityText(intensity: Double, clear: Boolean): String {
    // `String.format(locale, "%.1f", number)` turns a number into text with ONE decimal.
    // `Locale("es", "ES")` asks for the format used in Spain, with a decimal comma: 5,2 and not
    // 5.2. `"%.1f"` is the pattern: "a number with decimals (f), showing 1 decimal".
    val times = String.format(Locale("es", "ES"), "%.1f", intensity)
    // The same for the threshold from which an alliteration is clear (4,5).
    val threshold = String.format(Locale("es", "ES"), "%.1f", Recursos.UMBRAL_CLARA)
    // Return the first sentence (with `$times` replaced by the number; `$name` without braces is
    // the short form of a string template) glued with `+` to a second sentence that depends on
    // whether it is clear or not. In Kotlin, `if (...) a else b` is an expression that "is
    // worth" `a` or `b`, so it can be used directly as a value.
    return "Este sonido aparece $times veces más de lo habitual en español. " +
        if (clear) "Por eso se considera una aliteración clara (a partir de $threshold)."
        else "Es una aliteración posible: se considera clara a partir de $threshold."
}
