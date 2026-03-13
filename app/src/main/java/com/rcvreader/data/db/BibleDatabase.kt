package com.rcvreader.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.rcvreader.data.model.Book
import com.rcvreader.data.model.Footnote
import com.rcvreader.data.model.Verse
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory

@Database(
    entities = [Book::class, Verse::class, Footnote::class],
    version = 1,
    exportSchema = false
)
abstract class BibleDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun verseDao(): VerseDao
    abstract fun footnoteDao(): FootnoteDao
    abstract fun searchDao(): SearchDao

    companion object {
        @Volatile
        private var INSTANCE: BibleDatabase? = null

        fun getInstance(context: Context): BibleDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BibleDatabase::class.java,
                    "bible.db"
                )
                    .openHelperFactory(RequerySQLiteOpenHelperFactory())
                    .createFromAsset("bible.db")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
