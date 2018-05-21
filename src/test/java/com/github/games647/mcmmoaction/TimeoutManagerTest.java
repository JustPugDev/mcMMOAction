package com.github.games647.mcmmoaction;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;

import static org.junit.Assert.assertThat;

public class TimeoutManagerTest {

    private TimeoutManager timeoutManager;

    @Before
    public void setUp() throws Exception {
        timeoutManager = new TimeoutManager(Duration.ofSeconds(2));
    }

    @Test
    public void testWaiting() throws Exception {
        UUID uuid = UUID.randomUUID();

        assertThat(timeoutManager.isAllowed(uuid), is(true));
        TimeUnit.SECONDS.sleep(1);
        assertThat(timeoutManager.isAllowed(uuid), is(false));

        TimeUnit.SECONDS.sleep(1);
        assertThat(timeoutManager.isAllowed(uuid), is(true));
    }

    @Test
    public void testMultipleUsers() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        assertThat(timeoutManager.isAllowed(first), is(true));
        TimeUnit.SECONDS.sleep(1);
        assertThat(timeoutManager.isAllowed(first), is(false));
        assertThat(timeoutManager.isAllowed(second), is(true));
    }
}
