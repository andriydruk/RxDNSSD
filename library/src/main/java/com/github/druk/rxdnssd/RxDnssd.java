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

import android.content.Context;
import android.support.annotation.NonNull;

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

    private RxDnssd() {
    }

    /**
     * Initialize wrapper with context
     *
     * @param ctx Context of application or any other android component
     */
    public static void init(Context ctx) {
        INSTANCE.context = ctx.getApplicationContext();
    }

    /**
     * Browse for instances of a service.<P>
     *
     * @param regType The registration type being browsed for followed by the protocol, separated by a
     *                dot (e.g. "_ftp._tcp"). The transport protocol must be "_tcp" or "_udp".
     *                <P>
     * @param domain  If non-null, specifies the domain on which to browse for services.
     *                Most applications will not specify a domain, instead browsing on the
     *                default domain(s).
     *                <P>
     * @return A {@link Observable<BonjourService>} that represents the active browse operation.
     */
    public static Observable<BonjourService> browse(@NonNull final String regType, @NonNull final String domain) {
        return INSTANCE.createObservable(new DNSSDServiceCreator<BonjourService>() {
            @Override
            public DNSSDService getService(Subscriber<? super BonjourService> subscriber) throws DNSSDException {
                return DNSSD.browse(0, DNSSD.ALL_INTERFACES, regType, domain, new RxBrowseListener(subscriber));
            }
        });
    }

    /**
     * Resolve a {@link Observable<BonjourService>} to a target host name, port number, and txt record.<P>
     *
     * Note: Applications should NOT use resolve() solely for txt record monitoring - use
     * queryRecord() instead, as it is more efficient for this task.<P>
     *
     * Note: resolve() behaves correctly for typical services that have a single SRV record and
     * a single TXT record (the TXT record may be empty.)  To resolve non-standard services with
     * multiple SRV or TXT records, use queryRecord().<P>
     *
     * @return A {@link Observable.Transformer<BonjourService, BonjourService>} that transform not resolved object to resolved.
     */
    @NonNull
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
                                return DNSSD.resolve(bs.getFlags(), bs.getIfIndex(), bs.getServiceName(), bs.getRegType(), bs.getDomain(),
                                        new RxResolveListener(subscriber, bs));
                            }
                        });
                    }
                });
            }
        };
    }

    /**
     * Query ipv4 and ipv6 addresses
     *
     * @return A {@link Observable.Transformer<BonjourService, BonjourService>} that transform object without addresses to object with addresses.
     */
    @NonNull
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
                                return DNSSD.queryRecord(0, bs.getIfIndex(), bs.getHostname(), 1 /* ns_t_a */, 1 /* ns_c_in */,
                                        new RxQueryListener(subscriber, builder));
                            }
                        }).mergeWith(INSTANCE.createObservable(new DNSSDServiceCreator<BonjourService>() {
                            @Override
                            public DNSSDService getService(Subscriber<? super BonjourService> subscriber) throws DNSSDException {
                                return DNSSD.queryRecord(0, bs.getIfIndex(), bs.getHostname(), 28 /* ns_t_aaaa */, 1 /* ns_c_in */,
                                        new RxQueryListener(subscriber, builder));
                            }
                        }));
                    }
                });
            }
        };
    }

    /**
     * Query ipv4 address
     *
     * @return A {@link Observable.Transformer<BonjourService, BonjourService>} that transform object without address to object with address.
     */
    @NonNull
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
                                return DNSSD.queryRecord(0, bs.getIfIndex(), bs.getHostname(), 1 /* ns_t_a */, 1 /* ns_c_in */,
                                        new RxQueryListener(subscriber, new BonjourService.Builder(bs)));
                            }
                        });
                    }
                });
            }
        };
    }

    /**
     * Query ipv6 address
     *
     * @return A {@link Observable.Transformer<BonjourService, BonjourService>} that transform object without address to object with address.
     */
    @NonNull
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
                                return DNSSD.queryRecord(0, bs.getIfIndex(), bs.getHostname(), 28 /* ns_t_aaaa */, 1 /* ns_c_in */,
                                        new RxQueryListener(subscriber, new BonjourService.Builder(bs)));
                            }
                        });
                    }
                });
            }
        };
    }

    private <T> Observable<T> createObservable(DNSSDServiceCreator<T> creator) {
        DNSSDServiceAction<T> action = new DNSSDServiceAction<>(creator);
        return Observable.create(action).doOnUnsubscribe(action);
    }

    private interface DNSSDServiceCreator<T> {
        DNSSDService getService(Subscriber<? super T> subscriber) throws DNSSDException;
    }

    private class DNSSDServiceAction<T> implements OnSubscribe<T>, Action0 {

        private final DNSSDServiceCreator<T> creator;
        private DNSSDService service;

        DNSSDServiceAction(DNSSDServiceCreator<T> creator) {
            this.creator = creator;
        }

        @Override
        public void call(Subscriber<? super T> subscriber) {
            if (!subscriber.isUnsubscribed() && creator != null) {
                context.getSystemService(Context.NSD_SERVICE);
                try {
                    service = creator.getService(subscriber);
                } catch (DNSSDException e) {
                    subscriber.onError(e);
                }
            }
        }

        @Override
        public void call() {
            if (service != null) {
                Observable.just(service)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<DNSSDService>() {
                            @Override
                            public void call(DNSSDService service) {
                                service.stop();
                            }
                        });
                service = null;
            }
        }
    }
}
