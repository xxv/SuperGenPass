package info.staticfree.SuperGenPass;

/*
 	Android SuperGenPass
    Copyright (C) 2009-2010  Steve Pomeroy <steve@staticfree.info>

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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import junit.framework.Assert;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
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
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView.OnEditorActionListener;

// TODO Wipe generated password from clipboard after delay.
// TODO Wipe master password after 5 minute timeout or screen lock.
public class Super_Gen_Pass extends Activity implements OnClickListener, OnLongClickListener,
			OnCheckedChangeListener, OnEditorActionListener {
	private final static String TAG = Super_Gen_Pass.class.getSimpleName();
	MessageDigest md5;
	private ArrayList<String> domains;
	private static final int
		DIALOG_ABOUT = 0,
		DIALOG_CONFIRM_MASTER = 1;
	private static final int REQUEST_CODE_PREFERENCES = 0;

	private int pwLength;
	private String pwType;
	private String pwSalt;
	private boolean copyToClipboard;
	private boolean rememberDomains;
	private boolean noDomainCheck;

	private GeneratedPasswordView genPwView;
	private EditText domainEdit;

	private RememberedDBHelper dbHelper;
	private SQLiteDatabase db;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
		final Intent intent = getIntent();
		final Uri data = intent.getData();

		// Make window transient-looking if coming from a share intent
		if (Intent.ACTION_SEND.equals(intent.getAction())){
			setTheme(android.R.style.Theme_Dialog);
		}else{
			// this is necessary as the default theme is translucent to make the dimming work
			setTheme(android.R.style.Theme);
		}

		super.onCreate(savedInstanceState);

        setContentView(R.layout.main);


        domainEdit = (EditText)findViewById(R.id.domain_edit);

        genPwView = (GeneratedPasswordView) findViewById(R.id.password_output);
        genPwView.setOnLongClickListener(this);
        final EditText masterPwEdit = ((EditText)findViewById(R.id.password_edit));

        masterPwEdit.setOnEditorActionListener(this);

		// hook in our buttons
		((Button)findViewById(R.id.go)).setOnClickListener(this);
		((ToggleButton)findViewById(R.id.show_gen_password)).setOnCheckedChangeListener(this);

        updatePreferences();

		dbHelper = new RememberedDBHelper(getApplicationContext());
		db = dbHelper.getWritableDatabase();

		loadDomains();

        try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (final NoSuchAlgorithmException e) {
			e.printStackTrace();
			Toast.makeText(getApplicationContext(),
					String.format(getString(R.string.err_no_md5), e.getLocalizedMessage()),
					Toast.LENGTH_LONG).show();
		}


		// initialize the autocompletion
		((AutoCompleteTextView)findViewById(R.id.domain_edit))
			.setAdapter(dbHelper.getDomainPrefixAdapter(this, db));

		// check for the "share page" intent. If present, pre-fill.

		if (data == null){

			final String maybeUrl = intent.getStringExtra(Intent.EXTRA_TEXT);
			if (maybeUrl != null){
				try{
					// populate the URL and give the password entry focus
					final Uri uri = Uri.parse(maybeUrl);
					domainEdit.setText(getDomain(uri.getHost()));
					((EditText)findViewById(R.id.password_edit)).requestFocus();

				}catch(final Exception e){
					// nothing much to be done here.
					// Let the user figure it out.
					Log.e(TAG, "Could not find valid URI in shared text", e);
					Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
				}
			}
		}
    }

    @Override
    protected void onDestroy() {
    	db.close();
    	super.onDestroy();
    }

    /**
     * This list should remain the same and in sync with the canonical SGP,
	 * so that passwords generated in one place are the same as others.
     */
    private void loadDomains(){
    	final InputStream is = getResources().openRawResource(R.raw.domains);

    	final StringBuilder jsonString = new StringBuilder();
    	try{

    		for (final BufferedReader isReader = new BufferedReader(new InputStreamReader(is), 16000);
    		isReader.ready();){
    			jsonString.append(isReader.readLine());
    		}

    		final JSONArray domainJson = new JSONArray(jsonString.toString());
    		domains = new ArrayList<String>(domainJson.length());
    		for (int i = 0; i < domainJson.length(); i++){
    			domains.add(domainJson.getString(i));
    		}
    	}catch (final Exception e){
    		Toast.makeText(this, getString(R.string.err_json_load, e.getLocalizedMessage()),
    				Toast.LENGTH_LONG).show();
    		Log.d(TAG, getString(R.string.err_json_load), e);
    		finish();
    	}

    	Assert.assertTrue("Domains did not seem to load", domains.size() > 100);
    }


    /**
     * Go!
     */
    void go(){
    	String genPw = "";
    	final String domain = getDomain();
    	try {
        	if (pwType.equals("sgp")){
        		genPw = superGenPassGen(getMasterPassword() + pwSalt, domain, pwLength);

        	}else if (pwType.equals("pwc")){
        		genPw = passwordComposerGen(getMasterPassword() + pwSalt, domain, pwLength);
        	}

		} catch (final PasswordGenerationException e) {
			genPw = "";
			Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
		}

		genPwView.setDomainName(domain);
		genPwView.setText(genPw);

		if (rememberDomains){
			RememberedDBHelper.addRememberedDomain(db, domain);
		}

		if (copyToClipboard){
			genPwView.copyToClipboard();

			if (Intent.ACTION_SEND.equals(getIntent().getAction())){
				finish();
			}
		}
    }

    /**
     * @return the domain entered into the text box
     */
    String getDomain(){
    	final AutoCompleteTextView txt = (AutoCompleteTextView)findViewById(R.id.domain_edit);
    	return txt.getText().toString();
    }

    String getMasterPassword(){
    	final EditText txt = (EditText)findViewById(R.id.password_edit);
    	return txt.getText().toString();
    }


    /**
     * Generates a domain password based on the PasswordComposer algorithm.
     *
     * @param masterPass master password
     * @param domain pre-filtered domain (eg. example.org)
     * @return generated password
     * @throws PasswordGenerationException
     * @see http://www.xs4all.nl/~jlpoutre/BoT/Javascript/PasswordComposer/
     */
    public String passwordComposerGen(String masterPass, String domain, int length) throws PasswordGenerationException{
    	if (domain.equals("")){
    		throw new PasswordGenerationException("Missing domain");
    	}
    	return md5hex(new String(masterPass + ":" + getDomain(domain)).getBytes()).substring(0, length);
    }

    /**
     * Generates a domain password based on the SuperGenPass algorithm.
     *
     * @param masterPass
     * @param domain pre-filtered domain (eg. example.org)
     * @param length generated password length; an integer between 4 and 24, inclusive.
     * @return generated password
     * @throws PasswordGenerationException
     * @see http://supergenpass.com/
     */
    public String superGenPassGen(String masterPass, String domain, int length) throws PasswordGenerationException{
    	if (length < 4 || length > 24){
    		throw new PasswordGenerationException("Requested length out of range. Expecting value between 4 and 24 inclusive.");
    	}
    	if (domain.equals("")){
    		throw new PasswordGenerationException("Missing domain");
    	}

    	 String pwSeed = masterPass + ":" + getDomain(domain);

    	// wash ten times
    	for (int i = 0; i < 10; i++){
    		pwSeed = md5base64(pwSeed.getBytes());
    	}

    	/*   from http://supergenpass.com/about/#PasswordComplexity :
    	        *  Consist of alphanumerics (A-Z, a-z, 0-9)
			    * Always start with a lowercase letter of the alphabet
			    * Always contain at least one uppercase letter of the alphabet
			    * Always contain at least one numeral
			    * Can be any length from 4 to 24 characters (default: 10)
    	 */

    	// regex looks for:
    	// "lcletter stuff Uppercase stuff Number stuff" or
    	// "lcletter stuff Number stuff Uppercase stuff"
    	// which should satisfy the above requirements.
    	while (! pwSeed.substring(0,length).
    			matches("^[a-z][a-zA-Z0-9]*(?:(?:[A-Z][a-zA-Z0-9]*[0-9])|(?:[0-9][a-zA-Z0-9]*[A-Z]))[a-zA-Z0-9]*$")){
    		pwSeed = md5base64(pwSeed.getBytes());
    	}

    	// when the right pwSeed is found to have a
    	// password-appropriate substring, return it
    	return pwSeed.substring(0, length);
    }


    /**
     * Returns the standard hex-encoded string md5sum of the data.
     *
     * @param data
     * @return hex-encoded string of the md5sum of the data
     */
    public String md5hex(byte[] data){
    	final byte[] md5data = md5.digest(data);
    	String md5hex = new String();
    	for( int i = 0; i < md5data.length; i++){
    		md5hex += String.format("%02x", md5data[i]);
    	}
    	return md5hex;
    }

    /**
     * Returns a base64-encoded string of the md5sum of the data.
     * Caution: SuperGenPass-specific!
     * Includes substitutions to ensure that valid base64 characters
     * '=', '/', and '+' get mapped to
     * 'A', '8', and '9' respectively, so as to ensure alpha/num passwords.
     *
     * @param data
     * @return  base64-encoded string of the md5sum of the data
     */
    public String md5base64(byte[] data){

    	String b64 = new String(Base64.encodeBase64(md5.digest(data)));
    	// SuperGenPass-specific quirk so that these don't end up in the password.
    	b64 = b64.replace('=', 'A').replace('/', '8').replace('+', '9');
    	b64.trim();

    	return b64;
    }

    /**
     * Computes the site's domain, based on the provided hostname. This takes into account
     * things like "co.uk" and other such multi-level TLDs.
     *
     * @param hostname the full hostname
     * @return the domain of the URI
     * @throws PasswordGenerationException
     */
    public String getDomain(String hostname) throws PasswordGenerationException{

    	hostname = hostname.toLowerCase();

    	if (noDomainCheck){
    		return hostname;
    	}

    	// IP addresses should be composed based on the full address.
    	if (hostname.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")){
    		return hostname;
    	}

    	// for single-level TLDs, we only want the TLD and the 2nd level domain
    	final String[] hostParts = hostname.split("\\.");
    	if (hostParts.length < 2){
    		throw new PasswordGenerationException("Invalid domain: '"+hostname+"'");
    	}
    	String domain = hostParts[hostParts.length-2] + '.' + hostParts[hostParts.length-1];

    	// do a slow search of all the possible multi-level TLDs and
    	// see if we need to pull in one level deeper.
    	for (final String tld: domains){
    		if (domain.equals(tld)){
    			if (hostParts.length < 3){
    				throw new PasswordGenerationException("Invalid domain. '"+domain+"' seems to be a TLD.");
    			}
    			domain = hostParts[hostParts.length - 3] + '.' + domain;
    			break;
    		}
    	}
    	return domain;
    }

	public void onClick(View v) {
		switch (v.getId()){
		case R.id.go:
			go();
			break;
		}
	}

	public boolean onLongClick(View v) {
		switch (v.getId()){
		}
		return false;
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
		switch (buttonView.getId()){
			case R.id.show_gen_password:{
				if (isChecked){
					genPwView.setInputType(InputType.TYPE_CLASS_TEXT
							| InputType.TYPE_TEXT_VARIATION_NORMAL);
				}else{
					genPwView.setInputType(InputType.TYPE_CLASS_TEXT
							| InputType.TYPE_TEXT_VARIATION_PASSWORD);
				}

				// run on a thread as commit() can take a while.
				new Thread(new Runnable() {
					@Override
					public void run() {
						final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Super_Gen_Pass.this);
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
    	switch (item.getItemId()){
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

    	if (requestCode == REQUEST_CODE_PREFERENCES){
    		updatePreferences();
    	}
    }

    @Override
    protected Dialog onCreateDialog(int id) {
    	switch (id){
    	case DIALOG_ABOUT:{
        	final Builder builder = new AlertDialog.Builder(this);

        	builder.setTitle(R.string.about_title);
        	builder.setIcon(R.drawable.icon);

        	// using this instead of setMessage lets us have clickable links.
        	final LayoutInflater factory = LayoutInflater.from(this);
        	builder.setView(factory.inflate(R.layout.about, null));

        	builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
        		public void onClick(DialogInterface dialog, int which) {
        			setResult(RESULT_OK);
        		}
        	});
        	return builder.create();
    	}

    	case DIALOG_CONFIRM_MASTER:{
    		final Builder builder = new AlertDialog.Builder(this);
    		builder.setTitle(R.string.dialog_verify_title);
    		builder.setCancelable(true);
        	final LayoutInflater factory = LayoutInflater.from(this);
        	final EditText pwVerify = (EditText) factory.inflate(R.layout.master_pw_verify, null);

        	builder.setNegativeButton(android.R.string.cancel, new Dialog.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});

        	pwVerify.addTextChangedListener(new TextWatcher() {

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {}

				@Override
				public void beforeTextChanged(CharSequence s, int start, int count,
						int after) {}

				@Override
				public void afterTextChanged(Editable s) {
					if (((String)pwVerify.getTag()).equals(s.toString())){
						dismissDialog(DIALOG_CONFIRM_MASTER);
						Toast.makeText(getApplicationContext(), R.string.toast_verify_success, Toast.LENGTH_SHORT).show();
					}
				}
			});
        	builder.setView(pwVerify);
        	final Dialog d = builder.create();
        	// This is added below to ensure that the soft input doesn't get hidden if it's showing,
        	// which seems to be the default for dialogs.
        	d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED);
        	return d;
    	}
    	default:
    		throw new IllegalArgumentException("Unknown dialog ID: "+id);
    	}
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
    	switch (id) {
		case DIALOG_CONFIRM_MASTER:
			final EditText verify = (EditText)dialog.findViewById(R.id.verify);
			verify.setTag(getMasterPassword());
			verify.setText(null);
			verify.requestFocus();
			break;

		default:
			super.onPrepareDialog(id, dialog);
		}
    }

    protected void  updatePreferences(){
    	final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

    	// when adding items here, make sure default values are in sync with the xml file
    	this.pwType = prefs.getString("pw_type", "sgp");
    	this.pwLength = Integer.parseInt(prefs.getString("pw_length", "10"));
    	this.pwSalt = prefs.getString("pw_salt", "");
    	this.copyToClipboard = prefs.getBoolean("clipboard", true);
    	this.rememberDomains = prefs.getBoolean("domain_autocomplete", true);
    	this.noDomainCheck = prefs.getBoolean("domain_nocheck", false);


    	// While it doesn't really make sense to clear this every time this is saved,
    	// there isn't much of a better option beyond remembering more state.
    	if (! rememberDomains){
    		RememberedDBHelper.clearRememberedDomains(db);
    	}

    	if (noDomainCheck){
    		domainEdit.setHint(R.string.domain_hint_no_checking);
        }else{
        	domainEdit.setHint(R.string.domain_hint);
        }

    	((ToggleButton)findViewById(R.id.show_gen_password)).setChecked(prefs.getBoolean(Preferences.PREF_SHOW_GEN_PW, false));
    }
}

