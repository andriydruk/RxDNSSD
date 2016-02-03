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

import com.apple.dnssd.DNSSD;
import com.apple.dnssd.DNSSDException;
import com.apple.dnssd.DNSSDService;
import com.apple.dnssd.TXTRecord;

import android.content.Context;
import android.text.TextUtils;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * RxDnssd is reactive wrapper for DNSSD
 *
 * {@see com.apple.dnssd.DNSSD}
 */
public class RxDnssd {

    private final static RxDnssd INSTANCE = new RxDnssd();
    private Context context;

    private RxDnssd(){}

    /**
     * Initialize wrapper with context
     *
     * @param ctx Context of application or any other android component
     */
    public static void init(Context ctx) {
        INSTANCE.context = ctx.getApplicationContext();
    }

    /** Browse for instances of a service.<P>

     @param	regType
     The registration type being browsed for followed by the protocol, separated by a
     dot (e.g. "_ftp._tcp"). The transport protocol must be "_tcp" or "_udp".
     <P>
     @param	domain
     If non-null, specifies the domain on which to browse for services.
     Most applications will not specify a domain, instead browsing on the
     default domain(s).
     <P>
     @return A {@link Observable<BonjourService>} that represents the active browse operation.
     */
    public static Observable<BonjourService> browse(final String regType, final String domain) {
        return INSTANCE.createObservable(new DNSSDServiceCreator<BonjourService>() {
            @Override
            public DNSSDService getService(Subscriber<? super BonjourService> subscriber) throws DNSSDException {
                return DNSSD.browse(0, DNSSD.ALL_INTERFACES, regType, domain, new BrowseListener(subscriber));
            }
        });
    }

    /** Resolve a {@link Observable<BonjourService>} to a target host name, port number, and txt record.<P>

     Note: Applications should NOT use resolve() solely for txt record monitoring - use
     queryRecord() instead, as it is more efficient for this task.<P>

     Note: resolve() behaves correctly for typical services that have a single SRV record and
     a single TXT record (the TXT record may be empty.)  To resolve non-standard services with
     multiple SRV or TXT records, use queryRecord().<P>

     @return A {@link Observable.Transformer<BonjourService, BonjourService>} that transform not resolved object to resolved.

     */
    public static Observable.Transformer<BonjourService, BonjourService> resolve() {
        return new Observable.Transformer<BonjourService, BonjourService>() {
            @Override
            public Observable<BonjourService> call(Observable<BonjourService> observable) {
                return observable.flatMap(new Func1<BonjourService, Observable<? extends BonjourService>>() {
                    @Override
                    public Observable<? extends BonjourService> call(final BonjourService bs) {
                        if ((bs.getFlags() & BonjourService.LOST) == BonjourService.LOST) {
                            return Observable.just(bs);
                        }
                        return INSTANCE.createObservable(new DNSSDServiceCreator<BonjourService>() {
                            @Override
                            public DNSSDService getService(Subscriber<? super BonjourService> subscriber) throws DNSSDException {
                                return DNSSD.resolve(bs.getFlags(), bs.getIfIndex(), bs.getServiceName(), bs.getRegType(), bs.getDomain(), new ResolveListener(subscriber, bs));
                            }
                        });
                    }
                });
            }
        };
    }

