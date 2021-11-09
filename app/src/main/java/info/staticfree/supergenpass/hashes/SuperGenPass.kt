package info.staticfree.supergenpass.hashes

import android.content.Context
import kotlin.Throws
import info.staticfree.supergenpass.PasswordGenerationException
import info.staticfree.supergenpass.IllegalDomainException
import org.apache.commons.codec.binary.Base64
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.regex.Pattern

/*
 * Copyright (C) 2010 Steve Pomeroy
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
 * Generates a domain password based on the SuperGenPass algorithm.
 *
 * @see [supergenpass.com](http://supergenpass.com/)
 */
class SuperGenPass
@Throws(NoSuchAlgorithmException::class)
constructor(context: Context, hashAlgorithm: String) : DomainBasedHash(context) {
    private val digest: MessageDigest = MessageDigest.getInstance(hashAlgorithm)

    /**
     * Returns a base64-encoded string of the digest of the data. Caution: SuperGenPass-specific!
     * Includes substitutions to ensure that valid base64 characters '=', '/', and '+' get mapped to
     * 'A', '8', and '9' respectively, so as to ensure alpha/num passwords.
     *
     * @return base64-encoded string of the hash of the data
     */
    private fun hashBase64(data: ByteArray): String {
        var b64 = String(Base64.encodeBase64(digest.digest(data)))
        // SuperGenPass-specific quirk so that these don't end up in the password.
        b64 = b64.replace('=', 'A').replace('/', '8').replace('+', '9')
        b64 = b64.trim { it <= ' ' }
        return b64
    }

    /**
     * Generates a domain password based on the SuperGenPass algorithm.
     *
     * @param domain pre-filtered domain (eg. example.org)
     * @param length generated password length; an integer between 4 and 24, inclusive.
     * @return generated password
     * @see [supergenpass.com](http://supergenpass.com/)
     */
    @Throws(PasswordGenerationException::class)
    public override fun generateWithFilteredDomain(
        masterPass: String,
        domain: String,
        length: Int
    ): String {
        if (length < 4 || length > 24) {
            throw PasswordGenerationException(
                "Requested length out of range. Expecting value between 4 and 24 inclusive."
            )
        }
        if (domain.isEmpty()) {
            throw IllegalDomainException("Missing domain")
        }
        var pwSeed = "$masterPass:$domain"

        // wash ten times
        for (i in 0..9) {
            pwSeed = hashBase64(pwSeed.toByteArray())
        }
        var matcher = validPassword.matcher(pwSeed.substring(0, length))
        while (!matcher.matches()) {
            pwSeed = hashBase64(pwSeed.toByteArray())
            matcher = validPassword.matcher(pwSeed.substring(0, length))
        }

        // when the right pwSeed is found to have a
        // password-appropriate substring, return it
        return pwSeed.substring(0, length)
    }

    companion object {
        const val TYPE = "sgp"
        const val TYPE_SHA_512 = "sgp-sha-512"
        const val HASH_ALGORITHM_MD5 = "MD5"
        const val HASH_ALGORITHM_SHA512 = "SHA-512"

        /*
         * from http://supergenpass.com/about/#PasswordComplexity :
         *  Consist of alphanumerics (A-Z, a-z, 0-9)
         * Always start with a lowercase letter of the alphabet
         * Always contain at least one uppercase letter of the alphabet
         * Always contain at least one numeral
         * Can be any length from 4 to 24 characters (default: 10)
         */

        // regex looks for:
        // "lcletter stuff Uppercase stuff Number stuff" or
        // "lcletter stuff Number stuff Uppercase stuff"
        // which should satisfy the above requirements.
        private val validPassword = Pattern.compile(
            "^[a-z][a-zA-Z0-9]*(?:(?:[A-Z][a-zA-Z0-9]*[0-9])|(?:[0-9][a-zA-Z0-9]*[A-Z]))" +
                    "[a-zA-Z0-9]*$"
        )
    }
}