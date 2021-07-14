package info.staticfree.supergenpass;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

public class SgpPreferencesActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.preference_activity);
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        String action = intent.getAction();

        Preferences preferences =
                (Preferences) getFragmentManager().findFragmentByTag(Preferences.class.getName());

        if (Preferences.ACTION_SCAN_SALT.equals(action)) {
            preferences.scanSalt();
        } else if (Preferences.ACTION_GENERATE_SALT.equals(action)) {
            new Preferences.SaltFragment().show(getFragmentManager(), "salt");
        } else if (Preferences.ACTION_CLEAR_STORED_DOMAINS.equals(action)) {
            getContentResolver().delete(Domain.CONTENT_URI, null, null);
        }
    }
}
