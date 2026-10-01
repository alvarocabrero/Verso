// =================================================================================================
// FILE: VersoDatabase.kt
// -------------------------------------------------------------------------------------------------
// WHAT IS THIS FILE FOR?
// It defines the app's database: which tables it has, its version number, how to open it, and
// how to upgrade an old database to the new version (a "migration").
//
// The database is one SQLite file on the phone called "verso.db". Room (the Android database
// library) creates it and writes most of the code from the annotations (labels that start
// with @) in this file.
//
// Tables:
//   - "notas" (class `Note`, data/Note.kt): the notes.
//   - "audios" (class `Audio`, data/Audio.kt): the audio clips.
//   - "note_audios" (class `NoteAudio`, data/Audio.kt): links between notes and audios.
//
// History of versions:
//   - Version 1 (app 0.1.0): only notes.
//   - Version 2 (app 0.2.0): adds audios and their links. `MIGRATION_1_2` upgrades a
//     version 1 database without losing notes.
//
// Created once in VersoApp.kt (`database`), which also gets the DAOs from it.
// =================================================================================================

package com.tuapp.data

// `Context`: Android's "context", needed to find where to store the database file.
import android.content.Context
// Room pieces: the `@Database` annotation, the `Room` builder, the base class, and
// `@TypeConverters`.
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
// `Migration`: a step that upgrades the database from one version to the next.
import androidx.room.migration.Migration
// The low-level database object that a migration receives, to run SQL on it.
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The app's Room database.
 *
 * - `@Database(...)` tells Room:
 *   - `entities = [...]`: the tables (the classes marked with `@Entity`). `Note::class`
 *     refers to the class itself.
 *   - `version = 2`: the current version of the database shape. Every time the tables change,
 *     this number must go up and a migration must be added.
 *   - `exportSchema = true`: Room also writes a description of the tables to the project's
 *     "app/schemas" folder, useful to check and test migrations.
 * - `@TypeConverters(Converters::class)`: use the `Converters` class (in Note.kt) to store
 *   types SQLite does not know, like `NoteType`.
 * - `abstract class`: a class that cannot be created directly because some parts are missing
 *   (the `abstract` functions). Room generates, when the app is built, a real class that
 *   fills them in. It inherits from `RoomDatabase` (`: RoomDatabase()`).
 */
@Database(entities = [Note::class, Audio::class, NoteAudio::class], version = 2, exportSchema = true)
@TypeConverters(Converters::class)
abstract class VersoDatabase : RoomDatabase() {

    /** Gives the notes DAO (the database operations for notes). Room writes the code. */
    abstract fun noteDao(): NoteDao
    /** Gives the audio DAO (operations for audios and links). Room writes the code. */
    abstract fun audioDao(): AudioDao

    /**
     * `companion object`: things that belong to the class itself and not to one object, used
     * as `VersoDatabase.create(...)` (similar to "static" in other languages).
     */
    companion object {
        /**
         * Opens (or creates, the first time) the database file "verso.db" and returns it.
         *
         * - `Room.databaseBuilder(context, VersoDatabase::class.java, "verso.db")` prepares a
         *   builder: which database class and which file name. `::class.java` is the way to
         *   give the class to Java-based libraries like Room.
         * - `.addMigrations(MIGRATION_1_2)`: if the phone has an old version 1 database, run
         *   this migration to upgrade it (keeping the notes).
         * - `.build()`: creates the database object.
         * Each `.` on a new line continues the same chain of calls.
         */
        fun create(context: Context): VersoDatabase =
            Room.databaseBuilder(context, VersoDatabase::class.java, "verso.db")
                .addMigrations(MIGRATION_1_2)
                .build()

        /**
         * Version 2 (0.2.0): audios and their links with notes. Notes are untouched.
         *
         * - `object : Migration(1, 2) { ... }` creates, right here, a single unnamed object
         *   that inherits from `Migration` and goes from version 1 to version 2 (this is an
         *   "object expression").
         * - Inside, `override fun migrate(db)` replaces the empty `migrate` function of
         *   `Migration` with ours. Room calls it with the database when an upgrade is needed.
         * - `db.execSQL("...")` runs one SQL command. The long commands are split into pieces
         *   joined with `+` only to keep the lines short.
         * - The SQL must match EXACTLY what Room would create for `Audio` and `NoteAudio`,
         *   otherwise Room reports an error when it checks the tables.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create the "audios" table (only if it does not exist yet), with columns:
                // - id: a whole number (INTEGER), the primary key (PRIMARY KEY), filled in
                //   automatically with the next number (AUTOINCREMENT), never empty (NOT NULL).
                // - name, file_name: text (TEXT), never empty.
                // - duration_ms, created: whole numbers, never empty.
                // The backquotes around names are SQL's way to quote table and column names.
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `audios` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `file_name` TEXT NOT NULL, `duration_ms` INTEGER NOT NULL, " +
                        "`created` INTEGER NOT NULL)"
                )
                // Create the "note_audios" link table:
                // - note_id and audio_id: whole numbers, never empty.
                // - PRIMARY KEY(note_id, audio_id): the pair identifies the row (no duplicates).
                // - FOREIGN KEY ... REFERENCES: note_id must exist in "notas", and audio_id
                //   in "audios". ON DELETE CASCADE: if the note or the audio is deleted, the
                //   link row is deleted too. ON UPDATE NO ACTION: nothing special happens if
                //   an id changes (ids never change here).
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `note_audios` (`note_id` INTEGER NOT NULL, " +
                        "`audio_id` INTEGER NOT NULL, PRIMARY KEY(`note_id`, `audio_id`), " +
                        "FOREIGN KEY(`note_id`) REFERENCES `notas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                        "FOREIGN KEY(`audio_id`) REFERENCES `audios`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                // Create the index on audio_id (the `Index("audio_id")` in Audio.kt), which makes
                // looking up the links of one audio fast.
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_audios_audio_id` ON `note_audios` (`audio_id`)")
            }
        }
    }
}
