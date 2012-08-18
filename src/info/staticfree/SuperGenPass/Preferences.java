package info.staticfree.SuperGenPass;

import java.security.SecureRandom;

import org.apache.commons.codec.binary.Base64;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

import com.google.zxing.client.android.Intents;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class Preferences extends PreferenceActivity {

	public static final String SCAN_SALT = "info.staticfree.android.supergenpass.SCAN_SALT";
	public static final String GENERATE_SALT = "info.staticfree.android.supergenpass.GENERATE_SALT";

	// @formatter:off
	public static final String
		PREF_PW_TYPE            = "pw_type",
		PREF_PW_LENGTH          = "pw_length",
		PREF_PW_SALT            = "pw_salt",
		PREF_CLIPBOARD          = "clipboard",
		PREF_REMEMBER_DOMAINS   = "domain_autocomplete",
		PREF_DOMAIN_HAS_CONTENT = "domain_has_content",
		PREF_DOMAIN_NOCHECK     = "domain_nocheck",
		PREF_SHOW_GEN_PW        = "show_gen_pw",
		PREF_PW_CLEAR_TIMEOUT   = "pw_clear_timeout",
		PREF_VISUAL_HASH        = "visual_hash";

	// @formatter:on

	// idea borrowed from
	// http://stackoverflow.com/questions/3206765/number-preferences-in-preference-activity-in-android
	private final OnPreferenceChangeListener integerConformCheck = new OnPreferenceChangeListener() {

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			if (!isInteger(newValue)) {
				Toast.makeText(getApplicationContext(), R.string.pref_err_not_number,
						Toast.LENGTH_LONG).show();
				return false;
			}
			return true;
		}
	};

	public boolean isInteger(Object newValue) {
		try {
			Integer.parseInt((String) newValue);
		} catch (final NumberFormatException e) {
			return false;
		}
		return true;
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);
		findPreference(PREF_PW_CLEAR_TIMEOUT).setOnPreferenceChangeListener(integerConformCheck);
		findPreference(PREF_PW_LENGTH).setOnPreferenceChangeListener(integerConformCheck);

	}

	@Override
	protected void onNewIntent(Intent intent) {

		final String action = intent.getAction();

		if (SCAN_SALT.equals(action)) {
			scanSalt();

		} else if (GENERATE_SALT.equals(action)) {
			showDialog(DIALOG_GENERATE);
		}
	}

	private final int DIALOG_GENERATE = 100;

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			case DIALOG_GENERATE:

				return new AlertDialog.Builder(this)
						.setTitle(R.string.pref_generate_salt_title)
						.setMessage(
								R.string.pref_generate_salt_dialog_message)
						.setPositiveButton(R.string.pref_generate_salt_and_set,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog, int which) {
										generateSalt();

									}
								})
						.setCancelable(true)
						.setNegativeButton(android.R.string.cancel,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();

									}
								}).create();
			default:
				return super.onCreateDialog(id);
		}
	}

	private void scanSalt() {
		final IntentIntegrator qr = new IntentIntegrator(this);
		qr.addExtra(Intents.Scan.PROMPT_MESSAGE,
				getString(R.string.pref_scan_qr_code_to_load_zxing_message));
		qr.addExtra(Intents.Scan.SAVE_HISTORY, false);
		qr.initiateScan(IntentIntegrator.QR_CODE_TYPES);
	}

	private void generateSalt() {
		final IntentIntegrator qr = new IntentIntegrator(this);
		final SecureRandom sr = new SecureRandom();
		final byte[] salt = new byte[512];
		sr.nextBytes(salt);
		final String saltb64 = new String(Base64.encodeBase64(salt)).replaceAll("\\s", "");
		setSaltPref(saltb64);
		qr.addExtra(Intents.Encode.SHOW_CONTENTS, false);
		qr.shareText(saltb64);
	}

	private void setSaltPref(String salt) {
		((EditTextPreference) findPreference(PREF_PW_SALT)).setText(salt);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		final IntentResult res = IntentIntegrator
				.parseActivityResult(requestCode, resultCode, data);

		if (res != null && res.getContents() != null) {
			final String salt = res.getContents();
			setSaltPref(salt);
		}
	}

	public static int getStringAsInteger(SharedPreferences prefs, String key, int def) {
		final String defString = Integer.toString(def);
		int retval;
		try {
			retval = Integer.parseInt(prefs.getString(key, defString));

			// in case the value ever gets corrupt, reset it to the default instead of freaking out
		} catch (final NumberFormatException e) {
			prefs.edit().putString(key, defString).commit();
			retval = def;
		}
		return retval;
	}
}