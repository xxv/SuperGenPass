package info.staticfree.supergenpass.test;


import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import info.staticfree.supergenpass.hashes.PasswordGenerationException;
import info.staticfree.supergenpass.hashes.HotpPin;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class TestHotpPin {

    @Test
    public void testHotpPin() throws PasswordGenerationException, IOException {
        HotpPin pinGen = new HotpPin(getApplicationContext());

        // these are bad to give as an output

        assertTrue(pinGen.isNumericalRun("1111"));
        assertTrue(pinGen.isNumericalRun("1234"));
        assertTrue(pinGen.isNumericalRun("4321"));
        assertTrue(pinGen.isNumericalRun("2468"));
        assertTrue(pinGen.isNumericalRun("0000"));
        assertTrue(pinGen.isNumericalRun("9999"));
        assertTrue(pinGen.isNumericalRun("0369"));

        assertFalse(pinGen.isNumericalRun("0101"));
        assertFalse(pinGen.isNumericalRun("1235"));

        assertEquals("3097", pinGen.generate("foo", "example.org", 4));

    }

    @Test
    public void testDomainFiltering() throws IOException, PasswordGenerationException {
        HotpPin pinGen = new HotpPin(getApplicationContext());
        pinGen.setCheckDomain(true);
        assertEquals(pinGen.generate("foo", "foo.example.org", 4),
                pinGen.generate("foo", "example.org", 4));
    }

    @Test
    public void testDomainFilteringOff() throws IOException, PasswordGenerationException {
        HotpPin pinGen = new HotpPin(getApplicationContext());
        pinGen.setCheckDomain(false);
        assertFalse(pinGen.generate("foo", "foo.example.org", 4).equals(
                pinGen.generate("foo", "example.org", 4)));
    }

    @Test
    public void testNumericRuns() throws IOException {
        HotpPin pinGen = new HotpPin(getApplicationContext());

        // these are bad to give as an output

        assertTrue(pinGen.isNumericalRun("1111"));
        assertTrue(pinGen.isNumericalRun("1234"));
        assertTrue(pinGen.isNumericalRun("4321"));
        assertTrue(pinGen.isNumericalRun("2468"));
        assertTrue(pinGen.isNumericalRun("0000"));
        assertTrue(pinGen.isNumericalRun("9999"));
        assertTrue(pinGen.isNumericalRun("0369"));

        // these aren't runs

        assertFalse(pinGen.isNumericalRun("0101"));
        assertFalse(pinGen.isNumericalRun("1235"));
    }

    @Test
    public void testIncompleteNumericRuns() throws IOException {
        HotpPin pinGen = new HotpPin(getApplicationContext());

        // these are bad to give as an output

        assertTrue(pinGen.isIncompleteNumericalRun("1111"));
        assertTrue(pinGen.isIncompleteNumericalRun("1113"));
        assertTrue(pinGen.isIncompleteNumericalRun("3111"));
        assertTrue(pinGen.isIncompleteNumericalRun("10001"));
        assertTrue(pinGen.isIncompleteNumericalRun("011101"));

        // these aren't runs
        assertFalse(pinGen.isIncompleteNumericalRun("0010"));
        assertFalse(pinGen.isIncompleteNumericalRun("1234"));

    }

    @Test
    public void testGeneratedLength() throws PasswordGenerationException, IOException {
        HotpPin pinGen = new HotpPin(getApplicationContext());

        for (int i = 3; i <= 8; i++) {
            assertTrue(pinGen.generate("foo", "example.org", i).length() == i);
        }
    }

    @Test
    public void testInvalidLengths() throws IOException {
        HotpPin pinGen = new HotpPin(getApplicationContext());
        testInvalidLength(pinGen, -1);
        testInvalidLength(pinGen, 0);
        testInvalidLength(pinGen, 1);
        testInvalidLength(pinGen, 2);
        testInvalidLength(pinGen, 9);
        testInvalidLength(pinGen, 100);
    }

    private void testInvalidLength(@NonNull HotpPin pinGen, int len) {
        boolean thrown = false;
        try {
            pinGen.generate("foo", "example.org", len);
        } catch (@NonNull PasswordGenerationException e) {
            thrown = true;
        }
        assertTrue("exception not thrown for length " + len, thrown);
    }

    @Test
    public void testBadPins() throws IOException {
        HotpPin pinGen = new HotpPin(getApplicationContext());
        String[] badPins = new String[] { "0000", "1111", "1234", "1984", "2001", "1122",
                "553388", "1234567", "8844", "9876", "9753", "2000", "8000", "10001", "4111",
                "0007", "90210", "1004", "8068", "90210" };

        for (String badPin : badPins) {
            assertTrue("bad PIN: " + badPin + " not detected to be bad", pinGen.isBadPin(badPin));
        }
    }

    @Test
    public void testGoodPins() throws IOException {
        HotpPin pinGen = new HotpPin(getApplicationContext());
        String[] goodPins = new String[] { "1837", "7498", "8347", "7426", "7172", "9012",
                "8493", "400500", "4385719", "12349" };

        for (String goodPin : goodPins) {
            assertFalse(pinGen.isBadPin(goodPin));
        }
    }

    @Test
    @LargeTest
    public void testATonOfPasswords() throws PasswordGenerationException, IOException {
        HotpPin pinGen = new HotpPin(getApplicationContext());
        Utils.testATonOfPasswords(pinGen, 3, 8);
    }
}