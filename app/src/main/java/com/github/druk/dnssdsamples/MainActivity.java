package com.github.druk.dnssdsamples;

import com.github.druk.rxdnssd.BonjourService;
import com.github.druk.rxdnssd.RxDnssd;
import com.github.druk.rxdnssd.RxDnssdBindable;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private RxDnssd rxDnssd;

    private Subscription browseSubscription;
    private Subscription registerSubscription;

    private ServiceAdapter mServiceAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rxDnssd = new RxDnssdBindable(this);

        findViewById(R.id.check_threads).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 *   When make browse after all services were found and timeout exhausted (default 60 sec) should be only 5 threads:
                 *   - main
                 *   - NsdManager
                 *   - Thread #<n> (it's DNSSD browse thread)
                 *   - RxIoScheduler-1 (rx possibly can create more or less threads, in my case was 2)
                 *   - RxIoScheduler-2
                 */
                Log.i("Thread", "Thread count " + Thread.activeCount() + ":");
                Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
                for (Thread thread : threadSet) {
                    // We only interested in main group
                    if (thread.getThreadGroup().getName().equals("main")) {
                        Log.v("Thread", thread.getName());
                    }
                }
            }
        });

        findViewById(R.id.register).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (registerSubscription == null) {
                    register((Button) v);
                }
                else {
                    unregistered((Button) v);
                }
            }
        });

        findViewById(R.id.browse).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (browseSubscription == null) {
                    ((TextView)v).setText(R.string.browse_stop);
                    findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                    startBrowse();
                }
                else {
                    ((TextView)v).setText(R.string.browse_start);
                    findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
                    stopBrowse();
                    mServiceAdapter.clear();
                }
            }
        });

        mServiceAdapter = new ServiceAdapter(this);

        RecyclerView recyclerView =  (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mServiceAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (browseSubscription == null) {
            ((TextView) findViewById(R.id.browse)).setText(R.string.browse_stop);
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            startBrowse();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (browseSubscription != null) {
            ((TextView) findViewById(R.id.browse)).setText(R.string.browse_start);
            findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
            stopBrowse();
            mServiceAdapter.clear();
        }
    }

    private void startBrowse() {
        Log.i("TAG", "start browse");
        browseSubscription = rxDnssd.browse("_rxdnssd._tcp", "local.")
                .compose(rxDnssd.resolve())
                .compose(rxDnssd.queryRecords())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<BonjourService>() {
                    @Override
                    public void call(BonjourService bonjourService) {
                        Log.d("TAG", bonjourService.toString());
                        if (bonjourService.isLost()) {
                            mServiceAdapter.remove(bonjourService);
                        }
                        else {
                            mServiceAdapter.add(bonjourService);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e("TAG", "error", throwable);
                    }
                });
    }

    private void stopBrowse() {
        Log.d("TAG", "Stop browsing");
        browseSubscription.unsubscribe();
        browseSubscription = null;
    }

    private void register(final Button button) {
        Log.i("TAG", "register");
        button.setEnabled(false);
        BonjourService bs = new BonjourService.Builder(0, 0, Build.DEVICE, "_rxdnssd._tcp", null).port(123).build();
        registerSubscription = rxDnssd.register(bs)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<BonjourService>() {
                    @Override
                    public void call(BonjourService bonjourService) {
                        Log.i("TAG", "Register successfully " + bonjourService.toString());
                        button.setEnabled(true);
                        button.setText(R.string.unregister);
                        Toast.makeText(MainActivity.this, "Rgstrd " + Build.DEVICE, Toast.LENGTH_SHORT).show();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e("TAG", "error", throwable);
                        button.setEnabled(true);
                    }
                });
    }

    private void unregistered(final Button button) {
        Log.d("TAG", "unregister");
        registerSubscription.unsubscribe();
        registerSubscription = null;
        button.setText(R.string.register);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (browseSubscription != null) {
            browseSubscription.unsubscribe();
        }
        if (registerSubscription != null) {
            registerSubscription.unsubscribe();
        }
    }
}
