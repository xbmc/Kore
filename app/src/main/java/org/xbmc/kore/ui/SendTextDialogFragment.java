/*
 * Copyright 2015 Synced Synapse. All rights reserved.
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
package org.xbmc.kore.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import org.xbmc.kore.R;

/**
 * Dialog that allows the user to send text
 */
public class SendTextDialogFragment extends DialogFragment {
    private static final String TITLE_KEY = "TITLE";

    // The listener activity we will call when the user finishes the selection
    private SendTextDialogListener mListener;

    /**
     * Interface to pass events back to the calling activity
     * The calling activity must implement this interface
     */
    public interface SendTextDialogListener {
        public void onSendTextFinished(String text, boolean done);
        public void onSendTextCancel();
    }

    /**
     * UI views
     */
    private EditText textToSend;
    private CheckBox finishAfterSend;

    /**
     * Create a new instance of the dialog, providing arguments.

     * @param title
     *        Title of the dialog
     * @return
     *        New dialog
     */
    public static SendTextDialogFragment newInstance(String title) {
        SendTextDialogFragment dialog = new SendTextDialogFragment();

        Bundle args = new Bundle();
        args.putString(TITLE_KEY, title);
        dialog.setArguments(args);
        return dialog;
    }


    /**
     * Override the attach to the activity to guarantee that the activity implements required interface
     *
     * @param activity
     *        Context activity that implements listener interface
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (SendTextDialogListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement SendTextDialogListener");
        }
    }

    /**
     * Create the dialog
     * @param savedInstanceState Saved state
     * @return Created dialog
     */
    @Override
    @SuppressWarnings("InflateParams")
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final String title = getArguments().getString(TITLE_KEY, getString(R.string.send_text));
        View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_send_text, null);

        textToSend = (EditText)dialogView.findViewById(R.id.text_to_send);
        finishAfterSend = (CheckBox)dialogView.findViewById(R.id.send_text_done);

        builder.setTitle(title)
                .setView(dialogView)
                .setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        mListener.onSendTextFinished(
                                textToSend.getText().toString(),
                                finishAfterSend.isChecked());
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onSendTextCancel();
                    }
                });

        final Dialog dialog = builder.create();
        textToSend.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
        textToSend.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    mListener.onSendTextFinished(
                            textToSend.getText().toString(),
                            finishAfterSend.isChecked());
                }
                dialog.dismiss();
                return false;
            }
        });
        return dialog;
    }
}
