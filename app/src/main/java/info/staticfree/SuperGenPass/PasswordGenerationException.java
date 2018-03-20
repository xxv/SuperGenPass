package info.staticfree.SuperGenPass;

import android.support.annotation.NonNull;

/**
 * An exception raised if there was a problem generating a password with the given criteria.
 */
public class PasswordGenerationException extends Exception {

    public PasswordGenerationException(@NonNull String string,
            @NonNull Throwable source) {
        super(string, source);
    }

    public PasswordGenerationException(String string) {
        super(string);
    }

    private static final long serialVersionUID = 6491091736643793303L;
}
