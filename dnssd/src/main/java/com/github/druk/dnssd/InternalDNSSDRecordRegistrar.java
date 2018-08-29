package com.github.druk.dnssd;


class InternalDNSSDRecordRegistrar implements DNSSDRecordRegistrar {

    final private InternalDNSSDService.DnssdServiceListener listener;
    final private DNSSDRecordRegistrar originalService;

    InternalDNSSDRecordRegistrar(InternalDNSSDService.DnssdServiceListener listener, DNSSDRecordRegistrar registration) {
        this.listener = listener;
        this.originalService = registration;
    }

    @Override
    public DNSRecord registerRecord(int flags, int ifIndex, String fullname, int rrtype, int rrclass, byte[] rData, int ttl) throws DNSSDException {
        return originalService.registerRecord(flags, ifIndex, fullname, rrtype, rrclass, rData, ttl);
    }

    @Override
    public void stop() {
        originalService.stop();
        listener.onServiceStopped();
    }
}
