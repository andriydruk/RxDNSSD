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
package com.apple.dnssd;

import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * DNSSDEmbedded provides access to controls of embedded DNS-SD lifecycle.
 */
public class DNSSDEmbedded {

    public static final int DEFAULT_STOP_TIMER_DELAY = 5000; //5 sec

    private static final String TAG = "DNSSDEmbedded";
    private final int mStopTimerDelay;
    private Timer mStopTimer;
    private Thread mThread;
    private volatile boolean isStarted = false;

    public DNSSDEmbedded() {
        this(DEFAULT_STOP_TIMER_DELAY);
    }

    public DNSSDEmbedded(int stopTimerDelay) {
        mStopTimerDelay = stopTimerDelay;
    }

    protected static native int Init();

    protected static native int Loop();

    protected static native void Exit();

    /**
     * Init DNS-SD thread and start event loop. Should be called before using any of DNSSD operations.
     * If DNS-SD thread has already initialised will try to reuse it.
     *
     * Note: This method will block thread until DNS-SD initialization finish.
     */
    public synchronized void init() {
        if (mStopTimer != null) {
            mStopTimer.cancel();
            mStopTimer.purge();
        }

        if (mThread != null && mThread.isAlive()) {
            if (!isStarted) {
                waitUntilStarted();
            }
            return;
        }

        isStarted = false;

        DNSSD.getInstance();
        mThread = new Thread() {
            public void run() {
                Log.i(TAG, "init");
                int err = Init();
                isStarted = true;
                synchronized (DNSSDEmbedded.class) {
                    DNSSDEmbedded.class.notifyAll();
                }
                if (err != 0) {
                    Log.e(TAG, "error: " + err);
                    return;
                }
                Log.i(TAG, "start");
                int ret = Loop();
                isStarted = false;
                Log.i(TAG, "finish with code: " + ret);
            }
        };
        mThread.setPriority(Thread.MAX_PRIORITY);
        mThread.setName("DNS-SD");
        mThread.start();

        if (!isStarted) {
            waitUntilStarted();
        }
    }

    /**
     * Exit from embedded DNS-SD loop. This method will stop DNS-SD after the delay (it makes possible to reuse already initialised DNS-SD thread).
     *
     * Note: method isn't blocking, can be used from any thread.
     */
    public synchronized void exit() {
        mStopTimer = new Timer();
        mStopTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Exit();
            }
        }, mStopTimerDelay);
    }

    private void waitUntilStarted() {
        synchronized (DNSSDEmbedded.class) {
            try {
                DNSSDEmbedded.class.wait();
            } catch (InterruptedException e) {
                Log.e(TAG, "waitUntilStarted exception: ", e);
            }
        }
    }
}
