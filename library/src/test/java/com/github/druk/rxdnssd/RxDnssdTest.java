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
package com.github.druk.rxdnssd;

import com.apple.dnssd.BrowseListener;
import com.apple.dnssd.DNSSD;
import com.apple.dnssd.DNSSDException;
import com.apple.dnssd.DNSSDService;
import com.apple.dnssd.QueryListener;
import com.apple.dnssd.ResolveListener;
import com.apple.dnssd.TXTRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import android.content.Context;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

import rx.Observable;
import rx.Scheduler;
import rx.android.plugins.RxAndroidPlugins;
import rx.android.plugins.RxAndroidSchedulersHook;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DNSSD.class, RxQueryListener.class, Inet4Address.class, Inet6Address.class})
@SuppressStaticInitializationFor("com.apple.dnssd.DNSSD")
public class RxDnssdTest {

    static final int FLAGS = 0;
    static final int IF_INDEX = 0;
    static final String SERVICE_NAME = "serviceName";
    static final String REG_TYPE = "regType";
    static final String DOMAIN = "domain";
    static final int PORT = 443;
    static final String HOSTNAME = "hostname";

    static BonjourService bonjourService = new BonjourService.Builder(FLAGS, IF_INDEX, SERVICE_NAME, REG_TYPE, DOMAIN).build();
    static BonjourService lostBonjourService = new BonjourService.Builder(BonjourService.LOST, IF_INDEX, SERVICE_NAME, REG_TYPE, DOMAIN).build();
    static BonjourService resolvedBonjourService = new BonjourService.Builder(bonjourService).port(PORT).hostname(HOSTNAME)
            .dnsRecords(new HashMap<String, String>(0)).build();

    static Inet4Address inet4Address = PowerMockito.mock(Inet4Address.class);
    static Inet6Address inet6Address = PowerMockito.mock(Inet6Address.class);

    static BonjourService resolvedBonjourServiceWithIpv4 = new BonjourService.Builder(resolvedBonjourService).inet4Address(inet4Address).build();
    static BonjourService resolvedBonjourServiceWithIpv6 = new BonjourService.Builder(resolvedBonjourService).inet6Address(inet6Address).build();
    static BonjourService resolvedBonjourServiceWithBothIp = new BonjourService.Builder(resolvedBonjourService).inet4Address(inet4Address)
            .inet6Address(inet6Address).build();

    Context appContext;
    DNSSDService mockService;

    @Before
    public void setup() {
        appContext = mock(Context.class);
        Context context = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(appContext);
        RxDnssd.init(context);
        RxAndroidPlugins.getInstance().registerSchedulersHook(new RxAndroidSchedulersHook() {
            @Override
            public Scheduler getMainThreadScheduler() {
                return Schedulers.immediate();
            }
        });
        mockService = mock(DNSSDService.class);
        mockStatic(DNSSD.class);
        mockStatic(InetAddress.class);
    }

