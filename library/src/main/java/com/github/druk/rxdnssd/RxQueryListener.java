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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

import rx.Subscriber;

class RxQueryListener implements com.apple.dnssd.QueryListener {

    private final Subscriber<? super BonjourService> subscriber;
    private final BonjourService.Builder builder;

    RxQueryListener(Subscriber<? super BonjourService> subscriber, BonjourService.Builder builder) {
        this.subscriber = subscriber;
        this.builder = builder;
    }

    @Override
    public void queryAnswered(DNSSDService query, int flags, int ifIndex, String fullName, int rrtype, int rrclass, byte[] rdata, int ttl) {
        if (subscriber.isUnsubscribed()) {
            return;
        }
        try {
            InetAddress address = InetAddress.getByAddress(rdata);
            if (address instanceof Inet4Address) {
                builder.inet4Address((Inet4Address) address);
            } else if (address instanceof Inet6Address) {
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
        if (subscriber.isUnsubscribed()) {
            return;
        }
        subscriber.onError(new RuntimeException("DNSSD queryRecord error: " + errorCode));
    }
}
