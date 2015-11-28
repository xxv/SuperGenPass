package info.staticfree.SuperGenPass.test;

import info.staticfree.SuperGenPass.PasswordGenerationException;
import info.staticfree.SuperGenPass.hashes.DomainBasedHash;

import java.io.IOException;

import junit.framework.TestCase;

public class Utils {

    public static void testATonOfPasswords(DomainBasedHash hash, int minlen, int maxlen)
            throws PasswordGenerationException, IOException {
        for (int len = minlen; len < maxlen; len++) {
            for (int i = 0; i < 1000; i += 10) {
                final String generated = hash.generate(String.valueOf(i), "example.org", len);
                TestCase.assertNotNull(generated);
                TestCase.assertEquals(len, generated.length());
            }
        }
    }
}
