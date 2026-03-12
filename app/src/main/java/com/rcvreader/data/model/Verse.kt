package com.rcvreader.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "verses")
data class Verse(
    @PrimaryKey val id: Int,
    val book_id: Int,
    val chapter: Int,
    val verse_number: Int,
    val text: String,
    val has_footnotes: Int
)
