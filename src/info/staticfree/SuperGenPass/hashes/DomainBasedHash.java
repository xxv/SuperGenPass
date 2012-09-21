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
import info.staticfree.SuperGenPass.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import junit.framework.Assert;

import org.json.JSONArray;

import android.content.Context;

/**
 * A password hash that takes a password and a domain. Domains are optionally checked against
 * a database of known TLDs in order to generate domain-specific passwords.
 * For example, "www.example.org" and "www2.example.org" will generate the same password.
 *
 * @author Steve Pomeroy
 *
 */
public abstract class DomainBasedHash {
    private boolean checkDomain;
    private ArrayList<String> domains;
    private final Context mContext;

    public DomainBasedHash(Context context) throws IOException {
        mContext = context;
        loadDomains();
    }

    /**
     * This list should remain the same and in sync with the canonical SGP,
     * so that passwords generated in one place are the same as others.
     */
    public void loadDomains() throws IOException {
        final InputStream is = mContext.getResources().openRawResource(R.raw.domains);

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
            final IOException ioe = new IOException("Unable to load domains");
            ioe.initCause(e);
        }

        Assert.assertTrue("Domains did not seem to load", domains.size() > 100);
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

        if (!checkDomain){
            return hostname;
        }

        // IP addresses should be composed based on the full address.
        if (hostname.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")){
            return hostname;
        }

        // for single-level TLDs, we only want the TLD and the 2nd level domain
        final String[] hostParts = hostname.split("\\.");
        if (hostParts.length < 2){
            throw new IllegalDomainException("Invalid domain: '"+hostname+"'");
        }
        String domain = hostParts[hostParts.length-2] + '.' + hostParts[hostParts.length-1];

        // do a slow search of all the possible multi-level TLDs and
        // see if we need to pull in one level deeper.
        for (final String tld: domains){
            if (domain.equals(tld)){
                if (hostParts.length < 3){
                    throw new IllegalDomainException("Invalid domain. '"+domain+"' seems to be a TLD.");
                }
                domain = hostParts[hostParts.length - 3] + '.' + domain;
                break;
            }
        }
        return domain;
    }


    public void setCheckDomain(boolean checkDomain){
        this.checkDomain = checkDomain;
    }

    /**
     * Generates a domain password based on the PasswordComposer algorithm.
     *
     * @param masterPass master password
     * @param domain un-filtered domain (eg. www.example.org)
     * @return generated password
     * @throws PasswordGenerationException
     */
    public abstract String generate(String masterPass, String domain, int length) throws PasswordGenerationException;
}