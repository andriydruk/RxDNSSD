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

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.Log;

/**
 * RxDnssdBindable is implementation of RxDnssd with system's daemon {@link InternalDNSSD}
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public final class DNSSDBindable extends DNSSD {

    private static final String TAG = "DNSSDBindable";
    private final Context context;

    public DNSSDBindable(Context context) {
        super(context, "jdns_sd");
        this.context = context.getApplicationContext();
    }

    @Override
    public void onServiceStarting() {
        super.onServiceStarting();
        try {
            context.getSystemService(Context.NSD_SERVICE);
        }
        catch (Exception e) {
            Log.e(TAG, "Can't start NSD_SERVICE: ", e);
        }
    }

    @Override
    public void onServiceStopped() {
        super.onServiceStopped();
        // Not used in bindable version
    }

    /** Return the canonical name of a particular interface index.<P>
     @param	ifIndex
     A valid interface index. Must not be ALL_INTERFACES.
     <P>
     @return		The name of the interface, which should match java.net.NetworkInterface.getName().

     @throws SecurityException If a security manager is present and denies <tt>RuntimePermission("getDNSSDInstance")</tt>.
     @see    RuntimePermission
     */
    public String getNameForIfIndex(int ifIndex) {
        return InternalDNSSD.getNameForIfIndex(ifIndex);
    }
}
