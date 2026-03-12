package com.rcvreader.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey val id: Int,
    val abbreviation: String,
    val name: String,
    val testament: String,
    val chapter_count: Int
)
