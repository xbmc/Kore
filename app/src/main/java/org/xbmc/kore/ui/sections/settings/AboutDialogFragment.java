/*
 * Copyright (C) 2013, Antonio Mendes Silva
 */
package org.xbmc.kore.ui.sections.settings;


import android.app.Activity;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.xbmc.kore.R;

/**
 * Dialog fragment that presents about
 */
public class AboutDialogFragment
        extends DialogFragment {

    @NonNull
    @Override
    @SuppressWarnings("InflateParams")
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = requireActivity();
        View mainView = activity.getLayoutInflater().inflate(R.layout.fragment_about, null);

        String versionName;
        try {
            versionName = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException exc) {
            versionName = null;
        }
        TextView version = mainView.findViewById(R.id.app_version);
        version.setText(versionName);

        TextView about = mainView.findViewById(R.id.about_desc);
        about.setText(Html.fromHtml(getString(R.string.about_desc)));
        about.setMovementMethod(LinkMovementMethod.getInstance());

        return new AlertDialog.Builder(activity)
                .setView(mainView)
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }
}
