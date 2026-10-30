package org.xbmc.kore.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.xbmc.kore.host.HostConnection;
import org.xbmc.kore.host.HostInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ShareHostSelectorTest {
    private final HostInfo a = host(1), b = host(2), c = host(3);
    private final List<HostInfo> configured = Arrays.asList(a, b, c);

    private static HostInfo host(int id) {
        return new HostInfo(id, "host" + id, "10.0.0." + id, HostConnection.PROTOCOL_TCP, 8080, 9090,
                            null, null, null, 9, false, false, 9777, 0, 0, null, null, 0, false);
    }

    @Test
    public void noReachableHostsReturnsNone() {
        ShareHostSelector.Result r = ShareHostSelector.select(configured, Collections.emptyList(), a, true, false);
        assertEquals(ShareHostSelector.Action.NONE, r.action);
        assertNull(r.host);
    }

    @Test
    public void singleReachableHostIsSharedEvenIfNotCurrent() {
        ShareHostSelector.Result r = ShareHostSelector.select(configured, Arrays.asList(c), a, false, true);
        assertEquals(ShareHostSelector.Action.SHARE, r.action);
        assertEquals(c, r.host);
    }

    @Test
    public void multipleHostsWhenAppOpenUsesCurrentHost() {
        ShareHostSelector.Result r = ShareHostSelector.select(configured, Arrays.asList(a, b), b, true, false);
        assertEquals(ShareHostSelector.Action.SHARE, r.action);
        assertEquals(b, r.host);
    }

    @Test
    public void multipleHostsWhenAppClosedPicks() {
        ShareHostSelector.Result r = ShareHostSelector.select(configured, Arrays.asList(a, b), b, false, false);
        assertEquals(ShareHostSelector.Action.PICK, r.action);
    }

    @Test
    public void alwaysAskForcesPickerWhenAppOpen() {
        ShareHostSelector.Result r = ShareHostSelector.select(configured, Arrays.asList(a, b), b, true, true);
        assertEquals(ShareHostSelector.Action.PICK, r.action);
    }

    @Test
    public void currentHostNotReachablePicks() {
        ShareHostSelector.Result r = ShareHostSelector.select(configured, Arrays.asList(a, b), c, true, false);
        assertEquals(ShareHostSelector.Action.PICK, r.action);
    }

    @Test
    public void nullCurrentHostPicks() {
        ShareHostSelector.Result r = ShareHostSelector.select(configured, Arrays.asList(a, b), null, true, false);
        assertEquals(ShareHostSelector.Action.PICK, r.action);
    }

    @Test
    public void reachableHostsAreReturnedInConfiguredOrder() {
        ShareHostSelector.Result r = ShareHostSelector.select(configured, Arrays.asList(c, a, b), null, false, false);
        assertEquals(Arrays.asList(a, b, c), r.hosts);
    }
}
