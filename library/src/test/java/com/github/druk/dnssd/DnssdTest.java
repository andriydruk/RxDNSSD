/*
 * Copyright (C) 2016 Andriy Druk
 *
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
package com.github.druk.dnssd;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({InternalDNSSD.class, InetAddress.class, Inet4Address.class, Inet6Address.class})
@SuppressStaticInitializationFor("com.github,druk.dnssd.InternalDNSSD")
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class DnssdTest {

    static final int FLAGS = 0;
    static final int IF_INDEX = 0;
    static final String SERVICE_NAME_STRING = "serviceName";
    static final String REG_TYPE_STRING = "regType";
    static final String DOMAIN_STRING = "domain";
    static final int PORT = 443;
    static final String HOSTNAME_STRING = "hostname";

    static final byte[] SERVICE_NAME = SERVICE_NAME_STRING.getBytes();
    static final byte[] REG_TYPE = REG_TYPE_STRING.getBytes();
    static final byte[] DOMAIN = DOMAIN_STRING.getBytes();
    static final byte[] HOSTNAME = HOSTNAME_STRING.getBytes();

    static Inet4Address inet4Address = PowerMockito.mock(Inet4Address.class);

    Context appContext;
    DNSSDService mockService;
    DNSSD mDNSSD;

    InternalDNSSDService.DnssdServiceListener mockDNSSDServiceListener;

    @Before
    public void setup() {
        appContext = mock(Context.class);
        Context context = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(appContext);
        mockService = mock(DNSSDService.class);
        mockStatic(InetAddress.class);
        mockStatic(InternalDNSSD.class);
        mockDNSSDServiceListener = mock(InternalDNSSDService.DnssdServiceListener.class);
        Handler mockedHandler = mock(Handler.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Runnable runnable = (Runnable) invocation.getArguments()[0];
                runnable.run();
                return null;
            }
        }).when(mockedHandler).post(any(Runnable.class));
        mDNSSD = new DNSSD("test", mockedHandler) {

            @Override
            public void onServiceStarting() {
                mockDNSSDServiceListener.onServiceStarting();
            }

            @Override
            public void onServiceStopped() {
                mockDNSSDServiceListener.onServiceStopped();
            }
        };

        //PowerMockito.verifyStatic(times(1));
    }

    @Test
    public void test_browse_start_daemon() throws DNSSDException {
        PowerMockito.when(InternalDNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), any(InternalBrowseListener.class))).thenReturn(mockService);
        BrowseListener browseListener = mock(BrowseListener.class);
        mDNSSD.browse(REG_TYPE_STRING, browseListener);
        verify(mockDNSSDServiceListener).onServiceStarting();
    }

    @Test
    public void test_browse_found() throws Exception {
        PowerMockito.when(InternalDNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), any(InternalBrowseListener.class))).thenReturn(mockService);
        BrowseListener browseListener = mock(BrowseListener.class);
        mDNSSD.browse(REG_TYPE_STRING, browseListener);

        ArgumentCaptor<InternalBrowseListener> propertiesCaptor = ArgumentCaptor.forClass(InternalBrowseListener.class);
        PowerMockito.verifyStatic();
        InternalDNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), propertiesCaptor.capture());
        propertiesCaptor.getValue().serviceFound(mockService, FLAGS, IF_INDEX, SERVICE_NAME, REG_TYPE, DOMAIN);
        browseListener.serviceFound(mockService, FLAGS, IF_INDEX, SERVICE_NAME_STRING, REG_TYPE_STRING, DOMAIN_STRING);
    }

    @Test
    public void test_browse_lost() throws Exception {
        PowerMockito.when(InternalDNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), any(InternalBrowseListener.class))).thenReturn(mockService);
        BrowseListener browseListener = mock(BrowseListener.class);
        mDNSSD.browse(REG_TYPE_STRING, browseListener);

        ArgumentCaptor<InternalBrowseListener> propertiesCaptor = ArgumentCaptor.forClass(InternalBrowseListener.class);
        PowerMockito.verifyStatic();
        InternalDNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), propertiesCaptor.capture());
        propertiesCaptor.getValue().serviceLost(mockService, FLAGS, IF_INDEX, SERVICE_NAME, REG_TYPE, DOMAIN);
        verify(browseListener).serviceLost(any(DNSSDService.class), eq(FLAGS), eq(IF_INDEX), eq(SERVICE_NAME_STRING), eq(REG_TYPE_STRING), eq(DOMAIN_STRING));
    }

    @Test
    public void test_browse_operation_failed() throws Exception {
        PowerMockito.when(InternalDNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), any(InternalBrowseListener.class))).thenReturn(mockService);
        BrowseListener browseListener = mock(BrowseListener.class);
        mDNSSD.browse(REG_TYPE_STRING, browseListener);

        ArgumentCaptor<InternalBrowseListener> propertiesCaptor = ArgumentCaptor.forClass(InternalBrowseListener.class);
        PowerMockito.verifyStatic();
        InternalDNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), propertiesCaptor.capture());
        propertiesCaptor.getValue().operationFailed(mockService, -1);
        verify(browseListener).operationFailed(any(DNSSDService.class), eq(-1));
    }

    @Test
    public void test_resolve_start_daemon() throws DNSSDException {
        PowerMockito.when(InternalDNSSD.resolve(anyInt(), anyInt(), anyString(), anyString(), anyString(), any(InternalResolveListener.class))).thenReturn(mockService);
        ResolveListener resolveListener = mock(ResolveListener.class);
        mDNSSD.resolve(FLAGS, IF_INDEX, SERVICE_NAME_STRING, REG_TYPE_STRING, DOMAIN_STRING, resolveListener);
        verify(mockDNSSDServiceListener).onServiceStarting();
    }

    @Test
    public void test_resolve_successfully() throws DNSSDException {
        PowerMockito.when(InternalDNSSD.resolve(anyInt(), anyInt(), anyString(), anyString(), anyString(), any(InternalResolveListener.class))).thenReturn(mockService);
        ResolveListener resolveListener = mock(ResolveListener.class);
        mDNSSD.resolve(FLAGS, IF_INDEX, SERVICE_NAME_STRING, REG_TYPE_STRING, DOMAIN_STRING, resolveListener);

        ArgumentCaptor<InternalResolveListener> propertiesCaptor = ArgumentCaptor.forClass(InternalResolveListener.class);
        PowerMockito.verifyStatic();
        InternalDNSSD.resolve(anyInt(), anyInt(), anyString(), anyString(), anyString(), propertiesCaptor.capture());
        propertiesCaptor.getValue().serviceResolved(mockService, FLAGS, IF_INDEX, SERVICE_NAME, HOSTNAME, PORT, new TXTRecord());
        verify(resolveListener).serviceResolved(any(DNSSDService.class), eq(FLAGS), eq(IF_INDEX), eq(SERVICE_NAME_STRING), eq(HOSTNAME_STRING), eq(PORT), eq(new HashMap<String, String>()));
    }

    @Test
    public void test_resolve_failure() throws DNSSDException {
        PowerMockito.when(InternalDNSSD.resolve(anyInt(), anyInt(), anyString(), anyString(), anyString(), any(InternalResolveListener.class))).thenReturn(mockService);
        ResolveListener resolveListener = mock(ResolveListener.class);
        mDNSSD.resolve(FLAGS, IF_INDEX, SERVICE_NAME_STRING, REG_TYPE_STRING, DOMAIN_STRING, resolveListener);

        ArgumentCaptor<InternalResolveListener> propertiesCaptor = ArgumentCaptor.forClass(InternalResolveListener.class);
        PowerMockito.verifyStatic();
        InternalDNSSD.resolve(anyInt(), anyInt(), anyString(), anyString(), anyString(), propertiesCaptor.capture());
        propertiesCaptor.getValue().operationFailed(mockService, 0);
        verify(resolveListener).operationFailed(any(DNSSDService.class), eq(0));
    }

    @Test
    public void test_query_ipv4_records_start_daemon() throws DNSSDException {
        PowerMockito.when(InternalDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(InternalQueryListener.class))).thenReturn(mockService);
        QueryListener queryListener = mock(QueryListener.class);
        mDNSSD.queryRecord(FLAGS, IF_INDEX, SERVICE_NAME_STRING, 1, 1, queryListener);
        verify(mockDNSSDServiceListener).onServiceStarting();
    }

    @Ignore
    @Test
    public void test_query_ipv4_records_successfully() throws DNSSDException, UnknownHostException {
        PowerMockito.when(InternalDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(InternalQueryListener.class))).thenReturn(mockService);
        PowerMockito.when(InetAddress.getByAddress(any(byte[].class))).thenReturn(inet4Address);
        ArgumentCaptor<InternalQueryListener> propertiesCaptor = ArgumentCaptor.forClass(InternalQueryListener.class);

        QueryListener queryListener = mock(QueryListener.class);
        DNSSDService service = mDNSSD.queryRecord(FLAGS, IF_INDEX, SERVICE_NAME_STRING, 1, 1, queryListener);
        PowerMockito.verifyStatic();
        InternalDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, 0, 0, new byte[0], 0);

        verify(queryListener).queryAnswered(eq(service), eq(FLAGS), eq(IF_INDEX), eq(HOSTNAME_STRING), eq(0), eq(0), eq(inet4Address), eq(0));
    }

    @Test
    public void test_query_records_failure() throws DNSSDException {
        PowerMockito.when(InternalDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(InternalQueryListener.class))).thenReturn(mockService);
        ArgumentCaptor<InternalQueryListener> propertiesCaptor = ArgumentCaptor.forClass(InternalQueryListener.class);

        QueryListener queryListener = mock(QueryListener.class);
        mDNSSD.queryRecord(FLAGS, IF_INDEX, SERVICE_NAME_STRING, 1, 1, queryListener);

        PowerMockito.verifyStatic();
        InternalDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), propertiesCaptor.capture());
        propertiesCaptor.getValue().operationFailed(mockService, 0);

        verify(queryListener).operationFailed(any(DNSSDService.class), eq(0));
    }

    @Test
    public void test_query_records_exception() throws DNSSDException, UnknownHostException {
        PowerMockito.when(InternalDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(InternalQueryListener.class))).thenReturn(mockService);
        PowerMockito.when(InetAddress.getByAddress(any(byte[].class))).thenThrow(new UnknownHostException());
        ArgumentCaptor<InternalQueryListener> propertiesCaptor = ArgumentCaptor.forClass(InternalQueryListener.class);

        QueryListener queryListener = mock(QueryListener.class);
        mDNSSD.queryRecord(FLAGS, IF_INDEX, SERVICE_NAME_STRING, 1, 1, queryListener);

        PowerMockito.verifyStatic();
        InternalDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, 0, 0, new byte[0], 0);

        verify(queryListener).operationFailed(any(DNSSDService.class), eq(-1));
    }

}
