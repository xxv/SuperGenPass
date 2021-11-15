package info.staticfree.supergenpass

import android.content.res.Resources
import android.util.JsonReader
import androidx.annotation.WorkerThread
import org.json.JSONException
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import java.util.regex.Pattern

/**
 * Normalizes the domain name by lower-casing it and preserving common top-level domains.
 * e.g. "www.Google.com" will become "google.com"; "www.foo.co.uk" -> "foo.co.uk"
 */
class DomainNormalizer {
    private val domains: ArrayList<String> = ArrayList()

    /**
     * This list should remain the same and in sync with the canonical SGP, so that passwords
     * generated in one place are the same as others.
     *
     * @throws IOException on disk errors
     */
    @Throws(IOException::class)
    @WorkerThread
    fun loadDomains(resources: Resources) {
        val inputStream = resources.openRawResource(R.raw.domains)
        val jsonReader = JsonReader(InputStreamReader(inputStream))

        try {
            domains.clear()
            jsonReader.beginArray()

            while (jsonReader.hasNext()) {
                domains.add(jsonReader.nextString())
            }
            jsonReader.endArray()
            jsonReader.close()

        } catch (e: IOException) {
            val ioe = IOException("Unable to load domains")
            ioe.initCause(e)
            throw ioe
        } catch (e: JSONException) {
            val ioe = IOException("Unable to load domains")
            ioe.initCause(e)
            throw ioe
        }
    }

    /**
     * Computes the site's domain, based on the provided hostname. This takes into account things
     * like "co.uk" and other such multi-level TLDs.
     *
     * @param hostname the full hostname
     * @return the domain of the URI
     * @throws PasswordGenerationException if there is an error generating the password
     */
    @Throws(PasswordGenerationException::class)
    fun getDomain(hostname: String, stripDomain: Boolean): String {
        val hostnameLower = hostname.lowercase(Locale.US)
        if (!stripDomain) {
            return hostnameLower
        }

        // IP addresses should be composed based on the full address.
        if (PATTERN_IP_ADDRESS.matcher(hostnameLower).matches()) {
            return hostnameLower
        }

        // for single-level TLDs, we only want the TLD and the 2nd level domain
        val hostParts = hostnameLower.split(".").toTypedArray()
        if (hostParts.size < 2) {
            throw IllegalDomainException("Invalid domain: '$hostname'")
        }
        var domain = hostParts[hostParts.size - 2] + '.' + hostParts[hostParts.size - 1]

        // do a slow search of all the possible multi-level TLDs and
        // see if we need to pull in one level deeper.
        for (tld in domains) {
            if (domain == tld) {
                if (hostParts.size < 3) {
                    throw IllegalDomainException(
                        "Invalid domain. '$domain' seems to be a TLD."
                    )
                }
                domain = hostParts[hostParts.size - 3] + '.' + domain
                break
            }
        }
        return domain
    }

    companion object {
        private val PATTERN_IP_ADDRESS =
            Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")
    }
}