    /** Query ipv4 and ipv6 addresses

     @return A {@link Observable.Transformer<BonjourService, BonjourService>} that transform object without addresses to object with addresses.
     */
    public static Observable.Transformer<BonjourService, BonjourService> queryRecords() {
        return new Observable.Transformer<BonjourService, BonjourService>() {
            @Override
            public Observable<BonjourService> call(Observable<BonjourService> observable) {
                return observable.flatMap(new Func1<BonjourService, Observable<? extends BonjourService>>() {
                    @Override
                    public Observable<? extends BonjourService> call(final BonjourService bs) {
                        if ((bs.getFlags() & BonjourService.LOST) == BonjourService.LOST) {
                            return Observable.just(bs);
                        }
                        final BonjourService.Builder builder = new BonjourService.Builder(bs);
                        return INSTANCE.createObservable(new DNSSDServiceCreator<BonjourService>() {
                            @Override
                            public DNSSDService getService(Subscriber<? super BonjourService> subscriber) throws DNSSDException {
                                return DNSSD.queryRecord(0, bs.getIfIndex(), bs.getHostname(), 1 /* ns_t_a */, 1 /* ns_c_in */, new QueryListener(subscriber, builder));
                            }
                        }).mergeWith(INSTANCE.createObservable(new DNSSDServiceCreator<BonjourService>() {
                            @Override
                            public DNSSDService getService(Subscriber<? super BonjourService> subscriber) throws DNSSDException {
                                return DNSSD.queryRecord(0, bs.getIfIndex(), bs.getHostname(), 28 /* ns_t_aaaa */, 1 /* ns_c_in */, new QueryListener(subscriber, builder));
                            }
                        }));
                    }
                });
            }
        };
    }

    /** Query ipv4 address

     @return A {@link Observable.Transformer<BonjourService, BonjourService>} that transform object without address to object with address.
     */
    public static Observable.Transformer<BonjourService, BonjourService> queryIPV4Records() {
        return new Observable.Transformer<BonjourService, BonjourService>() {
            @Override
            public Observable<BonjourService> call(Observable<BonjourService> observable) {
                return observable.flatMap(new Func1<BonjourService, Observable<? extends BonjourService>>() {
                    @Override
                    public Observable<? extends BonjourService> call(final BonjourService bs) {
                        if ((bs.getFlags() & BonjourService.LOST) == BonjourService.LOST) {
                            return Observable.just(bs);
                        }
                        return INSTANCE.createObservable(new DNSSDServiceCreator<BonjourService>() {
                            @Override
                            public DNSSDService getService(Subscriber<? super BonjourService> subscriber) throws DNSSDException {
                                return DNSSD.queryRecord(0, bs.getIfIndex(), bs.getHostname(), 1 /* ns_t_a */, 1 /* ns_c_in */, new QueryListener(subscriber, new BonjourService.Builder(bs)));
                            }
                        });
                    }
                });
            }
        };
    }

    /** Query ipv6 address

     @return A {@link Observable.Transformer<BonjourService, BonjourService>} that transform object without address to object with address.
     */
    public static Observable.Transformer<BonjourService, BonjourService> queryIPV6Records() {
        return new Observable.Transformer<BonjourService, BonjourService>() {
            @Override
            public Observable<BonjourService> call(Observable<BonjourService> observable) {
                return observable.flatMap(new Func1<BonjourService, Observable<? extends BonjourService>>() {
                    @Override
                    public Observable<? extends BonjourService> call(final BonjourService bs) {
                        if ((bs.getFlags() & BonjourService.LOST) == BonjourService.LOST) {
                            return Observable.just(bs);
                        }
                        return INSTANCE.createObservable(new DNSSDServiceCreator<BonjourService>() {
                            @Override
                            public DNSSDService getService(Subscriber<? super BonjourService> subscriber) throws DNSSDException {
                                return DNSSD.queryRecord(0, bs.getIfIndex(), bs.getHostname(), 28 /* ns_t_aaaa */, 1 /* ns_c_in */, new QueryListener(subscriber, new BonjourService.Builder(bs)));
                            }
                        });
                    }
                });
            }
        };
    }

    private interface DNSSDServiceCreator<T>{
        DNSSDService getService(Subscriber<? super T> subscriber) throws DNSSDException;
    }