    @Test
    public void test_browse_unsubscribe() throws DNSSDException {
        PowerMockito.when(DNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), any(BrowseListener.class))).thenReturn(mockService);
        RxDnssd.browse(REG_TYPE, DOMAIN).subscribe(new TestSubscriber<>()).unsubscribe();
        verify(mockService).stop();
    }

    @Test
    public void test_browse_start_daemon() throws DNSSDException {
        PowerMockito.when(DNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), any(BrowseListener.class))).thenReturn(mockService);
        RxDnssd.browse(REG_TYPE, DOMAIN).subscribe(new TestSubscriber<>());
        verify(appContext).getSystemService(Context.NSD_SERVICE);
    }

    @Test
    public void test_browse_found() throws Exception {
        PowerMockito.when(DNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), any(BrowseListener.class))).thenReturn(mockService);
        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        RxDnssd.browse(REG_TYPE, DOMAIN).subscribe(testSubscriber);

        ArgumentCaptor<BrowseListener> propertiesCaptor = ArgumentCaptor.forClass(BrowseListener.class);
        PowerMockito.verifyStatic();
        DNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), propertiesCaptor.capture());
        propertiesCaptor.getValue().serviceFound(mockService, FLAGS, IF_INDEX, SERVICE_NAME, REG_TYPE, DOMAIN);
        testSubscriber.assertValue(bonjourService);
    }

    @Test
    public void test_browse_found_after_unsubscribe() throws Exception {
        PowerMockito.when(DNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), any(BrowseListener.class))).thenReturn(mockService);
        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        RxDnssd.browse(REG_TYPE, DOMAIN).subscribe(testSubscriber).unsubscribe();

        ArgumentCaptor<BrowseListener> propertiesCaptor = ArgumentCaptor.forClass(BrowseListener.class);
        PowerMockito.verifyStatic();
        DNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), propertiesCaptor.capture());
        propertiesCaptor.getValue().serviceFound(mockService, FLAGS, IF_INDEX, SERVICE_NAME, REG_TYPE, DOMAIN);
        testSubscriber.assertNoValues();
    }

    @Test
    public void test_browse_lost() throws Exception {
        PowerMockito.when(DNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), any(BrowseListener.class))).thenReturn(mockService);
        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        RxDnssd.browse(REG_TYPE, DOMAIN).subscribe(testSubscriber);

        ArgumentCaptor<BrowseListener> propertiesCaptor = ArgumentCaptor.forClass(BrowseListener.class);
        PowerMockito.verifyStatic();
        DNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), propertiesCaptor.capture());
        propertiesCaptor.getValue().serviceLost(mockService, FLAGS, IF_INDEX, SERVICE_NAME, REG_TYPE, DOMAIN);
        testSubscriber.assertValue(lostBonjourService);
    }

    @Test
    public void test_browse_lost_after_unsubscribe() throws Exception {
        PowerMockito.when(DNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), any(BrowseListener.class))).thenReturn(mockService);
        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        RxDnssd.browse(REG_TYPE, DOMAIN).subscribe(testSubscriber).unsubscribe();

        ArgumentCaptor<BrowseListener> propertiesCaptor = ArgumentCaptor.forClass(BrowseListener.class);
        PowerMockito.verifyStatic();
        DNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), propertiesCaptor.capture());
        propertiesCaptor.getValue().serviceLost(mockService, FLAGS, IF_INDEX, SERVICE_NAME, REG_TYPE, DOMAIN);
        testSubscriber.assertNoValues();
    }

    @Test
    public void test_browse_operation_failed() throws Exception {
        PowerMockito.when(DNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), any(BrowseListener.class))).thenReturn(mockService);
        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        RxDnssd.browse(REG_TYPE, DOMAIN).subscribe(testSubscriber);

        ArgumentCaptor<BrowseListener> propertiesCaptor = ArgumentCaptor.forClass(BrowseListener.class);
        PowerMockito.verifyStatic();
        DNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), propertiesCaptor.capture());
        propertiesCaptor.getValue().operationFailed(mockService, 0);
        testSubscriber.assertError(RuntimeException.class);
    }

    @Test
    public void test_browse_operation_failed_after_unsubscribe() throws Exception {
        DNSSDService mockService = mock(DNSSDService.class);
        mockStatic(DNSSD.class);
        PowerMockito.when(DNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), any(BrowseListener.class))).thenReturn(mockService);
        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        RxDnssd.browse(REG_TYPE, DOMAIN).subscribe(testSubscriber).unsubscribe();

        ArgumentCaptor<BrowseListener> propertiesCaptor = ArgumentCaptor.forClass(BrowseListener.class);
        PowerMockito.verifyStatic();
        DNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), propertiesCaptor.capture());
        propertiesCaptor.getValue().operationFailed(mockService, 0);
        testSubscriber.assertNoErrors();
    }

    @Test
    public void test_resolve_unsubscribe() throws DNSSDException {
        PowerMockito.when(DNSSD.resolve(anyInt(), anyInt(), anyString(), anyString(), anyString(), any(ResolveListener.class))).thenReturn(mockService);
        Observable.just(bonjourService).compose(RxDnssd.resolve()).subscribe(new TestSubscriber<BonjourService>()).unsubscribe();
        verify(mockService).stop();
    }

    @Test
    public void test_resolve_start_daemon() throws DNSSDException {
        PowerMockito.when(DNSSD.resolve(anyInt(), anyInt(), anyString(), anyString(), anyString(), any(ResolveListener.class))).thenReturn(mockService);
        Observable.just(bonjourService).compose(RxDnssd.resolve()).subscribe(new TestSubscriber<BonjourService>()).unsubscribe();
        verify(appContext).getSystemService(Context.NSD_SERVICE);
    }

    @Test
    public void test_resolve_ignore_lost() throws DNSSDException {
        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(lostBonjourService).compose(RxDnssd.resolve()).subscribe(testSubscriber);
        testSubscriber.assertValue(lostBonjourService);
        testSubscriber.assertCompleted();
    }

    @Test
    public void test_resolve_successfully() throws DNSSDException {
        PowerMockito.when(DNSSD.resolve(anyInt(), anyInt(), anyString(), anyString(), anyString(), any(ResolveListener.class))).thenReturn(mockService);
        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(bonjourService).compose(RxDnssd.resolve()).subscribe(testSubscriber);

        ArgumentCaptor<ResolveListener> propertiesCaptor = ArgumentCaptor.forClass(ResolveListener.class);
        PowerMockito.verifyStatic();
        DNSSD.resolve(anyInt(), anyInt(), anyString(), anyString(), anyString(), propertiesCaptor.capture());
        propertiesCaptor.getValue().serviceResolved(mockService, FLAGS, IF_INDEX, SERVICE_NAME, HOSTNAME, PORT, new TXTRecord());
        testSubscriber.assertValue(resolvedBonjourService);
        testSubscriber.assertCompleted();
    }

    @Test
    public void test_resolve_successfully_after_unsubscribe() throws DNSSDException {
        PowerMockito.when(DNSSD.resolve(anyInt(), anyInt(), anyString(), anyString(), anyString(), any(ResolveListener.class))).thenReturn(mockService);
        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(bonjourService).compose(RxDnssd.resolve()).subscribe(testSubscriber).unsubscribe();

        ArgumentCaptor<ResolveListener> propertiesCaptor = ArgumentCaptor.forClass(ResolveListener.class);
        PowerMockito.verifyStatic();
        DNSSD.resolve(anyInt(), anyInt(), anyString(), anyString(), anyString(), propertiesCaptor.capture());
        propertiesCaptor.getValue().serviceResolved(mockService, FLAGS, IF_INDEX, SERVICE_NAME, HOSTNAME, PORT, new TXTRecord());
        testSubscriber.assertNoValues();
        testSubscriber.assertNoTerminalEvent();
    }

    @Test
    public void test_resolve_failure() throws DNSSDException {
        PowerMockito.when(DNSSD.resolve(anyInt(), anyInt(), anyString(), anyString(), anyString(), any(ResolveListener.class))).thenReturn(mockService);
        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(bonjourService).compose(RxDnssd.resolve()).subscribe(testSubscriber);

        ArgumentCaptor<ResolveListener> propertiesCaptor = ArgumentCaptor.forClass(ResolveListener.class);
        PowerMockito.verifyStatic();
        DNSSD.resolve(anyInt(), anyInt(), anyString(), anyString(), anyString(), propertiesCaptor.capture());
        propertiesCaptor.getValue().operationFailed(mockService, 0);
        testSubscriber.assertError(RuntimeException.class);
    }

    @Test
    public void test_resolve_failure_after_unsubscribe() throws DNSSDException {
        PowerMockito.when(DNSSD.resolve(anyInt(), anyInt(), anyString(), anyString(), anyString(), any(ResolveListener.class))).thenReturn(mockService);
        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(bonjourService).compose(RxDnssd.resolve()).subscribe(testSubscriber).unsubscribe();

        ArgumentCaptor<ResolveListener> propertiesCaptor = ArgumentCaptor.forClass(ResolveListener.class);
        PowerMockito.verifyStatic();
        DNSSD.resolve(anyInt(), anyInt(), anyString(), anyString(), anyString(), propertiesCaptor.capture());
        propertiesCaptor.getValue().operationFailed(mockService, 0);
        testSubscriber.assertNoErrors();
    }

    @Test
    public void test_query_ipv4_records_unsubscribe() throws DNSSDException {
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);
        Observable.just(resolvedBonjourService).compose(RxDnssd.queryIPV4Records()).subscribe(new TestSubscriber<BonjourService>()).unsubscribe();
        verify(mockService).stop();
    }

    @Test
    public void test_query_ipv6_records_unsubscribe() throws DNSSDException {
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);
        Observable.just(resolvedBonjourService).compose(RxDnssd.queryIPV6Records()).subscribe(new TestSubscriber<BonjourService>()).unsubscribe();
        verify(mockService).stop();
    }

    @Test
    public void test_query_records_unsubscribe() throws DNSSDException {
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);
        Observable.just(resolvedBonjourService).compose(RxDnssd.queryRecords()).subscribe(new TestSubscriber<BonjourService>()).unsubscribe();
        verify(mockService, times(2)).stop();
    }

    @Test
    public void test_query_ipv4_records_start_daemon() throws DNSSDException {
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);
        Observable.just(resolvedBonjourService).compose(RxDnssd.queryIPV4Records()).subscribe(new TestSubscriber<BonjourService>()).unsubscribe();
        verify(appContext).getSystemService(Context.NSD_SERVICE);
    }

    @Test
    public void test_query_ipv6_records_start_daemon() throws DNSSDException {
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);
        Observable.just(resolvedBonjourService).compose(RxDnssd.queryIPV6Records()).subscribe(new TestSubscriber<BonjourService>()).unsubscribe();
        verify(appContext).getSystemService(Context.NSD_SERVICE);
    }

    @Test
    public void test_query_records_start_daemon() throws DNSSDException {
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);
        Observable.just(resolvedBonjourService).compose(RxDnssd.queryRecords()).subscribe(new TestSubscriber<BonjourService>()).unsubscribe();
        verify(appContext, times(2)).getSystemService(Context.NSD_SERVICE);
    }

    @Test
    public void test_query_ipv4_records_ignore_lost() throws DNSSDException {
        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(lostBonjourService).compose(RxDnssd.queryIPV4Records()).subscribe(testSubscriber);
        testSubscriber.assertValue(lostBonjourService);
        testSubscriber.assertCompleted();
    }

    @Test
    public void test_query_ipv6_records_ignore_lost() throws DNSSDException {
        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(lostBonjourService).compose(RxDnssd.queryIPV6Records()).subscribe(testSubscriber);
        testSubscriber.assertValue(lostBonjourService);
        testSubscriber.assertCompleted();
    }

    @Test
    public void test_query_records_ignore_lost() throws DNSSDException {
        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(lostBonjourService).compose(RxDnssd.queryRecords()).subscribe(testSubscriber);
        testSubscriber.assertValue(lostBonjourService);
        testSubscriber.assertCompleted();
    }

    @Test
    public void test_query_ipv4_records_successfully() throws DNSSDException, UnknownHostException {
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);
        PowerMockito.when(InetAddress.getByAddress(any(byte[].class))).thenReturn(inet4Address);
        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(RxDnssd.queryIPV4Records()).subscribe(testSubscriber);
        PowerMockito.verifyStatic();
        DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, 0, 0, new byte[0], 0);
        testSubscriber.assertValue(resolvedBonjourServiceWithIpv4);
        testSubscriber.assertCompleted();
    }

    @Test
    public void test_query_ipv6_records_successfully() throws DNSSDException, UnknownHostException {
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);
        PowerMockito.when(InetAddress.getByAddress(any(byte[].class))).thenReturn(inet6Address);
        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(RxDnssd.queryIPV6Records()).subscribe(testSubscriber);
        PowerMockito.verifyStatic();
        DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, 0, 0, null, 0);
        testSubscriber.assertValue(resolvedBonjourServiceWithIpv6);
        testSubscriber.assertCompleted();
    }

    @Test
    public void test_query_records_successfully_ipv4_first() throws DNSSDException, UnknownHostException {
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);
        byte[] ipv4 = new byte[]{0};
        byte[] ipv6 = new byte[]{1};
        PowerMockito.when(InetAddress.getByAddress(ipv4)).thenReturn(inet4Address);
        PowerMockito.when(InetAddress.getByAddress(ipv6)).thenReturn(inet6Address);
        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(RxDnssd.queryRecords()).subscribe(testSubscriber);
        PowerMockito.verifyStatic();
        DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, 0, 0, ipv4, 0);
        PowerMockito.verifyStatic();
        DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, 0, 0, ipv6, 0);
        testSubscriber.assertValues(resolvedBonjourServiceWithIpv4, resolvedBonjourServiceWithBothIp);
        testSubscriber.assertCompleted();
    }

    @Test
    public void test_query_records_successfully_ipv6_first() throws DNSSDException, UnknownHostException {
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);
        byte[] ipv4 = new byte[]{0};
        byte[] ipv6 = new byte[]{1};
        PowerMockito.when(InetAddress.getByAddress(ipv4)).thenReturn(inet4Address);
        PowerMockito.when(InetAddress.getByAddress(ipv6)).thenReturn(inet6Address);
        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(RxDnssd.queryRecords()).subscribe(testSubscriber);
        PowerMockito.verifyStatic();
        DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, 0, 0, ipv6, 0);
        PowerMockito.verifyStatic();
        DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, 0, 0, ipv4, 0);
        testSubscriber.assertValues(resolvedBonjourServiceWithIpv6, resolvedBonjourServiceWithBothIp);
        testSubscriber.assertCompleted();
    }

    @Test
    public void test_query_ipv4_records_exception() throws DNSSDException, UnknownHostException {
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);
        PowerMockito.when(InetAddress.getByAddress(any(byte[].class))).thenThrow(new UnknownHostException());

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(RxDnssd.queryIPV4Records()).subscribe(testSubscriber);

        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);
        PowerMockito.verifyStatic();
        DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), propertiesCaptor.capture());
        QueryListener queryListener = propertiesCaptor.getValue();

        queryListener.queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, 0, 0, new byte[0], 0);
        testSubscriber.assertError(UnknownHostException.class);
    }

    @Test
    public void test_query_ipv6_records_exception() throws DNSSDException, UnknownHostException {
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);
        PowerMockito.when(InetAddress.getByAddress(any(byte[].class))).thenThrow(new UnknownHostException());

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(RxDnssd.queryIPV6Records()).subscribe(testSubscriber);

        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);
        PowerMockito.verifyStatic();
        DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), propertiesCaptor.capture());
        QueryListener queryListener = propertiesCaptor.getValue();

        queryListener.queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, 0, 0, new byte[0], 0);
        testSubscriber.assertError(UnknownHostException.class);
    }

    @Test
    public void test_query_records_exception_ipv4_first() throws DNSSDException, UnknownHostException {
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);
        byte[] ipv4 = new byte[]{0};
        byte[] ipv6 = new byte[]{1};
        PowerMockito.when(InetAddress.getByAddress(ipv4)).thenThrow(new UnknownHostException());
        PowerMockito.when(InetAddress.getByAddress(ipv6)).thenReturn(inet6Address);
        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(RxDnssd.queryRecords()).subscribe(testSubscriber);
        PowerMockito.verifyStatic();
        DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, 0, 0, ipv4, 0);
        PowerMockito.verifyStatic();
        DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, 0, 0, ipv6, 0);
        testSubscriber.assertError(UnknownHostException.class);
        testSubscriber.assertNoValues();
    }

    @Test
    public void test_query_records_exception_ipv4_second() throws DNSSDException, UnknownHostException {
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);
        byte[] ipv4 = new byte[]{0};
        byte[] ipv6 = new byte[]{1};
        PowerMockito.when(InetAddress.getByAddress(ipv4)).thenThrow(new UnknownHostException());
        PowerMockito.when(InetAddress.getByAddress(ipv6)).thenReturn(inet6Address);
        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(RxDnssd.queryRecords()).subscribe(testSubscriber);
        PowerMockito.verifyStatic();
        DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, 0, 0, ipv6, 0);
        PowerMockito.verifyStatic();
        DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, 0, 0, ipv4, 0);
        testSubscriber.assertValue(resolvedBonjourServiceWithIpv6);
        testSubscriber.assertError(UnknownHostException.class);
    }

    @Test
    public void test_query_records_exception_ipv6_first() throws DNSSDException, UnknownHostException {
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);
        byte[] ipv4 = new byte[]{0};
        byte[] ipv6 = new byte[]{1};
        PowerMockito.when(InetAddress.getByAddress(ipv4)).thenReturn(inet4Address);
        PowerMockito.when(InetAddress.getByAddress(ipv6)).thenThrow(new UnknownHostException());
        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(RxDnssd.queryRecords()).subscribe(testSubscriber);
        PowerMockito.verifyStatic();
        DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, 0, 0, ipv6, 0);
        PowerMockito.verifyStatic();
        DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, 0, 0, ipv4, 0);
        testSubscriber.assertError(UnknownHostException.class);
        testSubscriber.assertNoValues();
    }

    @Test
    public void test_query_records_exception_ipv6_second() throws DNSSDException, UnknownHostException {
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);
        byte[] ipv4 = new byte[]{0};
        byte[] ipv6 = new byte[]{1};
        PowerMockito.when(InetAddress.getByAddress(ipv4)).thenReturn(inet4Address);
        PowerMockito.when(InetAddress.getByAddress(ipv6)).thenThrow(new UnknownHostException());
        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(RxDnssd.queryRecords()).subscribe(testSubscriber);
        PowerMockito.verifyStatic();
        DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, 0, 0, ipv4, 0);
        PowerMockito.verifyStatic();
        DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, 0, 0, ipv6, 0);
        testSubscriber.assertValue(resolvedBonjourServiceWithIpv4);
        testSubscriber.assertError(UnknownHostException.class);
    }

    @Test
    public void test_query_ipv4_records_successfully_after_unsubscribe() throws DNSSDException, UnknownHostException {
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);
        PowerMockito.when(InetAddress.getByAddress(any(byte[].class))).thenReturn(inet4Address);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(RxDnssd.queryIPV4Records()).subscribe(testSubscriber).unsubscribe();

        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);
        PowerMockito.verifyStatic();
        DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), propertiesCaptor.capture());
        QueryListener queryListener = propertiesCaptor.getValue();

        queryListener.queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, 0, 0, new byte[0], 0);
        testSubscriber.assertNoValues();
        testSubscriber.assertNoTerminalEvent();
    }

    @Test
    public void test_query_ipv6_records_successfully_after_unsubscribe() throws DNSSDException, UnknownHostException {
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);
        PowerMockito.when(InetAddress.getByAddress(any(byte[].class))).thenReturn(inet6Address);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(RxDnssd.queryIPV6Records()).subscribe(testSubscriber).unsubscribe();

        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);
        PowerMockito.verifyStatic();
        DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), propertiesCaptor.capture());
        QueryListener queryListener = propertiesCaptor.getValue();

        queryListener.queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, 0, 0, new byte[0], 0);
        testSubscriber.assertNoValues();
        testSubscriber.assertNoTerminalEvent();
    }

    @Test
    public void test_query_records_successfully_after_unsubscribe() throws DNSSDException, UnknownHostException {
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);
        byte[] ipv4 = new byte[]{0};
        byte[] ipv6 = new byte[]{1};
        PowerMockito.when(InetAddress.getByAddress(ipv4)).thenReturn(inet4Address);
        PowerMockito.when(InetAddress.getByAddress(ipv6)).thenThrow(new UnknownHostException());
        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(RxDnssd.queryRecords()).subscribe(testSubscriber).unsubscribe();

        PowerMockito.verifyStatic();
        DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, 0, 0, ipv4, 0);

        PowerMockito.verifyStatic();
        DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, 0, 0, ipv6, 0);

        testSubscriber.assertNoValues();
        testSubscriber.assertNoTerminalEvent();
    }

    @Test
    public void test_query_ipv4_records_failure() throws DNSSDException {
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(RxDnssd.queryIPV4Records()).subscribe(testSubscriber);

        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);
        PowerMockito.verifyStatic();
        DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), propertiesCaptor.capture());
        QueryListener queryListener = propertiesCaptor.getValue();

        queryListener.operationFailed(mockService, 0);
        testSubscriber.assertError(RuntimeException.class);
    }

    @Test
    public void test_query_ipv6_records_failure() throws DNSSDException {
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(RxDnssd.queryIPV6Records()).subscribe(testSubscriber);

        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);
        PowerMockito.verifyStatic();
        DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), propertiesCaptor.capture());
        QueryListener queryListener = propertiesCaptor.getValue();

        queryListener.operationFailed(mockService, 0);
        testSubscriber.assertError(RuntimeException.class);
    }

    @Test
    public void test_query_records_failure() throws DNSSDException {
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);
        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(RxDnssd.queryRecords()).subscribe(testSubscriber);

        PowerMockito.verifyStatic();
        DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), propertiesCaptor.capture());
        propertiesCaptor.getValue().operationFailed(mockService, 0);

        PowerMockito.verifyStatic();
        DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), propertiesCaptor.capture());
        propertiesCaptor.getValue().operationFailed(mockService, 0);

        testSubscriber.assertError(RuntimeException.class);
    }

    @Test
    public void test_query_ipv4_records_failure_after_unsubscribe() throws DNSSDException {
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(RxDnssd.queryIPV4Records()).subscribe(testSubscriber).unsubscribe();

        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);
        PowerMockito.verifyStatic();
        DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), propertiesCaptor.capture());
        QueryListener queryListener = propertiesCaptor.getValue();

        queryListener.operationFailed(mockService, 0);
        testSubscriber.assertNoErrors();
    }

    @Test
    public void test_query_ipv6_records_failure_after_unsubscribe() throws DNSSDException {
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(RxDnssd.queryIPV6Records()).subscribe(testSubscriber).unsubscribe();

        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);
        PowerMockito.verifyStatic();
        DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), propertiesCaptor.capture());
        QueryListener queryListener = propertiesCaptor.getValue();

        queryListener.operationFailed(mockService, 0);
        testSubscriber.assertNoErrors();
    }

    @Test
    public void test_query_records_failure_after_unsubscribe() throws DNSSDException {
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);
        PowerMockito.when(DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);
        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(RxDnssd.queryRecords()).subscribe(testSubscriber).unsubscribe();

        PowerMockito.verifyStatic();
        DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), propertiesCaptor.capture());
        propertiesCaptor.getValue().operationFailed(mockService, 0);

        PowerMockito.verifyStatic();
        DNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), propertiesCaptor.capture());
        propertiesCaptor.getValue().operationFailed(mockService, 0);

        testSubscriber.assertNoErrors();
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.getInstance().reset();
    }

}
