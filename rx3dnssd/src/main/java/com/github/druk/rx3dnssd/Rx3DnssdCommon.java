package com.github.druk.rx3dnssd;

import androidx.annotation.NonNull;

import com.github.druk.dnssd.DNSSD;
import com.github.druk.dnssd.DNSSDException;
import com.github.druk.dnssd.DNSSDService;
import com.github.druk.dnssd.NSClass;
import com.github.druk.dnssd.NSType;
import com.github.druk.dnssd.TXTRecord;

import java.util.Map;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableEmitter;
import io.reactivex.rxjava3.core.FlowableOnSubscribe;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.functions.Action;

abstract class Rx3DnssdCommon implements Rx3Dnssd {

    final private DNSSD mDNSSD;

    Rx3DnssdCommon(DNSSD dnssd) {
        mDNSSD = dnssd;
    }

    private static TXTRecord createTxtRecord(Map<String, String> records) {
        TXTRecord txtRecord = new TXTRecord();
        for (Map.Entry<String, String> entry : records.entrySet()) {
            txtRecord.set(entry.getKey(), entry.getValue());
        }
        return txtRecord;
    }

    public DNSSD getDNSSD() {
        return mDNSSD;
    }

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
    @Override
    //TODO: Finbugs: new DNSSDServiceCreator<BonjourService> should be a static class (Performance issue ???)
    public Flowable<BonjourService> browse(@NonNull final String regType, @NonNull final String domain) {
        return createFlowable(emitter ->
                mDNSSD.browse(0, DNSSD.ALL_INTERFACES, regType, domain,
                        new Rx3BrowseListener(emitter)));
    }

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
    @Override
    public FlowableTransformer<BonjourService, BonjourService> resolve() {
        return flowable -> flowable.flatMap(bs -> {
            if ((bs.getFlags() & BonjourService.LOST) == BonjourService.LOST) {
                return Flowable.just(bs);
            }
            return createFlowable(emitter ->
                    mDNSSD.resolve(bs.getFlags(), bs.getIfIndex(), bs.getServiceName(), bs.getRegType(), bs.getDomain(),
                            new Rx3ResolveListener(emitter, bs)));
        });
    }

    /**
     * Query ipv4 and ipv6 addresses
     *
     * @return A {@link FlowableTransformer} that transform object without addresses to object with addresses.
     */
    @NonNull
    @Override
    @Deprecated
    public FlowableTransformer<BonjourService, BonjourService> queryRecords() {
        return flowable -> flowable.flatMap(bs -> {
            if ((bs.getFlags() & BonjourService.LOST) == BonjourService.LOST) {
                return Flowable.just(bs);
            }
            BonjourService.Builder builder = new BonjourService.Builder(bs);

            Flowable<BonjourService> flowableV4 = createFlowable(emitter ->
                    mDNSSD.queryRecord(0, bs.getIfIndex(), bs.getHostname(), NSType.A, NSClass.IN, true,
                            new Rx3QueryListener(emitter, builder, true)));
            Flowable<BonjourService> flowableV6 = createFlowable(emitter ->
                    mDNSSD.queryRecord(0, bs.getIfIndex(), bs.getHostname(), NSType.AAAA, NSClass.IN, true,
                            new Rx3QueryListener(emitter, builder, true)));

            return flowableV4.mergeWith(flowableV6);
        });
    }

    /**
     * Query ipv4 and ipv6 addresses with auto-stop (first response or timeout)
     *
     * @return A {@link FlowableTransformer} that transform object without addresses to object with addresses.
     */
    @NonNull
    @Override
    public FlowableTransformer<BonjourService, BonjourService> queryIPRecords() {
        return flowable -> flowable.flatMap(bs -> {
            if ((bs.getFlags() & BonjourService.LOST) == BonjourService.LOST) {
                return Flowable.just(bs);
            }
            BonjourService.Builder builder = new BonjourService.Builder(bs);

            Flowable<BonjourService> flowableV4 = createFlowable(emitter ->
                    mDNSSD.queryRecord(0, bs.getIfIndex(), bs.getHostname(), NSType.A, NSClass.IN, true,
                            new Rx3QueryListener(emitter, builder, true)));
            Flowable<BonjourService> flowableV6 = createFlowable(emitter ->
                    mDNSSD.queryRecord(0, bs.getIfIndex(), bs.getHostname(), NSType.AAAA, NSClass.IN, true,
                            new Rx3QueryListener(emitter, builder, true)));

            return flowableV4.mergeWith(flowableV6);
        });
    }

