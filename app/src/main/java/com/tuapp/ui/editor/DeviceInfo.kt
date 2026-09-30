package com.tuapp.ui.editor

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

/**
 * What each literary device is, with a classic example. Shown in Spanish in the
 * info dialog of the analysis panel.
 */
private data class DeviceExplanation(
    val definition: String,
    val example: String,
    val source: String? = null
)

private val EXPLANATIONS = mapOf(
    Tipo.ALITERACION to DeviceExplanation(
        "Repetición de un mismo sonido consonántico en palabras cercanas. Crea música " +
            "y a menudo imita lo que se describe: el susurro, el golpe, el viento.",
        "bajo el ala aleve del leve abanico",
        "Rubén Darío"
    ),
    Tipo.ANAFORA to DeviceExplanation(
        "Repetición de una o varias palabras al principio de versos seguidos. Da ritmo " +
            "y va insistiendo en una idea.",
        "Temprano levantó la muerte el vuelo,\ntemprano madrugó la madrugada,\ntemprano estás rodando por el suelo.",
        "Miguel Hernández"
    ),
    Tipo.EPIFORA to DeviceExplanation(
        "Repetición de una o varias palabras al final de versos seguidos. Es la anáfora " +
            "al revés: cierra cada verso con el mismo eco.",
        "te busco en la noche,\nte pienso en la noche,\nte sueño en la noche"
    ),
    Tipo.ANADIPLOSIS to DeviceExplanation(
        "Un verso empieza con la misma palabra con la que termina el anterior, " +
            "encadenando las ideas.",
        "pero lo nuestro es pasar,\npasar haciendo caminos,\ncaminos sobre la mar.",
        "Antonio Machado"
    ),
    Tipo.EPANADIPLOSIS to DeviceExplanation(
        "Un verso empieza y termina con la misma palabra, que queda enmarcándolo.",
        "Verde que te quiero verde.",
        "Federico García Lorca"
    ),
    Tipo.GEMINACION to DeviceExplanation(
        "Repetición seguida de una misma palabra. Intensifica una emoción o una imagen.",
        "Verde, verde, verde\nes el campo al amanecer."
    ),
    Tipo.POLISINDETON to DeviceExplanation(
        "Uso repetido de conjunciones (y, ni, o) donde no harían falta. Ralentiza el " +
            "ritmo y da sensación de acumulación.",
        "y el mar y la tierra y el cielo\ny todo lo que respira"
    ),
    Tipo.ASINDETON to DeviceExplanation(
        "Enumeración sin conjunciones, solo con comas. Acelera el ritmo y da viveza.",
        "Desmayarse, atreverse, estar furioso,\náspero, tierno, liberal, esquivo",
        "Lope de Vega"
    ),
    Tipo.PARALELISMO to DeviceExplanation(
        "Versos seguidos con la misma estructura gramatical. Crea simetría y hace " +
            "que el lector compare lo que dicen.",
        "Los suspiros son aire y van al aire.\nLas lágrimas son agua y van al mar.",
        "Gustavo Adolfo Bécquer"
    ),
    Tipo.ESTRIBILLO to DeviceExplanation(
        "Verso o grupo de versos que se repite a lo largo del poema o la canción. " +
            "Da unidad y es lo que más se recuerda.",
        "a las cinco de la tarde.",
        "Federico García Lorca"
    )
)

/** Dialog explaining a detected literary device and why it was detected. */
@Composable
fun DeviceInfoDialog(device: Recursos.Recurso, where: String, onDismiss: () -> Unit) {
    val info = EXPLANATIONS[device.tipo]
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Entendido") } },
        title = { Text(device.tipo.nombre) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(info?.definition ?: device.tipo.descripcion, style = MaterialTheme.typography.bodyMedium)

                if (info != null) {
                    Spacer(Modifier.height(16.dp))
                    Text("Ejemplo", style = MaterialTheme.typography.labelLarge, color = colors.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(info.example, style = VerseStyle.cardBody.copy(fontStyle = FontStyle.Italic))
                    if (info.source != null) Text(
                        "— ${info.source}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text("En tu texto", style = MaterialTheme.typography.labelLarge, color = colors.primary)
                Spacer(Modifier.height(4.dp))
                Text(
                    listOf(where.replaceFirstChar { it.uppercase() }, device.evidencia)
                        .filter { it.isNotEmpty() }.joinToString(": "),
                    style = MaterialTheme.typography.bodyMedium
                )
                device.intensidad?.let { intensity ->
                    Spacer(Modifier.height(8.dp))
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

private fun intensityText(intensity: Double, clear: Boolean): String {
    val times = String.format(Locale("es", "ES"), "%.1f", intensity)
    val threshold = String.format(Locale("es", "ES"), "%.1f", Recursos.UMBRAL_CLARA)
    return "Este sonido aparece $times veces más de lo habitual en español. " +
        if (clear) "Por eso se considera una aliteración clara (a partir de $threshold)."
        else "Es una aliteración posible: se considera clara a partir de $threshold."
}
