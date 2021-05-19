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

package com.github.druk.dnssd;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public abstract class DNSSD implements InternalDNSSDService.DnssdServiceListener {

    public static final int DNSSD_DEFAULT_TIMEOUT = 60 * 1000; // 60 sec

    /**	Flag indicates to a {@link BrowseListener} that another result is
     queued. Applications should not update their UI to display browse
     results if the MORE_COMING flag is set; they will be called at least once
     more with the flag clear.
     */
    public static final int		MORE_COMING = 1 << 0;

    /** If flag is set in a {@link DomainListener} callback, indicates that the result is the default domain. */
    public static final int		DEFAULT = 1 << 2;

    /**	If flag is set, a name conflict will trigger an exception when registering non-shared records.<P>
     A name must be explicitly specified when registering a service if this bit is set
     (i.e. the default name may not not be used).
     */
    public static final int		NO_AUTO_RENAME = 1 << 3;

    /**	If flag is set, allow multiple records with this name on the network (e.g. PTR records)
     when registering individual records on a {@link DNSSDRegistration}.
     */
    public static final int		SHARED = 1 << 4;

    /**	If flag is set, records with this name must be unique on the network (e.g. SRV records). */
    public static final int		UNIQUE = 1 << 5;

    /** Set flag when calling enumerateDomains() to restrict results to domains recommended for browsing. */
    public static final int		BROWSE_DOMAINS = 1 << 6;
    /** Set flag when calling enumerateDomains() to restrict results to domains recommended for registration. */
    public static final int		REGISTRATION_DOMAINS = 1 << 7;

    /** Maximum length, in bytes, of a domain name represented as an escaped C-String. */
    public static final int     MAX_DOMAIN_NAME = 1009;

    /** Pass for ifIndex to specify all available interfaces. */
    public static final int     ALL_INTERFACES = 0;

    /** Pass for ifIndex to specify the localhost interface. */
    public static final int     LOCALHOST_ONLY = -1;

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final String MULTICAST_LOCK_NAME = "com.github.druk.dnssd.DNSSD";

    private final Handler handler;
    private final Context context;

    // Lock for multicast packages
    private volatile WifiManager.MulticastLock multicastLock = null;

    /** Timeout for resolve and query records operations. Default value: {@value #DNSSD_DEFAULT_TIMEOUT} */
    private final int serviceTimeout;

    DNSSD(Context context, String lib) {
        this(context, lib, Looper.getMainLooper());
    }

    DNSSD(Context context, String lib, Looper looper) {
        this.context = context.getApplicationContext();
        InternalDNSSD.init(lib);
        this.handler = new Handler(looper);
        this.serviceTimeout = DNSSD_DEFAULT_TIMEOUT;
    }

    DNSSD(Context context, String lib, Handler handler) {
        this.context = context.getApplicationContext();
        InternalDNSSD.init(lib);
        this.handler = handler;
        this.serviceTimeout = DNSSD_DEFAULT_TIMEOUT;
    }

    DNSSD(Context context, String lib, Handler handler, int serviceTimeout) {
        this.context = context.getApplicationContext();
        InternalDNSSD.init(lib);
        this.handler = handler;
        this.serviceTimeout = serviceTimeout;
    }

    /** Browse for instances of a service.<P>

     Note: browsing consumes network bandwidth. Call {@link InternalDNSSDService#stop} when you have finished browsing.<P>

     @param	flags
     Currently ignored, reserved for future use.
     <P>
     @param	ifIndex
     If non-zero, specifies the interface on which to browse for services
     (the index for a given interface is determined via the if_nametoindex()
     family of calls.)  Most applications will pass 0 to browse on all available
     interfaces.  Pass -1 to only browse for services provided on the local host.
     <P>
     @param	regType
     The registration type being browsed for followed by the protocol, separated by a
     dot (e.g. "_ftp._tcp"). The transport protocol must be "_tcp" or "_udp".
     <P>
     @param	domain
     If non-null, specifies the domain on which to browse for services.
     Most applications will not specify a domain, instead browsing on the
     default domain(s).
     <P>
     @param	listener
     This object will get called when instances of the service are discovered (or disappear).
     <P>
     @return		A {@link InternalDNSSDService} that represents the active browse operation.

     @throws SecurityException If a security manager is present and denies <tt>RuntimePermission("getDNSSDInstance")</tt>.
     @see    RuntimePermission
     */
    public DNSSDService browse(int flags, int ifIndex, String regType, String domain, final BrowseListener listener) throws DNSSDException {
        onServiceStarting();
        final InternalDNSSDService[] services = new InternalDNSSDService[1];
        services[0] = new InternalDNSSDService(this, InternalDNSSD.browse(flags, ifIndex, regType, domain, new InternalBrowseListener() {
            @Override
            public void serviceFound(final DNSSDService browser, final int flags, final int ifIndex, final byte[] serviceName, final byte[] regType, final byte[] domain) {
                final String serviceNameStr = new String(serviceName, UTF_8);
                final String regTypeStr = new String(regType, UTF_8);
                final String domainStr = new String(domain, UTF_8);
                handler.post(() -> listener.serviceFound(services[0], flags, ifIndex, serviceNameStr, regTypeStr, domainStr));
            }

            @Override
            public void serviceLost(DNSSDService browser, final int flags, final int ifIndex, byte[] serviceName, byte[] regType, byte[] domain) {
                final String serviceNameStr = new String(serviceName, UTF_8);
                final String regTypeStr = new String(regType, UTF_8);
                final String domainStr = new String(domain, UTF_8);
                handler.post(() -> listener.serviceLost(services[0], flags, ifIndex, serviceNameStr, regTypeStr, domainStr));
            }

            @Override
            public void operationFailed(final DNSSDService service, final int errorCode) {
                handler.post(() -> listener.operationFailed(services[0], errorCode));
            }
        }));
        return services[0];

    }

    /** Browse for instances of a service. Use default flags, ifIndex and domain.<P>

     @param	regType
     The registration type being browsed for followed by the protocol, separated by a
     dot (e.g. "_ftp._tcp"). The transport protocol must be "_tcp" or "_udp".
     <P>
     @param	listener
     This object will get called when instances of the service are discovered (or disappear).
     <P>
     @return		A {@link DNSSDService} that represents the active browse operation.

     @throws SecurityException If a security manager is present and denies <tt>RuntimePermission("getDNSSDInstance")</tt>.
     @see    RuntimePermission
     */
    public DNSSDService browse(String regType, BrowseListener listener) throws DNSSDException {
        return browse(0, 0, regType, "", listener);
    }

    /** Resolve a service name discovered via browse() to a target host name, port number, and txt record.<P>

     Note: Applications should NOT use resolve() solely for txt record monitoring - use
     queryRecord() instead, as it is more efficient for this task.<P>

     Note: When the desired results have been returned, the client MUST terminate the resolve by
     calling {@link DNSSDService#stop}.<P>

     Note: resolve() behaves correctly for typical services that have a single SRV record and
     a single TXT record (the TXT record may be empty.)  To resolve non-standard services with
     multiple SRV or TXT records, use queryRecord().<P>

     @param	flags
     Currently ignored, reserved for future use.
     <P>
     @param	ifIndex
     The interface on which to resolve the service.  The client should
     pass the interface on which the serviceName was discovered (i.e.
     the ifIndex passed to the serviceFound() callback)
     or 0 to resolve the named service on all available interfaces.
     <P>
     @param	serviceName
     The servicename to be resolved.
     <P>
     @param	regType
     The registration type being resolved followed by the protocol, separated by a
     dot (e.g. "_ftp._tcp").  The transport protocol must be "_tcp" or "_udp".
     <P>
     @param	domain
     The domain on which the service is registered, i.e. the domain passed
     to the serviceFound() callback.
     <P>
     @param	listener
     This object will get called when the service is resolved.
     <P>
     @return		A {@link DNSSDService} that represents the active resolve operation.

     @throws SecurityException If a security manager is present and denies <tt>RuntimePermission("getDNSSDInstance")</tt>.
     @see    RuntimePermission
     */
    public DNSSDService resolve(int flags, int ifIndex, String serviceName, String regType, String domain, final ResolveListener listener) throws DNSSDException {
        onServiceStarting();
        final DNSSDService[] services = new DNSSDService[1];

        final Runnable timeoutRunnable = () -> services[0].stop();

        services[0] = new InternalDNSSDService(this, InternalDNSSD.resolve(flags, ifIndex, serviceName, regType, domain, new InternalResolveListener() {
            @Override
            public void serviceResolved(final DNSSDService resolver, final int flags, final int ifIndex, byte[] fullName, byte[] hostName, final int port, TXTRecord txtRecord) {
                final String fullNameStr =  new String(fullName, UTF_8);
                final String hostNameStr =  new String(hostName, UTF_8);
                final Map<String, String> record = parseTXTRecords(txtRecord);
                handler.removeCallbacks(timeoutRunnable);
                handler.post(() -> {
                    listener.serviceResolved(services[0], flags, ifIndex, fullNameStr, hostNameStr, port, record);
                    services[0].stop();
                });
            }

            @Override
            public void operationFailed(final DNSSDService service, final int errorCode) {
                handler.removeCallbacks(timeoutRunnable);
                handler.post(() -> {
                    listener.operationFailed(services[0], errorCode);
                    services[0].stop();
                });
            }
        }));

        handler.postDelayed(timeoutRunnable, serviceTimeout);
        return services[0];
    }

    /** Register a service, to be discovered via browse() and resolve() calls.<P>
     @param	flags
     Possible values are: NO_AUTO_RENAME.
     <P>
     @param	ifIndex
     If non-zero, specifies the interface on which to register the service
     (the index for a given interface is determined via the if_nametoindex()
     family of calls.)  Most applications will pass 0 to register on all
     available interfaces.  Pass -1 to register a service only on the local
     machine (service will not be visible to remote hosts).
     <P>
     @param	serviceName
     If non-null, specifies the service name to be registered.
     Applications need not specify a name, in which case the
     computer name is used (this name is communicated to the client via
     the serviceRegistered() callback).
     <P>
     @param	regType
     The registration type being registered followed by the protocol, separated by a
     dot (e.g. "_ftp._tcp").  The transport protocol must be "_tcp" or "_udp".
     <P>
     @param	domain
     If non-null, specifies the domain on which to advertise the service.
     Most applications will not specify a domain, instead automatically
     registering in the default domain(s).
     <P>
     @param	host
     If non-null, specifies the SRV target host name.  Most applications
     will not specify a host, instead automatically using the machine's
     default host name(s).  Note that specifying a non-null host does NOT
     create an address record for that host - the application is responsible
     for ensuring that the appropriate address record exists, or creating it
     via {@link DNSSDRegistration#addRecord}.
     <P>
     @param	port
     The port on which the service accepts connections.  Pass 0 for a
     "placeholder" service (i.e. a service that will not be discovered by
     browsing, but will cause a name conflict if another client tries to
     register that same name.)  Most clients will not use placeholder services.
     <P>
     @param	txtRecord
     The txt record rdata.  May be null.  Note that a non-null txtRecord
     MUST be a properly formatted DNS TXT record, i.e. &lt;length byte&gt; &lt;data&gt;
     &lt;length byte&gt; &lt;data&gt; ...
     <P>
     @param	listener
     This object will get called when the service is registered.
     <P>
     @return		A {@link DNSSDRegistration} that controls the active registration.

     @throws SecurityException If a security manager is present and denies <tt>RuntimePermission("getDNSSDInstance")</tt>.
     @see    RuntimePermission
     */
    public DNSSDRegistration register(int flags, int ifIndex, String serviceName, String regType, String domain, String host, int port, TXTRecord txtRecord,
                                      final RegisterListener listener) throws DNSSDException {
        onServiceStarting();
        final DNSSDRegistration[] services = new DNSSDRegistration[1];
        services[0] = new InternalDNSSDRegistration(this, InternalDNSSD.register(flags, ifIndex, serviceName, regType, domain, host, port, txtRecord,
                new InternalRegisterListener() {

            @Override
            public void serviceRegistered(DNSSDRegistration registration, final int flags, final byte[] serviceName, byte[] regType, final byte[] domain) {
                final String serviceNameStr =  new String(serviceName, UTF_8);
                final String regTypeStr = new String(regType, UTF_8);
                final String domainStr = new String(domain, UTF_8);
                handler.post(() -> listener.serviceRegistered(services[0], flags, serviceNameStr, regTypeStr, domainStr));
            }

            @Override
            public void operationFailed(DNSSDService service, final int errorCode) {
                handler.post(() -> listener.operationFailed(services[0], errorCode));
            }
        }));
        return services[0];
    }

    /** Register a service, to be discovered via browse() and resolve() calls. Use default flags, ifIndex, domain, host and txtRecord.<P>
     @param	serviceName
     If non-null, specifies the service name to be registered.
     Applications need not specify a name, in which case the
     computer name is used (this name is communicated to the client via
     the serviceRegistered() callback).
     <P>
     @param	regType
     The registration type being registered followed by the protocol, separated by a
     dot (e.g. "_ftp._tcp").  The transport protocol must be "_tcp" or "_udp".
     <P>
     @param	port
     The port on which the service accepts connections.  Pass 0 for a
     "placeholder" service (i.e. a service that will not be discovered by
     browsing, but will cause a name conflict if another client tries to
     register that same name.)  Most clients will not use placeholder services.
     <P>
     @param	listener
     This object will get called when the service is registered.
     <P>
     @return		A {@link DNSSDRegistration} that controls the active registration.

     @throws SecurityException If a security manager is present and denies <tt>RuntimePermission("getDNSSDInstance")</tt>.
     @see    RuntimePermission
     */
    public DNSSDService register(String serviceName, String regType, int port, RegisterListener listener) throws DNSSDException {
        return register(0, 0, serviceName, regType, null, null, port, null, listener);
    }

    /** Create a {@link DNSSDRecordRegistrar} allowing efficient registration of
     multiple individual records.<P>
     <P>
     @return		A {@link DNSSDRecordRegistrar} that can be used to register records.

     @throws SecurityException If a security manager is present and denies <tt>RuntimePermission("getDNSSDInstance")</tt>.
     @see    RuntimePermission
     */
    public DNSSDRecordRegistrar createRecordRegistrar(RegisterRecordListener listener) throws DNSSDException {
        onServiceStarting();
        return new InternalDNSSDRecordRegistrar(this, InternalDNSSD.createRecordRegistrar(listener));
    }

    /** Query for an arbitrary DNS record.<P>
     @param	flags
     Possible values are: MORE_COMING.
     <P>
     @param	ifIndex
     If non-zero, specifies the interface on which to issue the query
     (the index for a given interface is determined via the if_nametoindex()
     family of calls.)  Passing 0 causes the name to be queried for on all
     interfaces.  Passing -1 causes the name to be queried for only on the
     local host.
     <P>
     @param	serviceName
     The full domain name of the resource record to be queried for.
     <P>
     @param	rrtype
     The numerical type of the resource record to be queried for (e.g. PTR, SRV, etc)
     as defined in nameser.h.
     <P>
     @param	rrclass
     The class of the resource record, as defined in nameser.h
     (usually 1 for the Internet class).
     <P>
     @param	listener
     This object will get called when the query completes.
     <P>
     @return		A {@link DNSSDService} that controls the active query.

     @throws SecurityException If a security manager is present and denies <tt>RuntimePermission("getDNSSDInstance")</tt>.
     @see    RuntimePermission
     */
    public DNSSDService queryRecord(int flags, int ifIndex, final String serviceName, int rrtype, int rrclass, final QueryListener listener) throws DNSSDException {
        return this.queryRecord(flags, ifIndex, serviceName, rrtype, rrclass, false, listener);
    }

    /** Query for an arbitrary DNS record.<P>
     @param	flags
     Possible values are: MORE_COMING.
     <P>
     @param	ifIndex
     If non-zero, specifies the interface on which to issue the query
     (the index for a given interface is determined via the if_nametoindex()
     family of calls.)  Passing 0 causes the name to be queried for on all
     interfaces.  Passing -1 causes the name to be queried for only on the
     local host.
     <P>
     @param	serviceName
     The full domain name of the resource record to be queried for.
     <P>
     @param	rrtype
     The numerical type of the resource record to be queried for (e.g. PTR, SRV, etc)
     as defined in nameser.h.
     <P>
     @param	rrclass
     The class of the resource record, as defined in nameser.h
     (usually 1 for the Internet class).
     <P>
     @param	autoStop
     Stop querying after the first response or timeout
     <P>
     @param	listener
     This object will get called when the query completes.
     <P>
     @return		A {@link DNSSDService} that controls the active query.

     @throws SecurityException If a security manager is present and denies <tt>RuntimePermission("getDNSSDInstance")</tt>.
     @see    RuntimePermission
     */
    public DNSSDService queryRecord(int flags, int ifIndex, final String serviceName, int rrtype, int rrclass, boolean autoStop, final QueryListener listener) throws DNSSDException {
        onServiceStarting();
        final DNSSDService[] services = new DNSSDService[1];

        final Runnable timeoutRunnable = () -> services[0].stop();

        services[0] = new InternalDNSSDService(this, InternalDNSSD.queryRecord(flags, ifIndex, serviceName, rrtype, rrclass, new InternalQueryListener() {
            @Override
            public void queryAnswered(DNSSDService query, final int flags, final int ifIndex, byte[] fullName, final int rrtype, final int rrclass, byte[] rdata, final int ttl) {
                final String fullNameStr = new String(fullName, UTF_8);
                handler.removeCallbacks(timeoutRunnable);
                handler.post(() -> {
                    listener.queryAnswered(services[0], flags, ifIndex, fullNameStr, rrtype, rrclass, rdata, ttl);
                    if (autoStop) {
                        services[0].stop();
                    }
                });
            }

            @Override
            public void operationFailed(DNSSDService service, final int errorCode) {
                handler.removeCallbacks(timeoutRunnable);
                handler.post(() -> {
                    listener.operationFailed(services[0], errorCode);
                    services[0].stop();
                });
            }
        }));

        if (autoStop) {
            handler.postDelayed(timeoutRunnable, serviceTimeout);
        }

        return services[0];
    }

    /** Asynchronously enumerate domains available for browsing and registration.<P>

     Currently, the only domain returned is "local.", but other domains will be returned in future.<P>

     The enumeration MUST be cancelled by calling {@link DNSSDService#stop} when no more domains
     are to be found.<P>
     @param	flags
     Possible values are: BROWSE_DOMAINS, REGISTRATION_DOMAINS.
     <P>
     @param	ifIndex
     If non-zero, specifies the interface on which to look for domains.
     (the index for a given interface is determined via the if_nametoindex()
     family of calls.)  Most applications will pass 0 to enumerate domains on
     all interfaces.
     <P>
     @param	listener
     This object will get called when domains are found.
     <P>
     @return		A {@link DNSSDService} that controls the active enumeration.

     @throws SecurityException If a security manager is present and denies <tt>RuntimePermission("getDNSSDInstance")</tt>.
     @see    RuntimePermission
     */
    public DNSSDService enumerateDomains(int flags, int ifIndex, final DomainListener listener) throws DNSSDException {
        onServiceStarting();
        final DNSSDService[] services = new DNSSDService[1];
        services[0] = new InternalDNSSDService(this, InternalDNSSD.enumerateDomains(flags, ifIndex, new InternalDomainListener() {
            @Override
            public void domainFound(DNSSDService domainEnum, final int flags, final int ifIndex, byte[] domain) {
                final String domainStr = new String(domain, UTF_8);
                handler.post(() -> listener.domainFound(services[0], flags, ifIndex, domainStr));
            }

            @Override
            public void domainLost(DNSSDService domainEnum, final int flags, final int ifIndex, byte[] domain) {
                final String domainStr = new String(domain, UTF_8);
                handler.post(() -> listener.domainLost(services[0], flags, ifIndex, domainStr));
            }

            @Override
            public void operationFailed(final DNSSDService service, final int errorCode) {
                handler.post(() -> listener.operationFailed(services[0], errorCode));
            }
        }));
        return services[0];
    }

    /**	Concatenate a three-part domain name (as provided to the listeners) into a
     properly-escaped full domain name. Note that strings passed to listeners are
     ALREADY ESCAPED where necessary.<P>
     @param	serviceName
     The service name - any dots or slashes must NOT be escaped.
     May be null (to construct a PTR record name, e.g. "_ftp._tcp.apple.com").
     <P>
     @param	regType
     The registration type followed by the protocol, separated by a dot (e.g. "_ftp._tcp").
     <P>
     @param	domain
     The domain name, e.g. "apple.com".  Any literal dots or backslashes must be escaped.
     <P>
     @return		The full domain name.

     @throws SecurityException If a security manager is present and denies <tt>RuntimePermission("getDNSSDInstance")</tt>.
     @see    RuntimePermission
     */
    public String constructFullName(String serviceName, String regType, String domain) throws DNSSDException {
        onServiceStarting();
        String result = InternalDNSSD.constructFullName(serviceName, regType, domain);
        onServiceStopped();
        return  result;
    }

    /** Instruct the daemon to verify the validity of a resource record that appears to
     be out of date. (e.g. because tcp connection to a service's target failed.) <P>

     Causes the record to be flushed from the daemon's cache (as well as all other
     daemons' caches on the network) if the record is determined to be invalid.<P>
     @param	flags
     Currently unused, reserved for future use.
     <P>
     @param	ifIndex
     If non-zero, specifies the interface on which to reconfirm the record
     (the index for a given interface is determined via the if_nametoindex()
     family of calls.)  Passing 0 causes the name to be reconfirmed on all
     interfaces.  Passing -1 causes the name to be reconfirmed only on the
     local host.
     <P>
     @param	fullName
     The resource record's full domain name.
     <P>
     @param	rrtype
     The resource record's type (e.g. PTR, SRV, etc) as defined in nameser.h.
     <P>
     @param	rrclass
     The class of the resource record, as defined in nameser.h (usually 1).
     <P>
     @param	rdata
     The raw rdata of the resource record.

     @return		Error DNSServiceErrorType

     @throws SecurityException If a security manager is present and denies <tt>RuntimePermission("getDNSSDInstance")</tt>.
     @see    RuntimePermission
     */
    public int reconfirmRecord(int flags, int ifIndex, String fullName, int rrtype, int rrclass, byte[] rdata) {
        onServiceStarting();
        int error = InternalDNSSD.reconfirmRecord(flags, ifIndex, fullName, rrtype, rrclass, rdata);
        onServiceStopped();
        return error;
    }

    @Override
    public void onServiceStarting() {
        if (multicastLock == null) {
            synchronized (this) { // Double-check lock
                if (multicastLock == null) {
                    WifiManager wifi = (WifiManager) context.getApplicationContext()
                            .getSystemService(Context.WIFI_SERVICE);
                    if (wifi == null) {
                        Log.wtf("DNSSD", "Can't get WIFI Service");
                        return;
                    }
                    multicastLock = wifi.createMulticastLock(MULTICAST_LOCK_NAME);
                    multicastLock.setReferenceCounted(true);
                }
            }
        }
        multicastLock.acquire();
    }

    @Override
    public void onServiceStopped() {
        if (multicastLock == null) {
            Log.wtf("DNSSD", "Multicast lock doesn't exist");
            return;
        }
        multicastLock.release();
    }

    /** Return the index of a named interface.<P>
     @param	ifName
     A valid interface name. An example is java.net.NetworkInterface.getName().
     <P>
     @return		The interface index.

     @throws SecurityException If a security manager is present and denies <tt>RuntimePermission("getDNSSDInstance")</tt>.
     @see    RuntimePermission
     */
    public static int getIfIndexForName(String ifName) {
        return InternalDNSSD.getIfIndexForName(ifName);
    }

    public static Map<String, String> parseTXTRecords(byte[] data) {
        return parseTXTRecords(new TXTRecord(data));
    }

    static Map<String, String> parseTXTRecords(TXTRecord record) {
        Map<String, String> result = new HashMap<>(record.size());
        for (int i = 0; i < record.size(); i++) {
            try {
                if (!TextUtils.isEmpty(record.getKey(i))) {
                    result.put(record.getKey(i), record.getValueAsString(i));
                }
            } catch (Exception e) {
                Log.w("RxResolveListener", "Parsing error of " + i + " TXT record", e);
            }
        }
        return result;
    }

}