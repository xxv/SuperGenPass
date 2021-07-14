package info.staticfree.supergenpass.nfc;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import info.staticfree.supergenpass.R;

/**
 * A simple dialog to prompt the user to turn on NFC if it's off.
 */
public final class NfcEnableFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity()).setMessage(R.string.nfc_enable_message)
                .setPositiveButton(R.string.nfc_enable_dialog_go_to_settings,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                            }
                        })
                .setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                dismiss();
                            }
                        })
                .setCancelable(true)
                .create();
    }
}
