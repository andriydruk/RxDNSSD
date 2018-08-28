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

import com.github.druk.dnssd.DNSSD;
import com.github.druk.dnssd.DNSSDService;
import com.github.druk.dnssd.NSType;
import com.github.druk.dnssd.QueryListener;

import java.net.InetAddress;
import java.net.UnknownHostException;

import rx.Subscriber;

class RxQueryListener implements QueryListener {

    private final Subscriber<? super BonjourService> subscriber;
    private final BonjourService.Builder builder;
    private final boolean completable;

    RxQueryListener(Subscriber<? super BonjourService> subscriber, BonjourService.Builder builder, boolean completable) {
        this.subscriber = subscriber;
        this.builder = builder;
        this.completable = completable;
    }

    @Override
    public void queryAnswered(DNSSDService query, int flags, int ifIndex, String fullName, int rrtype, int rrclass, byte[] rdata, int ttl) {
        if (subscriber.isUnsubscribed()) {
            return;
        }
        if (rrtype == NSType.A || rrtype == NSType.AAAA) {
            try {
                InetAddress inetAddress = InetAddress.getByAddress(rdata);
                builder.inetAddress(inetAddress);
            } catch (UnknownHostException e) {
                subscriber.onError(e);
            }
        } else if (rrtype == NSType.TXT) {
            builder.dnsRecords(DNSSD.parseTXTRecords(rdata));
        } else {
            subscriber.onError(new Exception("Unsupported type of record: " + rrtype));
        }
        subscriber.onNext(builder.build());
        if (completable) {
            subscriber.onCompleted();
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
