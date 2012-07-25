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
import info.staticfree.SuperGenPass.hashes.PasswordComposer;
import info.staticfree.SuperGenPass.hashes.SuperGenPass;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
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
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.widget.ToggleButton;

// TODO Wipe generated password from clipboard after delay.
// TODO Wipe passwords on screen lock event.
public class Super_Gen_Pass extends Activity implements OnClickListener, OnLongClickListener,
		OnCheckedChangeListener, OnEditorActionListener, FilterQueryProvider {
	private final static String TAG = Super_Gen_Pass.class.getSimpleName();

	DomainBasedHash hasher;

	// @formatter:off
	private static final int
		DIALOG_ABOUT = 0,
		DIALOG_CONFIRM_MASTER = 1;
	private static final int REQUEST_CODE_PREFERENCES = 0;
	private static final String
		STATE_LAST_STOPPED_TIME = "info.staticfree.SuperGenPass.STATE_LAST_STOPPED_TIME";
	// @formatter:on

	private int pwLength;
	private String pwType;
	private String pwSalt;
	private boolean copyToClipboard;
	private boolean rememberDomains;
	private boolean noDomainCheck;

	private GeneratedPasswordView genPwView;
	private EditText domainEdit;
	private EditText mMasterPwEdit;

	private long lastStoppedTime;
	private int pwClearTimeout;

	private ContentResolver mContentResolver;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		final Intent intent = getIntent();
		final Uri data = intent.getData();

		if (savedInstanceState != null && savedInstanceState.containsKey(STATE_LAST_STOPPED_TIME)) {
			lastStoppedTime = savedInstanceState.getLong(STATE_LAST_STOPPED_TIME);
		}

		domainEdit = (EditText) findViewById(R.id.domain_edit);

		genPwView = (GeneratedPasswordView) findViewById(R.id.password_output);
		genPwView.setOnLongClickListener(this);

		mMasterPwEdit = ((EditText) findViewById(R.id.password_edit));

		mMasterPwEdit.setOnEditorActionListener(this);

		// hook in our buttons
		((Button) findViewById(R.id.go)).setOnClickListener(this);
		((ToggleButton) findViewById(R.id.show_gen_password)).setOnCheckedChangeListener(this);

		loadFromPreferences();

		mContentResolver = getContentResolver();

		@SuppressWarnings("deprecation")
		final SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_dropdown_item_1line, null, new String[] { "domain" },
				new int[] { android.R.id.text1 });

		adapter.setFilterQueryProvider(this);
		adapter.setStringConversionColumn(DOMAIN_COLUMN);

		// initialize the autocompletion
		final AutoCompleteTextView domainEdit = (AutoCompleteTextView) findViewById(R.id.domain_edit);
		domainEdit.setAdapter(adapter);

		// check for the "share page" intent. If present, pre-fill.

		if (data == null) {

			final String maybeUrl = intent.getStringExtra(Intent.EXTRA_TEXT);
			if (maybeUrl != null) {
				try {
					// populate the URL and give the password entry focus
					final Uri uri = Uri.parse(maybeUrl);
					domainEdit.setText(hasher.getDomain(uri.getHost()));
					mMasterPwEdit.requestFocus();

				} catch (final Exception e) {
					// nothing much to be done here.
					// Let the user figure it out.
					Log.e(TAG, "Could not find valid URI in shared text", e);
					Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
				}
			}
		}
	}

	@Override
	protected void onPause() {

		super.onPause();
		lastStoppedTime = SystemClock.elapsedRealtime();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// when the user has left the app for more than pwClearTimeout minutes,
		// wipe master password and generated password.
		if (SystemClock.elapsedRealtime() - lastStoppedTime > pwClearTimeout * 60 * 1000) {
			((EditText) findViewById(R.id.password_edit)).getText().clear();
			genPwView.setText("");
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(STATE_LAST_STOPPED_TIME, lastStoppedTime);
	}

	/**
	 * Go! Validates the forms, computes the password, displays it, remembers the domain, and copies
	 * to clipboard.
	 */
	void go() {
		String genPw = "";
		final String domain = getDomain();
		try {
			genPw = hasher.generate(getMasterPassword() + pwSalt, domain, pwLength);

		} catch (final IllegalDomainException e) {
			genPwView.setText("");
			domainEdit.setError(e.getLocalizedMessage());
			domainEdit.requestFocus();
			return;
		} catch (final PasswordGenerationException e) {
			genPwView.setText("");
			Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
			return;
		}

		genPwView.setDomainName(domain);
		genPwView.setText(genPw);

		if (rememberDomains) {
			RememberedDomainProvider.addRememberedDomain(mContentResolver, domain);
		}

		if (copyToClipboard) {
			genPwView.copyToClipboard();

			if (Intent.ACTION_SEND.equals(getIntent().getAction())
					&& (genPwView.getInputType() & InputType.TYPE_TEXT_VARIATION_PASSWORD) > 0) {
				finish();
			}
		}
	}

	/**
	 * @return the domain entered into the text box
	 */
	String getDomain() {
		final AutoCompleteTextView txt = (AutoCompleteTextView) findViewById(R.id.domain_edit);
		return txt.getText().toString().trim();
	}

	String getMasterPassword() {
		final EditText txt = (EditText) findViewById(R.id.password_edit);
		return txt.getText().toString();
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
				if (isChecked) {
					genPwView.setInputType(InputType.TYPE_CLASS_TEXT
							| InputType.TYPE_TEXT_VARIATION_NORMAL);
				} else {
					genPwView.setInputType(InputType.TYPE_CLASS_TEXT
							| InputType.TYPE_TEXT_VARIATION_PASSWORD);
				}

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
		go();
		return false;
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
				final LayoutInflater factory = LayoutInflater.from(this);
				final EditText pwVerify = (EditText) factory.inflate(R.layout.master_pw_verify,
						null);

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
				builder.setView(pwVerify);
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

	/**
	 * Loads the preferences and updates the program state based on them.
	 */
	protected void loadFromPreferences() {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		// when adding items here, make sure default values are in sync with the xml file
		this.pwType = prefs.getString(Preferences.PREF_PW_TYPE, SuperGenPass.TYPE);
		this.pwLength = Preferences.getStringAsInteger(prefs, Preferences.PREF_PW_LENGTH, 10);
		this.pwSalt = prefs.getString(Preferences.PREF_PW_SALT, "");
		this.copyToClipboard = prefs.getBoolean(Preferences.PREF_CLIPBOARD, true);
		this.rememberDomains = prefs.getBoolean(Preferences.PREF_REMEMBER_DOMAINS, true);
		this.noDomainCheck = prefs.getBoolean(Preferences.PREF_DOMAIN_NOCHECK, false);
		this.pwClearTimeout = Preferences.getStringAsInteger(prefs,
				Preferences.PREF_PW_CLEAR_TIMEOUT, 2);

		// While it doesn't really make sense to clear this every time this is saved,
		// there isn't much of a better option beyond remembering more state.
		if (!rememberDomains) {
			mContentResolver.delete(Domain.CONTENT_URI, null, null);
		}

		try {
			if (pwType.equals(SuperGenPass.TYPE)) {

				hasher = new SuperGenPass(this, SuperGenPass.HASH_ALGORITHM_MD5);

			} else if (pwType.equals(SuperGenPass.TYPE_SHA_512)) {

				hasher = new SuperGenPass(this, SuperGenPass.HASH_ALGORITHM_SHA512);

			} else if (pwType.equals(PasswordComposer.TYPE)) {
				hasher = new PasswordComposer(this);

			} else {
				hasher = new SuperGenPass(this, SuperGenPass.HASH_ALGORITHM_MD5);
				Log.e(TAG, "password type was set to unknown algorithm: " + pwType);
			}

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

		hasher.setCheckDomain(!noDomainCheck);

		if (noDomainCheck) {
			domainEdit.setHint(R.string.domain_hint_no_checking);
		} else {
			domainEdit.setHint(R.string.domain_hint);
		}

		showVisualHash(prefs.getBoolean(Preferences.PREF_VISUAL_HASH, true));

		((ToggleButton) findViewById(R.id.show_gen_password)).setChecked(prefs.getBoolean(
				Preferences.PREF_SHOW_GEN_PW, false));
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

	private VisualHashWatcher mVisualHashWatcher;

	private void showVisualHash(boolean showVisualHash) {
		if (mVisualHashWatcher == null && showVisualHash) {
			final VisualHash vh = new VisualHash();

			final float scale = getResources().getDisplayMetrics().scaledDensity;

			// this number is based on what looks good with standard edit boxes.
			vh.setBounds(0, 0, (int) (45 * scale), (int) (45 * scale));
			mMasterPwEdit.setCompoundDrawables(null, null, vh, null);

			mVisualHashWatcher = new VisualHashWatcher(vh);
			mMasterPwEdit.addTextChangedListener(mVisualHashWatcher);

		} else if (mVisualHashWatcher != null && !showVisualHash) {
			mMasterPwEdit.setCompoundDrawables(null, null, null, null);
			mMasterPwEdit.removeTextChangedListener(mVisualHashWatcher);
			mVisualHashWatcher = null;
		}
	}

	private class VisualHashWatcher implements TextWatcher {

		private final VisualHash mVisualHash;

		public VisualHashWatcher(VisualHash visualHash) {
			mVisualHash = visualHash;
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			mVisualHash.setData(s.toString().getBytes());
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		@Override
		public void afterTextChanged(Editable s) {
		}
	};
}
