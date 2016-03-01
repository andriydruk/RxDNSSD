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

import com.apple.dnssd.DNSSDService;

import rx.Subscriber;

class RxBrowseListener implements com.apple.dnssd.BrowseListener {
    private final Subscriber<? super BonjourService> subscriber;

    RxBrowseListener(Subscriber<? super BonjourService> subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void serviceFound(DNSSDService browser, int flags, int ifIndex, String serviceName, String regType, String domain) {
        if (subscriber.isUnsubscribed()) {
            return;
        }
        BonjourService service = new BonjourService.Builder(flags, ifIndex, serviceName, regType, domain).build();
        subscriber.onNext(service);
    }

    @Override
    public void serviceLost(DNSSDService browser, int flags, int ifIndex, String serviceName, String regType, String domain) {
        if (subscriber.isUnsubscribed()) {
            return;
        }
        BonjourService service = new BonjourService.Builder(flags | BonjourService.LOST, ifIndex, serviceName, regType, domain).build();
        subscriber.onNext(service);
    }

    @Override
    public void operationFailed(DNSSDService service, int errorCode) {
        if (subscriber.isUnsubscribed()) {
            return;
        }
        subscriber.onError(new RuntimeException("DNSSD browse error: " + errorCode));
    }
}
