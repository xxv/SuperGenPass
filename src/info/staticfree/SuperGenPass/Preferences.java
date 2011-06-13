package info.staticfree.SuperGenPass;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class Preferences extends PreferenceActivity {
	public static final String
		PREF_PW_TYPE          = "pw_type",
		PREF_PW_LENGTH        = "pw_length",
		PREF_PW_SALT          = "pw_salt",
		PREF_CLIPBOARD        = "clipboard",
		PREF_REMEMBER_DOMAINS = "domain_autocomplete",
		PREF_DOMAIN_NOCHECK   = "domain_nocheck",
		PREF_SHOW_GEN_PW      = "show_gen_pw",
		PREF_PW_CLEAR_TIMEOUT = "pw_clear_timeout",
		PREF_VISUAL_HASH      = "visual_hash";

	// idea borrowed from
	// http://stackoverflow.com/questions/3206765/number-preferences-in-preference-activity-in-android
	private final OnPreferenceChangeListener integerConformCheck = new OnPreferenceChangeListener() {

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			if (!isInteger(newValue)){
				Toast.makeText(getApplicationContext(), R.string.pref_err_not_number, Toast.LENGTH_LONG).show();
				return false;
			}
			return true;
		}
	};

	public boolean isInteger(Object newValue){
		try {
			Integer.parseInt((String) newValue);
		}catch (final NumberFormatException e) {
			return false;
		}
		return true;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);
		findPreference(PREF_PW_CLEAR_TIMEOUT).setOnPreferenceChangeListener(integerConformCheck);
		findPreference(PREF_PW_LENGTH).setOnPreferenceChangeListener(integerConformCheck);
	}

	public static int getStringAsInteger(SharedPreferences prefs, String key, int def){
		final String defString = Integer.toString(def);
		int retval;
		try{
    		retval = Integer.parseInt(prefs.getString(key, defString));

		// in case the value ever gets corrupt, reset it to the default instead of freaking out
    	}catch(final NumberFormatException e){
    		prefs.edit().putString(key, defString).commit();
    		retval = def;
    	}
    	return retval;
	}
}