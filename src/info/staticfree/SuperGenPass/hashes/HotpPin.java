package info.staticfree.SuperGenPass.hashes;

import info.staticfree.SuperGenPass.PasswordGenerationException;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.openauthentication.otp.OneTimePasswordAlgorithm;

import android.content.Context;
import android.util.Log;

public class HotpPin extends DomainBasedHash {

    private static final String TAG = HotpPin.class.getSimpleName();

    public HotpPin(Context context) throws IOException {
        super(context);
    }

    @Override
    public String generate(String masterPass, String domain, int length)
            throws PasswordGenerationException {

        if (length < 3 || length > 8) {
            throw new PasswordGenerationException("length must be >= 3 and <= 8");
        }
        try {
            String pin = OneTimePasswordAlgorithm.generateOTPFromText(masterPass.getBytes(),
                    domain.getBytes(), length, false, -1);
            final int suffix = 0;
            int loopOverrun = 0;
            while (isBadPin(pin)) {
                final String suffixedDomain = domain + " " + suffix;
                pin = OneTimePasswordAlgorithm.generateOTPFromText(masterPass.getBytes(),
                        suffixedDomain.getBytes(), length, false, -1);
                loopOverrun++;
                if (loopOverrun > 100) {
                    throw new PasswordGenerationException(
                            "Programming error: looped too many times");
                }
            }
            return pin;
        } catch (final InvalidKeyException e) {
            Log.e(TAG, "HotpPin generation error", e);
            return null;
        } catch (final NoSuchAlgorithmException e) {
            Log.e(TAG, "HotpPin generation error", e);
            return null;
        }
    }

    /**
     * Tests the string to see if it contains a numeric run. For example, "123456", "0000", "9876",
     * and "2468" would all match.
     *
     * @param pin
     * @return true if the string is a numeric run
     */
    public boolean isNumericalRun(String pin) {
        final int len = pin.length();
        // int[] diff = new int[len - 1];
        int prevDigit = Character.digit(pin.charAt(0), 10);
        int prevDiff = Integer.MAX_VALUE;
        boolean isRun = true; // assume it's true...

        for (int i = 1; isRun && i < len; i++) {
            final int digit = Character.digit(pin.charAt(i), 10);

            final int diff = digit - prevDigit;
            if (prevDiff != Integer.MAX_VALUE && diff != prevDiff) {
                isRun = false; // ... and prove it's false
            }

            prevDiff = diff;
            prevDigit = digit;
        }

        return isRun;
    }

    /**
     * Tests to see if the PIN is a "bad" pin. That is, one that is easily guessable. Essentially,
     * this is a blacklist of the most commonly used PINs like "1234", "0000" and "1984".
     *
     * @param pin
     * @return true if the PIN matches the bad PIN criteria
     */
    public boolean isBadPin(String pin) {
        final int len = pin.length();

        // special cases for 4-digit PINs (which are quite common)
        if (len == 4) {
            final int start = Integer.parseInt(pin.subSequence(0, 2).toString());
            final int end = Integer.parseInt(pin.subSequence(2, 4).toString());

            // 19xx pins look like years, so might as well ditch them.
            if (start == 19 || (start == 20 && end < 30)) {
                return true;
            }

            // 1515
            if (start == end) {
                return true;
            }

            // PINs that don't otherwise match the patterns from the top 20 list here:
            // http://www.datagenetics.com/blog/september32012/index.html
            if (start == 10 && end == 4) {
                return true;
            }
        }

        // find case where all digits are in pairs
        // eg 1122 3300447722

        if (len % 2 == 0){
            boolean paired = true;
            for (int i = 0; i < len - 1 ; i += 2){
                if (pin.charAt(i) != pin.charAt(i + 1)) {
                    paired = false;
                }
            }
            if (paired) {
                return true;
            }
        }

        if (isNumericalRun(pin)) {
            return true;
        }

        return false;
    }
}
