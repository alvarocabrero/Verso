package com.tuapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Nota::class], version = 1, exportSchema = false)
abstract class VersoDatabase : RoomDatabase() {

    abstract fun notaDao(): NotaDao

    companion object {
        fun crear(context: Context): VersoDatabase =
            Room.databaseBuilder(context, VersoDatabase::class.java, "verso.db").build()
    }
}
