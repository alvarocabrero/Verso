package com.tuapp.ui.notas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuapp.data.Nota
import com.tuapp.ui.theme.EstiloVerso
import com.tuapp.ui.theme.fondoNota

@Composable
fun NotasScreen(
    abrirNota: (Long) -> Unit,               // 0 = nota nueva
    vm: NotasViewModel = viewModel(factory = NotasViewModel.Factory)
) {
    val notas by vm.notas.collectAsStateWithLifecycle()
    val busqueda by vm.busqueda.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { abrirNota(0L) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Icon(Icons.Filled.Add, contentDescription = "Nueva nota") }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            CampoBusqueda(busqueda, vm::buscar)
            val lista = notas
            when {
                lista == null -> Unit
                lista.isEmpty() -> Vacio(busqueda)
                else -> LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(160.dp),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
                    verticalItemSpacing = 10.dp,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(lista, key = { it.id }) { nota ->
                        TarjetaNota(nota, onClick = { abrirNota(nota.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CampoBusqueda(texto: String, alCambiar: (String) -> Unit) {
    TextField(
        value = texto,
        onValueChange = alCambiar,
        placeholder = { Text("Buscar en tus notas") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (texto.isNotEmpty()) IconButton(onClick = { alCambiar("") }) {
                Icon(Icons.Filled.Close, contentDescription = "Borrar búsqueda")
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun TarjetaNota(nota: Nota, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = fondoNota(nota.color),
        border = if (nota.color == 0) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    ) {
        Column(Modifier.padding(14.dp)) {
            if (nota.titulo.isNotBlank()) {
                Text(nota.titulo, style = EstiloVerso.tarjetaTitulo, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
            }
            if (nota.contenido.isNotBlank()) {
                // Se respetan los saltos de verso: un poema no se lee como un párrafo.
                Text(
                    nota.contenido.trim().lines().take(8).joinToString("\n"),
                    style = EstiloVerso.tarjetaCuerpo,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    nota.tipo.nombre,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                if (nota.fijada) Icon(
                    Icons.Filled.PushPin,
                    contentDescription = "Fijada",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun Vacio(busqueda: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (busqueda.isBlank()) "La página está en blanco" else "Ninguna nota contiene «$busqueda»",
                style = EstiloVerso.vacio,
                textAlign = TextAlign.Center
            )
            if (busqueda.isBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Toca + para escribir tu primer verso.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
