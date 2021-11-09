package info.staticfree.supergenpass.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import java.nio.charset.StandardCharsets

/**
 * Utilities for working with SGP NDEF records.
 */
class NdefUtils private constructor() {
    companion object {
        /**
         * MIME type representing a password stored on an NFC tag as plain UTF-8 text. This uses a
         * different domain than the rest of the app in order to keep the MIME type as short as
         * possible, while still being standards-compliant. This must match the value stored in the
         * AndroidManifest.xml too.
         */
        const val SGP_NFC_MIME_TYPE = "application/xxv.so.sgp"
        private const val LANGUAGE_CODE_SIZE_BITMASK = 63
        private const val UTF_16_BITMASK = 128

        @JvmStatic
        fun toNdefMessage(password: String): NdefMessage {
            val ndefRecord = NdefRecord.createMime(
                SGP_NFC_MIME_TYPE,
                password.toByteArray(StandardCharsets.UTF_8)
            )
            return NdefMessage(ndefRecord)
        }

        @JvmStatic
        fun fromNdefRecord(record: NdefRecord): String {
            return String(record.payload, StandardCharsets.UTF_8)
        }

        @JvmStatic
        fun getMimeType(record: NdefRecord): String? {
            return record.toMimeType()
        }

        @JvmStatic
        fun decodeNdefText(record: NdefRecord): String {
            val data = record.payload
            val langCodeSize: Int = data[0].toInt() and LANGUAGE_CODE_SIZE_BITMASK
            val charset =
                if (data[0].toInt() and UTF_16_BITMASK == 0) StandardCharsets.UTF_8 else StandardCharsets.UTF_16
            return String(data, langCodeSize + 1, data.size - langCodeSize - 1, charset)
        }
    }

    init {
        throw UnsupportedOperationException("This cannot be instantiated")
    }
}