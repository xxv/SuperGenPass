package info.staticfree.supergenpass.activity

import android.os.Bundle
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import info.staticfree.supergenpass.db.Domain
import info.staticfree.supergenpass.fragment.Preferences
import info.staticfree.supergenpass.R
import info.staticfree.supergenpass.fragment.SaltFragment

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
                SaltFragment()
                    .show(fragmentManager, "salt")
            }
            Preferences.ACTION_CLEAR_STORED_DOMAINS == action -> {
                contentResolver.delete(Domain.CONTENT_URI, null, null)
            }
        }
    }
}