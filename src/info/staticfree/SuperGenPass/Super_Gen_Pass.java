package info.staticfree.SuperGenPass;

/*
 Android SuperGenPass
 Copyright (C) 2009-2012  Steve Pomeroy <steve@staticfree.info>

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

import info.staticfree.SuperGenPass.hashes.DomainBasedHash;
import info.staticfree.SuperGenPass.hashes.HotpPin;
import info.staticfree.SuperGenPass.hashes.PasswordComposer;
import info.staticfree.SuperGenPass.hashes.SuperGenPass;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.NoSuchAlgorithmException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.widget.ToggleButton;

// TODO Wipe generated password from clipboard after delay.

// the check below is a nice reminder, however this activity uses no delayed messages,
// so nothing should be holding onto references past this activity's lifetime.
@SuppressLint("HandlerLeak")
@SuppressWarnings("deprecation")
public class Super_Gen_Pass extends TabActivity implements OnClickListener, OnLongClickListener,
        OnCheckedChangeListener, OnEditorActionListener, FilterQueryProvider {
    private final static String TAG = Super_Gen_Pass.class.getSimpleName();

    DomainBasedHash mHasher;

    // @formatter:off
    private static final int
        DIALOG_ABOUT = 100,
        DIALOG_CONFIRM_MASTER = 101;
    private static final int REQUEST_CODE_PREFERENCES = 200;
    private static final String
        STATE_LAST_STOPPED_TIME = "info.staticfree.SuperGenPass.STATE_LAST_STOPPED_TIME";
    private static final String STATE_SHOWING_PASSWORD = "info.staticfree.SuperGenPass.STATE_SHOWING_PASSWORD";
    // @formatter:on

    private int pwLength;
    private String pwType;
    private String pwSalt;
    private boolean mCopyToClipboard;
    private boolean mRememberDomains;
    private boolean noDomainCheck;

    private GeneratedPasswordView mGenPwView;
    private AutoCompleteTextView mDomainEdit;
    private VisualHashEditText mMasterPwEdit;

    private long mLastStoppedTime;
    private int pwClearTimeout;

    private ContentResolver mContentResolver;

    private static final int MSG_UPDATE_PW_VIEW = 100;

    private static final int MIN_PIN_LENGTH = 3;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_PW_VIEW:
                    generateIfValid();
                    break;
            }
        };
    };

    private ToggleButton mShowGenPassword;

    private GeneratedPasswordView mGenPinView;

    private Spinner mPinDigitsSpinner;

    private final BroadcastReceiver mScreenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            clearEditTexts();
            unregisterReceiver(this);
        }
    };

    private boolean mPleaseDontClearDomain;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        // This is disabled in versions below honeycomb, as they seem to result in corrupt displays.
        // This is fine, as the stock UI doesn't use screenshots for the task management.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        final Intent intent = getIntent();
        final Uri data = intent.getData();

        if (savedInstanceState != null) {
            mLastStoppedTime = savedInstanceState.getLong(STATE_LAST_STOPPED_TIME, 0);
            mShowingPassword = savedInstanceState.getBoolean(STATE_SHOWING_PASSWORD, false);
        }

        mContentResolver = getContentResolver();

        initTabHost();

        initPinWidgets();

        initDomainPasswordEntry();
        initGenPassword();
        bindTextWatchers();

        loadFromPreferences();

        // sometimes the domain doesn't have focus when first started. Perhaps because of the tabs?
        mDomainEdit.requestFocus();

        // check for the "share page" intent. If present, pre-fill.

        if (data == null) {

            final String maybeUrl = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (maybeUrl != null) {
                try {
                    // populate the URL and give the password entry focus
                    final Uri uri = Uri.parse(maybeUrl);

                    mDomainEdit.setText(mHasher.getDomain(uri.getHost()));
                    mMasterPwEdit.requestFocus();
                    mPleaseDontClearDomain = true;

                } catch (final Exception e) {
                    // nothing much to be done here.
                    // Let the user figure it out.
                    Log.e(TAG, "Could not find valid URI in shared text", e);
                    Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void initDomainPasswordEntry() {
        mDomainEdit = (AutoCompleteTextView) findViewById(R.id.domain_edit);

        mMasterPwEdit = ((VisualHashEditText) findViewById(R.id.password_edit));

        mMasterPwEdit.setOnEditorActionListener(this);

        final SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_dropdown_item_1line, null, new String[] { "domain" },
                new int[] { android.R.id.text1 });

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

        mGenPwView = (GeneratedPasswordView) findViewById(R.id.password_output);
        mGenPwView.setOnLongClickListener(this);

        // hook in our buttons
        mShowGenPassword = ((ToggleButton) findViewById(R.id.show_gen_password));
        mShowGenPassword.setOnCheckedChangeListener(this);
    }

    private void initPinWidgets() {
        mGenPinView = (GeneratedPasswordView) findViewById(R.id.pin_output);
        mGenPinView.setOnLongClickListener(this);

        mPinDigitsSpinner = (Spinner) findViewById(R.id.pin_length);
        mPinDigitsSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mPinDigits = position + MIN_PIN_LENGTH;

                // run on a thread as commit() can take a while.
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final SharedPreferences prefs = PreferenceManager
                                .getDefaultSharedPreferences(Super_Gen_Pass.this);
                        prefs.edit().putInt(Preferences.PREF_PIN_DIGITS, mPinDigits).commit();
                    }
                }).start();
                generateIfValid();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void initTabHost() {

        final TabHost mTabHost = (TabHost) findViewById(android.R.id.tabhost);

        mTabHost.addTab(createTabSpec(mTabHost, R.string.tab_password, R.id.tab_password,
                "password"));
        mTabHost.addTab(createTabSpec(mTabHost, R.string.tab_pin, R.id.tab_pin, "pin"));
    }

    private TabSpec createTabSpec(TabHost tabhost, int title, int content, String tag) {
        if (Build.VERSION.SDK_INT < 11) {
            return tabhost.newTabSpec(tag).setContent(content)
                    .setIndicator(createTabIndicator(title));
        } else {
            return tabhost.newTabSpec(tag).setContent(content).setIndicator(getText(title));
        }
    }

    private View createTabIndicator(int titleRes) {
        final LayoutInflater inflater = LayoutInflater.from(this);
        final TextView tab = (TextView) inflater.inflate(R.layout.tab_indicator, null);
        tab.setText(titleRes);
        return tab;
    }

    @Override
    protected void onPause() {

        // listen for a screen off event and clear everything if received.
        if (mShowingPassword) {
            registerReceiver(mScreenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        }

        super.onPause();

        // this is overly cautious to avoid memory leaks
        mHandler.removeMessages(MSG_UPDATE_PW_VIEW);


        mLastStoppedTime = SystemClock.elapsedRealtime();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getTabWidget().setVisibility(mShowPin ? View.VISIBLE : View.GONE);
        findViewById(R.id.down_arrow).setVisibility(mShowPin ? View.GONE : View.VISIBLE);
        if (!mShowPin) {
            getTabHost().setCurrentTab(0);
        }
        // when the user has left the app for more than pwClearTimeout minutes,
        // wipe master password and generated password.
        if (!mPleaseDontClearDomain
                && SystemClock.elapsedRealtime() - mLastStoppedTime > pwClearTimeout * 60 * 1000) {
            clearEditTexts();
        }
        mPleaseDontClearDomain = false;
    }

    private void clearEditTexts() {
        mDomainEdit.getText().clear();
        mMasterPwEdit.getText().clear();
        clearGenPassword();
        mDomainEdit.requestFocus();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_LAST_STOPPED_TIME, mLastStoppedTime);
        outState.putBoolean(STATE_SHOWING_PASSWORD, mShowingPassword);
    }

    private boolean mShowingPassword = false;

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
        } catch (final PasswordGenerationException e) {

            clearGenPassword();
        }
    }

    private void clearGenPassword() {
        if (mShowingPassword) {
            mGenPwView.setText(null);
            mGenPinView.setText(null);
            mShowingPassword = false;
            honeycombInvalidateOptionsMenu();
        }
    }

    private void honeycombInvalidateOptionsMenu() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            try {
                final Method invalidateOptionsMenu = Activity.class
                        .getMethod("invalidateOptionsMenu");
                invalidateOptionsMenu.invoke(this);
            } catch (final NoSuchMethodException e) {
                Log.e(TAG, "error refreshing menu");
            } catch (final IllegalArgumentException e) {
                Log.e(TAG, "error refreshing menu");
            } catch (final IllegalAccessException e) {
                Log.e(TAG, "error refreshing menu");
            } catch (final InvocationTargetException e) {
                Log.e(TAG, "error refreshing menu");
            }
        }
    }

    private String generateAndDisplay() throws PasswordGenerationException {
        String domain = getDomain();

        if (!noDomainCheck) {
            domain = extractDomain(domain);
        }
        final String masterPw = getMasterPassword() + pwSalt;
        final String genPw = mHasher.generate(masterPw, domain, pwLength);

        mGenPwView.setDomainName(domain);
        mGenPwView.setText(genPw);

        if (mPinGen != null && mShowPin) {
            final String pin = mPinGen.generate(masterPw, domain, mPinDigits);
            mGenPinView.setDomainName(domain);
            mGenPinView.setText(pin);
        }
        mShowingPassword = true;
        honeycombInvalidateOptionsMenu();

        return genPw;
    }

    private void postGenerate(boolean copyToClipboard) {

        if (mRememberDomains) {
            RememberedDomainProvider.addRememberedDomain(mContentResolver, getDomain());
        }

        if (copyToClipboard) {
            mGenPwView.copyToClipboard();

            if (Intent.ACTION_SEND.equals(getIntent().getAction()) && mGenPwView.getHidePassword()) {
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

        } catch (final IllegalDomainException e) {
            clearGenPassword();
            mDomainEdit.setError(e.getLocalizedMessage());
            mDomainEdit.requestFocus();

        } catch (final PasswordGenerationException e) {
            clearGenPassword();
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
        return false;
    }

    /**
     * @return the domain entered into the text box
     */
    String getDomain() {
        final AutoCompleteTextView txt = (AutoCompleteTextView) findViewById(R.id.domain_edit);
        return txt.getText().toString().trim();
    }

    String getMasterPassword() {
        return mMasterPwEdit.getText().toString();
    }

    /**
     * Returns the hostname portion of the supplied URL.
     *
     * @param maybeUrl
     *            : either a hostname or a URL.
     * @return the hostname portion of maybeUrl, or null if maybeUrl was null.
     */
    String extractDomain(String maybeUrl) {
        try {
            final Uri uri = Uri.parse(maybeUrl);
            return mHasher.getDomain(uri.getHost());
        } catch (final NullPointerException e) {
            return maybeUrl;
        } catch (final PasswordGenerationException e) {
            return maybeUrl;
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.go:
                go();
                break;
        }
    }

    public boolean onLongClick(View v) {
        switch (v.getId()) {
        }
        return false;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.show_gen_password: {

                mGenPwView.setHidePassword(!isChecked);

                // run on a thread as commit() can take a while.
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final SharedPreferences prefs = PreferenceManager
                                .getDefaultSharedPreferences(Super_Gen_Pass.this);
                        prefs.edit().putBoolean(Preferences.PREF_SHOW_GEN_PW, isChecked).commit();
                    }
                }).start();
            }
        }
    }

    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        return !go();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final MenuItem verify = menu.findItem(R.id.verify);
        verify.setEnabled(getMasterPassword().length() != 0);
        menu.findItem(R.id.copy).setEnabled(mShowingPassword);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:

                final Intent preferencesIntent = new Intent().setClass(this, Preferences.class);
                startActivityForResult(preferencesIntent, REQUEST_CODE_PREFERENCES);

                return true;

            case R.id.about:
                showDialog(DIALOG_ABOUT);
                return true;

            case R.id.verify:
                showDialog(DIALOG_CONFIRM_MASTER);
                return true;

            case R.id.go:
                go();
                return true;

            case R.id.copy:
                postGenerate(true);
                return true;

            default:
                return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PREFERENCES) {
            loadFromPreferences();
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_ABOUT: {
                final Builder builder = new AlertDialog.Builder(this);

                builder.setTitle(R.string.about_title);
                builder.setIcon(R.drawable.icon);

                // using this instead of setMessage lets us have clickable links.
                final LayoutInflater factory = LayoutInflater.from(this);
                builder.setView(factory.inflate(R.layout.about, null));

                builder.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                setResult(RESULT_OK);
                            }
                        });
                return builder.create();
            }

            case DIALOG_CONFIRM_MASTER: {
                final Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.dialog_verify_title);
                builder.setCancelable(true);
                final LayoutInflater inflater = LayoutInflater.from(this);
                final View pwVerifyLayout = inflater.inflate(R.layout.master_pw_verify, null);
                final EditText pwVerify = (EditText) pwVerifyLayout.findViewById(R.id.verify);

                builder.setNegativeButton(android.R.string.cancel, new Dialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                pwVerify.addTextChangedListener(new TextWatcher() {

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (pwVerify.getTag() instanceof String) {
                            final String masterPw = (String) pwVerify.getTag();
                            if (masterPw.length() > 0 && masterPw.equals(s.toString())) {
                                dismissDialog(DIALOG_CONFIRM_MASTER);
                                Toast.makeText(getApplicationContext(),
                                        R.string.toast_verify_success, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
                builder.setView(pwVerifyLayout);
                final Dialog d = builder.create();
                // This is added below to ensure that the soft input doesn't get hidden if it's
                // showing,
                // which seems to be the default for dialogs.
                d.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED);
                return d;
            }
            default:
                throw new IllegalArgumentException("Unknown dialog ID: " + id);
        }
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
            case DIALOG_CONFIRM_MASTER:
                final EditText verify = (EditText) dialog.findViewById(R.id.verify);
                verify.setTag(getMasterPassword());
                verify.setText(null);
                verify.requestFocus();
                break;

            default:
                super.onPrepareDialog(id, dialog);
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
                mHandler.sendEmptyMessage(MSG_UPDATE_PW_VIEW);
            }
        });

        mMasterPwEdit.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mHandler.sendEmptyMessage(MSG_UPDATE_PW_VIEW);
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
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // when adding items here, make sure default values are in sync with the xml file
        this.pwType = prefs.getString(Preferences.PREF_PW_TYPE, SuperGenPass.TYPE);
        this.pwLength = Preferences.getStringAsInteger(prefs, Preferences.PREF_PW_LENGTH, 10);
        this.pwSalt = prefs.getString(Preferences.PREF_PW_SALT, "");
        this.mCopyToClipboard = prefs.getBoolean(Preferences.PREF_CLIPBOARD, true);
        this.mRememberDomains = prefs.getBoolean(Preferences.PREF_REMEMBER_DOMAINS, true);
        this.noDomainCheck = prefs.getBoolean(Preferences.PREF_DOMAIN_NOCHECK, false);
        this.pwClearTimeout = Preferences.getStringAsInteger(prefs,
                Preferences.PREF_PW_CLEAR_TIMEOUT, 2);

        // PIN
        mPinDigits = prefs.getInt(Preferences.PREF_PIN_DIGITS, 4);
        mPinDigitsSpinner.setSelection(mPinDigits - MIN_PIN_LENGTH);
        mShowPin = prefs.getBoolean(Preferences.PREF_SHOW_PIN, true);

        try {
            if (pwType.equals(SuperGenPass.TYPE)) {

                mHasher = new SuperGenPass(this, SuperGenPass.HASH_ALGORITHM_MD5);

            } else if (pwType.equals(SuperGenPass.TYPE_SHA_512)) {

                mHasher = new SuperGenPass(this, SuperGenPass.HASH_ALGORITHM_SHA512);

            } else if (pwType.equals(PasswordComposer.TYPE)) {
                mHasher = new PasswordComposer(this);

            } else {
                mHasher = new SuperGenPass(this, SuperGenPass.HASH_ALGORITHM_MD5);
                Log.e(TAG, "password type was set to unknown algorithm: " + pwType);
            }

            mPinGen = new HotpPin(this);

        } catch (final NoSuchAlgorithmException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),
                    String.format(getString(R.string.err_no_md5), e.getLocalizedMessage()),
                    Toast.LENGTH_LONG).show();
            finish();

        } catch (final IOException e) {
            Toast.makeText(this, getString(R.string.err_json_load, e.getLocalizedMessage()),
                    Toast.LENGTH_LONG).show();
            Log.d(TAG, getString(R.string.err_json_load), e);
            finish();
        }

        mHasher.setCheckDomain(!noDomainCheck);

        if (noDomainCheck) {
            mDomainEdit.setHint(R.string.domain_hint_no_checking);
        } else {
            mDomainEdit.setHint(R.string.domain_hint);
        }

        mMasterPwEdit.setShowVisualHash(prefs.getBoolean(Preferences.PREF_VISUAL_HASH, true));

        if (mCopyToClipboard) {
            mMasterPwEdit.setImeActionLabel(getText(android.R.string.copy), R.id.go);
        } else {
            mMasterPwEdit.setImeActionLabel(getText(R.string.done), R.id.go);
        }

        final boolean showPassword = prefs.getBoolean(Preferences.PREF_SHOW_GEN_PW, false);

        mGenPwView.setHidePassword(!showPassword);
        mShowGenPassword.setChecked(showPassword);
    }

    private static final String[] PROJECTION = { Domain.DOMAIN, Domain._ID };
    private static final int DOMAIN_COLUMN = 0;

    // a filter that searches for domains starting with the given constraint
    @Override
    public Cursor runQuery(CharSequence constraint) {
        Cursor c;
        if (constraint == null || constraint.length() == 0) {
            c = mContentResolver.query(Domain.CONTENT_URI, PROJECTION, null, null,
                    Domain.SORT_ORDER);
        } else {
            c = mContentResolver.query(Domain.CONTENT_URI, PROJECTION, Domain.DOMAIN + " GLOB ?",
                    new String[] { constraint.toString() + "*" }, Domain.SORT_ORDER);
        }

        return c;
    }
}
