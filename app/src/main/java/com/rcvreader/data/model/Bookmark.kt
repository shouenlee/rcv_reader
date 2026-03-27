package com.rcvreader.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "verse" or "footnote"
    @ColumnInfo(name = "verse_id") val verseId: Int? = null,
    @ColumnInfo(name = "footnote_id") val footnoteId: Int? = null,
    @ColumnInfo(name = "book_id") val bookId: Int,
    @ColumnInfo(name = "book_name") val bookName: String,
    val abbreviation: String,
    val chapter: Int,
    @ColumnInfo(name = "verse_number") val verseNumber: Int,
    @ColumnInfo(name = "preview_text") val previewText: String,
    val keyword: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
