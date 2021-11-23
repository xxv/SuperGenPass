package info.staticfree.supergenpass.fragment

import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.os.Bundle
import android.text.InputType
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.integration.android.IntentIntegrator
import info.staticfree.supergenpass.R
import info.staticfree.supergenpass.db.Domain

class Preferences : PreferenceFragmentCompat() {
    private val domainCountLoaderCallbacks: LoaderManager.LoaderCallbacks<Cursor> =
        object : LoaderManager.LoaderCallbacks<Cursor> {
            override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
                return CursorLoader(
                    requireContext(), Domain.CONTENT_URI, arrayOf(),
                    null, null, null
                )
            }

            override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
                val domainCount = data.count
                if (isResumed && !isRemoving) {
                    val clear: Preference? = findPreference(PREF_CLEAR_REMEMBERED)
                    clear?.apply {
                        isEnabled = domainCount > 0
                        summary = resources
                            .getQuantityString(
                                R.plurals.pref_autocomplete_count, domainCount,
                                domainCount
                            )
                    }
                }
            }

            override fun onLoaderReset(loader: Loader<Cursor>) {}
        }

    override fun onResume() {
        super.onResume()
        LoaderManager.getInstance(this).restartLoader(0, null, domainCountLoaderCallbacks)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
        (findPreference(PREF_PW_CLEAR_TIMEOUT) as EditTextPreference?)?.setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_NUMBER
        }
        (findPreference(PREF_PW_LENGTH) as EditTextPreference?)?.setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_NUMBER
        }
        (findPreference(PREF_SCAN_SALT) as Preference?)?.onPreferenceClickListener =
            onPreferenceClickListener
        (findPreference(PREF_CLEAR_REMEMBERED) as Preference?)?.onPreferenceClickListener =
            onPreferenceClickListener
        (findPreference(PREF_GENERATE_SALT) as Preference?)?.onPreferenceClickListener =
            onPreferenceClickListener
    }

    fun scanSalt() {
        val qr = IntentIntegrator(activity)
        qr.addExtra("PROMPT_MESSAGE", getString(R.string.pref_scan_qr_code_to_load_zxing_message))
        qr.addExtra("SAVE_HISTORY", false)
        qr.initiateScan(IntentIntegrator.QR_CODE_TYPES)
    }

    private fun setSaltPref(salt: String) {
        (findPreference(PREF_PW_SALT) as EditTextPreference?)?.text = salt
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val res = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (res != null && res.contents != null) {
            val salt = res.contents
            setSaltPref(salt)
        }
    }

    private val onPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
        when (preference.key) {
            PREF_SCAN_SALT -> {
                scanSalt()
                return@OnPreferenceClickListener true
            }
            PREF_GENERATE_SALT -> {
                SaltFragment().show(childFragmentManager, "salt")
                return@OnPreferenceClickListener true
            }
            PREF_CLEAR_REMEMBERED -> {
                showClearRememberedDomainDialog()
                return@OnPreferenceClickListener true
            }
            else -> false
        }
    }

    private fun showClearRememberedDomainDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.pref_clear_remembered_domains)
            .setMessage(getString(R.string.pref_clear_domain_confirm_message))
            .setPositiveButton(getString(R.string.dialog_button_clear)) { _, _ ->
                requireContext().contentResolver.delete(Domain.CONTENT_URI, null, null)
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setCancelable(true)
            .show()
    }

    companion object {
        const val ACTION_SCAN_SALT = "info.staticfree.android.supergenpass.action.SCAN_SALT"
        const val ACTION_GENERATE_SALT = "info.staticfree.android.supergenpass.action.GENERATE_SALT"
        const val ACTION_CLEAR_STORED_DOMAINS =
            "info.staticfree.android.supergenpass.action.CLEAR_STORED_DOMAINS"
        const val PREF_PW_TYPE = "pw_type"
        const val PREF_PW_LENGTH = "pw_length"
        const val PREF_PW_SALT = "pw_salt"
        const val PREF_GENERATE_SALT = "generate_salt"
        const val PREF_CLIPBOARD = "clipboard"
        const val PREF_REMEMBER_DOMAINS = "domain_autocomplete"
        const val PREF_DOMAIN_CHECK = "domain_check"
        const val PREF_SHOW_GEN_PW = "show_gen_pw"
        const val PREF_PW_CLEAR_TIMEOUT = "pw_clear_timeout"
        const val PREF_CLEAR_REMEMBERED = "clear_remembered"
        const val PREF_SHOW_PIN = "show_pin"
        const val PREF_SCAN_SALT = "scan_salt"
        const val PREF_PIN_DIGITS = "pw_pin_digits"
        const val PREF_VISUAL_HASH = "visual_hash"

        @JvmStatic
        fun getStringAsInteger(prefs: SharedPreferences, key: String?, def: Int): Int {
            val defString = def.toString()
            return try {
                prefs.getString(key, defString)!!.toInt()

                // in case the value ever gets corrupt, reset it to the default instead of freaking out
            } catch (e: NumberFormatException) {
                prefs.edit().putString(key, defString).apply()
                def
            }
        }
    }
}