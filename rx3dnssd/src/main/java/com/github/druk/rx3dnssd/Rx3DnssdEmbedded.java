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
package com.github.druk.rx3dnssd;

import android.content.Context;

import com.github.druk.dnssd.DNSSDEmbedded;

/**
 * RxDnssdEmbedded is implementation of RxDnssd with embedded DNS-SD  {@link Rx3Dnssd}
 */
public class Rx3DnssdEmbedded extends Rx3DnssdCommon {

    public Rx3DnssdEmbedded(Context context) {
        super(new DNSSDEmbedded(context));
    }

}
