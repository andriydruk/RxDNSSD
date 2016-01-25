/*
 * Copyright (C) 2015 Andriy Druk
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
package com.github.druk;

import com.apple.dnssd.DNSSD;
import com.apple.dnssd.DNSSDException;
import com.apple.dnssd.DNSSDService;
import com.apple.dnssd.TXTRecord;

import android.content.Context;
import android.text.TextUtils;

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

public class RxDnssd {

    private final static RxDnssd INSTANCE = new RxDnssd();
    private Context context;

    private RxDnssd(){}

    public static void init(Context ctx) {
        INSTANCE.context = ctx.getApplicationContext();
    }

    public static Observable<BonjourService> browse(final String regType, final String domain) {
        return INSTANCE.createObservable(new DNSSDServiceCreator<BonjourService>() {
            @Override
            public DNSSDService getService(Subscriber<? super BonjourService> subscriber) throws DNSSDException {
                return DNSSD.browse(0, DNSSD.ALL_INTERFACES, regType, domain, new BrowseListener(subscriber));
            }
        });
    }

    public static Observable.Transformer<BonjourService, BonjourService> resolve() {
        return new Observable.Transformer<BonjourService, BonjourService>() {
            @Override
            public Observable<BonjourService> call(Observable<BonjourService> observable) {
                return observable.flatMap(new Func1<BonjourService, Observable<? extends BonjourService>>() {
                    @Override
                    public Observable<? extends BonjourService> call(final BonjourService bs) {
                        if ((bs.getFlags() & BonjourService.DELETED) == BonjourService.DELETED) {
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

    public static Observable.Transformer<BonjourService, BonjourService> queryRecords() {
        return new Observable.Transformer<BonjourService, BonjourService>() {
            @Override
            public Observable<BonjourService> call(Observable<BonjourService> observable) {
                return observable.flatMap(new Func1<BonjourService, Observable<? extends BonjourService>>() {
                    @Override
                    public Observable<? extends BonjourService> call(final BonjourService bs) {
                        if ((bs.getFlags() & BonjourService.DELETED) == BonjourService.DELETED) {
                            return Observable.just(bs);
                        }
                        return INSTANCE.createObservable(new DNSSDServiceCreator<BonjourService>() {
                            @Override
                            public DNSSDService getService(Subscriber<? super BonjourService> subscriber) throws DNSSDException {
                                return DNSSD.queryRecord(0, bs.getIfIndex(), bs.getHostname(), 1 /* ns_t_a */, 1 /* ns_c_in */, new QueryListener(subscriber, bs));
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
            BonjourService service = new BonjourService.Builder(flags | BonjourService.DELETED, ifIndex, serviceName, regType, domain).build();
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

        private QueryListener(Subscriber<? super BonjourService> subscriber, BonjourService bonjourService){
            this.subscriber = subscriber;
            builder = new BonjourService.Builder(bonjourService);
        }

        @Override
        public void queryAnswered(DNSSDService query, int flags, int ifIndex, String fullName, int rrtype, int rrclass, byte[] rdata, int ttl) {
            if (subscriber.isUnsubscribed()){
                return;
            }
            try {
                InetAddress address = InetAddress.getByAddress(rdata);
                builder.addresses(address);
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
