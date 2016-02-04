#RxDNSSD

Android library which is Rx wrapper for Apple DNSSD Java API.

#####Why RxDNSSD?
My [explanation](http://andriydruk.com/post/mdnsresponder/) about why jmDNS, Android NSD Services and Google Nearby API are not good enough, and why I maintain this library.


####Some examples

#####Browse services example
```java
mSubscription = RxDnssd.browse("_ftp._tcp" /*reqType*/, "." /*domain*/)
	.compose(RxDnssd.resolve())
    .compose(RxDnssd.queryRecords())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(bonjourService -> {
		if (bonjourService.isLost()){
        	mAdapter.add(bonjourService);
        } else {
            mAdapter.remove(bonjourService);
        }
        mAdapter.notifyDataSetChanged();
     }, throwable -> {
        Log.e("DNSSD", "Error: ", throwable);
     });

```

Android library which is Rx wrapper for Apple DNSSD Java API. It contains native client for mDnsResponder for all architectures.

## Why RxDNSSD?
My [explanation](http://andriydruk.com/post/mdnsresponder/) about why jmDNS, Android NSD Services and Google Nearby API are not good enough, and why I maintain this library.

## Download
```groovy
compile 'com.github.andriydruk:rxdnssd:0.5.0'
```

##Some examples

#####Browse services
```java
mSubscription = RxDnssd.browse("_ftp._tcp" /*reqType*/, "." /*domain*/)
	.compose(RxDnssd.resolve())
    .compose(RxDnssd.queryRecords())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(bonjourService -> {
		if (bonjourService.isLost(){
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
