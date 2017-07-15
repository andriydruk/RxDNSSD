package com.github.druk.rxdnssd;

import com.github.druk.dnssd.DNSSD;
import com.github.druk.dnssd.DNSSDException;
import com.github.druk.dnssd.DNSSDService;
import com.github.druk.dnssd.TXTRecord;

import android.support.annotation.NonNull;

import java.util.Map;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

abstract class RxDnssdCommon implements RxDnssd {

    final private DNSSD mDNSSD;

    RxDnssdCommon(DNSSD dnssd) {
        mDNSSD = dnssd;
    }

    public DNSSD getDNSSD() {
        return mDNSSD;
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
     * @return A {@link Observable} that represents the active browse operation.
     */
    @NonNull
    @Override
    //TODO: Finbugs: new DNSSDServiceCreator<BonjourService> should be a static class (Performance issue ???)
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "SIC_INNER_SHOULD_BE_STATIC_ANON", justification = "I know what I'm doing")
    public Observable<BonjourService> browse(@NonNull final String regType, @NonNull final String domain) {
        return createObservable(new DNSSDServiceCreator<BonjourService>() {
            @Override
            public DNSSDService getService(Subscriber<? super BonjourService> subscriber) throws DNSSDException {
                return mDNSSD.browse(0, DNSSD.ALL_INTERFACES, regType, domain, new RxBrowseListener(subscriber));
            }
        });
    }

