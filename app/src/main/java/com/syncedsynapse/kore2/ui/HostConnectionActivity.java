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
package com.syncedsynapse.kore2.ui;

import android.os.Bundle;

import com.syncedsynapse.kore2.host.HostManager;
import com.syncedsynapse.kore2.jsonrpc.HostConnection;

/**
 * This activity manages the closing of the {@link HostConnection} singleton provided by
 * {@link HostManager}.
 * All activities that plan to use the {@link HostConnection}, or their fragments do,
 * should inherit from this class to make sure the connection is closed onPause/
 */
public class HostConnectionActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

    @Override
    public void onPause() {
        super.onPause();

        // Disconnect from the connections used in the fragments
        HostConnection connection = HostManager.getInstance(this).getConnection();
        if (connection != null) {
            connection.disconnect();
        }

    }
}
