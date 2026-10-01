// =================================================================================================
// FILE: NotesRepository.kt
// -------------------------------------------------------------------------------------------------
// WHAT IS THIS FILE FOR?
// The "repository" is the door the screens use to work with notes. The screens never talk to
// the database directly: they ask the repository ("give me the notes", "save this note"), and
// the repository uses the DAO (data/NoteDao.kt) to do the work.
//
// Why this extra layer? It keeps the screens simple and puts small rules in one place, for
// example "if the search is empty, show all notes" or "if the note is new, insert it; if not,
// update it".
//
// It is created once in VersoApp.kt (`repository`) and used by the ViewModels of the notes
// list and the editor.
// =================================================================================================

package com.tuapp.data

// `Flow`: a "live feed" of values that sends a new value each time the data changes.
import kotlinx.coroutines.flow.Flow

/**
 * Entry point for reading and saving notes.
 *
 * - `class NotesRepository(private val dao: NoteDao)` declares the class and, in the same
 *   line, its constructor: to create a NotesRepository you must give it a `NoteDao`.
 * - `private val dao` turns that parameter into a property kept inside the object.
 *   `private` means only code inside this class can use it.
 */
class NotesRepository(private val dao: NoteDao) {

    /**
     * Returns a live feed of notes that match [query] (the search text).
     *
     * - `if (...) A else B` in Kotlin is an expression: it gives back A or B, so it can be used
     *   directly as the function's result (after the `=`).
     * - `query.isBlank()` is true when the text is empty or only spaces: then we show every
     *   note (`dao.observeAll()`).
     * - Otherwise we search, after `trim()` removes spaces at the start and the end.
     */
    fun notes(query: String): Flow<List<Note>> =
        if (query.isBlank()) dao.observeAll() else dao.search(query.trim())

    /**
     * Gets one note by its id, or `null` if it does not exist (`Note?`).
     * `suspend`: it must run inside a coroutine (a background task), because it waits for the
     * database.
     */
    suspend fun get(id: Long): Note? = dao.get(id)

    /**
     * Inserts the note if it is new (id = 0), otherwise updates it. Returns the final id.
     *
     * - `0L` is the number 0 written as a `Long` (the `L` says "long whole number"), to compare
     *   it with `note.id`, which is a Long. `==` checks if two values are equal.
     * - New note: `dao.insert(note)` adds it and returns the new id.
     * - Existing note: the `else { ... }` block runs two steps separated by `;`: first it
     *   updates the note, then `note.id` is the last value of the block, so that is what is
     *   returned.
     */
    suspend fun save(note: Note): Long =
        if (note.id == 0L) dao.insert(note) else { dao.update(note); note.id }

    /** Deletes the note with this id. */
    suspend fun delete(id: Long) = dao.delete(id)
}
