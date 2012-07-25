package info.staticfree.SuperGenPass.hashes;
/*
 * Copyright (C) 2010 Steve Pomeroy
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

import info.staticfree.SuperGenPass.IllegalDomainException;
import info.staticfree.SuperGenPass.PasswordGenerationException;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;

import android.content.Context;

public class SuperGenPass extends DomainBasedHash {
	public static final String TYPE = "sgp";
	public static final String TYPE_SHA_512 = "sgp-sha-512";

	public static final String HASH_ALGORITHM_MD5 = "MD5";
	public static final String HASH_ALGORITHM_SHA512 = "SHA-512";

	private final MessageDigest mHasher;

	public SuperGenPass(Context context, String hashAlgorithm) throws NoSuchAlgorithmException,
			IOException {
		super(context);
		mHasher = MessageDigest.getInstance(hashAlgorithm);
	}

    /**
	 * Returns a base64-encoded string of the digest of the data. Caution: SuperGenPass-specific!
	 * Includes substitutions to ensure that valid base64 characters '=', '/', and '+' get mapped to
	 * 'A', '8', and '9' respectively, so as to ensure alpha/num passwords.
	 *
	 * @param data
	 * @return base64-encoded string of the hash of the data
	 */
	private String hashBase64(byte[] data) {

		String b64 = new String(Base64.encodeBase64(mHasher.digest(data)));
    	// SuperGenPass-specific quirk so that these don't end up in the password.
    	b64 = b64.replace('=', 'A').replace('/', '8').replace('+', '9');
    	b64.trim();

    	return b64;
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
    private static final Pattern validPassword =
    	Pattern.compile("^[a-z][a-zA-Z0-9]*(?:(?:[A-Z][a-zA-Z0-9]*[0-9])|(?:[0-9][a-zA-Z0-9]*[A-Z]))[a-zA-Z0-9]*$");

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
    @Override
	public String generate(String masterPass, String domain, int length) throws PasswordGenerationException{
    	if (length < 4 || length > 24){
    		throw new PasswordGenerationException("Requested length out of range. Expecting value between 4 and 24 inclusive.");
    	}
    	if (domain.equals("")){
    		throw new IllegalDomainException("Missing domain");
    	}

    	 String pwSeed = masterPass + ":" + getDomain(domain);

    	// wash ten times
    	for (int i = 0; i < 10; i++){
			pwSeed = hashBase64(pwSeed.getBytes());
    	}

    	Matcher matcher = validPassword.matcher(pwSeed.substring(0,length));
    	while (!matcher.matches()){
			pwSeed = hashBase64(pwSeed.getBytes());
    		matcher = validPassword.matcher(pwSeed.substring(0,length));
    	}

    	// when the right pwSeed is found to have a
    	// password-appropriate substring, return it
    	return pwSeed.substring(0, length);
    }
}