package com.github.druk.dnssdsamples;

import com.github.druk.dnssd.BrowseListener;
import com.github.druk.dnssd.DNSSD;
import com.github.druk.dnssd.DNSSDEmbedded;
import com.github.druk.dnssd.DNSSDException;
import com.github.druk.dnssd.DNSSDRegistration;
import com.github.druk.dnssd.DNSSDService;
import com.github.druk.dnssd.QueryListener;
import com.github.druk.dnssd.RegisterListener;
import com.github.druk.dnssd.ResolveListener;
import com.github.druk.rxdnssd.BonjourService;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Map;

public class DNSSDActivity extends AppCompatActivity {

    private DNSSD dnssd;

    private ServiceAdapter mServiceAdapter;
    private Handler mHandler;

    private DNSSDService browseService;
    private DNSSDService registerService;

    private static final String SERVICE_NAME = Build.DEVICE;
    private static final int SERVICE_PORT = 123;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dnssd = new DNSSDEmbedded();

        mHandler = new Handler(Looper.getMainLooper());

        findViewById(R.id.register).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (registerService == null) {
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
                if (browseService == null) {
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

        mServiceAdapter = new ServiceAdapter(this) {

        };

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mServiceAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (browseService == null) {
            ((TextView) findViewById(R.id.browse)).setText(R.string.browse_stop);
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            startBrowse();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (browseService != null) {
            ((TextView)findViewById(R.id.browse)).setText(R.string.browse_start);
            findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
            stopBrowse();
            mServiceAdapter.clear();
        }
    }

    private void startBrowse() {
        Log.i("TAG", "start browse");
        try {
            browseService = dnssd.browse("_rxdnssd._tcp", new BrowseListener() {
                @Override
                public void serviceFound(DNSSDService browser, int flags, int ifIndex, final String serviceName, String regType, String domain) {
                    Log.d("TAG", "Found " + serviceName);
                    startResolve(flags, ifIndex, serviceName, regType, domain);
                }

                @Override
                public void serviceLost(DNSSDService browser, int flags, int ifIndex, String serviceName, String regType, String domain) {
                    mServiceAdapter.remove(new BonjourService.Builder(flags, ifIndex, serviceName, regType, domain).build());
                }

                @Override
                public void operationFailed(DNSSDService service, int errorCode) {
                    Log.e("TAG", "error: " + errorCode);
                }
            });
        } catch (DNSSDException e) {
            e.printStackTrace();
            Log.e("TAG", "error", e);
        }
    }

    private void startResolve(int flags, int ifIndex, final String serviceName, final String regType, final String domain) {
        try {
            dnssd.resolve(flags, ifIndex, serviceName, regType, domain, new ResolveListener() {
                @Override
                public void serviceResolved(DNSSDService resolver, int flags, int ifIndex, String fullName, String hostName, int port, Map<String, String> txtRecord) {
                    Log.d("TAG", "Resolved " + hostName);
                    startQueryRecords(flags, ifIndex, serviceName, regType, domain, hostName, port, txtRecord);
                    resolver.stop();
                }

                @Override
                public void operationFailed(DNSSDService service, int errorCode) {

                }
            });
        } catch (DNSSDException e) {
            e.printStackTrace();
        }
    }

    private void startQueryRecords(int flags, int ifIndex, final String serviceName, final String regType, final String domain, final String hostName, final int port, final Map<String, String> txtRecord) {
        try {
            QueryListener listener = new QueryListener() {
                @Override
                public void queryAnswered(DNSSDService query, final int flags, final int ifIndex, final String fullName, int rrtype, int rrclass, final InetAddress address, int ttl) {
                    Log.d("TAG", "Query address " + fullName);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            BonjourService.Builder builder = new BonjourService.Builder(flags, ifIndex, serviceName, regType, domain).dnsRecords(txtRecord).port(port).hostname(hostName);
                            if (address instanceof Inet4Address) {
                                builder.inet4Address((Inet4Address) address);
                            } else if (address instanceof Inet6Address) {
                                builder.inet6Address((Inet6Address) address);
                            }
                            mServiceAdapter.add(builder.build());
                            query.stop();
                        }
                    });
                }

                @Override
                public void operationFailed(DNSSDService service, int errorCode) {

                }
            };
            dnssd.queryRecord(0, ifIndex, hostName, 1, 1, listener);
            dnssd.queryRecord(0, ifIndex, hostName, 28, 1, listener);
        } catch (DNSSDException e) {
            e.printStackTrace();
        }
    }

    private void stopBrowse() {
        Log.d("TAG", "Stop browsing");
        browseService.stop();
        browseService = null;
    }

    private void register(final Button button) {
        Log.i("TAG", "register");
        button.setEnabled(false);
        try {
            registerService = dnssd.register(Build.DEVICE, "_rxdnssd._tcp", 123,  new RegisterListener() {
                @Override
                public void serviceRegistered(DNSSDRegistration registration, int flags, String serviceName, String regType, String domain) {
                    Log.i("TAG", "Register successfully " + Build.DEVICE);
                    button.setEnabled(true);
                    button.setText(R.string.unregister);
                    Toast.makeText(DNSSDActivity.this, "Rgstrd " + Build.DEVICE, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void operationFailed(DNSSDService service, int errorCode) {
                    Log.e("TAG", "error " + errorCode);
                    button.setEnabled(true);
                }
            });
        } catch (DNSSDException e) {
            e.printStackTrace();
        }
    }

    private void unregistered(final Button button) {
        Log.d("TAG", "unregister");
        registerService.stop();
        registerService = null;
        button.setText(R.string.register);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (browseService != null) {
            browseService.stop();
        }
        if (registerService != null) {
            registerService.stop();
        }
    }
}