    /**
     * Query ipv4 address with auto-stop (first response or timeout)
     *
     * @return A {@link FlowableTransformer} that transform object without address to object with address.
     */
    @NonNull
    @Override
    public FlowableTransformer<BonjourService, BonjourService> queryIPV4Records() {
        return flowable -> flowable.flatMap(bs -> {
            if ((bs.getFlags() & BonjourService.LOST) == BonjourService.LOST) {
                return Flowable.just(bs);
            }
            return createFlowable(emitter ->
                    mDNSSD.queryRecord(0, bs.getIfIndex(), bs.getHostname(), NSType.A, NSClass.IN, true,
                            new Rx3QueryListener(emitter, new BonjourService.Builder(bs), true)));
        });
    }

    /**
     * Query ipv6 address with auto-stop (first response or timeout)
     *
     * @return A {@link FlowableTransformer} that transform object without address to object with address.
     */
    @NonNull
    @Override
    public FlowableTransformer<BonjourService, BonjourService> queryIPV6Records() {
        return flowable -> flowable.flatMap(bs -> {
            if ((bs.getFlags() & BonjourService.LOST) == BonjourService.LOST) {
                return Flowable.just(bs);
            }
            return createFlowable(emitter ->
                    mDNSSD.queryRecord(0, bs.getIfIndex(), bs.getHostname(), NSType.AAAA, NSClass.IN, true,
                            new Rx3QueryListener(emitter, new BonjourService.Builder(bs), true)));
        });
    }

    /**
     * Query ipv4 and ipv6 addresses
     *
     * @return A {@link Flowable} with ip addresses
     */
    @NonNull
    @Override
    public Flowable<BonjourService> queryIPRecords(BonjourService bs) {
        if ((bs.getFlags() & BonjourService.LOST) == BonjourService.LOST) {
            return Flowable.just(bs);
        }
        final BonjourService.Builder builder = new BonjourService.Builder(bs);
        return createFlowable((DNSSDServiceCreator<BonjourService>) subscriber ->
                mDNSSD.queryRecord(0, bs.getIfIndex(), bs.getHostname(), NSType.A, NSClass.IN, false, new Rx3QueryListener(subscriber, builder, false)))
                .mergeWith(createFlowable(subscriber ->
                        mDNSSD.queryRecord(0, bs.getIfIndex(), bs.getHostname(), NSType.AAAA, NSClass.IN, false, new Rx3QueryListener(subscriber, builder, false))));
    }

    /**
     * Query ipv4 address
     *
     * @return A {@link Flowable}
     */
    @NonNull
    @Override
    public Flowable<BonjourService> queryIPV4Records(BonjourService bs) {
        return queryRecords(bs, NSType.A);
    }

    /**
     * Query ipv6 address
     *
     * @return A {@link Flowable}
     */
    @NonNull
    @Override
    public Flowable<BonjourService> queryIPV6Records(BonjourService bs) {
        return queryRecords(bs, NSType.AAAA);
    }

    /**
     * Query TXT records
     *
     * @return A {@link Flowable}
     */
    @NonNull
    @Override
    public Flowable<BonjourService> queryTXTRecords(BonjourService bs) {
        return queryRecords(bs, NSType.TXT);
    }

    private Flowable<BonjourService> queryRecords(BonjourService bs, int type) {
        if ((bs.getFlags() & BonjourService.LOST) == BonjourService.LOST) {
            return Flowable.just(bs);
        }
        return createFlowable(subscriber -> mDNSSD.queryRecord(0, bs.getIfIndex(), bs.getHostname(), type, NSClass.IN, false,
                new Rx3QueryListener(subscriber, new BonjourService.Builder(bs), false)));
    }

    @NonNull
    @Override
    public Flowable<BonjourService> register(@NonNull final BonjourService bs) {
        return createFlowable(emitter ->
                mDNSSD.register(bs.getFlags(), bs.getIfIndex(), bs.getServiceName(), bs.getRegType(), bs.getDomain(), null, bs.getPort(),
                        createTxtRecord(bs.getTxtRecords()), new Rx3RegisterListener(emitter)));
    }

    private <T> Flowable<T> createFlowable(DNSSDServiceCreator<T> creator) {
        DNSSDServiceAction<T> action = new DNSSDServiceAction<>(creator);
        return Flowable.create(action, BackpressureStrategy.BUFFER)
                .doFinally(action);
    }

    private interface DNSSDServiceCreator<T> {
        DNSSDService getService(FlowableEmitter<? super T> emitter) throws DNSSDException;
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private static class DNSSDServiceAction<T> implements FlowableOnSubscribe<T>, Action {

        private final DNSSDServiceCreator<T> creator;
        private DNSSDService service;

        DNSSDServiceAction(DNSSDServiceCreator<T> creator) {
            this.creator = creator;
        }

        @Override
        public void subscribe(FlowableEmitter<T> emitter) {
            if (!emitter.isCancelled() && creator != null) {
                try {
                    service = creator.getService(emitter);
                } catch (DNSSDException e) {
                    emitter.onError(e);
                }
            }
        }

        @Override
        public void run() {
            if (service != null) {
                service.stop();
                service = null;
            }
        }
    }
}
