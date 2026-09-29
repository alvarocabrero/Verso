package com.tuapp.ui.notas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tuapp.VersoApp
import com.tuapp.data.Nota
import com.tuapp.data.NotasRepositorio
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class NotasViewModel(private val repo: NotasRepositorio) : ViewModel() {

    private val _busqueda = MutableStateFlow("")
    val busqueda: StateFlow<String> = _busqueda.asStateFlow()

    /** null mientras carga por primera vez. */
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val notas: StateFlow<List<Nota>?> = _busqueda
        .debounce { if (it.isEmpty()) 0L else 200L }
        .flatMapLatest { repo.notas(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun buscar(texto: String) { _busqueda.value = texto }

    companion object {
        val Factory = viewModelFactory {
            initializer { NotasViewModel((this[APPLICATION_KEY] as VersoApp).repositorio) }
        }
    }
}
