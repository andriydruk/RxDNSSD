package com.apple.dnssd;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class DNSSDEmbedded {

	private static final String TAG = "DNSSDEmbedded";
	private static final int STOP_TIMER = 5000; //5 sec

	private Thread mThread;
	private Handler mTimerHandler;
	private volatile boolean isStarted = false;

	public DNSSDEmbedded(){
		mTimerHandler = new Handler(Looper.getMainLooper());
	}

	public void init() {
		if (mThread != null && mThread.isAlive()){
			mTimerHandler.removeCallbacks(stopRunnable);
			if (!isStarted){
				waitUntilStarted();
			}
			return;
		}

		isStarted = false;

		DNSSD.getInstance();
		mThread = new Thread() {
			public void run() {
				Log.v(TAG, "init");
				int err = Init();
				isStarted = true;
				synchronized (DNSSDEmbedded.class) {
					DNSSDEmbedded.class.notifyAll();
				}
				if (err != 0) {
					Log.e(TAG,"error: " + err);
					return;
				}
				Log.v(TAG, "loop");
				int ret = Loop();
				isStarted = false;
				Log.v(TAG, "ret from loop: " + ret);
			}
		};
		mThread.setPriority(Thread.MAX_PRIORITY);
		mThread.setName("DNS-SD");
		mThread.start();

		if (!isStarted){
			waitUntilStarted();
		}
	}

	public void exit() {
		mTimerHandler.postDelayed(stopRunnable, STOP_TIMER);
	}

	private void waitUntilStarted(){
		synchronized (DNSSDEmbedded.class){
			try {
				DNSSDEmbedded.class.wait();
			} catch (InterruptedException e) {
			}
		}
	}

	private Runnable stopRunnable = new Runnable() {
		@Override
		public void run() {
			DNSSD.getInstance();
			Exit();
		}
	};

	protected static native int Init();

	protected static native int Loop();

	protected static native void Exit();
}
