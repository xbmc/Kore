/*
 * Copyright (C) 2013, Antonio Mendes Silva
 */
package org.xbmc.kore.ui.sections.settings;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import org.xbmc.kore.R;


/**
 * Dialog fragment that presents about
 */
public class AboutDialogFragment
        extends DialogFragment {

    @Override
    @SuppressWarnings("InflateParams")
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Activity activity = getActivity();
        View mainView = activity.getLayoutInflater().inflate(R.layout.fragment_about, null);

        String versionName;
        try {
            versionName = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException exc) {
            versionName = null;
        }
        TextView version = (TextView) mainView.findViewById(R.id.app_version);
        version.setText(versionName);

        TextView about = (TextView)mainView.findViewById(R.id.about_desc);
        about.setText(Html.fromHtml(getString(R.string.about_desc)));
        about.setMovementMethod(LinkMovementMethod.getInstance());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Build the dialog
        builder.setView(mainView)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                });

        return builder.create();

    }
}
