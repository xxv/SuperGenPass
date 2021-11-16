package info.staticfree.supergenpass;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

/**
 * A Dialog that verifies that the master password was typed correctly.
 */
public class VerifyFragment extends DialogFragment {
    private static final String ARG_PASSWORD = "password";
    @NonNull
    private String mPasswordToCheck = "";

    /**
     * Shows the password verification dialog
     *
     * @param fragmentManager  Activity's fragment manager
     * @param passwordToVerify the password that must be entered to dismiss the dialog
     */
    // TODO pass in a salted hash of the password, not the password itself to avoid leakage.
    public static void showVerifyFragment(@NonNull FragmentManager fragmentManager,
                                          @NonNull String passwordToVerify) {
        VerifyFragment vf = new VerifyFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PASSWORD, passwordToVerify);
        vf.setArguments(args);
        vf.show(fragmentManager, VerifyFragment.class.getSimpleName());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPasswordToCheck = getArguments().getString(ARG_PASSWORD, "");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.dialog_verify_title);
        builder.setCancelable(true);
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View pwVerifyLayout =
                inflater.inflate(R.layout.master_pw_verify, (ViewGroup) getView());
        EditText pwVerify = pwVerifyLayout.findViewById(R.id.verify);

        builder.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(@NonNull DialogInterface dialog,
                                        int which) {
                        dialog.cancel();
                    }
                });

        pwVerify.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(@NonNull Editable s) {
                if (mPasswordToCheck.length() > 0 && mPasswordToCheck.equals(s.toString())) {
                    getDialog().dismiss();
                    Toast.makeText(getActivity().getApplicationContext(),
                            R.string.toast_verify_success, Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setView(pwVerifyLayout);
        Dialog d = builder.create();
        // This is added below to ensure that the soft input doesn't get hidden if it's
        // showing, which seems to be the default for dialogs.
        Window window = d.getWindow();

        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED);
        }

        return d;
    }
}
