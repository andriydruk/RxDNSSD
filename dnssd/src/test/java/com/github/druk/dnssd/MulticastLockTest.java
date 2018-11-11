package com.github.druk.dnssd;

import android.content.Context;
import android.net.wifi.WifiManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({InternalDNSSD.class})
public class MulticastLockTest {

    private WifiManager.MulticastLock multicastLock;
    private Context context;

    @Before
    public void prepare() {
        mockStatic(InternalDNSSD.class);
        multicastLock = mock(WifiManager.MulticastLock.class);
        WifiManager wifiManager = mock(WifiManager.class);
        when(wifiManager.createMulticastLock(any())).thenReturn(multicastLock);
        context = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(context);
        when(context.getSystemService(Context.WIFI_SERVICE)).thenReturn(wifiManager);
    }

    @Test
    public void testDNSSDBindable() throws DNSSDException {
        testDNSSD(new DNSSDBindable(context));
    }

    @Test
    public void testDNSSDEmbedded() throws DNSSDException {
        DNSSDEmbedded embedded = spy(new DNSSDEmbedded(context));
        doNothing().when(embedded).init();
        testDNSSD(embedded);
    }

    private void testDNSSD(DNSSD dnssd) throws DNSSDException {
        PowerMockito.when(InternalDNSSD.enumerateDomains(anyInt(), anyInt(), any(InternalDomainListener.class))).thenReturn(mock(DNSSDService.class));
        DomainListener listener = mock(DomainListener.class);
        DNSSDService service = dnssd.enumerateDomains(DNSSD.BROWSE_DOMAINS, 0, listener);
        verify(multicastLock).acquire();
        service.stop();
        verify(multicastLock).release();
    }

}
