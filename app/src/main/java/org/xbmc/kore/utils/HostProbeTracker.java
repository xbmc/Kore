/*
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
package org.xbmc.kore.utils;

import org.xbmc.kore.host.HostInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Tracks reachability probes of several hosts. Each probe completes exactly once, whether by its
 * own result or by {@link #expireUnfinished()}, and the listener fires once when all are done.
 * Not thread safe: use it from a single thread, e.g. the main thread.
 */
public final class HostProbeTracker {
    private final List<HostInfo> hosts;
    private final boolean[] done;
    private final List<HostInfo> reachable = new ArrayList<>();
    private final Consumer<List<HostInfo>> onAllDone;
    private int pending;

    /**
     * @param hosts Hosts being probed
     * @param onAllDone Called once with the reachable hosts, in completion order, when every probe is done
     */
    public HostProbeTracker(List<HostInfo> hosts, Consumer<List<HostInfo>> onAllDone) {
        this.hosts = new ArrayList<>(hosts);
        this.done = new boolean[hosts.size()];
        this.pending = hosts.size();
        this.onAllDone = onAllDone;
    }

    /**
     * @return true if this call completed the probe, false if it was already completed
     */
    public boolean complete(int index, boolean isReachable) {
        if (done[index]) {
            return false;
        }
        done[index] = true;
        if (isReachable) {
            reachable.add(hosts.get(index));
        }
        if (--pending == 0) {
            onAllDone.accept(Collections.unmodifiableList(reachable));
        }
        return true;
    }

    /**
     * Completes every unfinished probe as unreachable.
     * @return Indexes of the probes completed by this call
     */
    public List<Integer> expireUnfinished() {
        List<Integer> expired = new ArrayList<>();
        for (int i = 0; i < done.length; i++) {
            if (complete(i, false)) {
                expired.add(i);
            }
        }
        return expired;
    }
}
