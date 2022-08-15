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
package org.xbmc.kore.ui.generic;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.xbmc.kore.R;
import org.xbmc.kore.databinding.DialogSendTextBinding;

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
        void onSendTextFinished(String text, boolean done);
        void onSendTextCancel();
    }

    private DialogSendTextBinding binding;

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
     * @param context Context activity that implements listener interface
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (SendTextDialogListener)context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement SendTextDialogListener");
        }
    }

    /**
     * Create the dialog
     * @param savedInstanceState Saved state
     * @return Created dialog
     */
    @NonNull
    @Override
    @SuppressWarnings("InflateParams")
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());

        assert getArguments() != null;
        final String title = getArguments().getString(TITLE_KEY, getString(R.string.send_text));
        binding = DialogSendTextBinding.inflate(requireActivity().getLayoutInflater(), null, false);

        builder.setTitle(title)
               .setView(binding.getRoot())
               .setPositiveButton(R.string.send, (dialog, which) -> {
                   if (binding.textToSend.getText() != null)
                       mListener.onSendTextFinished(binding.textToSend.getText().toString(),
                                                    binding.finishAfterSend.isChecked());
               })
               .setNegativeButton(android.R.string.cancel, (dialog, which) -> mListener.onSendTextCancel());

        final Dialog dialog = builder.create();
        binding.textToSend.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
        });
        binding.textToSend.requestFocus();
        binding.textToSend.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND ) {
                    onSendTextFinished();
                }  // handles enter key on external keyboard, issue #99
                else if (actionId == EditorInfo.IME_ACTION_UNSPECIFIED &&
                        (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    onSendTextFinished();
                }
                dialog.dismiss();
                return false;
            }

            private void onSendTextFinished() {
                if (binding.textToSend.getText() != null)
                    mListener.onSendTextFinished(binding.textToSend.getText().toString(),
                                                 binding.finishAfterSend.isChecked());}
        });
        return dialog;
    }
}
