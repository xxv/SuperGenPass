package info.staticfree.SuperGenPass.nfc;

import android.app.Fragment;
import android.app.PendingIntent;
import android.content.ClipDescription;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.support.annotation.NonNull;

/**
 * A non-layout fragment for handling NFC tag reading interactions. If NFC is disabled or not
 * present, this will do nothing.
 */
public abstract class NfcFragment extends Fragment {
    private static final IntentFilter[] SGP_PASSWORD_INTENT_FILTER =
            { new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED) };

    static {
        try {
            SGP_PASSWORD_INTENT_FILTER[0].addDataType(ClipDescription.MIMETYPE_TEXT_PLAIN);
            SGP_PASSWORD_INTENT_FILTER[0].addDataType(NdefUtils.SGP_NFC_MIME_TYPE);
        } catch (final IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean mShouldDisableForegroundDispatch;

    @Override
    public void onPause() {
        super.onPause();

        final NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

        if (nfcAdapter != null && mShouldDisableForegroundDispatch) {
            nfcAdapter.disableForegroundDispatch(getActivity());
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        final NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            final PendingIntent pendingIntent = PendingIntent.getActivity(getActivity(), 0,
                    new Intent(getActivity(), getActivity().getClass())
                            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

            nfcAdapter.enableForegroundDispatch(getActivity(), pendingIntent,
                    SGP_PASSWORD_INTENT_FILTER, null);
            mShouldDisableForegroundDispatch = true;

            handleNfcIntent(getActivity().getIntent());
        }
    }

    /**
     * Call this from {@link android.app.Activity#onNewIntent(Intent)} to handle NFC intents.
     *
     * @param intent the intent delivered from Android
     */
    public void handleNfcIntent(@NonNull final Intent intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            final Parcelable[] messages =
                    intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            if (messages != null && messages.length > 0) {
                for (final Parcelable messageParcelable : messages) {
                    for (final NdefRecord record : ((NdefMessage) messageParcelable).getRecords()) {
                        final String type = NdefUtils.getMimeType(record);

                        if (type == null) {
                            return;
                        }

                        switch (type) {
                            case ClipDescription.MIMETYPE_TEXT_PLAIN:
                                onNfcPasswordTag(NdefUtils.decodeNdefText(record));
                                break;
                            case NdefUtils.SGP_NFC_MIME_TYPE:
                                onNfcPasswordTag(NdefUtils.fromNdefRecord(record));
                                break;
                            default:
                                throw new IllegalArgumentException("Unhandled NFC content type");
                        }
                    }
                }
            }
        }
    }

    /**
     * Called when a password NFC tag is scanned in the foreground.
     *
     * @param password the password stored in the tag
     */
    public abstract void onNfcPasswordTag(@NonNull final CharSequence password);
}
