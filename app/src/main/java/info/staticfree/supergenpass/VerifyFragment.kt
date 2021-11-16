package info.staticfree.supergenpass

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.text.TextWatcher
import android.text.Editable
import android.widget.Toast
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import java.math.BigInteger
import java.security.MessageDigest

/**
 * A Dialog that verifies that the master password was typed correctly.
 */
class VerifyFragment : DialogFragment() {
    private var passwordToCheck = ""
    private var pwVerify: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        passwordToCheck = arguments?.getString(ARG_PASSWORD, "") ?: ""
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity).apply {
            setTitle(R.string.dialog_verify_title)
            setCancelable(true)
            setNegativeButton(
                android.R.string.cancel
            ) { dialog, _ -> dialog.cancel() }
        }

        val inflater = LayoutInflater.from(activity)
        val pwVerifyLayout = inflater.inflate(R.layout.master_pw_verify, view as ViewGroup?)
        pwVerify = pwVerifyLayout.findViewById(R.id.verify)

        pwVerify?.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (passwordToCheck.isNotEmpty() && passwordToCheck == hashPassword(s.toString())) {
                    dialog?.dismiss()
                    Toast.makeText(
                        activity?.applicationContext,
                        R.string.toast_verify_success, Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
        builder.setView(pwVerifyLayout)
        val d: Dialog = builder.create()
        // This is added below to ensure that the soft input doesn't get hidden if it's
        // showing, which seems to be the default for dialogs.
        val window = d.window
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED)

        pwVerify?.requestFocus()

        return d
    }

    companion object {
        private const val ARG_PASSWORD = "password"

        fun hashPassword(password: String): String {
            val digester = MessageDigest.getInstance("sha-512")

            return BigInteger(1, digester.digest(password.toByteArray())).toString(16)
        }

        /**
         * Shows the password verification dialog
         *
         * @param fragmentManager  Activity's fragment manager
         * @param passwordToVerify the password that must be entered to dismiss the dialog
         */
        @JvmStatic
        fun showVerifyFragment(
            fragmentManager: FragmentManager,
            passwordToVerify: String
        ) {
            val vf = VerifyFragment()
            val args = Bundle()
            args.putString(ARG_PASSWORD, hashPassword(passwordToVerify))
            vf.arguments = args
            vf.show(fragmentManager, VerifyFragment::class.java.simpleName)
        }
    }
}