package com.tuapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/** [label] is the name shown in the (Spanish) UI. */
enum class NoteType(val label: String) {
    POEM("Poema"),
    SONG("Canción")
}

/**
 * Table and column names keep their original Spanish names so that databases
 * created by version 0.1.0 keep working without a migration.
 */
@Entity(tableName = "notas")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "titulo") val title: String = "",
    @ColumnInfo(name = "contenido") val content: String = "",
    @ColumnInfo(name = "tipo") val type: NoteType = NoteType.POEM,
    val color: Int = 0,                 // index into NotePalette
    @ColumnInfo(name = "fijada") val pinned: Boolean = false,
    @ColumnInfo(name = "creada") val created: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "modificada") val modified: Long = System.currentTimeMillis()
)

/** Stores [NoteType] with the values used by version 0.1.0 ("POEMA", "CANCION"). */
class Converters {
    @TypeConverter
    fun fromNoteType(type: NoteType): String = when (type) {
        NoteType.POEM -> "POEMA"
        NoteType.SONG -> "CANCION"
    }

    @TypeConverter
    fun toNoteType(value: String): NoteType = when (value) {
        "CANCION" -> NoteType.SONG
        else -> NoteType.POEM
    }
}
