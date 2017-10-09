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
package org.xbmc.kore.ui.sections.addon;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.MenuItem;

import org.xbmc.kore.R;
import org.xbmc.kore.ui.AbstractFragment;
import org.xbmc.kore.ui.BaseMediaActivity;
import org.xbmc.kore.ui.sections.remote.RemoteActivity;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.Utils;

/**
 * Controls the presentation of Addons information (list, details)
 * All the information is presented by specific fragments
 */
public class AddonsActivity extends BaseMediaActivity
        implements AddonListFragment.OnAddonSelectedListener {
    private static final String TAG = LogUtils.makeLogTag(AddonsActivity.class);

    public static final String ADDONID = "addon_id";
    public static final String ADDONTITLE = "addon_title";

    private String selectedAddonId;
    private String selectedAddonTitle;

    @Override
    protected Fragment createFragment() {
        return new AddonListFragment();
    }

    @Override
    protected String getActionBarTitle() {
        return TextUtils.isEmpty(selectedAddonTitle) ? getResources().getString(R.string.addons)
                                                     : selectedAddonTitle;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            selectedAddonId = savedInstanceState.getString(ADDONID, null);
            selectedAddonTitle = savedInstanceState.getString(ADDONTITLE, null);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ADDONID, selectedAddonId);
        outState.putString(ADDONTITLE, selectedAddonTitle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_show_remote:
                // Starts remote
                Intent launchIntent = new Intent(this, RemoteActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(launchIntent);
                return true;
            case android.R.id.home:
                // Only respond to this if we are showing the details in portrait mode,
                // which can be checked by checking if selected movie != -1, in which case we
                // should go back to the previous fragment, which is the list.
                if (selectedAddonId != null) {
                    selectedAddonId = null;
                    selectedAddonTitle = null;
                    updateActionBar(getActionBarTitle(), false);
                    getSupportFragmentManager().popBackStack();
                    return true;
                }
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // If we are showing details in portrait, clear selected and show action bar
        if (selectedAddonId != null) {
            selectedAddonId = null;
            selectedAddonTitle = null;
            updateActionBar(getActionBarTitle(), false);
        }
        super.onBackPressed();
    }

    /**
     * Callback from list fragment when a addon is selected.
     * Switch fragment in portrait
     * @param vh
     */
    @TargetApi(21)
    public void onAddonSelected(AddonListFragment.ViewHolder vh) {
        Bundle bundle = vh.dataHolder.getBundle();
        selectedAddonId = bundle.getString(AddonInfoFragment.BUNDLE_KEY_ADDONID);
        selectedAddonTitle = vh.dataHolder.getTitle();

        // Replace list fragment
        final AbstractFragment addonDetailsFragment =
                bundle.getBoolean(AddonInfoFragment.BUNDLE_KEY_BROWSABLE)
            ? new AddonDetailsFragment()
            : new AddonInfoFragment()
            ;
        addonDetailsFragment.setDataHolder(vh.dataHolder);
        vh.dataHolder.setSquarePoster(true);
        if(Utils.isLollipopOrLater()) {
            vh.dataHolder.setPosterTransitionName(vh.artView.getTransitionName());
        }
        showFragment(addonDetailsFragment, vh.artView, vh.dataHolder);

        updateActionBar(getActionBarTitle(), true);
    }
}
