/*
 * Copyright (C) 2015 Andriy Druk
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
package com.druk;

import android.os.Parcel;
import android.os.Parcelable;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class BonjourService implements Parcelable {

    public static final int DELETED = ( 1 << 8 );
    private static final String REG_TYPE_SEPARATOR = Pattern.quote(".");

    private final int flags;
    private final String serviceName;
    private final String regType;
    private final String domain;
    private List<InetAddress> addresses;
    private final Map<String, String> dnsRecords;
    private final int ifIndex;
    private final String hostname;
    private final int port;
    private final long timestamp;

    private BonjourService(Builder builder) {
        this.flags = builder.flags;
        this.serviceName = builder.serviceName;
        this.regType = builder.regType;
        this.domain = builder.domain;
        this.ifIndex = builder.ifIndex;
        this.addresses = Collections.unmodifiableList(builder.addresses);
        this.dnsRecords = Collections.unmodifiableMap(builder.dnsRecords);
        this.hostname = builder.hostname;
        this.port = builder.port;
        this.timestamp = System.currentTimeMillis();
    }

    public String[] getRegTypeParts() {
        return regType.split(REG_TYPE_SEPARATOR);
    }

    public int getFlags() {
        return flags;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getRegType() {
        return regType;
    }

    public String getDomain() {
        return domain;
    }

    public int getIfIndex() {
        return ifIndex;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BonjourService that = (BonjourService) o;
        return serviceName.equals(that.serviceName) && regType.equals(that.regType) && domain.equals(that.domain);
    }

    @Override
    public int hashCode() {
        int result = serviceName.hashCode();
        result = 31 * result + regType.hashCode();
        result = 31 * result + domain.hashCode();
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.flags);
        dest.writeString(this.serviceName);
        dest.writeString(this.regType);
        dest.writeString(this.domain);
        writeMap(dest, this.dnsRecords);
        dest.writeInt(this.ifIndex);
        dest.writeString(this.hostname);
        dest.writeInt(this.port);
        dest.writeLong(this.timestamp);
    }

    protected BonjourService(Parcel in) {
        this.flags = in.readInt();
        this.serviceName = in.readString();
        this.regType = in.readString();
        this.domain = in.readString();
        this.dnsRecords = readMap(in);
        this.ifIndex = in.readInt();
        this.hostname = in.readString();
        this.port = in.readInt();
        this.timestamp = in.readLong();
    }

    public static final Parcelable.Creator<BonjourService> CREATOR = new Parcelable.Creator<BonjourService>() {
        public BonjourService createFromParcel(Parcel source) {
            return new BonjourService(source);
        }

        public BonjourService[] newArray(int size) {
            return new BonjourService[size];
        }
    };

    public static void writeMap(Parcel dest, Map<String, String> val) {
        if (val == null) {
            dest.writeInt(-1);
            return;
        }
        int N = val.size();
        dest.writeInt(N);
        for (String key : val.keySet()) {
            dest.writeString(key);
            dest.writeString(val.get(key));
        }
    }

    public final Map<String, String> readMap(Parcel in) {
        int N = in.readInt();
        if (N < 0) {
            return null;
        }
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < N; i++) {
            result.put(in.readString(), in.readString());
        }
        return result;
    }

    @Override
    public String toString() {
        return "BonjourService{" +
                "serviceName='" + serviceName + '\'' +
                ", regType='" + regType + '\'' +
                ", domain='" + domain + '\'' +
                '}';
    }

    public List<InetAddress> getAddresses() {
        return addresses;
    }

    public static class Builder {
        private final int flags;
        private final String serviceName;
        private final String regType;
        private final String domain;
        private final int ifIndex;

        //modified version
        private List<InetAddress> addresses;
        //modified version
        private Map<String, String> dnsRecords;
        private String hostname;
        private int port;

        public Builder(int flags, int ifIndex, String serviceName, String regType, String domain) {
            this.flags = flags;
            this.serviceName = serviceName;
            this.regType = regType;
            this.domain = domain;
            this.ifIndex = ifIndex;
        }

        public Builder(BonjourService service) {
            this.flags = service.flags;
            this.serviceName = service.serviceName;
            this.regType = service.regType;
            this.domain = service.domain;
            this.ifIndex = service.ifIndex;
            this.dnsRecords = new HashMap<>(service.dnsRecords);
            this.addresses = new LinkedList<>(service.addresses);
            this.hostname = service.hostname;
            this.port = service.port;
        }

        public Builder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder dnsRecords(Map<String, String> dnsRecords) {
            this.dnsRecords = dnsRecords;
            return this;
        }

        public Builder addresses(InetAddress address) {
            this.addresses.add(address);
            return this;
        }

        public BonjourService build() {
            return new BonjourService(this);
        }

    }
}
