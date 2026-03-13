package com.rcvreader.data.model

data class SearchResult(
    val id: Int,
    val bookId: Int,
    val bookName: String,
    val abbreviation: String,
    val chapter: Int,
    val verseNumber: Int,
    val text: String,
    val keyword: String?,
    /** 1 = exact phrase, 2 = all words, 3 = any words. Used for merge ordering only, not shown in UI. */
    val tier: Int
)
