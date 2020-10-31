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

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class representing bonjour service
 */
//I don't wanna split this class and its builder
@SuppressWarnings("PMD.GodClass")
public class BonjourService implements Parcelable {

    /**
     * Flag that indicate that Bonjour service was lost
     */
    public static final int LOST = 1 << 8;
    public static final Creator<BonjourService> CREATOR = new Creator<BonjourService>() {
        @NonNull
        public BonjourService createFromParcel(@NonNull Parcel source) {
            return new BonjourService(source);
        }

        @NonNull
        public BonjourService[] newArray(int size) {
            return new BonjourService[size];
        }
    };

    private final int flags;
    private final String serviceName;
    private final String regType;
    private final String domain;
    private final List<InetAddress> inetAddresses;
    private final Map<String, String> dnsRecords;
    private final int ifIndex;
    private final String hostname;
    private final int port;

    protected BonjourService(@NonNull Builder builder) {
        this.flags = builder.flags;
        this.serviceName = builder.serviceName;
        this.regType = builder.regType;
        this.domain = builder.domain;
        this.ifIndex = builder.ifIndex;
        this.inetAddresses = Collections.unmodifiableList(builder.inetAddresses);
        this.dnsRecords = Collections.unmodifiableMap(builder.dnsRecords);
        this.hostname = builder.hostname;
        this.port = builder.port;
    }

    protected BonjourService(@NonNull Parcel in) {
        this.flags = in.readInt();
        this.serviceName = in.readString();
        this.regType = in.readString();
        this.domain = in.readString();
        this.dnsRecords = readMap(in);
        this.inetAddresses = readAddresses(in);
        this.ifIndex = in.readInt();
        this.hostname = in.readString();
        this.port = in.readInt();
    }

    private static void writeAddresses(Parcel dest, List<InetAddress> val) {
        if (val == null) {
            dest.writeInt(-1);
            return;
        }
        int n = val.size();
        dest.writeInt(n);
        for (InetAddress address : val) {
            dest.writeSerializable(address);
        }
    }

