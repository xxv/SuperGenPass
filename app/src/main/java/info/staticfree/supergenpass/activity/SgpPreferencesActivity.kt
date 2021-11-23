package info.staticfree.supergenpass.activity

import android.os.Bundle
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import info.staticfree.supergenpass.db.Domain
import info.staticfree.supergenpass.fragment.Preferences
import info.staticfree.supergenpass.R
import info.staticfree.supergenpass.fragment.SaltFragment

class SgpPreferencesActivity : FragmentActivity(R.layout.preference_activity) {
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        when (intent.action) {
            Preferences.ACTION_SCAN_SALT -> {
                val preferences =
                    supportFragmentManager.findFragmentByTag(Preferences::class.java.name) as Preferences
                preferences.scanSalt()
            }
            Preferences.ACTION_GENERATE_SALT -> {
                SaltFragment().show(supportFragmentManager, "salt")
            }
            Preferences.ACTION_CLEAR_STORED_DOMAINS -> {
                contentResolver.delete(Domain.CONTENT_URI, null, null)
            }
        }
    }
}