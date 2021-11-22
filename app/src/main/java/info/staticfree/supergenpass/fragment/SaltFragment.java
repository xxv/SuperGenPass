package info.staticfree.supergenpass.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.zxing.integration.android.IntentIntegrator;

import org.apache.commons.codec.binary.Base64;

import java.security.SecureRandom;
import java.util.regex.Pattern;

import info.staticfree.supergenpass.R;

public class SaltFragment extends DialogFragment {
    /**
     * The size of the salt, in bytes.
     */
    private static final int SALT_SIZE_BYTES = 512;
    private static final Pattern PATTERN_WHITESPACE = Pattern.compile("\\s");

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.pref_generate_salt_title)
                .setMessage(R.string.pref_generate_salt_dialog_message)
                .setPositiveButton(R.string.pref_generate_salt_and_set,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                generateSalt();
                            }
                        }).setCancelable(true).setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(@NonNull DialogInterface dialog,
                                                int which) {
                                dialog.dismiss();
                            }
                        }).create();
    }

    private void generateSalt() {
        IntentIntegrator qr = new IntentIntegrator(getActivity());
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[SALT_SIZE_BYTES];
        sr.nextBytes(salt);
        String saltb64 = PATTERN_WHITESPACE.matcher(new String(Base64.encodeBase64(salt)))
                .replaceAll("");
        // TODO ((Preferences) getFragmentManager().findFragmentById(R.id.preferences)).setSaltPref(saltb64);
        qr.addExtra("SHOW_CONTENTS", false);
        qr.shareText(saltb64);
    }
}
