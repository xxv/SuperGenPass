package info.staticfree.SuperGenPass.test;

import androidx.annotation.NonNull;

import info.staticfree.SuperGenPass.PasswordGenerationException;
import info.staticfree.SuperGenPass.hashes.DomainBasedHash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public final class Utils {

    private Utils() {
        // This class cannot be instantiated.
    }

    public static void testATonOfPasswords(@NonNull DomainBasedHash hash, int minlen, int maxlen)
            throws PasswordGenerationException {
        for (int len = minlen; len < maxlen; len++) {
            for (int i = 0; i < 1000; i += 10) {
                String generated = hash.generate(String.valueOf(i), "example.org", len);
                assertNotNull(generated);
                assertEquals(len, generated.length());
            }
        }
    }
}
