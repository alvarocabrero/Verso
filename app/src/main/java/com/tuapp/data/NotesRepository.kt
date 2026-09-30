package com.tuapp.data

import kotlinx.coroutines.flow.Flow

class NotesRepository(private val dao: NoteDao) {

    fun notes(query: String): Flow<List<Note>> =
        if (query.isBlank()) dao.observeAll() else dao.search(query.trim())

    suspend fun get(id: Long): Note? = dao.get(id)

    /** Inserts the note if it is new (id = 0), otherwise updates it. Returns the final id. */
    suspend fun save(note: Note): Long =
        if (note.id == 0L) dao.insert(note) else { dao.update(note); note.id }

    suspend fun delete(id: Long) = dao.delete(id)
}
