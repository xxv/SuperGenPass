package info.staticfree.supergenpass.nfc;

import android.annotation.TargetApi;
import android.content.ClipDescription;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Utilities for working with SGP NDEF records.
 */
public final class NdefUtils {
    /**
     * MIME type representing a password stored on an NFC tag as plain UTF-8 text. This uses a
     * different domain than the rest of the app in order to keep the MIME type as short as
     * possible, while still being standards-compliant. This must match the value stored in the
     * AndroidManifest.xml too.
     */
    public static final String SGP_NFC_MIME_TYPE = "application/xxv.so.sgp";
    private static final int LANGUAGE_CODE_SIZE_BITMASK = 0b11_1111;
    private static final int UTF_16_BITMASK = 0b1000_0000;

    private NdefUtils() {
        throw new UnsupportedOperationException("This cannot be instantiated");
    }

    @NonNull
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static NdefMessage toNdefMessage(@NonNull final String password) {
        final NdefRecord ndefRecord = NdefRecord.createMime(SGP_NFC_MIME_TYPE,
                password.getBytes(StandardCharsets.UTF_8));

        return new NdefMessage(ndefRecord);
    }

    @NonNull
    public static String fromNdefRecord(@NonNull final NdefRecord record) {
        return new String(record.getPayload(), Charset.forName("utf-8"));
    }

    @Nullable
    public static String getMimeType(@NonNull final NdefRecord record) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return getMimeTypeV16(record);
        }

        /*
         * The below section
         * Copyright (C) 2010 The Android Open Source Project
         *
         * Licensed under the Apache License, Version 2.0 (the "License");
         * you may not use this file except in compliance with the License.
         * You may obtain a copy of the License at
         *
         *      http://www.apache.org/licenses/LICENSE-2.0
         *
         * Unless required by applicable law or agreed to in writing, software
         * distributed under the License is distributed on an "AS IS" BASIS,
         * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
         * See the License for the specific language governing permissions and
         * limitations under the License.
         */
        switch (record.getTnf()) {
            case NdefRecord.TNF_WELL_KNOWN:
                if (Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
                    return ClipDescription.MIMETYPE_TEXT_PLAIN;
                }
                break;
            case NdefRecord.TNF_MIME_MEDIA:
                final String mimeType = new String(record.getType(), StandardCharsets.US_ASCII);
                return Intent.normalizeMimeType(mimeType);
        }
        return null;
    }

    @Nullable
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static String getMimeTypeV16(@NonNull final NdefRecord record) {
        return record.toMimeType();
    }

    @NonNull
    public static String decodeNdefText(@NonNull final NdefRecord record) {
        final byte[] data = record.getPayload();
        final int langCodeSize = data[0] & LANGUAGE_CODE_SIZE_BITMASK;
        final Charset charset = ((data[0] & UTF_16_BITMASK) == 0) ? Charset.forName("utf-8") :
                Charset.forName("utf-16");

        return new String(data, langCodeSize + 1, data.length - langCodeSize - 1, charset);
    }
}
