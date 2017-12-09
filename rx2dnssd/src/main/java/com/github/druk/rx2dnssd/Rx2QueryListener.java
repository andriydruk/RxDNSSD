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
package com.github.druk.rx2dnssd;

import com.github.druk.dnssd.DNSSDService;
import com.github.druk.dnssd.QueryListener;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

import io.reactivex.FlowableEmitter;

class Rx2QueryListener implements QueryListener {

    private final FlowableEmitter<? super BonjourService> emitter;
    private final BonjourService.Builder builder;

    Rx2QueryListener(FlowableEmitter<? super BonjourService> emitter, BonjourService.Builder builder) {
        this.emitter = emitter;
        this.builder = builder;
    }

    @Override
    public void queryAnswered(DNSSDService query, int flags, int ifIndex, String fullName, int rrtype, int rrclass, InetAddress address, int ttl) {
        if (emitter.isCancelled()) {
            return;
        }
        if (address instanceof Inet4Address) {
            builder.inet4Address((Inet4Address) address);
        } else if (address instanceof Inet6Address) {
            builder.inet6Address((Inet6Address) address);
        }
        emitter.onNext(builder.build());
        emitter.onComplete();
    }

    @Override
    public void operationFailed(DNSSDService service, int errorCode) {
        if (emitter.isCancelled()) {
            return;
        }
        emitter.onError(new RuntimeException("DNSSD queryRecord error: " + errorCode));
    }
}
