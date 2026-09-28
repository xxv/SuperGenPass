package info.staticfree.SuperGenPass;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;

public class SgpPreferencesActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.preference_activity);
        SystemBarInsets.setUp(this, R.id.preferences_content);
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        if (Preferences.ACTION_CLEAR_STORED_DOMAINS.equals(intent.getAction())) {
            getContentResolver().delete(Domain.CONTENT_URI, null, null);
        }
    }
}
