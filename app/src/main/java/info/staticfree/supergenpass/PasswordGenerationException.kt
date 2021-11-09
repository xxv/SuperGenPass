package info.staticfree.supergenpass

/**
 * An exception raised if there was a problem generating a password with the given criteria.
 */
open class PasswordGenerationException : Exception {
    constructor(
        string: String,
        source: Throwable
    ) : super(string, source) {
    }

    constructor(string: String?) : super(string)

    companion object {
        private const val serialVersionUID = 6491091736643793303L
    }
}