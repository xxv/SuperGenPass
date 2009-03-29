package info.staticfree.SuperGenPass;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Base64;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class Super_Gen_Pass extends Activity {
	MessageDigest md5;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// really, what else could we do at this point?
			e.printStackTrace();
		}
		
		Button bGo = (Button)findViewById(R.id.go);
		bGo.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				go();
			}
		});
		
		/*
		Spinner pwgenSelector = (Spinner)findViewById(R.id.pwgen_spinner);
		pwgenSelector.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				// maybe have it clear the password result?
				
			}
		})
		*/
    }
    
    void go(){
    	Spinner pwgenSelector = (Spinner)findViewById(R.id.pwgen_spinner);
    	String genPw = "";
    	try {
    		// XXX probably the wrong way to do this, but it should work. Is there some sort of ID that strings can be assigned?
        	if (pwgenSelector.getSelectedItemPosition() == 0){
        		genPw = superGenPassGen(getMasterPassword(), getDomain(), 10);	
        	}else if (pwgenSelector.getSelectedItemPosition() == 1){
        		genPw = passwordComposerGen(getMasterPassword(), getDomain());
        	}
			
		} catch (PasswordGenerationException e) {
			genPw = "";
		}

		EditText txt = (EditText)findViewById(R.id.password_output);
		txt.setText(genPw);
    }
    
    String getDomain(){
    	EditText txt = (EditText)findViewById(R.id.domain_edit);
    	return txt.getText().toString();
    }
    
    String getMasterPassword(){
    	EditText txt = (EditText)findViewById(R.id.password_edit);
    	return txt.getText().toString();
    } 
    
    /**
     * Generates a domain password based on the PasswordComposer algorithm.
     * 
     * @param masterPass
     * @param domain
     * @return
     * @throws PasswordGenerationException 
     * @see http://www.xs4all.nl/~jlpoutre/BoT/Javascript/PasswordComposer/
     */
    String passwordComposerGen(String masterPass, String domain) throws PasswordGenerationException{
    	if (domain.equals("")){
    		throw new PasswordGenerationException("Missing domain");
    	}
    	return md5hex(new String(masterPass + ":" + domain).getBytes()).substring(0, 8);
    }
    
    /**
     * Generates a domain password based on the SuperGenPass algorithm.
     * 
     * @param masterPass
     * @param domain this should be a pre-filtered domain
     * @param length an integer between 4 and 24, inclusive.
     * @return
     * @throws PasswordGenerationException 
     * @see http://supergenpass.com/
     */
    String superGenPassGen(String masterPass, String domain, int length) throws PasswordGenerationException{
    	if (length < 4 || length > 24){
    		throw new PasswordGenerationException("Requested length out of range. Expecting value between 4 and 24 inclusive.");
    	}
    	if (domain.equals("")){
    		throw new PasswordGenerationException("Missing domain");
    	}
    	
    	 String pwSeed = masterPass + ":" + domain;
 
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
    String md5hex(byte[] data){
    	byte[] md5data = md5.digest(data);
    	String md5hex = new String();
    	for( int i = 0; i < md5data.length; i++){
    		md5hex += String.format("%02x", md5data[i]); 
    	}
    	return md5hex;
    }
    
    /**
     * Returns a base64-encoded string of the md5sum of the data.
     * Caution: SuperGenPass-specific!
     *  
     * @param data
     * @return  base64-encoded string of the md5sum of the data
     */
    String md5base64(byte[] data){
    	
    	String b64 = new String(Base64.encodeBase64(md5.digest(data)));
    	// SuperGenPass-specific quirk so that these don't end up in the password.
    	b64 = b64.replace('=', 'A').replace('/', '8').replace('+', '9');
    	b64.trim();
    	
    	return b64;
    }
}