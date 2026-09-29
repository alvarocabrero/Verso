package com.tuapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TipoNota(val nombre: String) {
    POEMA("Poema"),
    CANCION("Canción")
}

@Entity(tableName = "notas")
data class Nota(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String = "",
    val contenido: String = "",
    val tipo: TipoNota = TipoNota.POEMA,
    val color: Int = 0,                 // índice en PaletaNotas
    val fijada: Boolean = false,
    val creada: Long = System.currentTimeMillis(),
    val modificada: Long = System.currentTimeMillis()
)
