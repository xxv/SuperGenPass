package info.staticfree.supergenpass.test;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import info.staticfree.supergenpass.hashes.PasswordGenerationException;
import info.staticfree.supergenpass.hashes.PasswordComposer;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


@RunWith(AndroidJUnit4.class)
public class PasswordComposerTest {

    @Test
    public void testKnownGood() throws Exception {
        PasswordComposer pwc = new PasswordComposer(getApplicationContext());

        String[][] knownGoods = {

                //@formatter:off
                { "12345", "example.org", "8", "41affed2" },

                { "12345", "www.example.org", "8", "ce9c9736" },

                { "a", "example.org", "8", "343e55c8" },
                { "aaaaaaaaaaaaaaaaaaaa", "example.org", "8", "bcfc5184" },
                { " ", "example.org", "8", "9840922e" },

                // this differs from the javascript implementation
                // {"flambeé", "example.org", "8", "5ec4cedc"},
                // {"♥", "example.org", "8", "d510d806"},
        };
        //@formatter:on

        for (String[] knownGood : knownGoods) {
            String msg = "for secret '" + knownGood[0] + "' and domain '" + knownGood[1] +
                    "' of length " + knownGood[2];
            assertEquals(msg, knownGood[3],
                    pwc.generate(knownGood[0], knownGood[1], Integer.parseInt(knownGood[2])));
        }
    }

    @Test
    public void testKnownBad() throws Exception {
        PasswordComposer pwc = new PasswordComposer(getApplicationContext());

        String[][] knownBads = {
                { "", "", "8" }, // Empty strings
                { "", "example.org", "8" }, // Empty password
                { "12345", "", "8" }, // Empty domain
        };

        for (String[] knownBad : knownBads) {
            String msg =
                    "for secret '" + knownBad[0] + "' and domain '" + knownBad[1] + "' of length " +
                            knownBad[2];
            try {
                pwc.generate(knownBad[0], knownBad[1], Integer.parseInt(knownBad[2]));
                fail("Expecting exception " + msg);
            } catch (@NonNull PasswordGenerationException ignored) {
                // Expected exception
            }
        }
    }

    @Test
    public void testLength() throws Exception {
        PasswordComposer pwc = new PasswordComposer(getApplicationContext());

        int i = 0;
        try {
            for (i = 1; i < 32; i++) {
                String s = pwc.generate("12345", "example.org", i);
                assertEquals(i, s.length());
            }
        } catch (@NonNull PasswordGenerationException e) {
            fail("got an exception for a known-good length " + i);
        }

        try {
            pwc.generate("12345", "example.org", 0);
            fail("Expecting exception to be caught for length 0");
        } catch (@NonNull PasswordGenerationException ignored) {
            // Expected exception
        }

        try {
            pwc.generate("12345", "example.org", 32);
            fail("Expecting exception to be caught for length 32");
        } catch (@NonNull PasswordGenerationException ignored) {
            // Expected exception
        }
    }

    @LargeTest
    public void testATonOfPasswords() throws Exception {
        PasswordComposer pwc = new PasswordComposer(getApplicationContext());
        Utils.testATonOfPasswords(pwc, 3, 8);
    }
}
