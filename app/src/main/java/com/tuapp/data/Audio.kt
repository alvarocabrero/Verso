package com.tuapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** An audio clip (recorded in the app or imported). The file lives in the app's private storage. */
@Entity(tableName = "audios")
data class Audio(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    val created: Long = System.currentTimeMillis()
)

/** Many-to-many link: a note can have several audios and an audio can be in several notes. */
@Entity(
    tableName = "note_audios",
    primaryKeys = ["note_id", "audio_id"],
    foreignKeys = [
        ForeignKey(entity = Note::class, parentColumns = ["id"], childColumns = ["note_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Audio::class, parentColumns = ["id"], childColumns = ["audio_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("audio_id")]
)
data class NoteAudio(
    @ColumnInfo(name = "note_id") val noteId: Long,
    @ColumnInfo(name = "audio_id") val audioId: Long
)

/** A note an audio is linked to, as shown in the audio list. */
data class AudioNoteLink(val audioId: Long, val noteId: Long, val title: String)

/** How many audios a note has, for the note cards. */
data class NoteAudioCount(val noteId: Long, val count: Int)
