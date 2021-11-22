package info.staticfree.supergenpass.fragment;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import info.staticfree.supergenpass.db.Domain;
import info.staticfree.supergenpass.R;

public class Preferences extends PreferenceFragment {

    public static final String ACTION_SCAN_SALT =
            "info.staticfree.android.supergenpass.action.SCAN_SALT";
    public static final String ACTION_GENERATE_SALT =
            "info.staticfree.android.supergenpass.action.GENERATE_SALT";

    public static final String ACTION_CLEAR_STORED_DOMAINS =
            "info.staticfree.android.supergenpass.action.CLEAR_STORED_DOMAINS";

    public static final String PREF_PW_TYPE = "pw_type";
    public static final String PREF_PW_LENGTH = "pw_length";
    public static final String PREF_PW_SALT = "pw_salt";
    public static final String PREF_GENERATE_SALT = "generate_salt";
    public static final String PREF_CLIPBOARD = "clipboard";
    public static final String PREF_REMEMBER_DOMAINS = "domain_autocomplete";
    public static final String PREF_DOMAIN_CHECK = "domain_check";
    public static final String PREF_SHOW_GEN_PW = "show_gen_pw";
    public static final String PREF_PW_CLEAR_TIMEOUT = "pw_clear_timeout";
    public static final String PREF_CLEAR_REMEMBERED = "clear_remembered";
    public static final String PREF_SHOW_PIN = "show_pin";
    public static final String PREF_SCAN_SALT = "scan_salt";
    public static final String PREF_PIN_DIGITS = "pw_pin_digits";
    public static final String PREF_VISUAL_HASH = "visual_hash";

    // idea borrowed from
    // http://stackoverflow.com/questions/3206765/number-preferences-in-preference-activity-in
    // -android
    private final OnPreferenceChangeListener integerConformCheck =
            new OnPreferenceChangeListener() {

                @Override
                public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                    if (!isInteger(newValue)) {
                        Toast.makeText(getActivity().getApplicationContext(),
                                R.string.pref_err_not_number, Toast.LENGTH_LONG).show();
                        return false;
                    }
                    return true;
                }
            };

    private final LoaderManager.LoaderCallbacks<Cursor> mDomainCountLoaderCallbacks =
            new LoaderManager.LoaderCallbacks<Cursor>() {

                @Override
                public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                    return new CursorLoader(getActivity(), Domain.CONTENT_URI, new String[] {},
                            null, null, null);
                }

                @Override
                public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                    int domainCount = data.getCount();
                    if (isResumed() && !isRemoving()) {
                        Preference clear = findPreference(PREF_CLEAR_REMEMBERED);
                        clear.setEnabled(domainCount > 0);
                        clear.setSummary(getResources()
                                .getQuantityString(R.plurals.pref_autocomplete_count, domainCount,
                                        domainCount));
                    }
                }

                @Override
                public void onLoaderReset(Loader<Cursor> loader) {

                }
            };

    public boolean isInteger(Object newValue) {
        try {
            //noinspection ResultOfMethodCallIgnored
            Integer.parseInt((String) newValue);
        } catch (@NonNull NumberFormatException e) {
            return false;
        }
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        findPreference(PREF_PW_CLEAR_TIMEOUT).setOnPreferenceChangeListener(integerConformCheck);
        findPreference(PREF_PW_LENGTH).setOnPreferenceChangeListener(integerConformCheck);

        findPreference(PREF_SCAN_SALT).setOnPreferenceClickListener(mOnPreferenceClickListener);
        findPreference(PREF_CLEAR_REMEMBERED)
                .setOnPreferenceClickListener(mOnPreferenceClickListener);
        findPreference(PREF_GENERATE_SALT).setOnPreferenceClickListener(mOnPreferenceClickListener);
    }

    @Override
    public void onResume() {
        super.onResume();

        getLoaderManager().restartLoader(0, null, mDomainCountLoaderCallbacks);
    }

    public void scanSalt() {
        IntentIntegrator qr = new IntentIntegrator(getActivity());
        qr.addExtra("PROMPT_MESSAGE", getString(R.string.pref_scan_qr_code_to_load_zxing_message));
        qr.addExtra("SAVE_HISTORY", false);
        qr.initiateScan(IntentIntegrator.QR_CODE_TYPES);
    }

    private void setSaltPref(String salt) {
        ((EditTextPreference) findPreference(PREF_PW_SALT)).setText(salt);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
            @NonNull Intent data) {
        IntentResult res =
                IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (res != null && res.getContents() != null) {
            String salt = res.getContents();
            setSaltPref(salt);
        }
    }

    public static int getStringAsInteger(@NonNull SharedPreferences prefs, String key,
            int def) {
        String defString = Integer.toString(def);
        int retval;
        try {
            retval = Integer.parseInt(prefs.getString(key, defString));

            // in case the value ever gets corrupt, reset it to the default instead of freaking out
        } catch (@NonNull NumberFormatException e) {
            prefs.edit().putString(key, defString).apply();
            retval = def;
        }
        return retval;
    }

    private final Preference.OnPreferenceClickListener mOnPreferenceClickListener =
            new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    switch (preference.getKey()) {
                        case PREF_SCAN_SALT:
                            scanSalt();
                            return true;
                        case PREF_GENERATE_SALT:
                            new SaltFragment().show(getFragmentManager(), "salt");
                            return true;
                        case PREF_CLEAR_REMEMBERED:
                            getActivity().getContentResolver()
                                    .delete(Domain.CONTENT_URI, null, null);
                            return true;
                    }

                    return false;
                }
            };

}
