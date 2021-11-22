package info.staticfree.supergenpass.db

import android.content.ContentResolver
import android.database.Cursor
import android.provider.BaseColumns
import android.widget.FilterQueryProvider

class DomainQueryProvider(private val contentResolver: ContentResolver) : FilterQueryProvider {
    override fun runQuery(constraint: CharSequence?): Cursor? {
        val c: Cursor? = if (constraint == null || constraint.isEmpty()) {
            contentResolver
                .query(
                    Domain.CONTENT_URI,
                    PROJECTION,
                    null,
                    null,
                    Domain.SORT_ORDER
                )
        } else {
            contentResolver
                .query(
                    Domain.CONTENT_URI,
                    PROJECTION,
                    Domain.DOMAIN + " GLOB ?",
                    arrayOf(
                        "$constraint*"
                    ),
                    Domain.SORT_ORDER
                )
        }
        return c
    }

    companion object {
        val PROJECTION = arrayOf(Domain.DOMAIN, BaseColumns._ID)
        const val DOMAIN_COLUMN = 0
    }
}