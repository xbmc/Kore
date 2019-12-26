package org.xbmc.kore.ui.sections.remote;

import android.content.Intent;

public class QueueActivity extends RemoteActivity {

    @Override
    protected void handleStartIntent(Intent intent) {
        handleStartIntent(intent, true);
    }

}
