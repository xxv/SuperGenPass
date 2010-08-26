package info.staticfree.SuperGenPass;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Preferences extends PreferenceActivity {
	public static final String
		PREF_SHOW_GEN_PW = "show_gen_pw",
		PREF_PW_CLEAR_TIMEOUT = "pw_clear_timeout";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);
	}
}
