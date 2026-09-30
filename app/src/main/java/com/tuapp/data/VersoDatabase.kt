package com.tuapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Note::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class VersoDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        fun create(context: Context): VersoDatabase =
            Room.databaseBuilder(context, VersoDatabase::class.java, "verso.db").build()
    }
}
