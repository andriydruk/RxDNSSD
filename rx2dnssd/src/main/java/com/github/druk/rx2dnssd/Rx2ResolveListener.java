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
import com.github.druk.dnssd.ResolveListener;

import java.util.Map;

import io.reactivex.FlowableEmitter;

class Rx2ResolveListener implements ResolveListener {
    private final FlowableEmitter<? super BonjourService> emitter;
    private final BonjourService.Builder builder;

    Rx2ResolveListener(FlowableEmitter<? super BonjourService> emitter, BonjourService service) {
        this.emitter = emitter;
        this.builder = new BonjourService.Builder(service);
    }

    @Override
    public void serviceResolved(DNSSDService resolver, int flags, int ifIndex, String fullName, String hostName, int port, Map<String, String> txtRecord) {
        if (emitter.isCancelled()) {
            return;
        }
        BonjourService bonjourService = builder.port(port).hostname(hostName).dnsRecords(txtRecord).build();
        emitter.onNext(bonjourService);
        emitter.onComplete();
    }

    @Override
    public void operationFailed(DNSSDService service, int errorCode) {
        if (emitter.isCancelled()) {
            return;
        }
        emitter.onError(new RuntimeException("DNSSD resolve error: " + errorCode));
    }
}