    private static List<InetAddress> readAddresses(Parcel in) {
        int n = in.readInt();
        if (n < 0) {
            return null;
        }
        List<InetAddress> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            result.add((InetAddress) in.readSerializable());
        }
        return Collections.unmodifiableList(result);
    }

    private static void writeMap(Parcel dest, Map<String, String> val) {
        if (val == null) {
            dest.writeInt(-1);
            return;
        }
        int n = val.size();
        dest.writeInt(n);
        for (Map.Entry<String, String> entry : val.entrySet()) {
            dest.writeString(entry.getKey());
            dest.writeString(entry.getValue());
        }
    }

    private static Map<String, String> readMap(Parcel in) {
        int n = in.readInt();
        if (n < 0) {
            return null;
        }
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < n; i++) {
            String key = in.readString();
            String value = in.readString();
            if (key != null && value != null) {
                result.put(key, value);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /** Get flags */
    public int getFlags() {
        return flags;
    }

    /** Get the service name */
    @NonNull
    public String getServiceName() {
        return serviceName;
    }

    /** Get reg type */
    @NonNull
    public String getRegType() {
        return regType;
    }

    /** Get domain */
    @Nullable
    public String getDomain() {
        return domain;
    }

    /** Get if index */
    public int getIfIndex() {
        return ifIndex;
    }

    /** Get hostname */
    @Nullable
    public String getHostname() {
        return hostname;
    }

    /** Get port */
    public int getPort() {
        return port;
    }

    /** Get TXT records */
    @NonNull
    public Map<String, String> getTxtRecords() {
        return dnsRecords;
    }

    /** Get ipv4 address */
    @Nullable
    public Inet4Address getInet4Address() {
        for (InetAddress inetAddress : inetAddresses) {
            if (inetAddress instanceof Inet4Address) {
                return (Inet4Address) inetAddress;
            }
        }
        return null;
    }

    /** Get ipv6 address */
    @Nullable
    public Inet6Address getInet6Address() {
        for (InetAddress inetAddress : inetAddresses) {
            if (inetAddress instanceof Inet6Address) {
                return (Inet6Address) inetAddress;
            }
        }
        return null;
    }

    public List<InetAddress> getInetAddresses() {
        return inetAddresses;
    }

    /**
     * Get status of bonjour service
     *
     * @return true if service was lost
     */
    public boolean isLost() {
        return (flags & LOST) == LOST;
    }

    @Override
    //This code was generated by IDEA, and I don't wanna make it less readable
    @SuppressWarnings({"PMD.IfStmtsMustUseBraces", "PMD.SimplifyBooleanReturns"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BonjourService)) return false;

        BonjourService that = (BonjourService) o;

        if (ifIndex != that.ifIndex) return false;
        if (serviceName != null ? !serviceName.equals(that.serviceName) : that.serviceName != null) return false;
        if (regType != null ? !regType.equals(that.regType) : that.regType != null) return false;
        return !(domain != null ? !domain.equals(that.domain) : that.domain != null);

    }

    @Override
    public int hashCode() {
        int result = serviceName != null ? serviceName.hashCode() : 0;
        result = 31 * result + (regType != null ? regType.hashCode() : 0);
        result = 31 * result + (domain != null ? domain.hashCode() : 0);
        result = 31 * result + ifIndex;
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(this.flags);
        dest.writeString(this.serviceName);
        dest.writeString(this.regType);
        dest.writeString(this.domain);
        writeMap(dest, this.dnsRecords);
        writeAddresses(dest, this.inetAddresses);
        dest.writeInt(this.ifIndex);
        dest.writeString(this.hostname);
        dest.writeInt(this.port);
    }

    @NonNull
    @Override
    public String toString() {
        return "BonjourService{"
                + "flags=" + flags
                + ", domain='" + domain + '\''
                + ", regType='" + regType + '\''
                + ", serviceName='" + serviceName + '\''
                + ", dnsRecords=" + dnsRecords
                + ", ifIndex=" + ifIndex
                + ", hostname='" + hostname + '\''
                + ", port=" + port
                + '}';
    }

    public static class Builder {
        private final int flags;
        private final String serviceName;
        private final String regType;
        private final String domain;
        private final int ifIndex;
        private List<InetAddress> inetAddresses = new ArrayList<>();
        //mutable version
        private Map<String, String> dnsRecords = new HashMap<>();
        private String hostname;
        private int port;

        /**
         * Constructs a builder initialized to input parameters
         *
         * @param flags       flags of BonjourService.
         * @param ifIndex     ifIndex of BonjourService.
         * @param serviceName serviceName of BonjourService.
         * @param regType     regType of BonjourService.
         * @param domain      domain of BonjourService.
         */
        public Builder(int flags, int ifIndex, @NonNull String serviceName, @NonNull String regType, String domain) {
            this.flags = flags;
            this.serviceName = serviceName;
            this.regType = regType;
            this.domain = domain;
            this.ifIndex = ifIndex;
        }

        /**
         * Constructs a builder initialized to the contents of existed BonjourService object
         *
         * @param service the initial contents of the object.
         */
        public Builder(@NonNull BonjourService service) {
            this.flags = service.flags;
            this.serviceName = service.serviceName;
            this.regType = service.regType;
            this.domain = service.domain;
            this.ifIndex = service.ifIndex;
            this.dnsRecords = new HashMap<>(service.dnsRecords);
            this.inetAddresses = new ArrayList<>(service.inetAddresses);
            this.hostname = service.hostname;
            this.port = service.port;
        }

        /**
         * Appends hostname of service
         *
         * @param hostname the hostname of service.
         * @return this builder.
         */
        @NonNull
        public Builder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        /**
         * Appends port
         *
         * @param port the port of service.
         * @return this builder.
         */
        @NonNull
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * Appends TXT records of service
         *
         * @param dnsRecords map of TXT records.
         * @return this builder.
         */
        @NonNull
        public Builder dnsRecords(Map<String, String> dnsRecords) {
            this.dnsRecords = dnsRecords;
            return this;
        }

        /**
         * Appends ipv4 address
         *
         * @param inet4Address ipv4 address of service.
         * @return this builder.
         */
        @NonNull
        public Builder inet4Address(Inet4Address inet4Address) {
            this.inetAddresses.add(inet4Address);
            return this;
        }

        /**
         * Appends ipv6 address
         *
         * @param inet6Address ipv6 address of service.
         * @return this builder.
         */
        @NonNull
        public Builder inet6Address(Inet6Address inet6Address) {
            this.inetAddresses.add(inet6Address);
            return this;
        }

        /**
         * Constructs a BonjourService object
         *
         * @return new BonjourService object.
         */
        @NonNull
        public BonjourService build() {
            return new BonjourService(this);
        }

        public void inetAddress(InetAddress inetAddress) {
            this.inetAddresses.add(inetAddress);
        }
    }
}
