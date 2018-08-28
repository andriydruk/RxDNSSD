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

import com.github.druk.dnssd.BrowseListener;
import com.github.druk.dnssd.DNSSD;
import com.github.druk.dnssd.DNSSDException;
import com.github.druk.dnssd.DNSSDService;
import com.github.druk.dnssd.NSType;
import com.github.druk.dnssd.QueryListener;
import com.github.druk.dnssd.ResolveListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;

import rx.Observable;
import rx.Scheduler;
import rx.android.plugins.RxAndroidPlugins;
import rx.android.plugins.RxAndroidSchedulersHook;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import static junit.framework.Assert.fail;
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
@SuppressStaticInitializationFor("com.github.druk.dnssd.DNSSD")
public class RxDnssdTest {

    static final int FLAGS = 0;
    static final int IF_INDEX = 0;
    static final String SERVICE_NAME_STRING = "serviceName";
    static final String REG_TYPE_STRING = "regType";
    static final String DOMAIN_STRING = "domain";
    static final int PORT = 443;
    static final String HOSTNAME_STRING = "hostname";

    static final String SERVICE_NAME = SERVICE_NAME_STRING;
    static final String REG_TYPE = REG_TYPE_STRING;
    static final String DOMAIN = DOMAIN_STRING;
    static final String HOSTNAME = HOSTNAME_STRING;

    static BonjourService bonjourService = new BonjourService.Builder(FLAGS, IF_INDEX, SERVICE_NAME_STRING, REG_TYPE_STRING, DOMAIN_STRING).build();
    static BonjourService lostBonjourService = new BonjourService.Builder(BonjourService.LOST, IF_INDEX, SERVICE_NAME_STRING, REG_TYPE_STRING, DOMAIN_STRING).build();
    static BonjourService resolvedBonjourService = new BonjourService.Builder(bonjourService).port(PORT).hostname(HOSTNAME_STRING)
            .dnsRecords(new HashMap<>(0)).build();

    static Inet4Address inet4Address;
    static Inet6Address inet6Address;

    static {
        try {
            inet4Address = (Inet4Address) InetAddress.getByName("127.0.0.1");
            inet6Address = (Inet6Address) InetAddress.getByName("[::1]");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    static BonjourService resolvedBonjourServiceWithIpv4 = new BonjourService.Builder(resolvedBonjourService).inet4Address(inet4Address).build();
    static BonjourService resolvedBonjourServiceWithIpv6 = new BonjourService.Builder(resolvedBonjourService).inet6Address(inet6Address).build();
    static BonjourService resolvedBonjourServiceWithBothIp = new BonjourService.Builder(resolvedBonjourService).inet4Address(inet4Address)
            .inet6Address(inet6Address).build();

    DNSSDService mockService;
    RxDnssdCommon rxDnssd;
    DNSSD mockDNSSD;

    @Before
    public void setup() {
        RxAndroidPlugins.getInstance().registerSchedulersHook(new RxAndroidSchedulersHook() {
            @Override
            public Scheduler getMainThreadScheduler() {
                return Schedulers.immediate();
            }
        });
        mockService = mock(DNSSDService.class);
        mockDNSSD = mock(DNSSD.class);
        rxDnssd = new RxDnssdCommon(mockDNSSD) {};
    }

    @Test
    public void test_browse_unsubscribe() throws DNSSDException {
        when(mockDNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), any(BrowseListener.class))).thenReturn(mockService);
        rxDnssd.browse(REG_TYPE_STRING, DOMAIN_STRING).subscribe(new TestSubscriber<>()).unsubscribe();
        verify(mockService).stop();
    }

//    @Test
//    public void test_browse_start_daemon() throws DNSSDException {
//        when(mockDNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), any(BrowseListener.class))).thenReturn(mockService);
//        rxDnssd.browse(REG_TYPE_STRING, DOMAIN_STRING).subscribe(new TestSubscriber<>());
//        verify(mockDNSSD).onServiceStarting();
//    }

