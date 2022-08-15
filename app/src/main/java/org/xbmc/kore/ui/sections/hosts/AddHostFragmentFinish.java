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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.preference.PreferenceManager;

import android.os.Looper;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fasterxml.jackson.databind.JsonNode;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.databinding.FragmentAddHostFinishBinding;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.host.HostConnection;
import org.xbmc.kore.service.library.LibrarySyncService;
import org.xbmc.kore.ui.generic.NavigationDrawerFragment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Fragment that presents the welcome message
 */
public class AddHostFragmentFinish extends Fragment {

    /**
     * Callback interface to communicate with the encolsing activity
     */
    public interface AddHostFinishListener {
        void onAddHostFinish();
    }

    private AddHostFinishListener listener;
    private FragmentAddHostFinishBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddHostFinishBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.doneMessage.setText(Html.fromHtml(getString(R.string.wizard_done_message)));
        binding.doneMessage.setMovementMethod(LinkMovementMethod.getInstance());

        // Finish button
        binding.includeWizardButtonBar.next.setText(R.string.finish);
        binding.includeWizardButtonBar.next.setOnClickListener(v -> listener.onAddHostFinish());

        // Previous button
        binding.includeWizardButtonBar.previous.setVisibility(View.INVISIBLE);

        // Check if PVR is enabled for the current host
        HostManager hostManager = HostManager.getInstance(requireContext());
        if (hostManager.getHostInfo() != null) {
            checkPVREnabledAndSetMenuItems(getActivity(), new Handler(Looper.getMainLooper()));
        }

        // Start the syncing process
        Intent syncIntent = new Intent(this.getActivity(), LibrarySyncService.class);
        syncIntent.putExtra(LibrarySyncService.SYNC_ALL_MOVIES, true);
        syncIntent.putExtra(LibrarySyncService.SYNC_ALL_TVSHOWS, true);
        syncIntent.putExtra(LibrarySyncService.SYNC_ALL_MUSIC, true);
        syncIntent.putExtra(LibrarySyncService.SYNC_ALL_MUSIC_VIDEOS, true);
        requireActivity().startService(syncIntent);

//        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
//        imm.hideSoftInputFromWindow(getActivity()
//                        .getWindow()
//                        .getDecorView()
//                        .getRootView()
//                        .getWindowToken(), 0);
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
            listener = (AddHostFinishListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement AddHostFinishListener interface.");
        }
    }

    /**
     * Checks wheter PVR is enabled and sets a Preference that controls the items to show on
     * the navigation drawer accordingly: if PVR is disabled, hide the PVR item, otherwise show it
     *
     * @param context Context
     */
    public static void checkPVREnabledAndSetMenuItems(final Context context, Handler handler) {
        final HostConnection conn = HostManager.getInstance(context).getConnection();
        final int hostId = HostManager.getInstance(context).getHostInfo().getId();
        org.xbmc.kore.jsonrpc.method.Settings.GetSettingValue getSettingValue =
                new org.xbmc.kore.jsonrpc.method.Settings.GetSettingValue(org.xbmc.kore.jsonrpc.method.Settings.PVRMANAGER_ENABLED);
        getSettingValue.execute(conn, new ApiCallback<JsonNode>() {
            @Override
            public void onSuccess(JsonNode result) {
                // Result is boolean
                boolean isEnabled = result.asBoolean(false);
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

                Set<String> barItems = new HashSet<>(Arrays.asList(
                        context.getResources()
                                .getStringArray(R.array.default_values_remote_bar_items)));
                Set<String> navItems = new HashSet<>(Arrays.asList(
                        context.getResources()
                               .getStringArray(R.array.entry_values_nav_drawer_items)));
                if (!isEnabled)
                    navItems.remove(String.valueOf(NavigationDrawerFragment.ACTIVITY_PVR));
                sp.edit()
                  .putStringSet(Settings.getRemoteBarItemsPrefKey(hostId), barItems)
                  .putStringSet(Settings.getNavDrawerItemsPrefKey(hostId), navItems)
                  .apply();
            }

            @Override
            public void onError(int errorCode, String description) {
                // Ignore, use defaults
            }
        }, handler);
    }
}
