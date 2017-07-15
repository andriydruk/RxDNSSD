package com.github.druk.dnssd;


class InternalDNSSDService implements DNSSDService {

    final private DnssdServiceListener listener;
    final private DNSSDService originalDNSSDService;

    interface DnssdServiceListener {
        void onServiceStarting();
        void onServiceStopped();
    }

    InternalDNSSDService(DnssdServiceListener listener, DNSSDService DNSSDService) {
        this.listener = listener;
        this.originalDNSSDService = DNSSDService;
    }

    public void stop() {
        originalDNSSDService.stop();
        listener.onServiceStopped();
    }
}
