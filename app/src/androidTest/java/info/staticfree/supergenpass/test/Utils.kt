package info.staticfree.supergenpass.test

import kotlin.Throws
import info.staticfree.supergenpass.hashes.PasswordGenerationException
import info.staticfree.supergenpass.hashes.DomainBasedHash
import junit.framework.TestCase

object Utils {
    @Throws(PasswordGenerationException::class)
    fun testATonOfPasswords(hash: DomainBasedHash, minlen: Int, maxlen: Int) {
        for (len in minlen until maxlen) {
            var i = 0
            while (i < 1000) {
                val generated = hash.generate(i.toString(), "example.org", len)
                TestCase.assertNotNull(generated)
                TestCase.assertEquals(len, generated.length)
                i += 10
            }
        }
    }
}