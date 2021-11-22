package info.staticfree.supergenpass.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns

class RememberedDBHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE '" + DB_DOMAINS_TABLE +
                "' ('" + BaseColumns._ID + "' INTEGER PRIMARY KEY, '" + Domain.DOMAIN +
                "' VARCHAR(255))")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $DB_DOMAINS_TABLE")
        onCreate(db)
    }

    companion object {
        private const val DB_NAME = "autocomplete_domains"
        const val DB_DOMAINS_TABLE = "domains"
        private const val DB_VERSION = 2
    }
}