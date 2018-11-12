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

import androidx.annotation.NonNull;

import com.github.druk.dnssd.DNSSD;

import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;

/**
 * RxDnssd is reactive wrapper for {@link DNSSD}
 */
public interface Rx2Dnssd {

    /**
     * Browse for instances of a service.<P>
     *
     * @param regType The registration type being browsed for followed by the protocol, separated by a
     *                dot (e.g. "_ftp._tcp"). The transport protocol must be "_tcp" or "_udp".
     *                <p>
     * @param domain  If non-null, specifies the domain on which to browse for services.
     *                Most applications will not specify a domain, instead browsing on the
     *                default domain(s).
     *                <p>
     * @return A {@link Flowable} that represents the active browse operation.
     */
    @NonNull
    Flowable<BonjourService> browse(@NonNull final String regType, @NonNull final String domain);

    /**
     * Resolve a {@link Flowable} to a target host name, port number, and txt record.<P>
     * <p>
     * Note: Applications should NOT use resolve() solely for txt record monitoring - use
     * queryRecord() instead, as it is more efficient for this task.<P>
     * <p>
     * Note: resolve() behaves correctly for typical services that have a single SRV record and
     * a single TXT record (the TXT record may be empty.)  To resolve non-standard services with
     * multiple SRV or TXT records, use queryRecord().<P>
     *
     * @return A {@link FlowableTransformer} that transform not resolved object to resolved.
     */
    @NonNull
    FlowableTransformer<BonjourService, BonjourService> resolve();

    /**
     * Query ipv4 and ipv6 addresses
     *
     * @return A {@link FlowableTransformer} that transform object without addresses to object with addresses.
     */
    @NonNull
    @Deprecated //renamed: queryIPRecords
    FlowableTransformer<BonjourService, BonjourService> queryRecords();

    /**
     * Query ipv4 and ipv6 addresses
     *
     * @return A {@link FlowableTransformer} that transform object without addresses to object with addresses.
     */
    @NonNull
    FlowableTransformer<BonjourService, BonjourService> queryIPRecords();

    /**
     * Query ipv4 address
     *
     * @return A {@link FlowableTransformer} that transform object without address to object with address.
     */
    @NonNull
    FlowableTransformer<BonjourService, BonjourService> queryIPV4Records();

    /**
     * Query ipv6 address
     *
     * @return A {@link FlowableTransformer} that transform object without address to object with address.
     */
    @NonNull
    FlowableTransformer<BonjourService, BonjourService> queryIPV6Records();

    /**
     * Query ipv4 and ipv6 addresses
     *
     * @return A {@link Flowable} with ip addresses
     */
    @NonNull
    Flowable<BonjourService> queryIPRecords(BonjourService bs);

    /**
     * Query ipv4 address
     *
     * @return A {@link Flowable}
     */
    @NonNull
    Flowable<BonjourService> queryIPV4Records(BonjourService bs);

    /**
     * Query ipv6 address
     *
     * @return A {@link Flowable}
     */
    @NonNull
    Flowable<BonjourService> queryIPV6Records(BonjourService bs);

    /**
     * Query ipv6 address
     *
     * @return A {@link Flowable}
     */
    @NonNull
    Flowable<BonjourService> queryTXTRecords(BonjourService bs);

    @NonNull
    Flowable<BonjourService> register(@NonNull final BonjourService bs);
}