    /**
     * Resolve a {@link Observable} to a target host name, port number, and txt record.<P>
     *
     * Note: Applications should NOT use resolve() solely for txt record monitoring - use
     * queryRecord() instead, as it is more efficient for this task.<P>
     *
     * Note: resolve() behaves correctly for typical services that have a single SRV record and
     * a single TXT record (the TXT record may be empty.)  To resolve non-standard services with
     * multiple SRV or TXT records, use queryRecord().<P>
     *
     * @return A {@link Observable.Transformer} that transform not resolved object to resolved.
     */
    @NonNull
    @Override
    public Observable.Transformer<BonjourService, BonjourService> resolve() {
        return new Observable.Transformer<BonjourService, BonjourService>() {
            @Override
            public Observable<BonjourService> call(Observable<BonjourService> observable) {
                return observable.flatMap(new Func1<BonjourService, Observable<? extends BonjourService>>() {
                    @Override
                    public Observable<? extends BonjourService> call(final BonjourService bs) {
                        if ((bs.getFlags() & BonjourService.LOST) == BonjourService.LOST) {
                            return Observable.just(bs);
                        }
                        return createObservable(new DNSSDServiceCreator<BonjourService>() {
                            @Override
                            public DNSSDService getService(Subscriber<? super BonjourService> subscriber) throws DNSSDException {
                                return mDNSSD.resolve(bs.getFlags(), bs.getIfIndex(), bs.getServiceName(), bs.getRegType(), bs.getDomain(),
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
     * @return A {@link Observable.Transformer} that transform object without addresses to object with addresses.
     */
    @NonNull
    @Override
    public Observable.Transformer<BonjourService, BonjourService> queryRecords() {
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
                        return createObservable(new DNSSDServiceCreator<BonjourService>() {
                            @Override
                            public DNSSDService getService(Subscriber<? super BonjourService> subscriber) throws DNSSDException {
                                return mDNSSD.queryRecord(0, bs.getIfIndex(), bs.getHostname(), 1 /* ns_t_a */, 1 /* ns_c_in */,
                                        new RxQueryListener(subscriber, builder));
                            }
                        }).mergeWith(createObservable(new DNSSDServiceCreator<BonjourService>() {
                            @Override
                            public DNSSDService getService(Subscriber<? super BonjourService> subscriber) throws DNSSDException {
                                return mDNSSD.queryRecord(0, bs.getIfIndex(), bs.getHostname(), 28 /* ns_t_aaaa */, 1 /* ns_c_in */,
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
     * @return A {@link Observable.Transformer} that transform object without address to object with address.
     */
    @NonNull
    @Override
    public Observable.Transformer<BonjourService, BonjourService> queryIPV4Records() {
        return new Observable.Transformer<BonjourService, BonjourService>() {
            @Override
            public Observable<BonjourService> call(Observable<BonjourService> observable) {
                return observable.flatMap(new Func1<BonjourService, Observable<? extends BonjourService>>() {
                    @Override
                    public Observable<? extends BonjourService> call(final BonjourService bs) {
                        if ((bs.getFlags() & BonjourService.LOST) == BonjourService.LOST) {
                            return Observable.just(bs);
                        }
                        return createObservable(new DNSSDServiceCreator<BonjourService>() {
                            @Override
                            public DNSSDService getService(Subscriber<? super BonjourService> subscriber) throws DNSSDException {
                                return mDNSSD.queryRecord(0, bs.getIfIndex(), bs.getHostname(), 1 /* ns_t_a */, 1 /* ns_c_in */,
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
     * @return A {@link Observable.Transformer} that transform object without address to object with address.
     */
    @NonNull
    @Override
    public Observable.Transformer<BonjourService, BonjourService> queryIPV6Records() {
        return new Observable.Transformer<BonjourService, BonjourService>() {
            @Override
            public Observable<BonjourService> call(Observable<BonjourService> observable) {
                return observable.flatMap(new Func1<BonjourService, Observable<? extends BonjourService>>() {
                    @Override
                    public Observable<? extends BonjourService> call(final BonjourService bs) {
                        if ((bs.getFlags() & BonjourService.LOST) == BonjourService.LOST) {
                            return Observable.just(bs);
                        }
                        return createObservable(new DNSSDServiceCreator<BonjourService>() {
                            @Override
                            public DNSSDService getService(Subscriber<? super BonjourService> subscriber) throws DNSSDException {
                                return mDNSSD.queryRecord(0, bs.getIfIndex(), bs.getHostname(), 28 /* ns_t_aaaa */, 1 /* ns_c_in */,
                                        new RxQueryListener(subscriber, new BonjourService.Builder(bs)));
                            }
                        });
                    }
                });
            }
        };
    }

    @NonNull
    @Override
    public Observable<BonjourService> register(@NonNull final BonjourService bs) {
        return createObservable(new DNSSDServiceCreator<BonjourService>() {
            @Override
            public DNSSDService getService(Subscriber<? super BonjourService> subscriber) throws DNSSDException {
                return mDNSSD.register(bs.getFlags(), bs.getIfIndex(), bs.getServiceName(), bs.getRegType(), bs.getDomain(), null, bs.getPort(),
                        createTxtRecord(bs.getTxtRecords()), new RxRegisterListener(subscriber));
            }
        });
    }

    private static class DNSSDServiceAction<T> implements Observable.OnSubscribe<T>, Action0 {

        private final DNSSDServiceCreator<T> creator;
        private DNSSDService service;

        DNSSDServiceAction(DNSSDServiceCreator<T> creator) {
            this.creator = creator;
        }

        @Override
        public void call(Subscriber<? super T> subscriber) {
            if (!subscriber.isUnsubscribed() && creator != null) {
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

    private interface DNSSDServiceCreator<T> {
        DNSSDService getService(Subscriber<? super T> subscriber) throws DNSSDException;
    }

    private <T> Observable<T> createObservable(DNSSDServiceCreator<T> creator) {
        DNSSDServiceAction<T> action = new DNSSDServiceAction<>(creator);
        return Observable.create(action).doOnUnsubscribe(action);
    }

    private static TXTRecord createTxtRecord(Map<String, String> records) {
        TXTRecord txtRecord = new TXTRecord();
        for (Map.Entry<String, String> entry : records.entrySet()) {
            txtRecord.set(entry.getKey(), entry.getValue());
        }
        return txtRecord;
    }
}
