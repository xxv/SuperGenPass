package info.staticfree.supergenpass.nfc;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.lang.ref.WeakReference;

import info.staticfree.supergenpass.R;

/**
 * A dialog box that prompts the user to tap an NFC tag and then writes it.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public final class NfcWriteFragment extends DialogFragment {
    private static final String TAG = NfcFragment.class.getSimpleName();
    private static final String ARG_PASSWORD = "password";

    private final MyHandler mHandler = new MyHandler(this);

    private final NfcAdapter.ReaderCallback mReaderCallback = new NfcAdapter.ReaderCallback() {
        @Override
        public void onTagDiscovered(final Tag tag) {
            mHandler.obtainMessage(MyHandler.MSG_WRITE_TAG, tag).sendToTarget();
        }
    };

    @Nullable
    private NfcAdapter mNfcAdapter;
    @NonNull
    private String mPassword = "";

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

        final String password = getArguments().getString(ARG_PASSWORD);

        if (password == null) {
            throw new IllegalArgumentException("Argument " + ARG_PASSWORD + " required");
        }

        mPassword = password;
    }

    /**
     * @param password the password to store
     * @return a new NfcWriteFragment that will write the given password to the tag.
     */
    @NonNull
    public static NfcWriteFragment newInstance(@NonNull final String password) {
        final NfcWriteFragment nfcWriteFragment = new NfcWriteFragment();
        final Bundle args = new Bundle();
        args.putString(ARG_PASSWORD, password);

        nfcWriteFragment.setArguments(args);

        return nfcWriteFragment;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mNfcAdapter != null) {
            if (mNfcAdapter.isEnabled()) {
                mNfcAdapter.enableReaderMode(getActivity(), mReaderCallback,
                        NfcAdapter.FLAG_READER_NFC_A, null);
            } else {
                new NfcEnableFragment()
                        .show(getFragmentManager(), NfcEnableFragment.class.getName());
                dismiss();
            }
        } else {
            Toast.makeText(getActivity(), R.string.nfc_error_no_adapter, Toast.LENGTH_LONG).show();
            dismiss();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mNfcAdapter != null) {
            mNfcAdapter.disableReaderMode(getActivity());
        }
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.nfc_write_tag_message);
        builder.setCancelable(true);

        builder.setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                dialog.cancel();
            }
        });

        return builder.create();
    }

    private void showWriteError(@NonNull final CharSequence message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
    }

    private void writeNfcTag(@NonNull final Tag tag) {
        final Ndef ndef = Ndef.get(tag);

        if (ndef == null) {
            showWriteError(getText(R.string.nfc_error_no_ndef));
            return;
        }

        if (!ndef.isWritable()) {
            showWriteError(getText(R.string.nfc_error_not_writable));
            return;
        }

        final NdefMessage message = NdefUtils.toNdefMessage(mPassword);

        final int messageLength = message.getByteArrayLength();
        final int maxSize = ndef.getMaxSize();

        if (messageLength > maxSize) {
            showWriteError(
                    getString(R.string.nfc_error_not_enough_space_format, messageLength, maxSize));
            return;
        }

        new TagWriteTask(this).execute(new TagWriteParam(ndef, message));
    }

    private static class MyHandler extends Handler {
        public static final int MSG_WRITE_TAG = 100;

        private final WeakReference<NfcWriteFragment> mNfcWriteFragmentRef;

        public MyHandler(@NonNull final NfcWriteFragment fragment) {
            super(Looper.getMainLooper());
            mNfcWriteFragmentRef = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(final Message msg) {
            final NfcWriteFragment fragment = mNfcWriteFragmentRef.get();
            if (fragment == null) {
                return;
            }

            switch (msg.what) {
                case MSG_WRITE_TAG:
                    fragment.writeNfcTag((Tag) msg.obj);
                    break;
            }
        }
    }

    private static class TagWriteTask extends AsyncTask<TagWriteParam, Void, Boolean> {
        @NonNull
        private final WeakReference<NfcWriteFragment> fragmentWeakReference;

        public TagWriteTask(@NonNull final NfcWriteFragment fragment) {
            fragmentWeakReference = new WeakReference<>(fragment);
        }

        @Nullable
        private Exception mException;

        @Override
        protected Boolean doInBackground(final TagWriteParam... params) {
            final Ndef ndef = params[0].mTag;
            final NdefMessage message = params[0].mMessage;

            try {
                ndef.connect();
                ndef.writeNdefMessage(message);

                Log.d(TAG, "Wrote tag");
                return true;
            } catch (IOException | FormatException e) {
                mException = e;
            }

            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            final NfcWriteFragment fragment = fragmentWeakReference.get();
            if (fragment != null && fragment.isResumed()) {
                if (success) {
                    Toast.makeText(fragment.getActivity(), R.string.nfc_message_write_success,
                            Toast.LENGTH_LONG).show();
                    fragment.dismiss();
                } else {
                    Log.e(TAG, "Error writing tag", mException);

                    final CharSequence userMessage;

                    if (mException != null) {
                        CharSequence message = mException.getMessage();

                        if (message == null) {
                            message = mException.getClass().getName();
                        }
                        userMessage =
                                fragment.getString(R.string.nfc_error_write_error_format, message);
                    } else {
                        userMessage = fragment.getText(R.string.nfc_error_write_error);
                    }

                    fragment.showWriteError(userMessage);
                }
            }
        }
    }

    private static final class TagWriteParam {
        public final Ndef mTag;
        public final NdefMessage mMessage;

        public TagWriteParam(@NonNull final Ndef tag, @NonNull final NdefMessage message) {
            mTag = tag;
            mMessage = message;
        }
    }
}
