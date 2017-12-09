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

import com.github.druk.dnssd.DNSSDRegistration;
import com.github.druk.dnssd.DNSSDService;
import com.github.druk.dnssd.RegisterListener;

import io.reactivex.FlowableEmitter;

class Rx2RegisterListener implements RegisterListener {
    private final FlowableEmitter<? super BonjourService> emitter;

    Rx2RegisterListener(FlowableEmitter<? super BonjourService> emitter) {
        this.emitter = emitter;
    }

    @Override
    public void serviceRegistered(DNSSDRegistration registration, int flags, String serviceName, String regType, String domain) {
        if (emitter.isCancelled()) {
            return;
        }
        BonjourService service = new BonjourService.Builder(flags, 0, serviceName, regType, domain).build();
        emitter.onNext(service);
    }

    @Override
    public void operationFailed(DNSSDService service, int errorCode) {
        if (emitter.isCancelled()) {
            return;
        }
        emitter.onError(new RuntimeException("DNSSD browse error: " + errorCode));
    }
}
