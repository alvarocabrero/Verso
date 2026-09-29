package com.tuapp.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tuapp.VersoApp
import com.tuapp.analisis.AnalisisPoema
import com.tuapp.analisis.Recursos
import com.tuapp.data.Nota
import com.tuapp.data.NotasRepositorio
import com.tuapp.data.Preferencias
import com.tuapp.data.TipoNota
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Estado del editor con autoguardado:
 * - guarda 600 ms después del último cambio;
 * - guarda al salir (en el ámbito de la app, que sobrevive a la pantalla);
 * - una nota que se deja vacía se descarta, como en Keep.
 * El Mutex evita que dos guardados simultáneos inserten la misma nota dos veces.
 *
 * El análisis (sílabas, rimas, recursos) se recalcula 300 ms después del
 * último cambio, fuera del hilo principal.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class EditorViewModel(
    private val repo: NotasRepositorio,
    private val scopeApp: CoroutineScope,
    val preferencias: Preferencias,
    estado: SavedStateHandle
) : ViewModel() {

    private var id: Long = estado["id"] ?: 0L
    private var creada = System.currentTimeMillis()

    var titulo by mutableStateOf(""); private set
    var contenido by mutableStateOf(""); private set
    var tipo by mutableStateOf(TipoNota.POEMA); private set
    var color by mutableIntStateOf(0); private set
    var fijada by mutableStateOf(false); private set
    var cargando by mutableStateOf(id != 0L); private set

    /** Último análisis; puede ir por detrás del texto mientras se escribe. */
    var analisis by mutableStateOf<AnalisisPoema.Resultado?>(null); private set
    var recursoElegido by mutableStateOf<Recursos.Recurso?>(null); private set

    private var hayCambios = false
    private var borrada = false
    private var guardadoPendiente: Job? = null
    private val mutex = Mutex()

    init {
        if (id != 0L) viewModelScope.launch {
            repo.obtener(id)?.let {
                titulo = it.titulo; contenido = it.contenido; tipo = it.tipo
                color = it.color; fijada = it.fijada; creada = it.creada
            }
            cargando = false
        }
        viewModelScope.launch {
            snapshotFlow { Triple(contenido, preferencias.seseo, preferencias.mostrarAnalisis) }
                .filter { it.third }
                .debounce(300)
                .mapLatest { (texto, seseo) ->
                    withContext(Dispatchers.Default) { AnalisisPoema.analizar(texto, seseo) }
                }
                .collect { r ->
                    analisis = r
                    if (recursoElegido !in r.recursos) recursoElegido = null
                }
        }
    }

    fun cambiarTitulo(t: String) { titulo = t; programarGuardado() }
    fun cambiarContenido(t: String) { contenido = t; programarGuardado() }
    fun cambiarTipo(t: TipoNota) { tipo = t; programarGuardado() }
    fun cambiarColor(c: Int) { color = c; programarGuardado() }
    fun alternarFijada() { fijada = !fijada; programarGuardado() }

    /** Toca un recurso para resaltarlo; tocarlo otra vez lo deselecciona. */
    fun elegirRecurso(r: Recursos.Recurso) {
        recursoElegido = if (recursoElegido == r) null else r
    }

    fun borrar() {
        borrada = true
        guardadoPendiente?.cancel()
        scopeApp.launch { mutex.withLock { if (id != 0L) repo.borrar(id) } }
    }

    private fun programarGuardado() {
        hayCambios = true
        guardadoPendiente?.cancel()
        guardadoPendiente = viewModelScope.launch {
            delay(600)
            guardar()
        }
    }

    private fun estaVacia() = titulo.isBlank() && contenido.isBlank()

    private fun nota() = Nota(
        id = id, titulo = titulo, contenido = contenido, tipo = tipo,
        color = color, fijada = fijada, creada = creada, modificada = System.currentTimeMillis()
    )

    private suspend fun guardar() = mutex.withLock {
        if (!hayCambios || borrada || estaVacia()) return@withLock
        // NonCancellable: si se inserta la nota, el id nuevo no debe perderse
        withContext(NonCancellable) { id = repo.guardar(nota()) }
        hayCambios = false
    }

    override fun onCleared() {
        guardadoPendiente?.cancel()
        if (borrada) return
        scopeApp.launch {
            mutex.withLock {
                when {
                    estaVacia() && id != 0L -> repo.borrar(id)
                    !estaVacia() && hayCambios -> id = repo.guardar(nota())
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as VersoApp
                EditorViewModel(app.repositorio, app.scopeApp, app.preferencias, createSavedStateHandle())
            }
        }
    }
}
