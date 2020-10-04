/*
 * Copyright 2018 Dan Pasanen. All rights reserved.
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

package org.xbmc.kore.service;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.service.chooser.ChooserTarget;
import android.service.chooser.ChooserTargetService;

import java.util.ArrayList;
import java.util.List;

import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.R;

@TargetApi(Build.VERSION_CODES.M)
public class HostChooserTargetService extends ChooserTargetService {

    @Override
    public List<ChooserTarget> onGetChooserTargets(ComponentName targetActivityName, IntentFilter matchedFilter) {
        final List<ChooserTarget> targets = new ArrayList<>();
        HostManager hostManager = HostManager.getInstance(this);

        final Icon icon = Icon.createWithResource(this, R.mipmap.ic_launcher);
        final float score = 0;
        final ComponentName componentName = new ComponentName(getPackageName(), "org.xbmc.kore.ui.sections.remote.RemoteActivity");

        for (HostInfo host : hostManager.getHosts()) {
            if (!host.getShowAsDirectShareTarget()) {
                continue;
            }
            Bundle intentExtras = new Bundle();
            intentExtras.putInt("hostId", host.getId());
            targets.add(new ChooserTarget(host.getName(), icon, score, componentName, intentExtras));
        }

        return targets;
    }
}