    @Test
    public void test_browse_found() throws Exception {
        when(mockDNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), any(BrowseListener.class))).thenReturn(mockService);
        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        rxDnssd.browse(REG_TYPE_STRING, DOMAIN_STRING).subscribe(testSubscriber);

        ArgumentCaptor<BrowseListener> propertiesCaptor = ArgumentCaptor.forClass(BrowseListener.class);
        verify(mockDNSSD).browse(anyInt(), anyInt(), anyString(), anyString(), propertiesCaptor.capture());
        propertiesCaptor.getValue().serviceFound(mockService, FLAGS, IF_INDEX, SERVICE_NAME, REG_TYPE, DOMAIN);
        assertServices(testSubscriber.getOnNextEvents(), bonjourService);
    }

    @Test
    public void test_browse_found_after_unsubscribe() throws Exception {
        when(mockDNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), any(BrowseListener.class))).thenReturn(mockService);
        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        rxDnssd.browse(REG_TYPE_STRING, DOMAIN_STRING).subscribe(testSubscriber).unsubscribe();

        ArgumentCaptor<BrowseListener> propertiesCaptor = ArgumentCaptor.forClass(BrowseListener.class);
        verify(mockDNSSD).browse(anyInt(), anyInt(), anyString(), anyString(), propertiesCaptor.capture());
        propertiesCaptor.getValue().serviceFound(mockService, FLAGS, IF_INDEX, SERVICE_NAME, REG_TYPE, DOMAIN);
        testSubscriber.assertNoValues();
    }

    @Test
    public void test_browse_lost() throws Exception {
        when(mockDNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), any(BrowseListener.class))).thenReturn(mockService);
        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        rxDnssd.browse(REG_TYPE_STRING, DOMAIN_STRING).subscribe(testSubscriber);

        ArgumentCaptor<BrowseListener> propertiesCaptor = ArgumentCaptor.forClass(BrowseListener.class);
        verify(mockDNSSD).browse(anyInt(), anyInt(), anyString(), anyString(), propertiesCaptor.capture());
        propertiesCaptor.getValue().serviceLost(mockService, FLAGS, IF_INDEX, SERVICE_NAME, REG_TYPE, DOMAIN);
        assertServices(testSubscriber.getOnNextEvents(), lostBonjourService);
    }

    @Test
    public void test_browse_lost_after_unsubscribe() throws Exception {
        when(mockDNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), any(BrowseListener.class))).thenReturn(mockService);
        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        rxDnssd.browse(REG_TYPE_STRING, DOMAIN_STRING).subscribe(testSubscriber).unsubscribe();

        ArgumentCaptor<BrowseListener> propertiesCaptor = ArgumentCaptor.forClass(BrowseListener.class);
        verify(mockDNSSD).browse(anyInt(), anyInt(), anyString(), anyString(), propertiesCaptor.capture());
        propertiesCaptor.getValue().serviceLost(mockService, FLAGS, IF_INDEX, SERVICE_NAME, REG_TYPE, DOMAIN);
        testSubscriber.assertNoValues();
    }

    @Test
    public void test_browse_operation_failed() throws Exception {
        when(mockDNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), any(BrowseListener.class))).thenReturn(mockService);
        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        rxDnssd.browse(REG_TYPE_STRING, DOMAIN_STRING).subscribe(testSubscriber);

        ArgumentCaptor<BrowseListener> propertiesCaptor = ArgumentCaptor.forClass(BrowseListener.class);
        verify(mockDNSSD).browse(anyInt(), anyInt(), anyString(), anyString(), propertiesCaptor.capture());
        propertiesCaptor.getValue().operationFailed(mockService, 0);
        testSubscriber.assertError(RuntimeException.class);
    }

    @Test
    public void test_browse_operation_failed_after_unsubscribe() throws Exception {
        DNSSDService mockService = mock(DNSSDService.class);
        mockStatic(DNSSD.class);
        when(mockDNSSD.browse(anyInt(), anyInt(), anyString(), anyString(), any(BrowseListener.class))).thenReturn(mockService);
        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        rxDnssd.browse(REG_TYPE_STRING, DOMAIN_STRING).subscribe(testSubscriber).unsubscribe();

        ArgumentCaptor<BrowseListener> propertiesCaptor = ArgumentCaptor.forClass(BrowseListener.class);
        verify(mockDNSSD).browse(anyInt(), anyInt(), anyString(), anyString(), propertiesCaptor.capture());
        propertiesCaptor.getValue().operationFailed(mockService, 0);
        testSubscriber.assertNoErrors();
    }

    @Test
    public void test_resolve_unsubscribe() throws DNSSDException {
        when(mockDNSSD.resolve(anyInt(), anyInt(), anyString(), anyString(), anyString(), any(ResolveListener.class))).thenReturn(mockService);
        Observable.just(bonjourService).compose(rxDnssd.resolve()).subscribe(new TestSubscriber<BonjourService>()).unsubscribe();
        verify(mockService).stop();
    }

