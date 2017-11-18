# Android mDNSResponder [![Circle CI](https://circleci.com/gh/andriydruk/RxDNSSD.svg?style=shield&circle-token=5f0cb1ee907a20bdb08aa4b073b5690afbaaabe1)](https://circleci.com/gh/andriydruk/RxDNSSD) [![Download](https://api.bintray.com/packages/andriydruk/maven/dnssd/images/download.svg)](https://bintray.com/andriydruk/maven/rxdnssd/_latestVersion) [![Download](https://api.bintray.com/packages/andriydruk/maven/rxdnssd/images/download.svg)](https://bintray.com/andriydruk/maven/rxdnssd/_latestVersion)



## Why I created this library?
My [explanation](http://andriydruk.com/post/mdnsresponder/) about why jmDNS, Android NSD Services and Google Nearby API are not good enough, and why I maintain this library.

## Hierarchy

```
                                   +--------------------+       +--------------------+
                                   |      RxDNSSD       |       |       RxDNSSD2     |
                                   +--------------------+       +--------------------+
                                           |                            |
                                           |   +--------------------+   |
                                            -->| Android Java DNSSD |<--
                                               +--------------------+
                                               |  Apple Java DNSSD  |
                 +------------------+          +--------------------+
                 |    daemon.c      |<-------->|     mDNS Client    |
                 +------------------+          +--------------------+
                 |    mDNS Core     |
                 +------------------+
                 | Platform Support |
                 +------------------+
                    System process                Your Android app

```

## Binaries

My dnssd library:

```groovy
compile 'com.github.andriydruk:dnssd:0.9.2'
```

My rxdnssd library:

```groovy
compile 'com.github.andriydruk:rxdnssd:0.9.2'
```

My rxdnssd2 library:

```
Still in progress ...
```

* It's built with Andorid NDK r16 for all platforms (2.18 MB). If you prefer another NDK version or subset of platforms, please build it from source with command:

```groovy
./gradlew clean build
```

## How to use

### DNSSD

```
DNSSD dnssd = new DNSSDBindable(context); 
```

##### Register service
```java
try {
	registerService = dnssd.register("service_name", "_rxdnssd._tcp", 123,  
   		new RegisterListener() {

			@Override
			public void serviceRegistered(DNSSDRegistration registration, int flags, 
				String serviceName, String regType, String domain) {
				Log.i("TAG", "Register successfully ");
			}

			@Override
         	public void operationFailed(DNSSDService service, int errorCode) {
				Log.e("TAG", "error " + errorCode);
        	}
   		});
} catch (DNSSDException e) {
	Log.e("TAG", "error", e);
}
```

##### Browse services example
```java
try {
	browseService = dnssd.browse("_rxdnssd._tcp", new BrowseListener() {
                
 		@Override
		public void serviceFound(DNSSDService browser, int flags, int ifIndex, 
			final String serviceName, String regType, String domain) {
			Log.i("TAG", "Found " + serviceName);
		}

		@Override
		public void serviceLost(DNSSDService browser, int flags, int ifIndex, 
			String serviceName, String regType, String domain) {
			Log.i("TAG", "Lost " + serviceName);
		}

		@Override
		public void operationFailed(DNSSDService service, int errorCode) {
			Log.e("TAG", "error: " + errorCode);
		}        
	});
} catch (DNSSDException e) {
	Log.e("TAG", "error", e);
}
```

You can find more samples in app inside this repository.

### RxDNSSD

```
RxDnssd rxdnssd = new RxDnssdBindable(context); 
```

##### Register service
```java
Subscription subscription = rxdnssd.register(bonjourService)
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(service -> {
      		updateUi();
      }, throwable -> {
        	Log.e("DNSSD", "Error: ", throwable);
      });
```

##### Browse services example
```java
Subscription subscription = rxDnssd.browse("_http._tcp", "local.")
	.compose(rxDnssd.resolve())
    .compose(rxDnssd.queryRecords())
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(new Action1<BonjourService>() {
    	@Override
        public void call(BonjourService bonjourService) {
        	Log.d("TAG", bonjourService.toString());
        }
    }, new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) {
        	Log.e("TAG", "error", throwable);
        }
	});
```

### RxDNSSD2

Still in progress ... (I recieve PR 😉)

License
-------
	Copyright (C) 2016 Andriy Druk

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
