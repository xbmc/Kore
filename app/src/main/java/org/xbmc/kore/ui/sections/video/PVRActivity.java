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
package org.xbmc.kore.ui.sections.video;

import android.annotation.TargetApi;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.transition.TransitionInflater;
import android.view.MenuItem;

import org.xbmc.kore.R;
import org.xbmc.kore.ui.BaseMediaActivity;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.Utils;

/**
 * Controls the presentation of Live TV/Radio and recordings information (list, details)
 * All the information is presented by specific fragments
 */
public class PVRActivity extends BaseMediaActivity
        implements PVRChannelsListFragment.OnPVRChannelSelectedListener {
    private static final String TAG = LogUtils.makeLogTag(PVRActivity.class);

    public static final String CHANNELID = "channel_id";
    public static final String CHANNELTITLE = "channel_title";

    public static final String CHANNELGROUPID = "channelgroupid";
    public static final String CHANNELGROUPTITLE = "channelgrouptitle";

    private static final String LISTFRAGMENTTAG = "listfragmenttag";

    private int selectedChannelId = -1;
    private String selectedChannelTitle = null;

    private int selectedChannelGroupId = -1;
    private String selectedChannelGroupTitle = null;

    @Override
    protected String getActionBarTitle() {
        if ( selectedChannelTitle != null ) {
            return selectedChannelTitle;
        } else if ( selectedChannelGroupTitle != null ) {
            return selectedChannelGroupTitle;
        }
        return getString(R.string.pvr);
    }

    @Override
    protected Fragment createFragment() {
        return new PVRListFragment();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            selectedChannelId = savedInstanceState.getInt(CHANNELID, -1);
            selectedChannelTitle = savedInstanceState.getString(CHANNELTITLE, null);

            selectedChannelGroupId = savedInstanceState.getInt(CHANNELGROUPID, -1);
            selectedChannelGroupTitle = savedInstanceState.getString(CHANNELGROUPTITLE, null);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CHANNELID, selectedChannelId);
        outState.putString(CHANNELTITLE, selectedChannelTitle);

        outState.putInt(CHANNELGROUPID, selectedChannelGroupId);
        outState.putString(CHANNELGROUPTITLE, selectedChannelGroupTitle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // If showing detail view, back up to list
                if (selectedChannelId != -1) {
                    selectedChannelId = -1;
                    selectedChannelTitle = null;
                    updateActionBar(getActionBarTitle(), true);
                    getSupportFragmentManager().popBackStack();
                    return true;
                } else if (selectedChannelGroupId != -1) {
                    selectedChannelGroupId = -1;
                    selectedChannelGroupTitle = null;
                    updateActionBar(getActionBarTitle(), false);

                    PVRListFragment fragment = (PVRListFragment)getSupportFragmentManager().findFragmentByTag(LISTFRAGMENTTAG);
                    if (fragment != null) {
                        fragment.onBackPressed();
                    }
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
        boolean handled = false;
        // If we are showing details in portrait, clear selected and show action bar
        if (selectedChannelId != -1) {
            selectedChannelId = -1;
            selectedChannelTitle = null;
            updateActionBar(getActionBarTitle(), true);
        } else {
            if (selectedChannelGroupId != -1) {
                selectedChannelGroupId = -1;
                selectedChannelGroupTitle = null;
                updateActionBar(getActionBarTitle(), true);
            }
            PVRListFragment fragment = (PVRListFragment)getSupportFragmentManager().findFragmentByTag(LISTFRAGMENTTAG);
            if (fragment != null) {
                handled = fragment.onBackPressed();
            }
        }
        if (!handled)
            super.onBackPressed();
    }

    /**
     * Callback from list fragment when the channel guide should be displayed.
     * Setup action bar and repolace list fragment
     * @param channelId Channel selected
     * @param channelTitle Title
     */
    @TargetApi(21)
    public void onChannelGuideSelected(int channelId, String channelTitle) {
        selectedChannelId = channelId;
        selectedChannelTitle = channelTitle;

        // Replace list fragment
        PVRChannelEPGListFragment pvrEPGFragment = PVRChannelEPGListFragment.newInstance(channelId);
        FragmentTransaction fragTrans = getSupportFragmentManager().beginTransaction();

        // Set up transitions
        if (Utils.isLollipopOrLater()) {
            pvrEPGFragment.setEnterTransition(
                    TransitionInflater.from(this)
                                      .inflateTransition(R.transition.media_details));
            pvrEPGFragment.setReturnTransition(null);
        } else {
            fragTrans.setCustomAnimations(R.anim.fragment_details_enter, 0,
                                          R.anim.fragment_list_popenter, 0);
        }

        fragTrans.replace(R.id.fragment_container, pvrEPGFragment)
                .addToBackStack(null)
                .commit();
        updateActionBar(getActionBarTitle(), true);
    }

    /**
     * Callback from list fragment when a channel group is selected
     * Just setup action bar
     * @param channelGroupId Channel group selected
     * @param channelGroupTitle Title
     */
    public void onChannelGroupSelected(int channelGroupId, String channelGroupTitle) {
        selectedChannelGroupId = channelGroupId;
        selectedChannelGroupTitle = channelGroupTitle;

        updateActionBar(getActionBarTitle(), true);
    }
}
