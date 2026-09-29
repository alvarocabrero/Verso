package com.tuapp.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuapp.data.TipoNota
import com.tuapp.ui.theme.EstiloVerso
import com.tuapp.ui.theme.PaletaNotas
import com.tuapp.ui.theme.fondoNota

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    alVolver: () -> Unit,
    vm: EditorViewModel = viewModel(factory = EditorViewModel.Factory)
) {
    var mostrarColores by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = fondoNota(vm.color),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = alVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    val mostrar = vm.preferencias.mostrarAnalisis
                    IconButton(onClick = { vm.preferencias.cambiarMostrarAnalisis(!mostrar) }) {
                        Icon(
                            if (mostrar) Icons.Filled.Numbers else Icons.Outlined.Numbers,
                            contentDescription = if (mostrar) "Ocultar análisis" else "Mostrar análisis",
                            tint = if (mostrar) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = vm::alternarFijada) {
                        Icon(
                            if (vm.fijada) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (vm.fijada) "Desfijar" else "Fijar"
                        )
                    }
                    IconButton(onClick = { mostrarColores = !mostrarColores }) {
                        Icon(Icons.Outlined.Palette, contentDescription = "Cambiar color")
                    }
                    IconButton(onClick = { vm.borrar(); alVolver() }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Borrar nota")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        val mostrarAnalisis = vm.preferencias.mostrarAnalisis
        val analisis = if (mostrarAnalisis) vm.analisis else null
        val elegido = vm.recursoElegido
        val resaltado = remember(analisis, elegido) {
            if (analisis != null && elegido != null) resaltadoDe(analisis, elegido) else null
        }
        val scroll = rememberScrollState()
        val densidad = LocalDensity.current
        var yVersos by remember { mutableFloatStateOf(0f) }
        var layoutVersos by remember { mutableStateOf<TextLayoutResult?>(null) }

        // Al elegir un recurso, lleva a la vista su primer verso
        LaunchedEffect(elegido) {
            val l = layoutVersos ?: return@LaunchedEffect
            val inicio = resaltado?.rangos?.firstOrNull()?.first ?: return@LaunchedEffect
            if (inicio > l.layoutInput.text.length) return@LaunchedEffect
            val y = yVersos + l.getLineTop(l.getLineForOffset(inicio)) - with(densidad) { 48.dp.toPx() }
            scroll.animateScrollTo(y.toInt().coerceAtLeast(0))
        }

        Column(
            Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .fillMaxSize()
        ) {
            AnimatedVisibility(visible = mostrarColores) {
                SelectorColor(seleccionado = vm.color, alElegir = vm::cambiarColor)
            }
            if (vm.cargando) return@Column

            Column(Modifier.weight(1f).verticalScroll(scroll)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    TipoNota.entries.forEach { t ->
                        FilterChip(
                            selected = vm.tipo == t,
                            onClick = { vm.cambiarTipo(t) },
                            label = { Text(t.nombre) }
                        )
                    }
                }

                TextField(
                    value = vm.titulo,
                    onValueChange = vm::cambiarTitulo,
                    placeholder = { Text("Título", style = EstiloVerso.titulo) },
                    textStyle = EstiloVerso.titulo,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = camposTransparentes(),
                    modifier = Modifier.fillMaxWidth()
                )
                EditorVersos(
                    valor = vm.contenido,
                    alCambiar = vm::cambiarContenido,
                    placeholder = if (vm.tipo == TipoNota.CANCION) "Escribe aquí tu letra…" else "Escribe aquí tus versos…",
                    analisis = analisis,
                    resaltado = resaltado,
                    conMargen = mostrarAnalisis,
                    alMedir = { layoutVersos = it },
                    modifier = Modifier.onGloballyPositioned { yVersos = it.positionInParent().y }
                )
            }

            if (mostrarAnalisis) PanelAnalisis(
                analisis = analisis,
                elegido = elegido,
                alElegir = vm::elegirRecurso,
                seseo = vm.preferencias.seseo,
                alCambiarSeseo = vm.preferencias::cambiarSeseo
            )
        }
    }
}


@Composable
private fun SelectorColor(seleccionado: Int, alElegir: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        PaletaNotas.forEachIndexed { i, c ->
            val elegido = i == seleccionado
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(fondoNota(i))           // el mismo fondo que tendrá la nota
                    .border(
                        width = if (elegido) 2.dp else 1.dp,
                        color = if (elegido) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    )
                    .clickable { alElegir(i) }
                    .semantics { contentDescription = c.nombre }
            ) {
                if (elegido) Icon(
                    Icons.Filled.Check, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun camposTransparentes() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent
)
