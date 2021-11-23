package info.staticfree.supergenpass.fragment

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import info.staticfree.supergenpass.R
import com.google.zxing.integration.android.IntentIntegrator
import info.staticfree.supergenpass.repository.HashRepository
import org.apache.commons.codec.binary.Base64
import java.security.SecureRandom
import java.util.regex.Pattern

class SaltFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(activity)
            .setTitle(R.string.pref_generate_salt_title)
            .setMessage(R.string.pref_generate_salt_dialog_message)
            .setPositiveButton(R.string.pref_generate_salt_and_set) { _, _ -> generateSaltAndShow() }
            .setCancelable(true)
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .create()

    private fun generateSaltAndShow() {
        val saltB64 = generateSalt()
        val repository = HashRepository()
        repository.loadFromPreferences(requireContext())
        repository.setSalt(saltB64)

        val qr = IntentIntegrator(activity)
        qr.addExtra("SHOW_CONTENTS", false)
        qr.shareText(saltB64)
    }

    private fun generateSalt(): String {
        val sr = SecureRandom()
        val salt = ByteArray(SALT_SIZE_BYTES)
        sr.nextBytes(salt)
        return PATTERN_WHITESPACE.matcher(String(Base64.encodeBase64(salt)))
            .replaceAll("")
    }

    companion object {
        /**
         * The size of the salt, in bytes.
         */
        private const val SALT_SIZE_BYTES = 512
        private val PATTERN_WHITESPACE = Pattern.compile("\\s")
    }
}