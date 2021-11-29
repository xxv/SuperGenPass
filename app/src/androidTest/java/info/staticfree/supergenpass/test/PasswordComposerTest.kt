package info.staticfree.supergenpass.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import kotlin.Throws
import info.staticfree.supergenpass.hashes.DomainNormalizer
import info.staticfree.supergenpass.hashes.PasswordComposer
import info.staticfree.supergenpass.hashes.PasswordGenerationException
import androidx.test.filters.LargeTest
import org.junit.Assert
import org.junit.Test
import java.lang.Exception

@RunWith(AndroidJUnit4::class)
class PasswordComposerTest {
    private val normalizer = DomainNormalizer()

    @Test
    @Throws(Exception::class)
    fun testKnownGood() {
        val pwc = PasswordComposer(normalizer, false)
        val knownGoods = arrayOf(
            arrayOf("12345", "example.org", "8", "41affed2"),
            arrayOf("12345", "www.example.org", "8", "ce9c9736"),
            arrayOf("a", "example.org", "8", "343e55c8"),
            arrayOf("aaaaaaaaaaaaaaaaaaaa", "example.org", "8", "bcfc5184"),
            arrayOf(" ", "example.org", "8", "9840922e")
        )
        //@formatter:on
        for (knownGood in knownGoods) {
            val msg = "for secret '" + knownGood[0] + "' and domain '" + knownGood[1] +
                    "' of length " + knownGood[2]
            Assert.assertEquals(
                msg, knownGood[3],
                pwc.generate(knownGood[0], knownGood[1], knownGood[2].toInt())
            )
        }
    }

    @Test
    @Throws(Exception::class)
    fun testKnownBad() {
        val pwc = PasswordComposer(normalizer, false)
        val knownBads = arrayOf(
            arrayOf("", "", "8"),
            arrayOf("", "example.org", "8"),
            arrayOf("12345", "", "8")
        )
        for (knownBad in knownBads) {
            val msg =
                "for secret '" + knownBad[0] + "' and domain '" + knownBad[1] + "' of length " +
                        knownBad[2]
            try {
                pwc.generate(knownBad[0], knownBad[1], knownBad[2].toInt())
                Assert.fail("Expecting exception $msg")
            } catch (ignored: PasswordGenerationException) {
                // Expected exception
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun testLength() {
        val pwc = PasswordComposer(normalizer, false)
        var i = 0
        try {
            i = 1
            while (i < 32) {
                val s = pwc.generate("12345", "example.org", i)
                Assert.assertEquals(i.toLong(), s.length.toLong())
                i++
            }
        } catch (e: PasswordGenerationException) {
            Assert.fail("got an exception for a known-good length $i")
        }
        try {
            pwc.generate("12345", "example.org", 0)
            Assert.fail("Expecting exception to be caught for length 0")
        } catch (ignored: PasswordGenerationException) {
            // Expected exception
        }
        try {
            pwc.generate("12345", "example.org", 32)
            Assert.fail("Expecting exception to be caught for length 32")
        } catch (ignored: PasswordGenerationException) {
            // Expected exception
        }
    }

    @LargeTest
    @Throws(Exception::class)
    fun testATonOfPasswords() {
        val pwc = PasswordComposer(normalizer, false)
        Utils.testATonOfPasswords(pwc, 3, 8)
    }
}