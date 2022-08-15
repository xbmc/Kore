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
package org.xbmc.kore.ui.sections.hosts;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.xbmc.kore.R;
import org.xbmc.kore.databinding.FragmentAddHostWelcomeBinding;

/**
 * Fragment that presents the welcome message
 */
public class AddHostFragmentWelcome extends Fragment {

    /**
     * Callback interface to communicate with the encolsing activity
     */
    public interface AddHostWelcomeListener {
        void onAddHostWelcomeNext();
        void onAddHostWelcomeCancel();
    }

    private AddHostWelcomeListener listener;
    private FragmentAddHostWelcomeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddHostWelcomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.addHostMessage.setText(Html.fromHtml(getString(R.string.wizard_welcome_message)));
        binding.addHostMessage.setMovementMethod(LinkMovementMethod.getInstance());

        // Next button
        binding.includeWizardButtonBar.next.setText(R.string.next);
        binding.includeWizardButtonBar.next.setOnClickListener(v -> listener.onAddHostWelcomeNext());

        // Previous button
        binding.includeWizardButtonBar.previous.setText(android.R.string.cancel);
        binding.includeWizardButtonBar.previous.setOnClickListener(v -> listener.onAddHostWelcomeCancel());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (AddHostWelcomeListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement AddHostWelcomeListener interface.");
        }
    }


}
