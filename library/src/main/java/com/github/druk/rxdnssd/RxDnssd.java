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

import android.support.annotation.NonNull;

import rx.Observable;

/**
 * RxDnssd is reactive wrapper for {@link com.apple.dnssd.DNSSD}
 */
public interface RxDnssd {

    /**
     * Browse for instances of a service.<P>
     *
     * @param regType The registration type being browsed for followed by the protocol, separated by a
     *                dot (e.g. "_ftp._tcp"). The transport protocol must be "_tcp" or "_udp".
     *                <P>
     * @param domain  If non-null, specifies the domain on which to browse for services.
     *                Most applications will not specify a domain, instead browsing on the
     *                default domain(s).
     *                <P>
     * @return A {@link Observable} that represents the active browse operation.
     */
    @NonNull
    Observable<BonjourService> browse(@NonNull final String regType, @NonNull final String domain);

    /**
     * Resolve a {@link Observable} to a target host name, port number, and txt record.<P>
     *
     * Note: Applications should NOT use resolve() solely for txt record monitoring - use
     * queryRecord() instead, as it is more efficient for this task.<P>
     *
     * Note: resolve() behaves correctly for typical services that have a single SRV record and
     * a single TXT record (the TXT record may be empty.)  To resolve non-standard services with
     * multiple SRV or TXT records, use queryRecord().<P>
     *
     * @return A {@link Observable.Transformer} that transform not resolved object to resolved.
     */
    @NonNull
    Observable.Transformer<BonjourService, BonjourService> resolve();

    /**
     * Query ipv4 and ipv6 addresses
     *
     * @return A {@link Observable.Transformer} that transform object without addresses to object with addresses.
     */
    @NonNull
    Observable.Transformer<BonjourService, BonjourService> queryRecords();

    /**
     * Query ipv4 address
     *
     * @return A {@link Observable.Transformer} that transform object without address to object with address.
     */
    @NonNull
    Observable.Transformer<BonjourService, BonjourService> queryIPV4Records();

    /**
     * Query ipv6 address
     *
     * @return A {@link Observable.Transformer} that transform object without address to object with address.
     */
    @NonNull
    Observable.Transformer<BonjourService, BonjourService> queryIPV6Records();

    @NonNull
    Observable<BonjourService> register(@NonNull final BonjourService bs);
}
