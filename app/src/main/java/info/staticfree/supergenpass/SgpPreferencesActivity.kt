package info.staticfree.supergenpass

import android.app.Activity
import android.os.Bundle
import info.staticfree.supergenpass.R
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import info.staticfree.supergenpass.Preferences.SaltFragment
import info.staticfree.supergenpass.Domain

class SgpPreferencesActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.preference_activity)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val action = intent.action
        val preferences = supportFragmentManager.findFragmentByTag(
            Preferences::class.java.name
        ) as Preferences

        when {
            Preferences.ACTION_SCAN_SALT == action -> {
                preferences.scanSalt()
            }
            Preferences.ACTION_GENERATE_SALT == action -> {
                SaltFragment().show(fragmentManager, "salt")
            }
            Preferences.ACTION_CLEAR_STORED_DOMAINS == action -> {
                contentResolver.delete(Domain.CONTENT_URI, null, null)
            }
        }
    }
}