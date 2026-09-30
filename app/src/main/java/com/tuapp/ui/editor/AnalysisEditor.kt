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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.IconButton
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
import com.tuapp.ui.theme.VerseStyle

/** How a selected literary device is marked on the text. */
enum class HighlightStyle { BACKGROUND, UNDERLINE, DOTTED }

data class Highlight(val ranges: List<IntRange>, val style: HighlightStyle)

fun highlightFor(analysis: AnalisisPoema.Resultado, device: Recursos.Recurso) = Highlight(
    AnalisisPoema.rangos(analysis, device),
    when {
        device.tipo != Recursos.Tipo.ALITERACION -> HighlightStyle.BACKGROUND
        device.clara -> HighlightStyle.UNDERLINE
        else -> HighlightStyle.DOTTED
    }
)

private val TEXT_TOP_PADDING = 8.dp
private val MARGIN_WIDTH = 56.dp

/**
 * The verse field. With [analysis], it shows the syllables and rhyme letter of
 * each line in the right margin, aligned with the line's last visual row (a
 * long line can wrap over several). Only what matches the current text is
 * drawn: while the analysis lags behind, the margin stays if the number of
 * lines hasn't changed, and the highlight is hidden.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VerseEditor(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    analysis: AnalisisPoema.Resultado?,
    highlight: Highlight?,
    showMargin: Boolean,
    onLayout: (TextLayoutResult) -> Unit,
    modifier: Modifier = Modifier
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val colors = MaterialTheme.colorScheme
    val current = analysis?.takeIf { it.texto == value }
    val margin = analysis?.takeIf { it.lineas.size == value.count { c -> c == '\n' } + 1 }

    // Own TextFieldValue so we know where the cursor is
    var field by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    if (field.text != value) field = TextFieldValue(value, TextRange(value.length))

    // The field has no scroll of its own: the cursor must be brought into view by hand
    val bringIntoView = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    val slack = with(LocalDensity.current) { 32.dp.toPx() }
    LaunchedEffect(field.selection, layout, focused) {
        val l = layout ?: return@LaunchedEffect
        if (!focused) return@LaunchedEffect
        val cursor = l.getCursorRect(field.selection.end.coerceIn(0, l.layoutInput.text.length))
        bringIntoView.bringIntoView(Rect(cursor.left, cursor.top - slack, cursor.right, cursor.bottom + slack))
    }

    Box(modifier.fillMaxWidth()) {
        BasicTextField(
            value = field,
            onValueChange = { new ->
                field = new
                if (new.text != value) onValueChange(new.text)
            },
            textStyle = VerseStyle.body.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            onTextLayout = { layout = it; onLayout(it) },
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, style = VerseStyle.body, color = colors.onSurfaceVariant)
                    inner()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 320.dp)
                .onFocusChanged { focused = it.hasFocus }
                .padding(
                    start = 16.dp, top = TEXT_TOP_PADDING, bottom = 24.dp,
                    end = if (showMargin) MARGIN_WIDTH else 16.dp
                )
                .bringIntoViewRequester(bringIntoView)
                .drawBehind {
                    val l = layout
                    if (l != null && current != null && highlight != null) drawHighlight(l, highlight, colors.primary)
                }
        )
        val l = layout
        if (showMargin && margin != null && l != null) VerseMargin(margin, l, Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun VerseMargin(analysis: AnalisisPoema.Resultado, layout: TextLayoutResult, modifier: Modifier) {
    val density = LocalDensity.current
    val colors = MaterialTheme.colorScheme
    val topPadding = with(density) { TEXT_TOP_PADDING.roundToPx() }
    val length = layout.layoutInput.text.length

    Box(modifier.width(MARGIN_WIDTH)) {
        analysis.lineas.forEach { line ->
            val syllables = line.silabas ?: return@forEach
            if (line.fin > length) return@forEach
            val row = layout.getLineForOffset(line.fin)
            val top = layout.getLineTop(row)
            val height = with(density) { (layout.getLineBottom(row) - top).toDp() }
            val letter = line.rima?.letra
            // Accessibility description, in Spanish like the rest of the UI
            val description = buildString {
                append("$syllables sílabas")
                if (!line.encaja) append(", no encaja en el metro")
                if (letter != null && letter != '-') append(", rima $letter")
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .offset { IntOffset(0, topPadding + top.toInt()) }
                    .width(MARGIN_WIDTH)
                    .height(height)
                    .padding(end = 12.dp)
                    .clearAndSetSemantics { contentDescription = description }
            ) {
                Text(
                    "$syllables",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (line.encaja) colors.onSurfaceVariant else colors.error,
                    textAlign = TextAlign.End
                )
                Text(
                    if (letter == null || letter == '-') "·" else "$letter",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (letter == null || letter == '-') colors.outlineVariant else colors.primary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(18.dp)
                )
            }
        }
    }
}

private fun DrawScope.drawHighlight(layout: TextLayoutResult, h: Highlight, color: Color) {
    val length = layout.layoutInput.text.length
    h.ranges.forEach { range ->
        val start = range.first
        val end = range.last + 1
        if (start < 0 || end > length || start >= end) return@forEach
        val first = layout.getLineForOffset(start)
        val last = layout.getLineForOffset(end - 1)
        for (ln in first..last) {
            val x1 = if (ln == first) layout.getHorizontalPosition(start, true) else layout.getLineLeft(ln)
            val x2 = if (ln == last) layout.getHorizontalPosition(end, true) else layout.getLineRight(ln)
            val left = minOf(x1, x2)
            val right = maxOf(x1, x2)
            val baseline = layout.getLineBaseline(ln)
            when (h.style) {
                HighlightStyle.BACKGROUND -> {
                    val lineHeight = layout.getLineBottom(ln) - layout.getLineTop(ln)
                    drawRoundRect(
                        color.copy(alpha = .16f),
                        topLeft = Offset(left - 2.dp.toPx(), baseline - lineHeight * .62f),
                        size = Size(right - left + 4.dp.toPx(), lineHeight * .8f),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
                HighlightStyle.UNDERLINE, HighlightStyle.DOTTED -> {
                    val y = baseline + 4.dp.toPx()
                    drawLine(
                        color, Offset(left, y), Offset(right, y),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = if (h.style == HighlightStyle.DOTTED)
                            PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 3.dp.toPx())) else null
                    )
                }
            }
        }
    }
}

/**
 * Bottom panel: a summary (metre and rhyme scheme) that expands into the list
 * of detected literary devices and the seseo setting.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalysisPanel(
    analysis: AnalisisPoema.Resultado?,
    selected: Recursos.Recurso?,
    onSelect: (Recursos.Recurso) -> Unit,
    seseo: Boolean,
    onSeseoChange: (Boolean) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    // With the keyboard open, the expanded panel would leave no room for the verses
    val keyboard = WindowInsets.isImeVisible
    LaunchedEffect(keyboard) { if (keyboard) expanded = false }
    val colors = MaterialTheme.colorScheme
    val devices = analysis?.recursos.orEmpty()

    Column(Modifier.fillMaxWidth().background(colors.surfaceVariant.copy(alpha = .5f))) {
        HorizontalDivider(color = colors.outlineVariant)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
        ) {
            Text(
                summary(analysis),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (expanded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                contentDescription = if (expanded) "Cerrar análisis" else "Abrir análisis",
                tint = colors.onSurfaceVariant
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                Row(Modifier.padding(horizontal = 16.dp)) {
                    FilterChip(
                        selected = seseo,
                        onClick = { onSeseoChange(!seseo) },
                        label = { Text("Seseo (casa = caza)") }
                    )
                }
                if (devices.isEmpty()) {
                    Text(
                        "No hay recursos detectados todavía.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    val numbers = verseNumbers(analysis!!)
                    LazyColumn(Modifier.heightIn(max = 260.dp)) {
                        items(devices) { d ->
                            DeviceRow(d, numbers, d == selected) { onSelect(d) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(d: Recursos.Recurso, numbers: List<Int?>, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var showInfo by remember { mutableStateOf(false) }
    val verses = d.lineas.mapNotNull { numbers.getOrNull(it) }
    val where = when {
        verses.isEmpty() -> ""
        verses.size == 1 -> "verso ${verses[0]}"
        verses.zipWithNext().all { (a, b) -> b == a + 1 } -> "versos ${verses.first()}–${verses.last()}"
        else -> "versos " + verses.joinToString(", ")
    }
    val degree = if (d.tipo == Recursos.Tipo.ALITERACION) (if (d.clara) " · clara" else " · posible") else ""
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) colors.secondaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
            Text(d.tipo.nombre + degree, style = MaterialTheme.typography.titleSmall)
            Text(
                listOf(where, d.evidencia).filter { it.isNotEmpty() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
        IconButton(onClick = { showInfo = true }) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = "Qué es: ${d.tipo.nombre}",
                tint = colors.onSurfaceVariant
            )
        }
    }
    if (showInfo) DeviceInfoDialog(d, where) { showInfo = false }
}

/** Panel header, shown in Spanish: "Endecasílabo · ABBA · 3 recursos". */
private fun summary(a: AnalisisPoema.Resultado?): String {
    if (a == null || a.metro == null) return "Escribe unos versos para analizarlos"
    val metre = Metrica.nombreMetro(a.metro).replaceFirstChar { it.uppercase() }
    val n = a.recursos.size
    val devices = when (n) { 0 -> "sin recursos"; 1 -> "1 recurso"; else -> "$n recursos" }
    return "$metre · ${a.esquema} · $devices"
}

/** Verse number (counting only lines with words) for each line of the text. */
private fun verseNumbers(a: AnalisisPoema.Resultado): List<Int?> {
    var n = 0
    return a.lineas.map { if (it.silabas != null) ++n else null }
}
