package info.staticfree.supergenpass;

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
import android.content.BroadcastReceiver;
import android.content.Context;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AutoCompleteTextView;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CursorAdapter;
import android.widget.FilterQueryProvider;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import info.staticfree.supergenpass.activity.SgpPreferencesActivity;
import info.staticfree.supergenpass.db.Domain;
import info.staticfree.supergenpass.db.RememberedDomainProvider;
import info.staticfree.supergenpass.fragment.Preferences;
import info.staticfree.supergenpass.hashes.DomainBasedHash;
import info.staticfree.supergenpass.hashes.HotpPin;
import info.staticfree.supergenpass.hashes.IllegalDomainException;
import info.staticfree.supergenpass.hashes.PasswordGenerationException;
import info.staticfree.supergenpass.nfc.NfcFragment;
import info.staticfree.supergenpass.nfc.NfcWriteFragment;
import info.staticfree.supergenpass.view.GeneratedPasswordView;
import info.staticfree.supergenpass.view.VisualHashEditText;

@Deprecated
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
                /*try {
                    // populate the URL and give the password entry focus
                    Uri uri = Uri.parse(maybeUrl);
                    String host = uri.getHost();

                    if (host != null) {
                        // XXX TODO mDomainEdit.setText(mDomainBasedHash.getDomain(host, true));
                    }

                    mMasterPwEdit.requestFocus();
                    mClearDomain = false;
                } catch (@NonNull PasswordGenerationException e) {
                    // nothing much to be done here.
                    // Let the user figure it out.
                    Log.e(TAG, "Could not find valid URI in shared text", e);
                    Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
                 */
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
                        new String[]{"domain"}, new int[]{android.R.id.text1},
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

        mGenPwView.setText(genPw);

        if (mPinGen != null && mShowPin) {
            String pin = mPinGen.generate(masterPw, domain, mPinDigits);
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
        //try {
        Uri uri = Uri.parse(maybeUrl);
        String host = uri.getHost();
        return host;// XXX TODO mDomainBasedHash.getDomain(host != null ? host : "");
        /*} catch (@NonNull PasswordGenerationException e) {
            return maybeUrl;
        }
         */
    }

    @Override
    public void onClick(@NonNull View v) {
        switch (v.getId()) {
            case R.integer.ime_go:
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
                //new AboutFragment().show(getFragmentManager(), "about");
                return true;

            case R.id.verify:
                //VerifyFragment.showVerifyFragment(getFragmentManager(), getMasterPassword());
                return true;

            case R.integer.ime_go:
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
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    /**
     * Loads the preferences and updates the program state based on them.
     */
    protected void loadFromPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mCopyToClipboard = prefs.getBoolean(Preferences.PREF_CLIPBOARD, true);

        // when adding items here, make sure default values are in sync with the xml file
        String pwType = "";
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


        mMasterPwEdit.setShowVisualHash(prefs.getBoolean(Preferences.PREF_VISUAL_HASH, true));

        if (mCopyToClipboard) {
            mMasterPwEdit.setImeActionLabel(getText(android.R.string.copy), R.integer.ime_go);
        } else {
            mMasterPwEdit.setImeActionLabel(getText(R.string.done), R.integer.ime_go);
        }

        boolean showPassword = prefs.getBoolean(Preferences.PREF_SHOW_GEN_PW, false);

        mGenPwView.setHidePassword(!showPassword);
        mGenPinView.setHidePassword(!showPassword);
        mShowGenPassword.setChecked(showPassword);
    }

    private static final String[] PROJECTION = {Domain.DOMAIN, Domain._ID};
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
                            new String[]{constraint + "*"}, Domain.SORT_ORDER);
        }

        return c;
    }

    public static class NfcFragmentImpl extends NfcFragment {
        @Override
        public void onNfcPasswordTag(@NonNull CharSequence password) {
            ((Super_Gen_Pass) getActivity()).mMasterPwEdit.append(password);
        }
    }
}
