package info.staticfree.supergenpass.hashes

import android.content.Context
import kotlin.Throws
import info.staticfree.supergenpass.R
import org.json.JSONArray
import org.json.JSONException
import info.staticfree.supergenpass.PasswordGenerationException
import info.staticfree.supergenpass.IllegalDomainException
import junit.framework.Assert
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.util.*
import java.util.regex.Pattern

/*
 * Copyright (C) 2010-2013 Steve Pomeroy
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
abstract class DomainBasedHash(private val context: Context) {
    private var checkDomain = false
    private var domains: ArrayList<String> = ArrayList()

    init {
        loadDomains()
    }

    /**
     * This list should remain the same and in sync with the canonical SGP, so that passwords
     * generated in one place are the same as others.
     *
     * @throws IOException on disk errors
     */
    @Throws(IOException::class)
    fun loadDomains() {
        val inputStream = context.resources.openRawResource(R.raw.domains)
        val jsonString = StringBuilder()
        try {
            val isReader = BufferedReader(InputStreamReader(inputStream), 16000)
            while (isReader.ready()) {
                jsonString.append(isReader.readLine())
            }
            val domainJson = JSONArray(jsonString.toString())
            domains = ArrayList(domainJson.length())

            for (i in 0 until domainJson.length()) {
                domains.add(domainJson.getString(i))
            }
        } catch (e: IOException) {
            val ioe = IOException("Unable to load domains")
            ioe.initCause(e)
            throw ioe
        } catch (e: JSONException) {
            val ioe = IOException("Unable to load domains")
            ioe.initCause(e)
            throw ioe
        }
    }

    /**
     * Computes the site's domain, based on the provided hostname. This takes into account things
     * like "co.uk" and other such multi-level TLDs.
     *
     * @param hostname the full hostname
     * @return the domain of the URI
     * @throws PasswordGenerationException if there is an error generating the password
     */
    @Throws(PasswordGenerationException::class)
    fun getDomain(hostname: String): String {
        val hostnameLower = hostname.lowercase(Locale.US)
        if (!checkDomain) {
            return hostnameLower
        }

        // IP addresses should be composed based on the full address.
        if (PATTERN_IP_ADDRESS.matcher(hostnameLower).matches()) {
            return hostnameLower
        }

        // for single-level TLDs, we only want the TLD and the 2nd level domain
        val hostParts = hostnameLower.split(".").toTypedArray()
        if (hostParts.size < 2) {
            throw IllegalDomainException("Invalid domain: '$hostname'")
        }
        var domain = hostParts[hostParts.size - 2] + '.' + hostParts[hostParts.size - 1]

        // do a slow search of all the possible multi-level TLDs and
        // see if we need to pull in one level deeper.
        for (tld in domains) {
            if (domain == tld) {
                if (hostParts.size < 3) {
                    throw IllegalDomainException(
                        "Invalid domain. '$domain' seems to be a TLD."
                    )
                }
                domain = hostParts[hostParts.size - 3] + '.' + domain
                break
            }
        }
        return domain
    }

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
        return generateWithFilteredDomain(masterPass, getDomain(domain), length)
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

    companion object {
        private val PATTERN_IP_ADDRESS =
            Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")
    }

}