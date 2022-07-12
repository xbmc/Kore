package org.xbmc.kore;

import android.content.Intent;

/**
 * Auxiliary activity with no UI that handles share intents to Queue an item on Kodi.
 * Delegates to {@link ShareOpenActivity} with queue set
 */
public class ShareQueueActivity extends ShareOpenActivity {

    @Override
    protected void handleStartIntent(Intent intent) {
        handleStartIntent(intent, true);
    }

}
