package edu.mit.mobile.android.utils

import android.text.TextUtils
import java.util.*

/*
 * Copyright (C) 2010-2011 MIT Mobile Experience Lab
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */   object ProviderUtils {
    /**
     * Adds extra where clauses
     * @param where
     * @param extraWhere
     * @return
     */
    @JvmStatic
    fun addExtraWhere(where: String?, vararg extraWhere: String?): String {
        val extraWhereJoined =
            ('('.toString() + TextUtils.join(") AND (", listOf(*extraWhere))
                    + ')')
        return extraWhereJoined + if (where != null && where.isNotEmpty()) " AND ($where)" else ""
    }

    /**
     * Adds in extra arguments to a where query. You'll have to put in the appropriate
     * @param whereArgs the original whereArgs passed in from the query. Can be null.
     * @param extraArgs Extra arguments needed for the query.
     * @return
     */
    @JvmStatic
    fun addExtraWhereArgs(whereArgs: Array<String>?, vararg extraArgs: String): Array<String> {
        val whereArgs2: MutableList<String> = ArrayList()
        if (whereArgs != null) {
            whereArgs2.addAll(listOf(*whereArgs))
        }
        whereArgs2.addAll(0, listOf(*extraArgs))
        return whereArgs2.toTypedArray()
    }
}