//    @Test
//    public void test_resolve_start_daemon() throws DNSSDException {
//        when(mockDNSSD.resolve(anyInt(), anyInt(), anyString(), anyString(), anyString(), any(ResolveListener.class))).thenReturn(mockService);
//        Observable.just(bonjourService).compose(rxDnssd.resolve()).subscribe(new TestSubscriber<BonjourService>()).unsubscribe();
//        verify(mockDNSSD).onServiceStarting();
//    }

    @Test
    public void test_resolve_ignore_lost() throws DNSSDException {
        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(lostBonjourService).compose(rxDnssd.resolve()).subscribe(testSubscriber);
        assertServices(testSubscriber.getOnNextEvents(), lostBonjourService);
        testSubscriber.assertCompleted();
    }

    @Test
    public void test_resolve_successfully() throws DNSSDException {
        when(mockDNSSD.resolve(anyInt(), anyInt(), anyString(), anyString(), anyString(), any(ResolveListener.class))).thenReturn(mockService);
        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(bonjourService).compose(rxDnssd.resolve()).subscribe(testSubscriber);

        ArgumentCaptor<ResolveListener> propertiesCaptor = ArgumentCaptor.forClass(ResolveListener.class);
        verify(mockDNSSD).resolve(anyInt(), anyInt(), anyString(), anyString(), anyString(), propertiesCaptor.capture());
        propertiesCaptor.getValue().serviceResolved(mockService, FLAGS, IF_INDEX, SERVICE_NAME, HOSTNAME, PORT, new HashMap<String, String>());
        assertServices(testSubscriber.getOnNextEvents(), resolvedBonjourService);
    }

    @Test
    public void test_resolve_successfully_after_unsubscribe() throws DNSSDException {
        when(mockDNSSD.resolve(anyInt(), anyInt(), anyString(), anyString(), anyString(), any(ResolveListener.class))).thenReturn(mockService);
        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(bonjourService).compose(rxDnssd.resolve()).subscribe(testSubscriber).unsubscribe();

        ArgumentCaptor<ResolveListener> propertiesCaptor = ArgumentCaptor.forClass(ResolveListener.class);
        verify(mockDNSSD).resolve(anyInt(), anyInt(), anyString(), anyString(), anyString(), propertiesCaptor.capture());
        propertiesCaptor.getValue().serviceResolved(mockService, FLAGS, IF_INDEX, SERVICE_NAME, HOSTNAME, PORT, new HashMap<String, String>());
        testSubscriber.assertNoValues();
        testSubscriber.assertNoTerminalEvent();
    }

    @Test
    public void test_resolve_failure() throws DNSSDException {
        when(mockDNSSD.resolve(anyInt(), anyInt(), anyString(), anyString(), anyString(), any(ResolveListener.class))).thenReturn(mockService);
        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(bonjourService).compose(rxDnssd.resolve()).subscribe(testSubscriber);

        ArgumentCaptor<ResolveListener> propertiesCaptor = ArgumentCaptor.forClass(ResolveListener.class);
        verify(mockDNSSD).resolve(anyInt(), anyInt(), anyString(), anyString(), anyString(), propertiesCaptor.capture());
        propertiesCaptor.getValue().operationFailed(mockService, 0);
        testSubscriber.assertError(RuntimeException.class);
    }

    @Test
    public void test_resolve_failure_after_unsubscribe() throws DNSSDException {
        when(mockDNSSD.resolve(anyInt(), anyInt(), anyString(), anyString(), anyString(), any(ResolveListener.class))).thenReturn(mockService);
        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(bonjourService).compose(rxDnssd.resolve()).subscribe(testSubscriber).unsubscribe();

        ArgumentCaptor<ResolveListener> propertiesCaptor = ArgumentCaptor.forClass(ResolveListener.class);
        verify(mockDNSSD).resolve(anyInt(), anyInt(), anyString(), anyString(), anyString(), propertiesCaptor.capture());
        propertiesCaptor.getValue().operationFailed(mockService, 0);
        testSubscriber.assertNoErrors();
    }

    @Test
    public void test_query_ipv4_records_unsubscribe() throws DNSSDException {
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), eq(true), any(QueryListener.class))).thenReturn(mockService);
        Observable.just(resolvedBonjourService).compose(rxDnssd.queryIPV4Records()).subscribe(new TestSubscriber<>()).unsubscribe();
        verify(mockService).stop();
    }

    @Test
    public void test_query_ipv6_records_unsubscribe() throws DNSSDException {
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), eq(true), any(QueryListener.class))).thenReturn(mockService);
        Observable.just(resolvedBonjourService).compose(rxDnssd.queryIPV6Records()).subscribe(new TestSubscriber<>()).unsubscribe();
        verify(mockService).stop();
    }

    @Test
    public void test_query_records_unsubscribe() throws DNSSDException {
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), eq(true), any(QueryListener.class))).thenReturn(mockService);
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), eq(true), any(QueryListener.class))).thenReturn(mockService);
        Observable.just(resolvedBonjourService).compose(rxDnssd.queryRecords()).subscribe(new TestSubscriber<>()).unsubscribe();
        verify(mockService, times(2)).stop();
    }

