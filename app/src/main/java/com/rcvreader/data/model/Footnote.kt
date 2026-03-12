package com.rcvreader.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "footnotes")
data class Footnote(
    @PrimaryKey val id: Int,
    val book_id: Int,
    val chapter: Int,
    val verse_number: Int,
    val footnote_number: Int,
    val keyword: String?,
    val content: String
)
