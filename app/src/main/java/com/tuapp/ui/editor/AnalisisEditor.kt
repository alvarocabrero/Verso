package com.tuapp.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tuapp.analisis.AnalisisPoema
import com.tuapp.analisis.Metrica
import com.tuapp.analisis.Recursos
import com.tuapp.ui.theme.EstiloVerso

/** Cómo se marca un recurso elegido sobre el texto. */
enum class EstiloResaltado { FONDO, SUBRAYADO, PUNTEADO }

data class Resaltado(val rangos: List<IntRange>, val estilo: EstiloResaltado)

fun resaltadoDe(analisis: AnalisisPoema.Resultado, recurso: Recursos.Recurso) = Resaltado(
    AnalisisPoema.rangos(analisis, recurso),
    when {
        recurso.tipo != Recursos.Tipo.ALITERACION -> EstiloResaltado.FONDO
        recurso.clara -> EstiloResaltado.SUBRAYADO
        else -> EstiloResaltado.PUNTEADO
    }
)

private val MARGEN_TEXTO = 8.dp
private val ANCHO_MARGEN = 56.dp

/**
 * Campo de los versos. Con [analisis], muestra al margen derecho las sílabas
 * y la letra de rima de cada verso, alineadas con su última línea visual
 * (un verso largo puede ocupar varias). Solo se dibuja lo que corresponde
 * al texto actual: mientras el análisis va por detrás, el margen se
 * mantiene si no han cambiado las líneas y el resaltado se oculta.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditorVersos(
    valor: String,
    alCambiar: (String) -> Unit,
    placeholder: String,
    analisis: AnalisisPoema.Resultado?,
    resaltado: Resaltado?,
    conMargen: Boolean,
    alMedir: (TextLayoutResult) -> Unit,
    modifier: Modifier = Modifier
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val colores = MaterialTheme.colorScheme
    val actual = analisis?.takeIf { it.texto == valor }
    val margen = analisis?.takeIf { it.lineas.size == valor.count { c -> c == '\n' } + 1 }

    // TextFieldValue propio para conocer la posición del cursor
    var campo by remember { mutableStateOf(TextFieldValue(valor, TextRange(valor.length))) }
    if (campo.text != valor) campo = TextFieldValue(valor, TextRange(valor.length))

    // El campo no tiene scroll propio: hay que llevar el cursor a la vista a mano
    val traerALaVista = remember { BringIntoViewRequester() }
    var enfocado by remember { mutableStateOf(false) }
    val holgura = with(LocalDensity.current) { 32.dp.toPx() }
    LaunchedEffect(campo.selection, layout, enfocado) {
        val l = layout ?: return@LaunchedEffect
        if (!enfocado) return@LaunchedEffect
        val cursor = l.getCursorRect(campo.selection.end.coerceIn(0, l.layoutInput.text.length))
        traerALaVista.bringIntoView(Rect(cursor.left, cursor.top - holgura, cursor.right, cursor.bottom + holgura))
    }

    Box(modifier.fillMaxWidth()) {
        BasicTextField(
            value = campo,
            onValueChange = { nuevo ->
                campo = nuevo
                if (nuevo.text != valor) alCambiar(nuevo.text)
            },
            textStyle = EstiloVerso.cuerpo.copy(color = colores.onSurface),
            cursorBrush = SolidColor(colores.primary),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            onTextLayout = { layout = it; alMedir(it) },
            decorationBox = { interior ->
                Box {
                    if (valor.isEmpty()) Text(placeholder, style = EstiloVerso.cuerpo, color = colores.onSurfaceVariant)
                    interior()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 320.dp)
                .onFocusChanged { enfocado = it.hasFocus }
                .padding(
                    start = 16.dp, top = MARGEN_TEXTO, bottom = 24.dp,
                    end = if (conMargen) ANCHO_MARGEN else 16.dp
                )
                .bringIntoViewRequester(traerALaVista)
                .drawBehind {
                    val l = layout
                    if (l != null && actual != null && resaltado != null) dibujarResaltado(l, resaltado, colores.primary)
                }
        )
        val l = layout
        if (conMargen && margen != null && l != null) MargenVersos(margen, l, Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun MargenVersos(analisis: AnalisisPoema.Resultado, layout: TextLayoutResult, modifier: Modifier) {
    val densidad = LocalDensity.current
    val colores = MaterialTheme.colorScheme
    val margenTop = with(densidad) { MARGEN_TEXTO.roundToPx() }
    val largo = layout.layoutInput.text.length

    Box(modifier.width(ANCHO_MARGEN)) {
        analisis.lineas.forEach { linea ->
            val silabas = linea.silabas ?: return@forEach
            if (linea.fin > largo) return@forEach
            val visual = layout.getLineForOffset(linea.fin)
            val arriba = layout.getLineTop(visual)
            val alto = with(densidad) { (layout.getLineBottom(visual) - arriba).toDp() }
            val letra = linea.rima?.letra
            val descripcion = buildString {
                append("$silabas sílabas")
                if (!linea.encaja) append(", no encaja en el metro")
                if (letra != null && letra != '-') append(", rima $letra")
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .offset { IntOffset(0, margenTop + arriba.toInt()) }
                    .width(ANCHO_MARGEN)
                    .height(alto)
                    .padding(end = 12.dp)
                    .clearAndSetSemantics { contentDescription = descripcion }
            ) {
                Text(
                    "$silabas",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (linea.encaja) colores.onSurfaceVariant else colores.error,
                    textAlign = TextAlign.End
                )
                Text(
                    if (letra == null || letra == '-') "·" else "$letra",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (letra == null || letra == '-') colores.outlineVariant else colores.primary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(18.dp)
                )
            }
        }
    }
}

private fun DrawScope.dibujarResaltado(layout: TextLayoutResult, r: Resaltado, color: Color) {
    val largo = layout.layoutInput.text.length
    r.rangos.forEach { rango ->
        val inicio = rango.first
        val fin = rango.last + 1
        if (inicio < 0 || fin > largo || inicio >= fin) return@forEach
        val primera = layout.getLineForOffset(inicio)
        val ultima = layout.getLineForOffset(fin - 1)
        for (ln in primera..ultima) {
            val x1 = if (ln == primera) layout.getHorizontalPosition(inicio, true) else layout.getLineLeft(ln)
            val x2 = if (ln == ultima) layout.getHorizontalPosition(fin, true) else layout.getLineRight(ln)
            val izq = minOf(x1, x2)
            val der = maxOf(x1, x2)
            val base = layout.getLineBaseline(ln)
            when (r.estilo) {
                EstiloResaltado.FONDO -> {
                    val alto = layout.getLineBottom(ln) - layout.getLineTop(ln)
                    drawRoundRect(
                        color.copy(alpha = .16f),
                        topLeft = Offset(izq - 2.dp.toPx(), base - alto * .62f),
                        size = Size(der - izq + 4.dp.toPx(), alto * .8f),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
                EstiloResaltado.SUBRAYADO, EstiloResaltado.PUNTEADO -> {
                    val y = base + 4.dp.toPx()
                    drawLine(
                        color, Offset(izq, y), Offset(der, y),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = if (r.estilo == EstiloResaltado.PUNTEADO)
                            PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 3.dp.toPx())) else null
                    )
                }
            }
        }
    }
}

/**
 * Panel inferior: resumen (metro y esquema de rima) que se despliega en la
 * lista de recursos detectados y el ajuste de seseo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PanelAnalisis(
    analisis: AnalisisPoema.Resultado?,
    elegido: Recursos.Recurso?,
    alElegir: (Recursos.Recurso) -> Unit,
    seseo: Boolean,
    alCambiarSeseo: (Boolean) -> Unit
) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    // Con el teclado abierto el panel dejaría sin sitio a los versos
    val teclado = WindowInsets.isImeVisible
    LaunchedEffect(teclado) { if (teclado) abierto = false }
    val colores = MaterialTheme.colorScheme
    val recursos = analisis?.recursos.orEmpty()

    Column(Modifier.fillMaxWidth().background(colores.surfaceVariant.copy(alpha = .5f))) {
        HorizontalDivider(color = colores.outlineVariant)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { abierto = !abierto }
                .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
        ) {
            Text(
                resumen(analisis),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (abierto) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                contentDescription = if (abierto) "Cerrar análisis" else "Abrir análisis",
                tint = colores.onSurfaceVariant
            )
        }
        AnimatedVisibility(visible = abierto) {
            Column {
                Row(Modifier.padding(horizontal = 16.dp)) {
                    FilterChip(
                        selected = seseo,
                        onClick = { alCambiarSeseo(!seseo) },
                        label = { Text("Seseo (casa = caza)") }
                    )
                }
                if (recursos.isEmpty()) {
                    Text(
                        "No hay recursos detectados todavía.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colores.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    val numeros = numerosDeVerso(analisis!!)
                    LazyColumn(Modifier.heightIn(max = 260.dp)) {
                        items(recursos) { r ->
                            FilaRecurso(r, numeros, r == elegido) { alElegir(r) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaRecurso(r: Recursos.Recurso, numeros: List<Int?>, elegido: Boolean, alTocar: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val versos = r.lineas.mapNotNull { numeros.getOrNull(it) }
    val donde = when {
        versos.isEmpty() -> ""
        versos.size == 1 -> "verso ${versos[0]}"
        versos.zipWithNext().all { (a, b) -> b == a + 1 } -> "versos ${versos.first()}–${versos.last()}"
        else -> "versos " + versos.joinToString(", ")
    }
    val grado = if (r.tipo == Recursos.Tipo.ALITERACION) (if (r.clara) " · clara" else " · posible") else ""
    Column(
        Modifier
            .fillMaxWidth()
            .background(if (elegido) colores.secondaryContainer else Color.Transparent)
            .clickable(onClick = alTocar)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(r.tipo.nombre + grado, style = MaterialTheme.typography.titleSmall)
        Text(
            listOf(donde, r.evidencia).filter { it.isNotEmpty() }.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = colores.onSurfaceVariant
        )
    }
}

private fun resumen(a: AnalisisPoema.Resultado?): String {
    if (a == null || a.metro == null) return "Escribe unos versos para analizarlos"
    val metro = Metrica.nombreMetro(a.metro).replaceFirstChar { it.uppercase() }
    val n = a.recursos.size
    val recursos = when (n) { 0 -> "sin recursos"; 1 -> "1 recurso"; else -> "$n recursos" }
    return "$metro · ${a.esquema} · $recursos"
}

/** Número de verso (contando solo líneas con palabras) de cada línea del texto. */
private fun numerosDeVerso(a: AnalisisPoema.Resultado): List<Int?> {
    var n = 0
    return a.lineas.map { if (it.silabas != null) ++n else null }
}
