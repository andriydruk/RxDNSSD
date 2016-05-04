#RxDNSSD [![Circle CI](https://circleci.com/gh/andriydruk/RxDNSSD.svg?style=shield&circle-token=5f0cb1ee907a20bdb08aa4b073b5690afbaaabe1)](https://circleci.com/gh/andriydruk/RxDNSSD)

Android library which is Rx wrapper for Apple DNSSD Java API.

##Why RxDNSSD?
My [explanation](http://andriydruk.com/post/mdnsresponder/) about why jmDNS, Android NSD Services and Google Nearby API are not good enough, and why I maintain this library.

##Binaries
```groovy
compile 'com.github.andriydruk:rxdnssd:0.8.0'
```

##How to use

RxDNSSD provides two implementations of RxDnssd interface: 

- RxDnssdBindable
- RxDnssdEmbedded

RxDnssdBindable is an implementation of RxDnssd with system's daemon. Use it for Android project with min API higher than 4.1 for an economy of battery consumption (Also some Samsung devices can don't work with this implementation).

```java
RxDnssd rxdnssd = new RxDnssdBindable(context); 
```

RxDnssdEmbedded is an implementation of RxDnssd with embedded DNS-SD core. Can be used for any Android device with min API higher than Android 4.0.

```java
RxDnssd rxdnssd = new RxDnssdEmbedded(); 
```

#####Register service
```java
Subscription subscription = rxdnssd.register(bonjourService)
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(service -> {
      		updateUi();
      }, throwable -> {
        	Log.e("DNSSD", "Error: ", throwable);
      });
```

#####Browse services example
```java
Subscription subscription = rxdnssd.browse("_ftp._tcp" /*reqType*/, "." /*domain*/)
	.compose(RxDnssd.resolve())
    .compose(RxDnssd.queryRecords())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(bonjourService -> {
		if (!bonjourService.isLost()){
        	mAdapter.add(bonjourService);
        } else {
            mAdapter.remove(bonjourService);
        }
        mAdapter.notifyDataSetChanged();
     }, throwable -> {
        Log.e("DNSSD", "Error: ", throwable);
     });
```

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
