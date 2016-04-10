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

import com.apple.dnssd.DNSSDEmbedded;
import com.apple.dnssd.DNSSDException;
import com.apple.dnssd.DNSSDService;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;

/**
 * RxDnssd is reactive wrapper for DNSSD
 *
 * {@see com.apple.dnssd.DNSSD}
 */
public class RxDnssdEmbedded extends RxDnssdCommon{

    private static final String TAG = "RxDnssdEmbedded";

    private int serviceCount = 0;
    private DNSSDEmbedded mDNSSDEmbedded = new DNSSDEmbedded();

    public RxDnssdEmbedded() {
        super( "jdns_sd_embedded");
    }

    @Override
    protected <T> Observable<T> createObservable(DNSSDServiceCreator<T> creator) {
        DNSSDServiceAction<T> action = new DNSSDServiceAction<>(creator);
        return Observable.create(action).doOnUnsubscribe(action);
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
                mDNSSDEmbedded.init();
                try {
                    service = creator.getService(subscriber);
                    serviceCount++;
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
                                serviceCount--;
                                if (serviceCount == 0) {
                                    mDNSSDEmbedded.exit();
                                }
                            }
                        });
                service = null;
            }
        }
    }
}
