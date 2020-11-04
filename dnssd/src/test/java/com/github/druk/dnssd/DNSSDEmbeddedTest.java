package com.github.druk.dnssd;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@Ignore
@PrepareForTest({DNSSDEmbedded.class})
public class DNSSDEmbeddedTest {

    private static final int TIMEOUT = 5000;

    DNSSDEmbedded mDNSSDEmbedded;

    @Before
    public void setup() {
        Context context = mock(Context.class);
        mDNSSDEmbedded = new DNSSDEmbedded(context, TIMEOUT);
        mockStatic(DNSSDEmbedded.class);
    }

    @After
    public void tearDown() {
    }
}
