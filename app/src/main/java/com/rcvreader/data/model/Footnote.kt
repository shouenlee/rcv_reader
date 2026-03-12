package com.rcvreader.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "footnotes",
    foreignKeys = [ForeignKey(
        entity = Book::class,
        parentColumns = ["id"],
        childColumns = ["book_id"]
    )],
    indices = [Index(value = ["book_id", "chapter", "verse_number"], name = "idx_footnotes_lookup")]
)
data class Footnote(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "book_id") val bookId: Int,
    val chapter: Int,
    @ColumnInfo(name = "verse_number") val verseNumber: Int,
    @ColumnInfo(name = "footnote_number") val footnoteNumber: Int,
    val keyword: String?,
    val content: String
)
