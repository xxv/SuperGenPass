package info.staticfree.SuperGenPass.test;

import info.staticfree.SuperGenPass.PasswordGenerationException;
import info.staticfree.SuperGenPass.hashes.HotpPin;

import java.io.IOException;

import android.test.AndroidTestCase;

public class TestHotpPin extends AndroidTestCase {

    public void testHotpPin() throws PasswordGenerationException, IOException {
        HotpPin pinGen;
        pinGen = new HotpPin(mContext);

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

        for (int i = 3; i <= 8; i++) {
            assertTrue(pinGen.generate("foo", "example.org", i).length() == i);
        }

        final String[] badPins = new String[] { "0000", "1111", "1234", "1984", "2001", "1122",
                "553388", "1234567", "8844", "9876", "9753" };

        for (final String badPin : badPins) {
            assertTrue(pinGen.isBadPin(badPin));
        }

        final String[] goodPins = new String[] { "1837", "7498", "8347", "7426", "7172", "9012",
                "8493" };

        for (final String goodPin : goodPins) {
            assertFalse(pinGen.isBadPin(goodPin));
        }
    }
}
