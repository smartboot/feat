package tech.smartboot.feat.cloud;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FeatCloudTest {

    @Test
    public void shouldDestroyApplicationWhenStartupFails() {
        final IllegalStateException startupFailure = new IllegalStateException("startup failed");
        final AtomicBoolean destroyed = new AtomicBoolean();

        try {
            FeatCloud.startApplicationContext(() -> {
                throw startupFailure;
            }, () -> destroyed.set(true));
            fail("Expected FeatException");
        } catch (tech.smartboot.feat.core.common.exception.FeatException e) {
            assertSame(startupFailure, e.getCause());
        }

        assertTrue("Application resources must be destroyed after startup failure", destroyed.get());
    }

    @Test
    public void shouldPreserveStartupFailureWhenCleanupAlsoFails() {
        final IllegalStateException startupFailure = new IllegalStateException("startup failed");
        final IllegalStateException cleanupFailure = new IllegalStateException("cleanup failed");

        try {
            FeatCloud.startApplicationContext(() -> {
                throw startupFailure;
            }, () -> {
                throw cleanupFailure;
            });
            fail("Expected FeatException");
        } catch (tech.smartboot.feat.core.common.exception.FeatException e) {
            assertSame(startupFailure, e.getCause());
            assertEquals(1, startupFailure.getSuppressed().length);
            assertArrayEquals(new Throwable[]{cleanupFailure}, startupFailure.getSuppressed());
        }
    }
}
