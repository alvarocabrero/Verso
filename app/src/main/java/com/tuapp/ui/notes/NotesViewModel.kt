package com.tuapp.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tuapp.VersoApp
import com.tuapp.data.Note
import com.tuapp.data.NotesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class NotesViewModel(private val repo: NotesRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** null while loading for the first time. */
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val notes: StateFlow<List<Note>?> = _query
        .debounce { if (it.isEmpty()) 0L else 200L }
        .flatMapLatest { repo.notes(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun search(text: String) { _query.value = text }

    companion object {
        val Factory = viewModelFactory {
            initializer { NotesViewModel((this[APPLICATION_KEY] as VersoApp).repository) }
        }
    }
}
