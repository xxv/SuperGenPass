package info.staticfree.supergenpass.test;

import androidx.annotation.NonNull;

import junit.framework.TestCase;

import info.staticfree.supergenpass.PasswordGenerationException;
import info.staticfree.supergenpass.hashes.DomainBasedHash;

public final class Utils {

    private Utils() {
        // This class cannot be instantiated.
    }

    public static void testATonOfPasswords(@NonNull DomainBasedHash hash, int minlen, int maxlen)
            throws PasswordGenerationException {
        for (int len = minlen; len < maxlen; len++) {
            for (int i = 0; i < 1000; i += 10) {
                String generated = hash.generate(String.valueOf(i), "example.org", len);
                TestCase.assertNotNull(generated);
                TestCase.assertEquals(len, generated.length());
            }
        }
    }
}
