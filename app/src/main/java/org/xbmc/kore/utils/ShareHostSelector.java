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
import java.util.Comparator;
import java.util.List;

/**
 * Decides which Kodi host a shared item should be sent to.
 */
public final class ShareHostSelector {
    public enum Action {
        /** No host is reachable */
        NONE,
        /** Share to {@link Result#host} without asking */
        SHARE,
        /** Let the user choose among {@link Result#hosts} */
        PICK
    }

    public static final class Result {
        public final Action action;
        /** Host to share to when action is SHARE, otherwise null */
        public final HostInfo host;
        /** Reachable hosts in configured order */
        public final List<HostInfo> hosts;

        private Result(Action action, HostInfo host, List<HostInfo> hosts) {
            this.action = action;
            this.host = host;
            this.hosts = hosts;
        }
    }

    private ShareHostSelector() {}

    /**
     * @param configuredHosts All hosts in their configured order
     * @param reachableHosts Hosts that answered, in any order
     * @param currentHost Host Kore is currently using, may be null
     * @param appOpen Whether Kore currently has a screen open
     * @param alwaysAsk Whether the user wants the picker regardless of Kore's state
     */
    public static Result select(List<HostInfo> configuredHosts, List<HostInfo> reachableHosts,
                                HostInfo currentHost, boolean appOpen, boolean alwaysAsk) {
        List<HostInfo> ordered = new ArrayList<>(reachableHosts);
        ordered.sort(Comparator.comparingInt(a -> indexOf(configuredHosts, a)));
        ordered = Collections.unmodifiableList(ordered);

        if (ordered.isEmpty()) {
            return new Result(Action.NONE, null, ordered);
        }
        if (ordered.size() == 1) {
            return new Result(Action.SHARE, ordered.get(0), ordered);
        }
        if (!alwaysAsk && appOpen && currentHost != null) {
            for (HostInfo host : ordered) {
                if (host.getId() == currentHost.getId()) {
                    return new Result(Action.SHARE, host, ordered);
                }
            }
        }
        return new Result(Action.PICK, null, ordered);
    }

    private static int indexOf(List<HostInfo> hosts, HostInfo host) {
        for (int i = 0; i < hosts.size(); i++) {
            if (hosts.get(i).getId() == host.getId()) return i;
        }
        return Integer.MAX_VALUE;
    }
}
