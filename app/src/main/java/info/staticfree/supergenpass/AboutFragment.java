package info.staticfree.supergenpass;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

/**
 * The about dialog
 */
public class AboutFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.about_title);
        builder.setIcon(R.drawable.icon);

        // using this instead of setMessage lets us have clickable links.
        LayoutInflater factory = LayoutInflater.from(getActivity());
        builder.setView(factory.inflate(R.layout.about, (ViewGroup) getView()));

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getDialog().dismiss();
            }
        });
        return builder.create();
    }
}
