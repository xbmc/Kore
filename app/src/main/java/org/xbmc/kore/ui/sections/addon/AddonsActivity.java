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

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.text.TextDirectionHeuristicsCompat;
import androidx.fragment.app.Fragment;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostConnection;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.method.Input;
import org.xbmc.kore.ui.AbstractFragment;
import org.xbmc.kore.ui.BaseMediaActivity;
import org.xbmc.kore.ui.generic.SendTextDialogFragment;
import org.xbmc.kore.ui.sections.remote.RemoteActivity;
import org.xbmc.kore.utils.LogUtils;

import java.util.concurrent.TimeUnit;

/**
 * Controls the presentation of Addons information (list, details)
 * All the information is presented by specific fragments
 */
public class AddonsActivity extends BaseMediaActivity
        implements AddonListFragment.OnAddonSelectedListener,
                   SendTextDialogFragment.SendTextDialogListener {
    private static final String TAG = LogUtils.makeLogTag(AddonsActivity.class);

    public static final String ADDONID = "addon_id";
    public static final String ADDONTITLE = "addon_title";
    
    private static final String DUMMY_INPUT = "kore_dummy_input";

    private String selectedAddonId;
    private String selectedAddonTitle;

    private boolean dialogShown;

    @Override
    protected Fragment createFragment() {
        return new AddonListContainerFragment();
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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ADDONID, selectedAddonId);
        outState.putString(ADDONTITLE, selectedAddonTitle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_show_remote) {
            // Starts remote
            Intent launchIntent = new Intent(this, RemoteActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(launchIntent);
            return true;
        } else if (itemId == android.R.id.home) {
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
     */
    public void onAddonSelected(AbstractFragment.DataHolder dataHolder, ImageView sharedImageView) {
        Bundle bundle = dataHolder.getBundle();
        selectedAddonId = bundle.getString(AddonInfoFragment.BUNDLE_KEY_ADDONID);
        selectedAddonTitle = dataHolder.getTitle();

        // Replace list fragment
        dataHolder.setSquarePoster(true);
        if (bundle.getBoolean(AddonInfoFragment.BUNDLE_KEY_BROWSABLE)) {
            showFragment(AddonTabsFragment.class, dataHolder.getBundle());
        } else {
            showFragment(AddonInfoFragment.class, dataHolder.getBundle());
        }

        updateActionBar(getActionBarTitle(), true);
    }

    @Override
    public void onInputRequested(String title, String type, String value) {
        final SendTextDialogFragment dialog =
                SendTextDialogFragment.newInstance(title);
        dialog.show(getSupportFragmentManager(), null);
        dialogShown = true;
        Thread t = new Thread(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(HostConnection.TCP_READ_TIMEOUT - 2000);
                if (dialogShown) {
                    dialog.dismissAllowingStateLoss();
                    sendTextInput(DUMMY_INPUT, true, true);
                }
            } catch (InterruptedException e) {
                // ignore
            }
        });
        t.start();
    }

    /**
     * Callbacks from Send text dialog
     */
    @Override
    public void onSendTextFinished(String text, boolean done) {
        dialogShown = false;
        if (TextDirectionHeuristicsCompat.FIRSTSTRONG_LTR.isRtl(text, 0, text.length())) {
            text = new StringBuilder(text).reverse().toString();
        }
        sendTextInput(text,done, false);
    }

    @Override
    public void onSendTextCancel() {
        dialogShown = false;
        sendTextInput(DUMMY_INPUT, true, true);
    }

    private void sendTextInput(String text, boolean done, boolean isDummy) {
        HostManager hostManager = HostManager.getInstance(this);
        hostManager.getConnection().setIgnoreTcpResponse(isDummy);

        HostConnection httpHostConnection = new HostConnection(hostManager.getHostInfo());
        httpHostConnection.setProtocol(HostConnection.PROTOCOL_HTTP);

        Input.SendText action = new Input.SendText(text, done);
        action.execute(httpHostConnection, null, null);
    }
}
