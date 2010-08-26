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

import android.content.Context;

public class PasswordComposer extends DomainBasedHash {
	public static final String TYPE = "pwc";

	private final MessageDigest md5;

	public PasswordComposer(Context context) throws NoSuchAlgorithmException, IOException {
		super(context);
		md5 = MessageDigest.getInstance("MD5");
	}

    /**
     * Returns the standard hex-encoded string md5sum of the data.
     *
     * @param data
     * @return hex-encoded string of the md5sum of the data
     */
    private String md5hex(byte[] data){
    	final byte[] md5data = md5.digest(data);
    	String md5hex = new String();
    	for( int i = 0; i < md5data.length; i++){
    		md5hex += String.format("%02x", md5data[i]);
    	}
    	return md5hex;
    }

    /**
     * Generates a domain password based on the PasswordComposer algorithm.
     *
     * @param masterPass master password
     * @param domain un-filtered domain (eg. www.example.org)
     * @return generated password
     * @throws PasswordGenerationException
     * @see http://www.xs4all.nl/~jlpoutre/BoT/Javascript/PasswordComposer/
     */
    @Override
	public String generate(String masterPass, String domain, int length) throws PasswordGenerationException {
    	if (domain.equals("")){
    		throw new IllegalDomainException("Missing domain");
    	}
    	return md5hex(new String(masterPass + ":" + getDomain(domain)).getBytes()).substring(0, length);
    }
}
