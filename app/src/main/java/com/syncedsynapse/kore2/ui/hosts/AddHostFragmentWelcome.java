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
package com.syncedsynapse.kore2.ui.hosts;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.syncedsynapse.kore2.R;

/**
 * Fragment that presents the welcome message
 */
public class AddHostFragmentWelcome extends Fragment {

    /**
     * Callback interface to communicate with the encolsing activity
     */
    public interface AddHostWelcomeListener {
        public void onAddHostWelcomeNext();
        public void onAddHostWelcomeCancel();
    }

    private AddHostWelcomeListener listener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_host_welcome, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getView() == null)
            return;

        TextView message = (TextView)getView().findViewById(R.id.add_host_message);
        message.setText(Html.fromHtml(getString(R.string.wizard_welcome_message)));
        message.setMovementMethod(LinkMovementMethod.getInstance());

        // Next button
        Button next = (Button)getView().findViewById(R.id.next);
        next.setText(R.string.next);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onAddHostWelcomeNext();
            }
        });

        // Previous button
        Button previous = (Button)getView().findViewById(R.id.previous);
        previous.setText(android.R.string.cancel);
        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onAddHostWelcomeCancel();
            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (AddHostWelcomeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement AddHostWelcomeListener interface.");
        }
    }


}