    private <T> Observable<T> createObservable(final DNSSDServiceCreator<T> creator){
        final DNSSDService[] service = new DNSSDService[1];
        return Observable.create(new OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                if (!subscriber.isUnsubscribed()) {
                    context.getSystemService(Context.NSD_SERVICE);
                    try {
                        service[0] = creator.getService(subscriber);
                    } catch (DNSSDException e) {
                        e.printStackTrace();
                        subscriber.onError(e);
                    }
                }
            }
        }).doOnUnsubscribe(new Action0() {
            @Override
            public void call() {
                if (service[0] != null) {
                    Observable.just(service[0])
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Action1<DNSSDService>() {
                                @Override
                                public void call(DNSSDService dnssdService) {
                                    dnssdService.stop();
                                }
                            });
                    service[0] = null;
                }
            }
        });
    }

    private static class BrowseListener implements com.apple.dnssd.BrowseListener{
        private Subscriber<? super BonjourService> subscriber;

        private BrowseListener(Subscriber<? super BonjourService> subscriber){
            this.subscriber = subscriber;
        }

        @Override
        public void serviceFound(DNSSDService browser, int flags, int ifIndex, String serviceName, String regType, String domain) {
            if (subscriber.isUnsubscribed()){
                return;
            }
            BonjourService service = new BonjourService.Builder(flags, ifIndex, serviceName, regType, domain).build();
            subscriber.onNext(service);
        }

        @Override
        public void serviceLost(DNSSDService browser, int flags, int ifIndex, String serviceName, String regType, String domain) {
            if (subscriber.isUnsubscribed()){
                return;
            }
            BonjourService service = new BonjourService.Builder(flags | BonjourService.LOST, ifIndex, serviceName, regType, domain).build();
            subscriber.onNext(service);
        }

        @Override
        public void operationFailed(DNSSDService service, int errorCode) {
            if (subscriber.isUnsubscribed()){
                return;
            }
            subscriber.onError(new RuntimeException("DNSSD browse error: " + errorCode));
        }
    }

    private static class ResolveListener implements com.apple.dnssd.ResolveListener{
        private Subscriber<? super BonjourService> subscriber;
        private BonjourService.Builder builder;

        private ResolveListener(Subscriber<? super BonjourService> subscriber, BonjourService service){
            this.subscriber = subscriber;
            builder = new BonjourService.Builder(service);
        }

        @Override
        public void serviceResolved(DNSSDService resolver, int flags, int ifIndex, String fullName, String hostName, int port, TXTRecord txtRecord) {
            if (subscriber.isUnsubscribed()){
                return;
            }
            BonjourService bonjourService = builder.port(port).hostname(hostName).dnsRecords(parseTXTRecords(txtRecord)).build();
            subscriber.onNext(bonjourService);
            subscriber.onCompleted();
        }

        @Override
        public void operationFailed(DNSSDService service, int errorCode) {
            if (subscriber.isUnsubscribed()){
                return;
            }
            subscriber.onError(new RuntimeException("DNSSD resolve error: " + errorCode));
        }
    }

    private static class QueryListener implements com.apple.dnssd.QueryListener{

        private final Subscriber<? super BonjourService> subscriber;
        private final BonjourService.Builder builder;

        private QueryListener(Subscriber<? super BonjourService> subscriber, BonjourService.Builder builder){
            this.subscriber = subscriber;
            this.builder = builder;
        }

        @Override
        public void queryAnswered(DNSSDService query, int flags, int ifIndex, String fullName, int rrtype, int rrclass, byte[] rdata, int ttl) {
            if (subscriber.isUnsubscribed()){
                return;
            }
            try {
                InetAddress address = InetAddress.getByAddress(rdata);
                if (address instanceof Inet4Address) {
                    builder.inet4Address((Inet4Address) address);
                }
                else if (address instanceof Inet6Address){
                    builder.inet6Address((Inet6Address) address);
                }
                subscriber.onNext(builder.build());
                subscriber.onCompleted();
            } catch (Exception e) {
                subscriber.onError(e);
            }
        }

        @Override
        public void operationFailed(DNSSDService service, int errorCode) {
            if (subscriber.isUnsubscribed()){
                return;
            }
            subscriber.onError(new RuntimeException("DNSSD queryRecord error: " + errorCode));
        }
    }

    private static Map<String, String> parseTXTRecords(TXTRecord record) {
        Map<String, String> result = new HashMap<>(record.size());
        for (int i = 0; i < record.size(); i++) {
            if (!TextUtils.isEmpty(record.getKey(i)) && !TextUtils.isEmpty(record.getValueAsString(i)))
                result.put(record.getKey(i), record.getValueAsString(i));
        }
        return result;
    }
}
