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

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;

import com.github.druk.dnssd.DNSSD;
import com.github.druk.dnssd.DNSSDBindable;

/**
 * RxDnssdBindable is implementation of RxDnssd with system's daemon {@link DNSSD}
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class Rx2DnssdBindable extends Rx2DnssdCommon {

    public Rx2DnssdBindable(Context context) {
        super(new DNSSDBindable(context));
    }

}
