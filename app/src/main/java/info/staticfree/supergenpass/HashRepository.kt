package info.staticfree.supergenpass

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import info.staticfree.supergenpass.hashes.DomainBasedHash
import info.staticfree.supergenpass.hashes.HashAlgorithm
import info.staticfree.supergenpass.hashes.HotpPin
import info.staticfree.supergenpass.hashes.SuperGenPass

class HashRepository() {
    private val normalizer = DomainNormalizer()
    private var hash: DomainBasedHash = SuperGenPass(normalizer, HashAlgorithm.MD5)
    private var pinHash = HotpPin(normalizer)
    private var length = 10
    private var salt = ""
    private var rememberDomains = true

    private var pinDigits = MutableLiveData<Int>()
    private val showOutput = MutableLiveData<Boolean>()

    private lateinit var prefs: SharedPreferences

    suspend fun loadDomains(resources: Resources) {
        normalizer.loadDomains(resources)
    }

    fun loadFromPreferences(context: Context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        prefs.registerOnSharedPreferenceChangeListener(onSharedPrefsChange)
        loadFromPreferences(prefs)
    }

    fun generate(masterPass: String, domain: String) =
        hash.generate(masterPass + salt, domain, length)

    fun generatePin(masterPassword: String, domain: String) =
        pinDigits.value?.let { pinHash.generate(masterPassword + salt, domain, it) }

    fun saveDomainIfEnabled(contentResolver: ContentResolver, domain: String) {
        if (rememberDomains) {
            RememberedDomainProvider.addRememberedDomain(contentResolver, domain)
        }
    }

    fun getShowOutput(): LiveData<Boolean> = showOutput

    fun setShowOutput(showOutput: Boolean) {
        prefs.edit { putBoolean(Preferences.PREF_SHOW_GEN_PW, showOutput) }
    }

    fun getPinDigits(): LiveData<Int> = pinDigits

    fun setPinDigits(pinDigits: Int) {
        prefs.edit() {
            putInt(Preferences.PREF_PIN_DIGITS, pinDigits)
        }
    }

    private fun loadFromPreferences(prefs: SharedPreferences) {
        // when adding items here, make sure default values are in sync with the xml file
        val pwType = prefs.getString(Preferences.PREF_PW_TYPE, SuperGenPass.TYPE_MD5)
        setAlgorithm(
            when (pwType) {
                SuperGenPass.TYPE_MD5 -> HashAlgorithm.MD5
                SuperGenPass.TYPE_SHA_512 -> HashAlgorithm.SHA512
                else -> HashAlgorithm.MD5
            }
        )
        hash.setCheckDomain(prefs.getBoolean(Preferences.PREF_DOMAIN_CHECK, true))
        length = Preferences.getStringAsInteger(prefs, Preferences.PREF_PW_LENGTH, 10)

        salt = prefs.getString(Preferences.PREF_PW_SALT, "") ?: ""

        pinDigits.value = prefs.getInt(Preferences.PREF_PIN_DIGITS, 4)

        rememberDomains = prefs.getBoolean(Preferences.PREF_REMEMBER_DOMAINS, true)

        showOutput.value = prefs.getBoolean(Preferences.PREF_SHOW_GEN_PW, false)
    }

    private val onSharedPrefsChange =
        SharedPreferences.OnSharedPreferenceChangeListener { prefs, _ -> loadFromPreferences(prefs) }

    private fun setAlgorithm(algorithm: HashAlgorithm) {
        hash = SuperGenPass(normalizer, algorithm)
    }

}