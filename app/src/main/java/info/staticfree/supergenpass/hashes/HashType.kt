package info.staticfree.supergenpass.hashes

import info.staticfree.supergenpass.R
import info.staticfree.supergenpass.fragment.Preferences

sealed interface HashType {
    fun getDescriptionResId(): Int
    fun getPreferenceKey(): String

    companion object {
        fun getHashTypeForKey(hashType: String) = when (hashType) {
            Preferences.TYPE_SGP_MD5 -> SgpMd5Type
            Preferences.TYPE_SGP_SHA_512 -> SgpSha512Type
            Preferences.TYPE_PWC -> PasswordComposerType
            else -> SgpMd5Type
        }

        fun getHash(type: HashType, normalizer: DomainNormalizer, checkDomain: Boolean) =
            when (type) {
                SgpMd5Type -> SuperGenPass(normalizer, SuperGenPass.HASH_MD5, checkDomain)
                SgpSha512Type -> SuperGenPass(normalizer, SuperGenPass.HASH_SHA_512, checkDomain)
                PasswordComposerType -> PasswordComposer(normalizer, checkDomain)
            }
    }
}

/**
 * SuperGenPass (MD5)
 */
object SgpMd5Type : HashType {
    override fun getDescriptionResId() = R.string.sgp_type
    override fun getPreferenceKey() = Preferences.TYPE_SGP_MD5
}

/**
 * SuperGenPass (SHA-512)
 */
object SgpSha512Type : HashType {
    override fun getDescriptionResId() = R.string.sgp_type_sha_512
    override fun getPreferenceKey() = Preferences.TYPE_SGP_SHA_512
}

/**
 * Password Composer (highly deprecated)
 */
object PasswordComposerType : HashType {
    override fun getDescriptionResId() = R.string.pwc_type
    override fun getPreferenceKey() = Preferences.TYPE_PWC
}