package com.github.druk.dnssd;


class InternalDNSSDService implements DNSSDService {

    final private DnssdServiceListener listener;
    final private DNSSDService originalDNSSDService;
    private boolean isStopped = false;

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
        synchronized (this) {
            if (!isStopped) {
                listener.onServiceStopped();
                isStopped = true;
            }
        }
    }
}
