package org.xbmc.kore.utils;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class TaskSequenceTest {

    static final Task<?> SUCCESS = new Task<Object>() {
        @Override
        public void start(OnFinish<? super Object> then) {
            then.got(null);
        }
    };

    static final Task<?> FAILURE = new Task<Object>() {
        @Override
        public void start(OnFinish<? super Object> then) {
        }
    };

    static Task<?> hit(final AtomicInteger count, final Task<?> next) {
        return new Task<Object>() {
            @Override
            public void start(OnFinish<? super Object> then) {
                count.incrementAndGet();
                next.start(then);
            }
        };
    }

    static Task.OnFinish<Object> done(final AtomicInteger count) {
        return new Task.OnFinish<Object>() {
            @Override
            public void got(Object result) {
                count.incrementAndGet();
            }
        };
    }

    @Test
    public void happy_path() throws InterruptedException {
        AtomicInteger count = new AtomicInteger(0);
        new Task.Sequence<>(SUCCESS)
                .then(hit(count, SUCCESS))
                .then(hit(count, SUCCESS))
                .start(done(count));
        assertEquals(3, count.get());
    }

    @Test
    public void does_nothing_until_start_is_called() {
        AtomicInteger count = new AtomicInteger(0);
        Task<?> seq = new Task.Sequence<>(SUCCESS).then(hit(count, SUCCESS));
        assertEquals(0, count.get());
        seq.start(done(count));
        assertEquals(2, count.get());
    }

    @Test
    public void chain_is_stopped_when_a_continuation_is_not_called() {
        AtomicInteger count = new AtomicInteger(0);
        new Task.Sequence<>(SUCCESS)
                .then(hit(count, SUCCESS))
                .then(hit(count, SUCCESS))
                .then(hit(count, SUCCESS))
                .then(hit(count, FAILURE))
                .then(hit(count, SUCCESS))
                .then(hit(count, SUCCESS))
                .then(hit(count, SUCCESS))
                .then(hit(count, SUCCESS))
                .then(hit(count, SUCCESS))
                .start(done(count));
        assertEquals(4, count.get());  // number of successes before first failure
    }

    @Test
    public void tasks_are_called_in_order() {
        final StringBuilder cs = new StringBuilder();
        final AtomicBoolean done = new AtomicBoolean(false);
        new Task.Sequence<>(new Task<Object>() {
            @Override
            public void start(OnFinish<? super Object> then) {
                cs.append(1);
                then.got(null);
            }
        }).then(new Task<Object>() {
            @Override
            public void start(OnFinish<? super Object> then) {
                cs.append(',').append(2);
                then.got(null);
            }
        }).then(new Task<Object>() {
            @Override
            public void start(OnFinish<? super Object> then) {
                cs.append(',').append(3);
                then.got(null);
            }
        }).then(new Task<String>() {
            @Override
            public void start(OnFinish<? super String> then) {
                then.got(cs.toString());
            }
        }).start(new Task.OnFinish<String>() {
            @Override
            public void got(String result) {
                assertEquals("1,2,3", result);
                done.set(true);
            }
        });
        assertTrue(done.get());
    }

}
