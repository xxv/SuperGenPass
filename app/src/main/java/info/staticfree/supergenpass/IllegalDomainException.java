package info.staticfree.supergenpass;

public class IllegalDomainException extends PasswordGenerationException {

    private static final long serialVersionUID = 1896452619111521996L;

    public IllegalDomainException(String string) {
        super(string);
    }
}
