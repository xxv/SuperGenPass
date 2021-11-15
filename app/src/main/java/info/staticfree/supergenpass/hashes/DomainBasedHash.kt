package info.staticfree.supergenpass.hashes

import android.content.Context
import android.content.res.Resources
import android.util.JsonReader
import androidx.annotation.WorkerThread
import info.staticfree.supergenpass.DomainNormalizer
import kotlin.Throws
import info.staticfree.supergenpass.R
import org.json.JSONArray
import org.json.JSONException
import info.staticfree.supergenpass.PasswordGenerationException
import info.staticfree.supergenpass.IllegalDomainException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.util.*
import java.util.regex.Pattern

/*
 * Copyright (C) 2010-2021 Steve Pomeroy
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
 */

/**
 * A password hash that takes a password and a domain. Domains are optionally checked against a
 * database of known TLDs in order to generate domain-specific passwords. For example,
 * "www.example.org" and "www2.example.org" will generate the same password.
 *
 * @author Steve Pomeroy
 */
abstract class DomainBasedHash(private val normalizer: DomainNormalizer) {
    private var checkDomain = false

    /**
     * @param checkDomain if true, sub-domains will be stripped from the hashing
     */
    fun setCheckDomain(checkDomain: Boolean) {
        this.checkDomain = checkDomain
    }

    /**
     * Generates a password based on the given domain and a master password. Each time the method is
     * passed a given master password / domain, it will output the same password for that pair.
     *
     * @param masterPass master password
     * @param domain un-filtered domain (eg. www.example.org)
     * @param length generated password length
     * @return generated password based on the master password and the domain
     * @throws PasswordGenerationException if the criteria for generating the password are not met.
     * Often a length or domain issue.
     */
    @Throws(PasswordGenerationException::class)
    fun generate(
        masterPass: String, domain: String,
        length: Int
    ): String {
        return generateWithFilteredDomain(
            masterPass,
            normalizer.getDomain(domain, checkDomain),
            length
        )
    }

    /**
     * Generates a password based on the given domain and a master password. Each time the method is
     * passed a given master password / domain, it will output the same password for that pair.
     *
     * @param masterPass master password
     * @param domain filtered domain (eg. example.org)
     * @param length generated password length
     * @return generated password based on the master password and the domain
     * @throws PasswordGenerationException if the criteria for generating the password are not met.
     * Often a length or domain issue.
     */
    @Throws(PasswordGenerationException::class)
    protected abstract fun generateWithFilteredDomain(
        masterPass: String,
        domain: String,
        length: Int
    ): String
}