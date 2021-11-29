package info.staticfree.supergenpass.test

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import kotlin.Throws
import info.staticfree.supergenpass.hashes.PasswordGenerationException
import info.staticfree.supergenpass.hashes.DomainNormalizer
import info.staticfree.supergenpass.hashes.HotpPin
import androidx.test.filters.LargeTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class HotpPinTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val normalizer = DomainNormalizer()

    @Before
    fun setup() {
        runBlocking {
            normalizer.loadDomains(context.resources)
        }
    }

    @Test
    @Throws(PasswordGenerationException::class, IOException::class)
    fun testHotpPin() {
        val pinGen = HotpPin(normalizer, true)

        // these are bad to give as an output
        Assert.assertTrue(pinGen.isNumericalRun("1111"))
        Assert.assertTrue(pinGen.isNumericalRun("1234"))
        Assert.assertTrue(pinGen.isNumericalRun("4321"))
        Assert.assertTrue(pinGen.isNumericalRun("2468"))
        Assert.assertTrue(pinGen.isNumericalRun("0000"))
        Assert.assertTrue(pinGen.isNumericalRun("9999"))
        Assert.assertTrue(pinGen.isNumericalRun("0369"))
        Assert.assertFalse(pinGen.isNumericalRun("0101"))
        Assert.assertFalse(pinGen.isNumericalRun("1235"))
        Assert.assertEquals("3097", pinGen.generate("foo", "example.org", 4))
    }

    @Test
    @Throws(IOException::class, PasswordGenerationException::class)
    fun testDomainFiltering() {
        val pinGen = HotpPin(normalizer, true)
        Assert.assertEquals(
            pinGen.generate("foo", "foo.example.org", 4),
            pinGen.generate("foo", "example.org", 4)
        )
    }

    @Test
    @Throws(IOException::class, PasswordGenerationException::class)
    fun testDomainFilteringOff() {
        val pinGen = HotpPin(normalizer, false)
        Assert.assertFalse(
            pinGen.generate("foo", "foo.example.org", 4) ==
                    pinGen.generate("foo", "example.org", 4)
        )
    }

    @Test
    @Throws(IOException::class)
    fun testNumericRuns() {
        val pinGen = HotpPin(normalizer, false)

        // these are bad to give as an output
        Assert.assertTrue(pinGen.isNumericalRun("1111"))
        Assert.assertTrue(pinGen.isNumericalRun("1234"))
        Assert.assertTrue(pinGen.isNumericalRun("4321"))
        Assert.assertTrue(pinGen.isNumericalRun("2468"))
        Assert.assertTrue(pinGen.isNumericalRun("0000"))
        Assert.assertTrue(pinGen.isNumericalRun("9999"))
        Assert.assertTrue(pinGen.isNumericalRun("0369"))

        // these aren't runs
        Assert.assertFalse(pinGen.isNumericalRun("0101"))
        Assert.assertFalse(pinGen.isNumericalRun("1235"))
    }

    @Test
    @Throws(IOException::class)
    fun testIncompleteNumericRuns() {
        val pinGen = HotpPin(normalizer, false)

        // these are bad to give as an output
        Assert.assertTrue(pinGen.isIncompleteNumericalRun("1111"))
        Assert.assertTrue(pinGen.isIncompleteNumericalRun("1113"))
        Assert.assertTrue(pinGen.isIncompleteNumericalRun("3111"))
        Assert.assertTrue(pinGen.isIncompleteNumericalRun("10001"))
        Assert.assertTrue(pinGen.isIncompleteNumericalRun("011101"))

        // these aren't runs
        Assert.assertFalse(pinGen.isIncompleteNumericalRun("0010"))
        Assert.assertFalse(pinGen.isIncompleteNumericalRun("1234"))
    }

    @Test
    @Throws(PasswordGenerationException::class, IOException::class)
    fun testGeneratedLength() {
        val pinGen = HotpPin(normalizer, false)
        for (i in 3..8) {
            Assert.assertTrue(pinGen.generate("foo", "example.org", i).length == i)
        }
    }

    @Test
    @Throws(IOException::class)
    fun testInvalidLengths() {
        val pinGen = HotpPin(normalizer, false)
        testInvalidLength(pinGen, -1)
        testInvalidLength(pinGen, 0)
        testInvalidLength(pinGen, 1)
        testInvalidLength(pinGen, 2)
        testInvalidLength(pinGen, 9)
        testInvalidLength(pinGen, 100)
    }

    private fun testInvalidLength(pinGen: HotpPin, len: Int) {
        var thrown = false
        try {
            pinGen.generate("foo", "example.org", len)
        } catch (e: PasswordGenerationException) {
            thrown = true
        }
        Assert.assertTrue("exception not thrown for length $len", thrown)
    }

    @Test
    @Throws(IOException::class)
    fun testBadPins() {
        val pinGen = HotpPin(normalizer, false)
        val badPins = arrayOf(
            "0000", "1111", "1234", "1984", "2001", "1122",
            "553388", "1234567", "8844", "9876", "9753", "2000", "8000", "10001", "4111",
            "0007", "90210", "1004", "8068", "90210"
        )
        for (badPin in badPins) {
            Assert.assertTrue("bad PIN: $badPin not detected to be bad", pinGen.isBadPin(badPin))
        }
    }

    @Test
    @Throws(IOException::class)
    fun testGoodPins() {
        val pinGen = HotpPin(normalizer, false)
        val goodPins = arrayOf(
            "1837", "7498", "8347", "7426", "7172", "9012",
            "8493", "400500", "4385719", "12349"
        )
        for (goodPin in goodPins) {
            Assert.assertFalse(pinGen.isBadPin(goodPin))
        }
    }

    @Test
    @LargeTest
    @Throws(PasswordGenerationException::class, IOException::class)
    fun testATonOfPasswords() {
        val pinGen = HotpPin(normalizer, false)
        Utils.testATonOfPasswords(pinGen, 3, 8)
    }
}