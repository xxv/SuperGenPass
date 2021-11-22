package info.staticfree.supergenpass.test;


import androidx.test.ext.junit.runners.AndroidJUnit4;


import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import info.staticfree.supergenpass.hashes.PasswordGenerationException;
import info.staticfree.supergenpass.hashes.HashAlgorithm;
import info.staticfree.supergenpass.hashes.SuperGenPass;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class SuperGenPassTest {

    @Test
    public void testKnownGoods() throws PasswordGenerationException {

        SuperGenPass sgp = new SuperGenPass(getApplicationContext(), HashAlgorithm.MD5);
        sgp.setCheckDomain(true);

        // these were generated using SGP's javascript itself by hand.

        //@formatter:off
        String[][] knownGoods = new String[][] {
                // basics
                { "a", "example.org", "10", "bieCWgE99X" },
                { "12345", "example.org", "10", "tHR8hvgs1D" },
                { "12345", "example.org", "24", "tHR8hvgs1DHOlfCScJlzsQAA" },
                { "12345", "example.org", "4", "tHR8" },

                // special characters in the password
                { "♥", "example.org", "10", "gd9hJAzinf" },
                { "flambé", "example.org", "10", "vqBh5a76L8" },
                { " ", "example.org", "10", "vKwe7kuLl8" },
                { "foo bar", "example.org", "10", "nZq3XcpQx2" },
                { " foo bar ", "example.org", "10", "qnyA2kcKz8" },

                // domain processing
                { "12345", "www.example.org", "10", "tHR8hvgs1D" },
                { "12345", "example.co.uk", "10", "fdQnYi75VT" },
                { "12345", "www.example.co.uk", "10", "fdQnYi75VT" },
        };
        //@formatter:on

        for (String[] knownGood : knownGoods) {
            String msg = "for secret '" + knownGood[0] + "' and domain '" + knownGood[1] +
                    "' of length " + knownGood[2];
            assertEquals(msg, knownGood[3],
                    sgp.generate(knownGood[0], knownGood[1], Integer.parseInt(knownGood[2])));
        }
    }

    @Test
    public void testInvalidOptions_checkDomain() throws Exception {
        SuperGenPass sgp = new SuperGenPass(getApplicationContext(), HashAlgorithm.MD5);
        sgp.setCheckDomain(true);

        // bad domain
        boolean caught = false;
        try {
            sgp.generate("12345", "bad domain", 10);
        } catch (PasswordGenerationException e) {
            caught = true;
        }
        assertTrue("exception thrown", caught);
    }

    @Test
    public void testInvalidOptions_tooShort() throws Exception {
        SuperGenPass sgp = new SuperGenPass(getApplicationContext(), HashAlgorithm.MD5);
        sgp.setCheckDomain(true);
        boolean caught = false;
        try {
            sgp.generate("12345", "example.org", 0);
        } catch (PasswordGenerationException e) {
            caught = true;
        }
        assertTrue("exception thrown", caught);
    }

    @Test
    public void testInvalidOptions_tooLong() throws Exception {
        SuperGenPass sgp = new SuperGenPass(getApplicationContext(), HashAlgorithm.MD5);
        sgp.setCheckDomain(true);
        // too long length
        boolean caught = false;
        try {
            sgp.generate("12345", "example.org", 100);
        } catch (PasswordGenerationException e) {
            caught = true;
        }
        assertTrue("exception thrown", caught);
    }

    @Test
    public void testSha1() throws NoSuchAlgorithmException, IOException {
        SuperGenPass sgp = new SuperGenPass(getApplicationContext(), HashAlgorithm.SHA1);
        sgp.setCheckDomain(true);
    }

    @Test
    public void testATonOfPasswordsSha1()
            throws PasswordGenerationException, IOException, NoSuchAlgorithmException {
        SuperGenPass sgp = new SuperGenPass(getApplicationContext(), HashAlgorithm.SHA1);
        Utils.testATonOfPasswords(sgp, 4, 10);
    }

    @Test
    public void testATonOfPasswordsMd5()
            throws PasswordGenerationException, IOException, NoSuchAlgorithmException {
        SuperGenPass sgp = new SuperGenPass(getApplicationContext(), HashAlgorithm.SHA1);
        Utils.testATonOfPasswords(sgp, 4, 10);
    }
}
