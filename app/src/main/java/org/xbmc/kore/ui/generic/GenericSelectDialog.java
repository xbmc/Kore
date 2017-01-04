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

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

/**
 * Dialog fragment that presents a list options to the user.
 */
public class GenericSelectDialog
        extends DialogFragment
{
    // The listener activity we will call when the user finishes the selection
    private GenericSelectDialogListener mListener;

    /**
     * Interface to pass events back to the calling activity
     * The calling activity must implement this interface
     */
    public interface GenericSelectDialogListener {
        public void onDialogSelect(int token, int which);
    }

    private static final String TOKEN_KEY = "TOKEN",
            TITLE_KEY = "TITLE",
            ARRAY_ID_KEY = "ARRAY_ID_KEY",
            ARRAY_ITEMS = "ARRAY_ITEMS",
            SELECTED_ITEM_KEY = "SELECTED_ITEM";

    /**
     * Create a new instance of the dialog, providing arguments.
     * @param listener Listener for selection
     * @param token Token to be returned on the callback, to differentiate different calls
     * @param title Title of the dialog
     * @param arrayId String array id of the options to show in the dialog
     * @param selectedItem Index of the selected item
     */
    public static GenericSelectDialog newInstance(GenericSelectDialogListener listener,
                                                  int token, String title, int arrayId, int selectedItem) {
        GenericSelectDialog dialog = new GenericSelectDialog();
        // TODO: This isn't going to survive destroys, but it's the easiast way to communicate
        dialog.mListener = listener;

        Bundle args = new Bundle();
        args.putInt(TOKEN_KEY, token);
        args.putString(TITLE_KEY, title);
        args.putInt(ARRAY_ID_KEY, arrayId);
        args.putInt(SELECTED_ITEM_KEY, selectedItem);
        dialog.setArguments(args);

        return dialog;
    }

    /**
     * Create a new instance of the dialog, providing arguments.
     * @param listener Listener for selection
     * @param token Token to be returned on the callback, to differentiate different calls
     * @param title Title of the dialog
     * @param items String array of the options to show in the dialog
     * @param selectedItem Index of the selected item
     * @return New dialog
     */
    public static GenericSelectDialog newInstance(GenericSelectDialogListener listener,
                                                  int token, String title, CharSequence[] items, int selectedItem) {
        GenericSelectDialog dialog = new GenericSelectDialog();
        // TODO: This isn't going to survive destroys, but it's the easiast way to communicate
        dialog.mListener = listener;

        Bundle args = new Bundle();
        args.putInt(TOKEN_KEY, token);
        args.putString(TITLE_KEY, title);
        args.putCharSequenceArray(ARRAY_ITEMS, items);
        args.putInt(SELECTED_ITEM_KEY, selectedItem);
        dialog.setArguments(args);

        return dialog;
    }


    /**
     * Override the attach to the activity to guarantee that the activity implements
     * GenericSelectDialogListener interface.
     *
     * @param activity Context activity that implements GenericSelectDialogListener
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

//        // Try class cast
//        try {
//            mListener = (GenericSelectDialogListener)activity;
//        } catch (ClassCastException e) {
//            throw new ClassCastException(activity.toString() + " must implement GenericSelectDialogListener");
//        }
    }


    /**
     * Build the dialog
     *
     * @param savedInstanceState State
     *
     * @return Dialog to select calendars
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        Bundle args = getArguments();
        final String title = args.getString(TITLE_KEY);
        final int token = args.getInt(TOKEN_KEY);
        final int selectedItem = args.getInt(SELECTED_ITEM_KEY);

        builder.setTitle(title);
        if (getArguments().containsKey(ARRAY_ID_KEY)) {
            final int arrayId = args.getInt(ARRAY_ID_KEY);
            builder.setSingleChoiceItems(arrayId, selectedItem,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (mListener != null)
                                mListener.onDialogSelect(token, which);
                            dialog.dismiss();
                        }
                    });
        } else {
            final CharSequence[] items = args.getCharSequenceArray(ARRAY_ITEMS);

            // TODO: This should be a singleChoiceItems, but how do we include actions in it?
//            builder.setSingleChoiceItems(items, selectedItem,
//                    new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            if (mListener != null)
//                                mListener.onDialogSelect(token, which);
//                            dialog.dismiss();
//                        }
//                    });

            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (mListener != null)
                        mListener.onDialogSelect(token, which);
                    dialog.dismiss();
                }
            });

        }
        return builder.create();

    }

}
