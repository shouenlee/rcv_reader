package com.rcvreader.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "verses",
    foreignKeys = [ForeignKey(
        entity = Book::class,
        parentColumns = ["id"],
        childColumns = ["book_id"]
    )],
    indices = [Index(value = ["book_id", "chapter"], name = "idx_verses_lookup")]
)
data class Verse(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "book_id") val bookId: Int,
    val chapter: Int,
    @ColumnInfo(name = "verse_number") val verseNumber: Int,
    val text: String,
    @ColumnInfo(name = "has_footnotes") val hasFootnotes: Boolean
)
