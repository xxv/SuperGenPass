package info.staticfree.SuperGenPass;

/**
 * An exception raised if there was a problem generating a password with the given criteria.
 *
 * @author steve
 */
public class PasswordGenerationException extends Exception {

    public PasswordGenerationException(final String string) {
        super(string);
    }

    /**
     *
     */
    private static final long serialVersionUID = 6491091736643793303L;
}
