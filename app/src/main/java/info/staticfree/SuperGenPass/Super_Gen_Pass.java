package info.staticfree.SuperGenPass;

/*
 Android SuperGenPass
 Copyright (C) 2009-2018  Steve Pomeroy <steve@staticfree.info>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.

 */

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AutoCompleteTextView;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import info.staticfree.SuperGenPass.hashes.DomainBasedHash;
import info.staticfree.SuperGenPass.hashes.HotpPin;
import info.staticfree.SuperGenPass.hashes.PasswordComposer;
import info.staticfree.SuperGenPass.hashes.SuperGenPass;
import info.staticfree.SuperGenPass.nfc.NfcFragment;
import info.staticfree.SuperGenPass.nfc.NfcWriteFragment;

public class Super_Gen_Pass extends Activity
        implements OnClickListener, OnLongClickListener, OnCheckedChangeListener,
        OnEditorActionListener, FilterQueryProvider {
    private static final String TAG = Super_Gen_Pass.class.getSimpleName();
    private static final int MINUTE_MS = 60000;

    private DomainBasedHash mDomainBasedHash;

    private static final int REQUEST_CODE_PREFERENCES = 200;
    private static final String STATE_LAST_STOPPED_TIME =
            "info.staticfree.SuperGenPass.STATE_LAST_STOPPED_TIME";
    private static final String STATE_SHOWING_PASSWORD =
            "info.staticfree.SuperGenPass.STATE_SHOWING_PASSWORD";
    private int mPwLength;
    @Nullable
    private String mPwSalt;
    private boolean mCopyToClipboard;
    private boolean mRememberDomains;
    private boolean mDomainCheck = true;

    private GeneratedPasswordView mGenPwView;
    private AutoCompleteTextView mDomainEdit;
    private VisualHashEditText mMasterPwEdit;

    private long mLastStoppedTime;
    private int mPwClearTimeout;

    private static final int MIN_PIN_LENGTH = 3;
    private CompoundButton mShowGenPassword;

    private GeneratedPasswordView mGenPinView;

    private Spinner mPinDigitsSpinner;

    private final BroadcastReceiver mScreenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            clearEditTexts();
            try {
                unregisterReceiver(this);
            } catch (IllegalArgumentException ignored) {
                // this has happened a few times, but this is just a clean-up so we can ignore this.
            }
        }
    };

    private boolean mClearDomain = true;
    @Nullable
    private NfcFragment mNfcFragment;
    private boolean mScreenOffReceiverRegistered;
    private boolean mHasNfc;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        Intent intent = getIntent();
        Uri data = intent.getData();

        if (savedInstanceState != null) {
            mLastStoppedTime = savedInstanceState.getLong(STATE_LAST_STOPPED_TIME, 0);
            mShowingPassword = savedInstanceState.getBoolean(STATE_SHOWING_PASSWORD, false);
        }

        initPinWidgets();

        initDomainPasswordEntry();
        initGenPassword();
        bindTextWatchers();
        initMasterPasswordHide();

        initNfc();

        loadFromPreferences();

        // sometimes the domain doesn't have focus when first started. Perhaps because of the tabs?
        mDomainEdit.requestFocus();

        // check for the "share page" intent. If present, pre-fill.

        if (data == null) {

            String maybeUrl = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (maybeUrl != null) {
                try {
                    // populate the URL and give the password entry focus
                    Uri uri = Uri.parse(maybeUrl);
                    String host = uri.getHost();

                    if (host != null) {
                        mDomainEdit.setText(mDomainBasedHash.getDomain(host));
                    }

                    mMasterPwEdit.requestFocus();
                    mClearDomain = false;
                } catch (@NonNull PasswordGenerationException e) {
                    // nothing much to be done here.
                    // Let the user figure it out.
                    Log.e(TAG, "Could not find valid URI in shared text", e);
                    Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void initNfc() {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter != null) {
            mHasNfc = true;
            mNfcFragment = (NfcFragment) getFragmentManager()
                    .findFragmentByTag(NfcFragment.class.getName());
            if (mNfcFragment == null) {
                mNfcFragment = new NfcFragmentImpl();
                getFragmentManager().beginTransaction()
                        .add(mNfcFragment, NfcFragment.class.getName()).commit();
            }
        }
    }

    private void initDomainPasswordEntry() {
        mDomainEdit = findViewById(R.id.domain_edit);

        mMasterPwEdit = findViewById(R.id.password_edit);

        mMasterPwEdit.setOnEditorActionListener(this);

        SimpleCursorAdapter adapter =
                new SimpleCursorAdapter(this, android.R.layout.simple_dropdown_item_1line, null,
                        new String[] { "domain" }, new int[] { android.R.id.text1 },
                        CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

        adapter.setFilterQueryProvider(this);
        adapter.setStringConversionColumn(DOMAIN_COLUMN);

        // initialize the autocompletion
        mDomainEdit.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                mMasterPwEdit.requestFocus();
            }
        });
        mDomainEdit.setAdapter(adapter);
    }

    private void initGenPassword() {

        mGenPwView = findViewById(R.id.password_output);
        mGenPwView.setOnLongClickListener(this);

        // hook in our buttons
        mShowGenPassword = findViewById(R.id.show_gen_password);
        mShowGenPassword.setOnCheckedChangeListener(this);
    }

    private void initPinWidgets() {
        mGenPinView = findViewById(R.id.pin_output);
        mGenPinView.setOnLongClickListener(this);

        mPinDigitsSpinner = findViewById(R.id.pin_length);
        mPinDigitsSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                mPinDigits = position + MIN_PIN_LENGTH;

                SharedPreferences prefs =
                        PreferenceManager.getDefaultSharedPreferences(Super_Gen_Pass.this);
                prefs.edit().putInt(Preferences.PREF_PIN_DIGITS, mPinDigits).apply();
                generateIfValid();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void initMasterPasswordHide() {
        CompoundButton masterPasswordHide =
                findViewById(R.id.hide_master_password);
        masterPasswordHide.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                int selStart = mMasterPwEdit.getSelectionStart();
                int selEnd = mMasterPwEdit.getSelectionEnd();
                mMasterPwEdit
                        .setTransformationMethod(b ? null : new PasswordTransformationMethod());
                mMasterPwEdit.setSelection(selStart, selEnd);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // listen for a screen off event and clear everything if received.
        if (mShowingPassword) {
            registerReceiver(mScreenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
            mScreenOffReceiverRegistered = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        mLastStoppedTime = SystemClock.elapsedRealtime();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mScreenOffReceiverRegistered) {
            unregisterReceiver(mScreenOffReceiver);
            mScreenOffReceiverRegistered = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        findViewById(R.id.tab_pin).setVisibility(mShowPin ? View.VISIBLE : View.GONE);
        // when the user has left the app for more than mPwClearTimeout minutes,
        // wipe master password and generated password.
        if (mClearDomain &&
                SystemClock.elapsedRealtime() - mLastStoppedTime > mPwClearTimeout * MINUTE_MS) {
            clearEditTexts();
        }
        mClearDomain = true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (mNfcFragment != null) {
            mNfcFragment.handleNfcIntent(intent);
        }
    }

    private void clearEditTexts() {
        mDomainEdit.getText().clear();
        mMasterPwEdit.getText().clear();
        clearGenPassword();
        mDomainEdit.requestFocus();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_LAST_STOPPED_TIME, mLastStoppedTime);
        outState.putBoolean(STATE_SHOWING_PASSWORD, mShowingPassword);
    }

    private boolean mShowingPassword;

    @Nullable
    private HotpPin mPinGen;

    private int mPinDigits;

    private boolean mShowPin;

    private void generateIfValid() {
        try {
            if (mMasterPwEdit.length() > 0 && mDomainEdit.length() > 0) {
                generateAndDisplay();
            } else {
                clearGenPassword();
            }
        } catch (@NonNull PasswordGenerationException e) {

            clearGenPassword();
        }
    }

    private void clearGenPassword() {
        if (mShowingPassword) {
            mGenPwView.setText(null);
            mGenPinView.setText(null);
            mShowingPassword = false;
            invalidateOptionsMenu();
        }
    }

    private void generateAndDisplay() throws PasswordGenerationException {
        String domain = getDomain();

        if (mDomainCheck) {
            domain = extractDomain(domain);
        }
        String masterPw = getMasterPassword() + mPwSalt;
        String genPw = mDomainBasedHash.generate(masterPw, domain, mPwLength);

        mGenPwView.setDomainName(domain);
        mGenPwView.setText(genPw);

        if (mPinGen != null && mShowPin) {
            String pin = mPinGen.generate(masterPw, domain, mPinDigits);
            mGenPinView.setDomainName(domain);
            mGenPinView.setText(pin);
        }
        mShowingPassword = true;
        invalidateOptionsMenu();
    }

    private void postGenerate(boolean copyToClipboard) {

        if (mRememberDomains) {
            RememberedDomainProvider.addRememberedDomain(getContentResolver(), getDomain());
        }

        if (copyToClipboard) {
            mGenPwView.copyToClipboard();

            if (Intent.ACTION_SEND.equals(getIntent().getAction()) &&
                    mGenPwView.getHidePassword()) {
                finish();
            }
        }
    }

    /**
     * Go! Validates the forms, computes the password, displays it, remembers the domain, and copies
     * to clipboard.
     */
    boolean go() {
        try {
            if (mMasterPwEdit.length() == 0) {
                clearGenPassword();
                mMasterPwEdit.setError(getText(R.string.err_empty_master_password));
                mMasterPwEdit.requestFocus();
                return false;
            }

            generateAndDisplay();

            postGenerate(mCopyToClipboard);
            return true;
        } catch (@NonNull IllegalDomainException e) {
            clearGenPassword();
            mDomainEdit.setError(e.getLocalizedMessage());
            mDomainEdit.requestFocus();
        } catch (@NonNull PasswordGenerationException e) {
            clearGenPassword();
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
        return false;
    }

    /**
     * @return the domain entered into the text box
     */
    @NonNull
    String getDomain() {
        AutoCompleteTextView txt = findViewById(R.id.domain_edit);
        return txt.getText().toString().trim();
    }

    @NonNull
    String getMasterPassword() {
        return mMasterPwEdit.getText().toString();
    }

    /**
     * Returns the hostname portion of the supplied URL.
     *
     * @param maybeUrl : either a hostname or a URL.
     * @return the hostname portion of maybeUrl, or null if maybeUrl was null.
     */
    String extractDomain(@NonNull String maybeUrl) {
        try {
            Uri uri = Uri.parse(maybeUrl);
            String host = uri.getHost();
            return mDomainBasedHash.getDomain(host != null ? host : "");
        } catch (@NonNull PasswordGenerationException e) {
            return maybeUrl;
        }
    }

    @Override
    public void onClick(@NonNull View v) {
        switch (v.getId()) {
            case R.id.go:
                go();
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }

    @Override
    public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.show_gen_password: {

                mGenPwView.setHidePassword(!isChecked);
                mGenPinView.setHidePassword(!isChecked);

                SharedPreferences prefs =
                        PreferenceManager.getDefaultSharedPreferences(this);
                prefs.edit().putBoolean(Preferences.PREF_SHOW_GEN_PW, isChecked).apply();
            }
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        return !go();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
        MenuItem verify = menu.findItem(R.id.verify);
        verify.setEnabled(getMasterPassword().length() != 0);
        menu.findItem(R.id.copy).setEnabled(mShowingPassword);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT || !mHasNfc) {
            menu.findItem(R.id.write_nfc).setVisible(false);
        } else {
            menu.findItem(R.id.write_nfc).setEnabled(mMasterPwEdit.getText().length() > 0);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:

                Intent preferencesIntent =
                        new Intent().setClass(this, SgpPreferencesActivity.class);
                startActivityForResult(preferencesIntent, REQUEST_CODE_PREFERENCES);

                return true;

            case R.id.about:
                new AboutFragment().show(getFragmentManager(), "about");
                return true;

            case R.id.verify:
                VerifyFragment.showVerifyFragment(getFragmentManager(), getMasterPassword());
                return true;

            case R.id.go:
                go();
                return true;

            case R.id.copy:
                postGenerate(true);
                return true;

            case R.id.write_nfc:
                writeNfc();
                return true;

            default:
                return false;
        }
    }

    private void writeNfc() {
        NfcWriteFragment writeNfc = (NfcWriteFragment) getFragmentManager()
                .findFragmentByTag(NfcWriteFragment.class.getName());

        if (writeNfc == null) {
            getFragmentManager().beginTransaction()
                    .add(NfcWriteFragment.newInstance(mMasterPwEdit.getText().toString()),
                            NfcWriteFragment.class.getName()).commit();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PREFERENCES) {
            loadFromPreferences();
        }
    }

    private void bindTextWatchers() {
        mDomainEdit.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) { }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                generateIfValid();
            }
        });

        mMasterPwEdit.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                generateIfValid();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    /**
     * Loads the preferences and updates the program state based on them.
     */
    protected void loadFromPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // when adding items here, make sure default values are in sync with the xml file
        String pwType = prefs.getString(Preferences.PREF_PW_TYPE, SuperGenPass.TYPE);
        mPwLength = Preferences.getStringAsInteger(prefs, Preferences.PREF_PW_LENGTH, 10);
        mPwSalt = prefs.getString(Preferences.PREF_PW_SALT, "");
        mCopyToClipboard = prefs.getBoolean(Preferences.PREF_CLIPBOARD, true);
        mRememberDomains = prefs.getBoolean(Preferences.PREF_REMEMBER_DOMAINS, true);

        if (prefs.contains("domain_nocheck")) {
            // Double negatives are so confusing
            boolean domainCheckDefault = !prefs.getBoolean("domain_nocheck", false);
            prefs.edit().remove("domain_nocheck")
                    .putBoolean(Preferences.PREF_DOMAIN_CHECK, domainCheckDefault).apply();
        }

        mDomainCheck = prefs.getBoolean(Preferences.PREF_DOMAIN_CHECK, true);
        mPwClearTimeout =
                Preferences.getStringAsInteger(prefs, Preferences.PREF_PW_CLEAR_TIMEOUT, 2);

        // PIN
        mPinDigits = prefs.getInt(Preferences.PREF_PIN_DIGITS, 4);
        mPinDigitsSpinner.setSelection(mPinDigits - MIN_PIN_LENGTH);
        mShowPin = prefs.getBoolean(Preferences.PREF_SHOW_PIN, true);

        try {
            switch (pwType) {
                case SuperGenPass.TYPE:
                    mDomainBasedHash = new SuperGenPass(this, SuperGenPass.HASH_ALGORITHM_MD5);

                    break;
                case SuperGenPass.TYPE_SHA_512:
                    mDomainBasedHash = new SuperGenPass(this, SuperGenPass.HASH_ALGORITHM_SHA512);

                    break;
                case PasswordComposer.TYPE:
                    mDomainBasedHash = new PasswordComposer(this);

                    break;
                default:
                    mDomainBasedHash = new SuperGenPass(this, SuperGenPass.HASH_ALGORITHM_MD5);
                    Log.e(TAG, "password type was set to unknown algorithm: " + pwType);
                    break;
            }

            mPinGen = new HotpPin(this);
        } catch (@NonNull NoSuchAlgorithmException e) {
            Log.e(TAG, "could not find MD5", e);
            Toast.makeText(getApplicationContext(),
                    String.format(getString(R.string.err_no_md5), e.getLocalizedMessage()),
                    Toast.LENGTH_LONG).show();
            finish();
        } catch (@NonNull IOException e) {
            Toast.makeText(this, getString(R.string.err_json_load, e.getLocalizedMessage()),
                    Toast.LENGTH_LONG).show();
            Log.e(TAG, getString(R.string.err_json_load), e);
            finish();
        }

        mDomainBasedHash.setCheckDomain(mDomainCheck);
        if (mPinGen != null) {
            mPinGen.setCheckDomain(mDomainCheck);
        }

        if (mDomainCheck) {
            mDomainEdit.setHint(R.string.domain_hint);
        } else {
            mDomainEdit.setHint(R.string.domain_hint_no_checking);
        }

        mMasterPwEdit.setShowVisualHash(prefs.getBoolean(Preferences.PREF_VISUAL_HASH, true));

        if (mCopyToClipboard) {
            mMasterPwEdit.setImeActionLabel(getText(android.R.string.copy), R.id.go);
        } else {
            mMasterPwEdit.setImeActionLabel(getText(R.string.done), R.id.go);
        }

        boolean showPassword = prefs.getBoolean(Preferences.PREF_SHOW_GEN_PW, false);

        mGenPwView.setHidePassword(!showPassword);
        mGenPinView.setHidePassword(!showPassword);
        mShowGenPassword.setChecked(showPassword);
    }

    private static final String[] PROJECTION = { Domain.DOMAIN, Domain._ID };
    private static final int DOMAIN_COLUMN = 0;

    // a filter that searches for domains starting with the given constraint
    @Nullable
    @Override
    public Cursor runQuery(@Nullable CharSequence constraint) {
        Cursor c;
        if (constraint == null || constraint.length() == 0) {
            c = getContentResolver()
                    .query(Domain.CONTENT_URI, PROJECTION, null, null, Domain.SORT_ORDER);
        } else {
            c = getContentResolver()
                    .query(Domain.CONTENT_URI, PROJECTION, Domain.DOMAIN + " GLOB ?",
                            new String[] { constraint + "*" }, Domain.SORT_ORDER);
        }

        return c;
    }

    /**
     * A Dialog that verifies that the master password was typed correctly.
     */
    public static class VerifyFragment extends DialogFragment {
        private static final String ARG_PASSWORD = "password";
        @NonNull
        private String mPasswordToCheck = "";

        /**
         * Shows the password verification dialog
         *
         * @param fragmentManager Activity's fragment manager
         * @param passwordToVerify the password that must be entered to dismiss the dialog
         */
        public static void showVerifyFragment(@NonNull FragmentManager fragmentManager,
                @NonNull String passwordToVerify) {
            VerifyFragment vf = new VerifyFragment();
            Bundle args = new Bundle();
            args.putString(ARG_PASSWORD, passwordToVerify);
            vf.setArguments(args);
            vf.show(fragmentManager, VerifyFragment.class.getSimpleName());
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mPasswordToCheck = getArguments().getString(ARG_PASSWORD, "");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.dialog_verify_title);
            builder.setCancelable(true);
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View pwVerifyLayout =
                    inflater.inflate(R.layout.master_pw_verify, (ViewGroup) getView());
            EditText pwVerify = pwVerifyLayout.findViewById(R.id.verify);

            builder.setNegativeButton(android.R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(@NonNull DialogInterface dialog,
                                int which) {
                            dialog.cancel();
                        }
                    });

            pwVerify.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) { }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                @Override
                public void afterTextChanged(@NonNull Editable s) {
                    if (mPasswordToCheck.length() > 0 && mPasswordToCheck.equals(s.toString())) {
                        getDialog().dismiss();
                        Toast.makeText(getActivity().getApplicationContext(),
                                R.string.toast_verify_success, Toast.LENGTH_SHORT).show();
                    }
                }
            });

            builder.setView(pwVerifyLayout);
            Dialog d = builder.create();
            // This is added below to ensure that the soft input doesn't get hidden if it's
            // showing, which seems to be the default for dialogs.
            Window window = d.getWindow();

            if (window != null) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED);
            }

            return d;
        }
    }

    /**
     * The about dialog
     */
    public static class AboutFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Builder builder = new AlertDialog.Builder(getActivity());

            builder.setTitle(R.string.about_title);
            builder.setIcon(R.drawable.icon);

            // using this instead of setMessage lets us have clickable links.
            LayoutInflater factory = LayoutInflater.from(getActivity());
            builder.setView(factory.inflate(R.layout.about, (ViewGroup) getView()));

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getDialog().dismiss();
                }
            });
            return builder.create();
        }
    }

    public static class NfcFragmentImpl extends NfcFragment {
        @Override
        public void onNfcPasswordTag(@NonNull CharSequence password) {
            ((Super_Gen_Pass) getActivity()).mMasterPwEdit.append(password);
        }
    }
}
