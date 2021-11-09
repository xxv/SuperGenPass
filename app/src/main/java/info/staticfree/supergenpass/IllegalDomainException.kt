package info.staticfree.supergenpass

class IllegalDomainException(string: String?) : PasswordGenerationException(string) {
    companion object {
        private const val serialVersionUID = 1896452619111521996L
    }
}