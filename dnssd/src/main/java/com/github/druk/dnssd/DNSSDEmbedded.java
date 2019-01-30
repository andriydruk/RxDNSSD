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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * RxDnssd is implementation of RxDnssd with embedded DNS-SD  {@link InternalDNSSD}
 */
public class DNSSDEmbedded extends DNSSD {

    public static final int DEFAULT_STOP_TIMER_DELAY = 5000; //5 sec

    private static final String TAG = "DNSSDEmbedded";
    private final long mStopTimerDelay;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Thread mThread;
    private volatile boolean isStarted = false;
    private int serviceCount = 0;

    public DNSSDEmbedded(Context context) {
        this(context, DEFAULT_STOP_TIMER_DELAY);
    }

    public DNSSDEmbedded(Context context, long stopTimerDelay) {
        super(context, "jdns_sd_embedded");
        mStopTimerDelay = stopTimerDelay;
    }

    static native int nativeInit();

    static native int nativeLoop();

    static native void nativeExit();

    /**
     * Init DNS-SD thread and start event loop. Should be called before using any of DNSSD operations.
     * If DNS-SD thread has already initialised will try to reuse it.
     *
     * Note: This method will block thread until DNS-SD initialization finish.
     */
    public void init() {
        handler.removeCallbacks(DNSSDEmbedded::nativeExit);

        if (mThread != null && mThread.isAlive()) {
            Log.i(TAG, "already started");
            waitUntilStarted();
            return;
        }

        isStarted = false;

        InternalDNSSD.getInstance();
        mThread = new Thread() {
            public void run() {
                Log.i(TAG, "init");
                int err = nativeInit();
                synchronized (DNSSDEmbedded.class) {
                    isStarted = true;
                    DNSSDEmbedded.class.notifyAll();
                }
                if (err != 0) {
                    Log.e(TAG, "error: " + err);
                    return;
                }
                Log.i(TAG, "start");
                int ret = nativeLoop();
                isStarted = false;
                Log.i(TAG, "finish with code: " + ret);
            }
        };
        mThread.setPriority(Thread.MAX_PRIORITY);
        mThread.setName("DNS-SDEmbedded");
        mThread.start();

        waitUntilStarted();
    }

    /**
     * Exit from embedded DNS-SD loop. This method will stop DNS-SD after the delay (it makes possible to reuse already initialised DNS-SD thread).
     *
     * Note: method isn't blocking, can be used from any thread.
     */
    public void exit() {
        synchronized (DNSSDEmbedded.class) {
            Log.i(TAG, "post exit");
            handler.postDelayed(DNSSDEmbedded::nativeExit, mStopTimerDelay);
        }
    }

    private void waitUntilStarted() {
        synchronized (DNSSDEmbedded.class) {
            while (!isStarted) {
                try {
                    DNSSDEmbedded.class.wait();
                } catch (InterruptedException e) {
                    Log.e(TAG, "waitUntilStarted exception: ", e);
                }
            }
        }
    }

    @Override
    public void onServiceStarting() {
        super.onServiceStarting();
        this.init();
        serviceCount++;
    }

    @Override
    public void onServiceStopped() {
        super.onServiceStopped();
        serviceCount--;
        if (serviceCount == 0) {
            this.exit();
        }
    }
}
