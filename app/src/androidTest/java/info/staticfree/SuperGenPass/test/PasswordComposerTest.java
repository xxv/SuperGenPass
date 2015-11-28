package info.staticfree.SuperGenPass.test;

import info.staticfree.SuperGenPass.PasswordGenerationException;
import info.staticfree.SuperGenPass.hashes.PasswordComposer;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.LargeTest;

public class PasswordComposerTest extends AndroidTestCase {

    public void testKnownGood() throws NoSuchAlgorithmException, IOException,
            NumberFormatException, PasswordGenerationException {
        final PasswordComposer pwc = new PasswordComposer(getContext());

        final String[][] knownGoods = {

                //@formatter:off
                {"12345", "example.org", "8", "41affed2"},

                {"12345", "www.example.org", "8", "ce9c9736"},

                {"a","example.org","8", "343e55c8"},
                {"aaaaaaaaaaaaaaaaaaaa", "example.org", "8", "bcfc5184"},
                {" ", "example.org", "8", "9840922e"},

                // this differs from the javascript implementation
//                {"flambeé", "example.org", "8", "5ec4cedc"},
//                {"♥", "example.org", "8", "d510d806"},


                };
        //@formatter:on

        for (final String[] knownGood : knownGoods) {
            final String msg = "for secret '" + knownGood[0] + "' and domain '" + knownGood[1]
                    + "' of length " + knownGood[2];
            assertEquals(msg, knownGood[3],
                    pwc.generate(knownGood[0], knownGood[1], Integer.parseInt(knownGood[2])));
        }
    }

    public void testKnownBad() throws NoSuchAlgorithmException, IOException {
        final PasswordComposer pwc = new PasswordComposer(getContext());

        final String[][] knownBads = {
//@formatter:off
                {"", "", "8"},
                {"", "example.org", "8"},
                {"12345", "", "8"},

        };
//@formatter:on

        for (final String[] knownBad : knownBads) {
            final String msg = "for secret '" + knownBad[0] + "' and domain '" + knownBad[1]
                    + "' of length " + knownBad[2];
            try {
                pwc.generate(knownBad[0], knownBad[1], Integer.parseInt(knownBad[2]));
                fail("Expecting exception " + msg);
            } catch (final PasswordGenerationException e) {

            }
        }
    }

    public void testLength() throws NoSuchAlgorithmException, IOException {
        final PasswordComposer pwc = new PasswordComposer(getContext());

        int i = 0;
        try {
            for (i = 1; i < 32; i++) {
                final String s = pwc.generate("12345", "example.org", i);
                assertEquals(i, s.length());
            }
        } catch (final PasswordGenerationException e) {
            fail("got an exception for a known-good length " + i);
        }

        try {
            pwc.generate("12345", "example.org", 0);
            fail("Expecting exception to be caught for length 0");
        } catch (final PasswordGenerationException e) {

        }

        try {
            pwc.generate("12345", "example.org", 32);
            fail("Expecting exception to be caught for length 32");
        } catch (final PasswordGenerationException e) {

        }
    }

    @LargeTest
    public void testATonOfPasswords() throws PasswordGenerationException, IOException,
            NoSuchAlgorithmException {
        final PasswordComposer pwc = new PasswordComposer(getContext());
        Utils.testATonOfPasswords(pwc, 3, 8);
    }
}