//    @Test
//    public void test_query_ipv4_records_start_daemon() throws DNSSDException {
//        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);
//        Observable.just(resolvedBonjourService).compose(rxDnssd.queryIPV4Records()).subscribe(new TestSubscriber<BonjourService>()).unsubscribe();
//        verify(mockDNSSD).onServiceStarting();
//    }
//
//    @Test
//    public void test_query_ipv6_records_start_daemon() throws DNSSDException {
//        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);
//        Observable.just(resolvedBonjourService).compose(rxDnssd.queryIPV6Records()).subscribe(new TestSubscriber<BonjourService>()).unsubscribe();
//        verify(mockDNSSD).onServiceStarting();
//    }
//
//    @Test
//    public void test_query_records_start_daemon() throws DNSSDException {
//        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);
//        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);
//        Observable.just(resolvedBonjourService).compose(rxDnssd.queryRecords()).subscribe(new TestSubscriber<BonjourService>()).unsubscribe();
//        verify(mockDNSSD, times(2)).onServiceStarting();
//    }

    @Test
    public void test_query_ipv4_records_ignore_lost() throws DNSSDException {
        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(lostBonjourService).compose(rxDnssd.queryIPV4Records()).subscribe(testSubscriber);
        assertServices(testSubscriber.getOnNextEvents(), lostBonjourService);
        testSubscriber.assertCompleted();
    }

    @Test
    public void test_query_ipv6_records_ignore_lost() throws DNSSDException {
        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(lostBonjourService).compose(rxDnssd.queryIPV6Records()).subscribe(testSubscriber);
        assertServices(testSubscriber.getOnNextEvents(), lostBonjourService);
        testSubscriber.assertCompleted();
    }

    @Test
    public void test_query_records_ignore_lost() throws DNSSDException {
        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(lostBonjourService).compose(rxDnssd.queryRecords()).subscribe(testSubscriber);
        assertServices(testSubscriber.getOnNextEvents(), lostBonjourService);
        testSubscriber.assertCompleted();
    }

    @Test
    public void test_query_ipv4_records_successfully() throws DNSSDException, UnknownHostException {
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), eq(true), any(QueryListener.class))).thenReturn(mockService);
        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(rxDnssd.queryIPV4Records()).subscribe(testSubscriber);
        verify(mockDNSSD).queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), eq(true), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, NSType.A, 0, inet4Address.getAddress(), 0);
        assertServices(testSubscriber.getOnNextEvents(), resolvedBonjourServiceWithIpv4);
        testSubscriber.assertCompleted();
    }

    @Test
    public void test_query_ipv6_records_successfully() throws DNSSDException, UnknownHostException {
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);
        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(rxDnssd.queryIPV6Records()).subscribe(testSubscriber);
        verify(mockDNSSD).queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), eq(true), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, NSType.AAAA, 0, inet6Address.getAddress(), 0);
        assertServices(testSubscriber.getOnNextEvents(), resolvedBonjourServiceWithIpv6);
        testSubscriber.assertCompleted();
    }

    @Test
    public void test_query_records_successfully_ipv4_first() throws DNSSDException, UnknownHostException {
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);
        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(rxDnssd.queryRecords()).subscribe(testSubscriber);
        verify(mockDNSSD).queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), eq(true), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, NSType.A, 0, inet4Address.getAddress(), 0);
        verify(mockDNSSD).queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), eq(true), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, NSType.AAAA, 0, inet6Address.getAddress(), 0);
        assertServices(testSubscriber.getOnNextEvents(), resolvedBonjourServiceWithBothIp, resolvedBonjourServiceWithBothIp);
        testSubscriber.assertCompleted();
    }

    @Test
    public void test_query_records_successfully_ipv6_first() throws DNSSDException, UnknownHostException {
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);
        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(rxDnssd.queryRecords()).subscribe(testSubscriber);
        verify(mockDNSSD).queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), eq(true), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, NSType.A, 0, inet6Address.getAddress(), 0);
        verify(mockDNSSD).queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), eq(true), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, NSType.AAAA, 0, inet4Address.getAddress(), 0);
        assertServices(testSubscriber.getOnNextEvents(), resolvedBonjourServiceWithBothIp, resolvedBonjourServiceWithBothIp);
        testSubscriber.assertCompleted();
    }

    @Test
    public void test_query_ipv4_records_exception() throws DNSSDException, UnknownHostException {
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(rxDnssd.queryIPV4Records()).subscribe(testSubscriber);

        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);
        verify(mockDNSSD).queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), eq(true), propertiesCaptor.capture());
        QueryListener queryListener = propertiesCaptor.getValue();

        queryListener.operationFailed(mockService, -1);
        testSubscriber.assertError(RuntimeException.class);
    }

    @Test
    public void test_query_ipv6_records_exception() throws DNSSDException, UnknownHostException {
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(rxDnssd.queryIPV6Records()).subscribe(testSubscriber);

        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);
        verify(mockDNSSD).queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), eq(true), propertiesCaptor.capture());
        QueryListener queryListener = propertiesCaptor.getValue();

        queryListener.operationFailed(mockService, -1);
        testSubscriber.assertError(RuntimeException.class);
    }

    @Test
    public void test_query_records_exception_ipv4_first() throws DNSSDException, UnknownHostException {
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);
        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(rxDnssd.queryRecords()).subscribe(testSubscriber);
        verify(mockDNSSD).queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), eq(true), propertiesCaptor.capture());
        propertiesCaptor.getValue().operationFailed(mockService, -1);

        verify(mockDNSSD).queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), eq(true), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, 0, 0, inet6Address.getAddress(), 0);
        testSubscriber.assertError(RuntimeException.class);
        testSubscriber.assertNoValues();
    }

    @Test
    public void test_query_records_exception_ipv4_second() throws DNSSDException, UnknownHostException {
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);
        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(rxDnssd.queryRecords()).subscribe(testSubscriber);

        verify(mockDNSSD).queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), eq(true), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, NSType.AAAA, 0, inet6Address.getAddress(), 0);

        verify(mockDNSSD).queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), eq(true), propertiesCaptor.capture());
        propertiesCaptor.getValue().operationFailed(mockService, -1);
        assertServices(testSubscriber.getOnNextEvents(), resolvedBonjourServiceWithIpv6);
        testSubscriber.assertError(RuntimeException.class);
    }

    @Test
    public void test_query_records_exception_ipv6_first() throws DNSSDException, UnknownHostException {
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);
        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(rxDnssd.queryRecords()).subscribe(testSubscriber);

        verify(mockDNSSD).queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), eq(true), propertiesCaptor.capture());
        propertiesCaptor.getValue().operationFailed(mockService, -1);

        verify(mockDNSSD).queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), eq(true), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, 0, 0, inet4Address.getAddress(), 0);
        testSubscriber.assertError(RuntimeException.class);
        testSubscriber.assertNoValues();
    }

    @Test
    public void test_query_records_exception_ipv6_second() throws DNSSDException, UnknownHostException {
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);
        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(rxDnssd.queryRecords()).subscribe(testSubscriber);

        verify(mockDNSSD).queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), eq(true), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, NSType.A, 0, inet4Address.getAddress(), 0);

        verify(mockDNSSD).queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), eq(true), propertiesCaptor.capture());
        propertiesCaptor.getValue().operationFailed(mockService, -1);
        assertServices(testSubscriber.getOnNextEvents(), resolvedBonjourServiceWithIpv4);
        testSubscriber.assertError(RuntimeException.class);
    }

    @Test
    public void test_query_ipv4_records_successfully_after_unsubscribe() throws DNSSDException, UnknownHostException {
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(rxDnssd.queryIPV4Records()).subscribe(testSubscriber).unsubscribe();

        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);
        verify(mockDNSSD).queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), eq(true), propertiesCaptor.capture());
        QueryListener queryListener = propertiesCaptor.getValue();

        queryListener.queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, NSType.A, 0, inet4Address.getAddress(), 0);
        testSubscriber.assertNoValues();
        testSubscriber.assertNoTerminalEvent();
    }

    @Test
    public void test_query_ipv6_records_successfully_after_unsubscribe() throws DNSSDException, UnknownHostException {
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(rxDnssd.queryIPV6Records()).subscribe(testSubscriber).unsubscribe();

        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);
        verify(mockDNSSD).queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), eq(true), propertiesCaptor.capture());
        QueryListener queryListener = propertiesCaptor.getValue();

        queryListener.queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, NSType.AAAA, 0, inet6Address.getAddress(), 0);
        testSubscriber.assertNoValues();
        testSubscriber.assertNoTerminalEvent();
    }

    @Test
    public void test_query_records_successfully_after_unsubscribe() throws DNSSDException, UnknownHostException {
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);
        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(rxDnssd.queryRecords()).subscribe(testSubscriber).unsubscribe();

        verify(mockDNSSD).queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), eq(true), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, NSType.A, 0, inet4Address.getAddress(), 0);

        verify(mockDNSSD).queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), eq(true), propertiesCaptor.capture());
        propertiesCaptor.getValue().queryAnswered(mockService, FLAGS, IF_INDEX, HOSTNAME, NSType.AAAA, 0, inet6Address.getAddress(), 0);

        testSubscriber.assertNoValues();
        testSubscriber.assertNoTerminalEvent();
    }

    @Test
    public void test_query_ipv4_records_failure() throws DNSSDException {
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(rxDnssd.queryIPV4Records()).subscribe(testSubscriber);

        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);
        verify(mockDNSSD).queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), eq(true), propertiesCaptor.capture());
        QueryListener queryListener = propertiesCaptor.getValue();

        queryListener.operationFailed(mockService, 0);
        testSubscriber.assertError(RuntimeException.class);
    }

    @Test
    public void test_query_ipv6_records_failure() throws DNSSDException {
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(rxDnssd.queryIPV6Records()).subscribe(testSubscriber);

        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);
        verify(mockDNSSD).queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), eq(true), propertiesCaptor.capture());
        QueryListener queryListener = propertiesCaptor.getValue();

        queryListener.operationFailed(mockService, 0);
        testSubscriber.assertError(RuntimeException.class);
    }

    @Test
    public void test_query_records_failure() throws DNSSDException {
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);
        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(rxDnssd.queryRecords()).subscribe(testSubscriber);

        verify(mockDNSSD).queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), eq(true), propertiesCaptor.capture());
        propertiesCaptor.getValue().operationFailed(mockService, 0);

        verify(mockDNSSD).queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), eq(true), propertiesCaptor.capture());
        propertiesCaptor.getValue().operationFailed(mockService, 0);

        testSubscriber.assertError(RuntimeException.class);
    }

    @Test
    public void test_query_ipv4_records_failure_after_unsubscribe() throws DNSSDException {
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(rxDnssd.queryIPV4Records()).subscribe(testSubscriber).unsubscribe();

        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);
        verify(mockDNSSD).queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), eq(true), propertiesCaptor.capture());
        QueryListener queryListener = propertiesCaptor.getValue();

        queryListener.operationFailed(mockService, 0);
        testSubscriber.assertNoErrors();
    }

    @Test
    public void test_query_ipv6_records_failure_after_unsubscribe() throws DNSSDException {
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(rxDnssd.queryIPV6Records()).subscribe(testSubscriber).unsubscribe();

        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);
        verify(mockDNSSD).queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), eq(true), propertiesCaptor.capture());
        QueryListener queryListener = propertiesCaptor.getValue();

        queryListener.operationFailed(mockService, 0);
        testSubscriber.assertNoErrors();
    }

    @Test
    public void test_query_records_failure_after_unsubscribe() throws DNSSDException {
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), any(QueryListener.class))).thenReturn(mockService);
        when(mockDNSSD.queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), any(QueryListener.class))).thenReturn(mockService);
        ArgumentCaptor<QueryListener> propertiesCaptor = ArgumentCaptor.forClass(QueryListener.class);

        TestSubscriber<BonjourService> testSubscriber = new TestSubscriber<>();
        Observable.just(resolvedBonjourService).compose(rxDnssd.queryRecords()).subscribe(testSubscriber).unsubscribe();

        verify(mockDNSSD).queryRecord(anyInt(), anyInt(), anyString(), eq(1), eq(1), eq(true), propertiesCaptor.capture());
        propertiesCaptor.getValue().operationFailed(mockService, 0);

        verify(mockDNSSD).queryRecord(anyInt(), anyInt(), anyString(), eq(28), eq(1), eq(true), propertiesCaptor.capture());
        propertiesCaptor.getValue().operationFailed(mockService, 0);

        testSubscriber.assertNoErrors();
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.getInstance().reset();
    }

    public static void assertServices(List<BonjourService> serviceList, BonjourService... services){
        int serviceIndex = 0;
        for (BonjourService service : services) {
            BonjourService origin = serviceList.get(serviceIndex++);
            if (origin == service) continue;
            if (origin.getFlags() != service.getFlags()) fail();
            if (origin.getIfIndex() != service.getIfIndex()) fail();
            if (origin.getPort() != service.getPort()) fail();
            if (!origin.getServiceName().equals(service.getServiceName())) fail();
            if (!origin.getRegType().equals(service.getRegType())) fail();
            if (!origin.getDomain().equals(service.getDomain())) fail();
            if (origin.getInet4Address() != null ? !origin.getInet4Address().equals(service.getInet4Address())
                    : service.getInet4Address() != null) fail();
            if (origin.getInet6Address() != null ? !origin.getInet6Address().equals(service.getInet6Address())
                    : service.getInet6Address() != null) fail();
            if (!origin.getTxtRecords().equals(service.getTxtRecords())) fail();
            if (origin.getHostname() != null ? !origin.getHostname().equals(service.getHostname()) : service.getHostname() != null) fail();
        }
        if (serviceList.size() != serviceIndex) fail();
    }

}
