package com.apple.dnssd;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DNSSDEmbedded.class})
public class DNSSDEmbeddedTest {

    private static final int DEFAULT_TIMEOUT = 50;
    private static final int EXIT_TIMEOUT = 500;
    private static final int EXIT_TIMEOUT_TIMEOUT = 1000;

    DNSSDEmbedded mDNSSDEmbedded;

    @Before
    public void setup() {
        mDNSSDEmbedded = new DNSSDEmbedded(EXIT_TIMEOUT);
        mockStatic(DNSSDEmbedded.class);
    }

    @Test
    public void init() throws InterruptedException {
        mDNSSDEmbedded.init();
        PowerMockito.verifyStatic();
        DNSSDEmbedded.Init();
        PowerMockito.verifyStatic(timeout(DEFAULT_TIMEOUT));
        DNSSDEmbedded.Loop();
    }

    @Test
    public void initWithError() throws InterruptedException {
        PowerMockito.when(DNSSDEmbedded.Init()).thenReturn(-1);
        mDNSSDEmbedded.init();
        PowerMockito.verifyStatic();
        DNSSDEmbedded.Init();
        PowerMockito.verifyStatic(after(EXIT_TIMEOUT_TIMEOUT).never());
        DNSSDEmbedded.Loop();
    }

    @Test
    public void exit() throws InterruptedException {
        mDNSSDEmbedded.exit();
        PowerMockito.verifyStatic(timeout(EXIT_TIMEOUT + DEFAULT_TIMEOUT));
        DNSSDEmbedded.Exit();
    }

    @Test
    public void cancelStopTimer() throws InterruptedException {
        mDNSSDEmbedded.init();
        PowerMockito.verifyStatic();
        DNSSDEmbedded.Init();
        PowerMockito.verifyStatic(timeout(DEFAULT_TIMEOUT));
        DNSSDEmbedded.Loop();
        mDNSSDEmbedded.exit();
        Thread.sleep(EXIT_TIMEOUT/2);
        mDNSSDEmbedded.init();
        Thread.sleep(EXIT_TIMEOUT/2);
        PowerMockito.verifyStatic(never());
        DNSSDEmbedded.Exit();
        mDNSSDEmbedded.exit();
        PowerMockito.verifyStatic(timeout(EXIT_TIMEOUT_TIMEOUT));
        DNSSDEmbedded.Exit();
    }

    @After
    public void tearDown() {
    }
}
