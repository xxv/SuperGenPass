package info.staticfree.supergenpass

import android.net.Uri
import android.provider.BaseColumns

object Domain : BaseColumns {
    const val DOMAIN = "domain"
    const val PATH = "domain"

    @JvmField
    val CONTENT_URI: Uri =
        Uri.parse("content://%s/%s".format(RememberedDomainProvider.AUTHORITY, PATH))
    const val SORT_ORDER = "$DOMAIN ASC"
}