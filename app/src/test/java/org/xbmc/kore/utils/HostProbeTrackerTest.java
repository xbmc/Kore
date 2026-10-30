package org.xbmc.kore.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.xbmc.kore.host.HostConnection;
import org.xbmc.kore.host.HostInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HostProbeTrackerTest {
    private final HostInfo a = host(1), b = host(2), c = host(3);
    private final List<List<HostInfo>> results = new ArrayList<>();

    private static HostInfo host(int id) {
        return new HostInfo(id, "host" + id, "10.0.0." + id, HostConnection.PROTOCOL_TCP, 8080, 9090,
                            null, null, null, 9, false, false, 9777, 0, 0, null, null, 0, false);
    }

    private HostProbeTracker tracker(HostInfo... hosts) {
        return new HostProbeTracker(Arrays.asList(hosts), results::add);
    }

    @Test
    public void firesOnceWhenAllProbesFinishWithReachableHosts() {
        HostProbeTracker t = tracker(a, b, c);
        t.complete(2, true);
        t.complete(0, false);
        assertTrue(results.isEmpty());

        t.complete(1, true);
        assertEquals(1, results.size());
        assertEquals(Arrays.asList(c, b), results.get(0));
    }

    @Test
    public void probeCompletesOnlyOnce() {
        HostProbeTracker t = tracker(a, b);
        assertTrue(t.complete(0, true));
        assertFalse(t.complete(0, false));
        assertFalse(t.complete(0, true));
        assertTrue(results.isEmpty());

        t.complete(1, false);
        assertEquals(Collections.singletonList(a), results.get(0));
    }

    @Test
    public void expireCompletesUnfinishedProbesAsUnreachable() {
        HostProbeTracker t = tracker(a, b, c);
        t.complete(1, true);

        assertEquals(Arrays.asList(0, 2), t.expireUnfinished());
        assertEquals(1, results.size());
        assertEquals(Collections.singletonList(b), results.get(0));
    }

    @Test
    public void lateResultsAfterExpiryAreIgnored() {
        HostProbeTracker t = tracker(a, b);
        t.expireUnfinished();

        assertFalse(t.complete(0, true));
        assertFalse(t.complete(1, true));
        assertEquals(1, results.size());
        assertTrue(results.get(0).isEmpty());
    }

    @Test
    public void expireAfterAllDoneDoesNothing() {
        HostProbeTracker t = tracker(a);
        t.complete(0, true);

        assertTrue(t.expireUnfinished().isEmpty());
        assertEquals(1, results.size());
    }
}
