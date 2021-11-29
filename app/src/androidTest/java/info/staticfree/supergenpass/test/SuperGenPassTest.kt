package info.staticfree.supergenpass.test

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import kotlin.Throws
import info.staticfree.supergenpass.hashes.PasswordGenerationException
import info.staticfree.supergenpass.hashes.DomainNormalizer
import info.staticfree.supergenpass.hashes.SuperGenPass
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.lang.Exception
import java.security.NoSuchAlgorithmException

@RunWith(AndroidJUnit4::class)
class SuperGenPassTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val normalizer = DomainNormalizer()

    @Before
    fun setup() {
        runBlocking {
            normalizer.loadDomains(context.resources)
        }
    }

    @Test
    @Throws(PasswordGenerationException::class)
    fun testKnownGoods() {
        val sgp = SuperGenPass(normalizer, SuperGenPass.HASH_MD5, true)

        // these were generated using SGP's javascript itself by hand.

        //@formatter:off
        val knownGoods = arrayOf(
            arrayOf("a", "example.org", "10", "bieCWgE99X"),
            arrayOf("a", "EXAMPLE.org", "10", "bieCWgE99X"),
            arrayOf("12345", "example.org", "10", "tHR8hvgs1D"),
            arrayOf("12345", "example.org", "24", "tHR8hvgs1DHOlfCScJlzsQAA"),
            arrayOf("12345", "example.org", "4", "tHR8"),
            arrayOf("♥", "example.org", "10", "gd9hJAzinf"),
            arrayOf("flambé", "example.org", "10", "vqBh5a76L8"),
            arrayOf(" ", "example.org", "10", "vKwe7kuLl8"),
            arrayOf("foo bar", "example.org", "10", "nZq3XcpQx2"),
            arrayOf(" foo bar ", "example.org", "10", "qnyA2kcKz8"),
            arrayOf("12345", "www.example.org", "10", "tHR8hvgs1D"),
            arrayOf("12345", "WWW.Example.Org", "10", "tHR8hvgs1D"),
            arrayOf("12345", "example.co.uk", "10", "fdQnYi75VT"),
            arrayOf("12345", "www.example.co.uk", "10", "fdQnYi75VT")
        )
        //@formatter:on
        for (knownGood in knownGoods) {
            val msg = "for secret '" + knownGood[0] + "' and domain '" + knownGood[1] +
                    "' of length " + knownGood[2]
            Assert.assertEquals(
                msg, knownGood[3],
                sgp.generate(knownGood[0], knownGood[1], knownGood[2].toInt())
            )
        }
    }

    @Test
    @Throws(Exception::class)
    fun testInvalidOptions_checkDomain() {
        val sgp = SuperGenPass(normalizer, SuperGenPass.HASH_MD5, true)

        // bad domain
        var caught = false
        try {
            sgp.generate("12345", "bad domain", 10)
        } catch (e: PasswordGenerationException) {
            caught = true
        }
        Assert.assertTrue("exception thrown", caught)
    }

    @Test
    @Throws(Exception::class)
    fun testInvalidOptions_tooShort() {
        val sgp = SuperGenPass(normalizer, SuperGenPass.HASH_MD5, true)
        var caught = false
        try {
            sgp.generate("12345", "example.org", 0)
        } catch (e: PasswordGenerationException) {
            caught = true
        }
        Assert.assertTrue("exception thrown", caught)
    }

    @Test
    @Throws(Exception::class)
    fun testInvalidOptions_tooLong() {
        val sgp = SuperGenPass(normalizer, SuperGenPass.HASH_MD5, true)
        // too long length
        var caught = false
        try {
            sgp.generate("12345", "example.org", 100)
        } catch (e: PasswordGenerationException) {
            caught = true
        }
        Assert.assertTrue("exception thrown", caught)
    }

    @Test
    @Throws(NoSuchAlgorithmException::class, IOException::class)
    fun testSha512() {
        val sgp = SuperGenPass(normalizer, SuperGenPass.HASH_SHA_512, true)
    }

    @Test
    @Throws(PasswordGenerationException::class)
    fun testATonOfPasswordsSha512() {
        val sgp = SuperGenPass(normalizer, SuperGenPass.HASH_SHA_512, true)
        Utils.testATonOfPasswords(sgp, 4, 10)
    }

    @Test
    @Throws(PasswordGenerationException::class)
    fun testATonOfPasswordsMd5() {
        val sgp = SuperGenPass(normalizer, SuperGenPass.HASH_MD5, true)
        Utils.testATonOfPasswords(sgp, 4, 10)
    }
}