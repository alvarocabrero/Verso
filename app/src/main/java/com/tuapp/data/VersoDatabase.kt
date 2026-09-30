package com.tuapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Note::class, Audio::class, NoteAudio::class], version = 2, exportSchema = true)
@TypeConverters(Converters::class)
abstract class VersoDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun audioDao(): AudioDao

    companion object {
        fun create(context: Context): VersoDatabase =
            Room.databaseBuilder(context, VersoDatabase::class.java, "verso.db")
                .addMigrations(MIGRATION_1_2)
                .build()

        /** Version 2 (0.2.0): audios and their links with notes. Notes are untouched. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `audios` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `file_name` TEXT NOT NULL, `duration_ms` INTEGER NOT NULL, " +
                        "`created` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `note_audios` (`note_id` INTEGER NOT NULL, " +
                        "`audio_id` INTEGER NOT NULL, PRIMARY KEY(`note_id`, `audio_id`), " +
                        "FOREIGN KEY(`note_id`) REFERENCES `notas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                        "FOREIGN KEY(`audio_id`) REFERENCES `audios`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_audios_audio_id` ON `note_audios` (`audio_id`)")
            }
        }
    }
}
