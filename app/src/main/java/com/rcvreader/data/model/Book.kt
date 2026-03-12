package com.rcvreader.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey val id: Int,
    val abbreviation: String,
    val name: String,
    val testament: String,
    @ColumnInfo(name = "chapter_count") val chapterCount: Int
)
