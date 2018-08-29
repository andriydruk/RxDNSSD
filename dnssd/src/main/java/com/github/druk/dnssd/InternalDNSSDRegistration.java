package com.github.druk.dnssd;

class InternalDNSSDRegistration implements DNSSDRegistration {

    final private InternalDNSSDService.DnssdServiceListener listener;
    final private DNSSDRegistration originalDNSSDService;

    InternalDNSSDRegistration(InternalDNSSDService.DnssdServiceListener listener, DNSSDRegistration registration) {
        this.listener = listener;
        this.originalDNSSDService = registration;
    }

    @Override
    public DNSRecord getTXTRecord() throws DNSSDException {
        return originalDNSSDService.getTXTRecord();
    }

    @Override
    public DNSRecord addRecord(int flags, int rrType, byte[] rData, int ttl) throws DNSSDException {
        return originalDNSSDService.addRecord(flags, rrType, rData, ttl);
    }

    @Override
    public void stop() {
        originalDNSSDService.stop();
        listener.onServiceStopped();
    }
}
