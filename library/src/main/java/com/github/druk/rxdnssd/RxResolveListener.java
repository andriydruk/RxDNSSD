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
import com.apple.dnssd.TXTRecord;

import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import rx.Subscriber;

class RxResolveListener implements com.apple.dnssd.ResolveListener {
    private final Subscriber<? super BonjourService> subscriber;
    private final BonjourService.Builder builder;

    RxResolveListener(Subscriber<? super BonjourService> subscriber, BonjourService service) {
        this.subscriber = subscriber;
        builder = new BonjourService.Builder(service);
    }

    @Override
    public void serviceResolved(DNSSDService resolver, int flags, int ifIndex, String fullName, String hostName, int port, TXTRecord txtRecord) {
        if (subscriber.isUnsubscribed()) {
            return;
        }
        BonjourService bonjourService = builder.port(port).hostname(hostName).dnsRecords(parseTXTRecords(txtRecord)).build();
        subscriber.onNext(bonjourService);
    }

    @Override
    public void operationFailed(DNSSDService service, int errorCode) {
        if (subscriber.isUnsubscribed()) {
            return;
        }
        subscriber.onError(new RuntimeException("DNSSD resolve error: " + errorCode));
    }

    private static Map<String, String> parseTXTRecords(TXTRecord record) {
        Map<String, String> result = new HashMap<>(record.size());
        for (int i = 0; i < record.size(); i++) {
            try {
                if (!TextUtils.isEmpty(record.getKey(i)) && !TextUtils.isEmpty(record.getValueAsString(i))) {
                    result.put(record.getKey(i), record.getValueAsString(i));
                }
            } catch (Exception e) {
                Log.w("RxResolveListener", "Parsing error of " + i + " TXT record", e);
            }
        }
        return result;
    }
}
