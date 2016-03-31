package com.apple.dnssd;

import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class DNSSDEmbedded {

	public static void init() {
		DNSSD.getInstance();
		Thread thread = new Thread() {
			public void run() {
				int ret = Init();
				Log.v("TAG", "ret from main: " + ret);
			}
		};
		thread.setPriority(Thread.MAX_PRIORITY);
		thread.setName("DNS-SD");
		thread.start();
	}

	public static void exit() {
		DNSSD.getInstance();
		Exit();
	}

	protected static native int Init();

	protected static native void Exit();
}
