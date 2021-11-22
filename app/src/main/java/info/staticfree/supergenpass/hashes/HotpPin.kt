package info.staticfree.supergenpass.hashes

import android.util.Log
import kotlin.Throws
import org.openauthentication.otp.OneTimePasswordAlgorithm
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException

/**
 * This generates strong Personal Identification Numbers (PINs).
 *
 * PINs generated with this can be used for bank accounts, phone lock screens, ATMs, etc. The
 * generator avoids common bad PINs ("1234", "0000", "0007", etc.) detected using a variety of
 * techniques.
 *
 *  The generation algorithm is a modified version of
 * [HOTP](http://tools.ietf.org/html/rfc4226) which uses the master password for the
 * HMAC secret and the domain instead of the moving factor. If a bad PIN is detected,
 * the text " 1" is added to the end of the domain and it's recomputed. If a bad PIN is still
 * generated, it suffixes " 2" instead and will continue in this way until a good PIN comes out.
 *
 * @param context application context
 * @throws IOException on read errors
 *
 * @author [Steve Pomeroy](mailto:steve@staticfree.info)
 * @see OneTimePasswordAlgorithm.generateOTPFromText
 */
class HotpPin(domainNormalizer: DomainNormalizer) : DomainBasedHash(domainNormalizer) {
    @Throws(PasswordGenerationException::class)
    override fun generateWithFilteredDomain(
        masterPass: String,
        domain: String,
        length: Int
    ): String {
        if (length < 3 || length > 8) {
            throw PasswordGenerationException("length must be >= 3 and <= 8")
        }
        if (masterPass.isEmpty() || domain.isEmpty()) {
            throw PasswordGenerationException("master password and domain must be at least one character")
        }
        return try {
            var pin = OneTimePasswordAlgorithm
                .generateOTPFromText(
                    masterPass.toByteArray(), domain.toByteArray(), length, false,
                    -1
                )
            if (pin.length != length) {
                throw PasswordGenerationException(
                    "PIN generator error; requested length " + length + ", but got " +
                            pin.length
                )
            }
            var suffix = 0
            var loopOverrun = 0
            while (isBadPin(pin)) {
                val suffixedDomain = "$domain $suffix"
                pin = OneTimePasswordAlgorithm
                    .generateOTPFromText(
                        masterPass.toByteArray(), suffixedDomain.toByteArray(),
                        length, false, -1
                    )
                loopOverrun++
                suffix++
                if (loopOverrun > 100) {
                    throw PasswordGenerationException(
                        "PIN generator programming error: looped too many times"
                    )
                }
            }
            pin
        } catch (e: InvalidKeyException) {
            Log.e(TAG, "HotpPin generation error", e)
            throw PasswordGenerationException("Error generating PIN", e)
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "HotpPin generation error", e)
            throw PasswordGenerationException("Error generating PIN", e)
        }
    }

    /**
     * Tests the string to see if it contains a numeric run. For example, "123456", "0000", "9876",
     * and "2468" would all match.
     *
     * @param pin the PIN to test
     * @return true if the string is a numeric run
     */
    fun isNumericalRun(pin: String): Boolean {
        val len = pin.length

        var prevDigit = Character.digit(pin[0], 10)
        var prevDiff = Int.MAX_VALUE
        var isRun = true // assume it's true...
        var i = 1

        while (isRun && i < len) {
            val digit = Character.digit(pin[i], 10)
            val diff = digit - prevDigit
            if (prevDiff != Int.MAX_VALUE && diff != prevDiff) {
                isRun = false // ... and prove it's false
            }
            prevDiff = diff
            prevDigit = digit
            i++
        }
        return isRun
    }

    /**
     * Tests the string to see if it contains a partial numeric run. Eg. 3000, 5553
     *
     * @param pin the PIN to check
     * @return true if it contains a partial run
     */
    fun isIncompleteNumericalRun(pin: String): Boolean {
        val len = pin.length
        var consecutive = 0
        var last = pin[0]

        for (i in 1 until len) {
            val c = pin[i]
            if (last == c) {
                consecutive++
            } else {
                consecutive = 0
            }
            last = c
            if (consecutive >= 2) {
                return true
            }
        }
        return false
    }

    /**
     * Tests to see if the PIN is a "bad" pin. That is, one that is easily guessable. Essentially,
     * this is a blacklist of the most commonly used PINs like "1234", "0000" and "1984".
     *
     * @param pin the PIN to test
     * @return true if the PIN matches the bad PIN criteria
     */
    fun isBadPin(pin: String): Boolean {
        val len = pin.length

        // special cases for 4-digit PINs (which are quite common)
        if (len == 4) {
            val start = pin.subSequence(0, 2).toString().toInt()
            val end = pin.subSequence(2, 4).toString().toInt()

            // 19xx pins look like years, so might as well ditch them.
            if (start == 19 || start == 20 && end < 30) {
                return true
            }

            // 1515
            if (start == end) {
                return true
            }
        }

        // find case where all digits are in pairs
        // eg 1122 3300447722
        if (len % 2 == 0) {
            var paired = true
            var i = 0
            while (i < len - 1) {
                if (pin[i] != pin[i + 1]) {
                    paired = false
                }
                i += 2
            }
            if (paired) {
                return true
            }
        }
        if (isNumericalRun(pin)) {
            return true
        }
        if (isIncompleteNumericalRun(pin)) {
            return true
        }

        // filter out special numbers
        for (blacklisted in BLACKLISTED_PINS) {
            if (blacklisted == pin) {
                return true
            }
        }
        return false
    }

    companion object {
        private val TAG = HotpPin::class.java.simpleName

        /**
         * This is a hard-coded list of specific PINs that have cultural meaning. While they may be
         * improbable, none the less they won't output from the generation.
         */
        private val BLACKLISTED_PINS = arrayOf(
            "90210",
            "8675309" /* Jenny */,
            "1004" /* 10-4 */,
            // in this document http://www.datagenetics.com/blog/september32012/index.html
            // these were shown to be the least commonly used. Now they won't be used at all.
            "8068",
            "8093",
            "9629",
            "6835",
            "7637",
            "0738",
            "8398",
            "6793",
            "9480",
            "8957",
            "0859",
            "7394",
            "6827",
            "6093",
            "7063",
            "8196",
            "9539",
            "0439",
            "8438",
            "9047",
            "8557"
        )
    }
}