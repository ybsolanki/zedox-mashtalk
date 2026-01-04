package com.zedox.meshtalk;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Main Activity - WiFi Direct Peer Discovery and Connection
 * Team ZEDOX - Imagine Cup 2025
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int PERMISSIONS_REQUEST_CODE = 1001;

    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;

    private ListView peerListView;
    private TextView statusText;
    private Button discoverButton;
    private ProgressBar progressBar;

    private ArrayAdapter<String> peerAdapter;
    private List<WifiP2pDevice> peers = new ArrayList<>();
    private List<String> peerNames = new ArrayList<>();

    private boolean isWifiP2pEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initializeViews();
        checkAndRequestPermissions();
    }

    private void initializeViews() {
        peerListView = findViewById(R.id.peerListView);
        statusText = findViewById(R.id.statusText);
        discoverButton = findViewById(R.id.discoverButton);
        progressBar = findViewById(R.id.progressBar);

        peerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, peerNames);
        peerListView.setAdapter(peerAdapter);

        discoverButton.setOnClickListener(v -> discoverPeers());

        peerListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < peers.size()) {
                connectToPeer(peers.get(position));
            }
        });
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.NEARBY_WIFI_DEVICES);
            }
        } else {
            // Android 12 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    PERMISSIONS_REQUEST_CODE);
        } else {
            initializeWifiP2p();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                initializeWifiP2p();
            } else {
                Toast.makeText(this, "Permissions required for WiFi Direct", Toast.LENGTH_LONG).show();
                statusText.setText("Permissions denied");
            }
        }
    }

    private void initializeWifiP2p() {
        wifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(this, getMainLooper(), null);

        receiver = new WiFiDirectBroadcastReceiver(wifiP2pManager, channel, this);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        statusText.setText("Ready to discover peers");
        Log.d(TAG, "WiFi P2P initialized");
    }

    private void discoverPeers() {
        if (wifiP2pManager == null || channel == null) {
            Toast.makeText(this, "WiFi Direct not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isWifiP2pEnabled) {
            Toast.makeText(this, "Please enable WiFi", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Discovering peers...");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            checkAndRequestPermissions();
            return;
        }

        wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Peer discovery initiated");
                Toast.makeText(MainActivity.this, "Searching for peers...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                progressBar.setVisibility(View.GONE);
                String errorMsg = "Discovery failed: " + getErrorMessage(reason);
                Log.e(TAG, errorMsg);
                statusText.setText(errorMsg);
                Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void connectToPeer(WifiP2pDevice device) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;

        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Connecting to " + device.deviceName + "...");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            checkAndRequestPermissions();
            return;
        }

        wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Connection initiated to " + device.deviceName);
                Toast.makeText(MainActivity.this, "Connecting...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                progressBar.setVisibility(View.GONE);
                String errorMsg = "Connection failed: " + getErrorMessage(reason);
                Log.e(TAG, errorMsg);
                statusText.setText("Ready to discover peers");
                Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void updatePeerList(WifiP2pDeviceList deviceList) {
        peers.clear();
        peerNames.clear();
        peers.addAll(deviceList.getDeviceList());

        for (WifiP2pDevice device : peers) {
            peerNames.add(device.deviceName + "\n" + device.deviceAddress);
        }

        peerAdapter.notifyDataSetChanged();
        progressBar.setVisibility(View.GONE);

        if (peers.isEmpty()) {
            statusText.setText("No peers found");
        } else {
            statusText.setText("Found " + peers.size() + " peer(s)");
        }

        Log.d(TAG, "Peer list updated: " + peers.size() + " peers");
    }

    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        progressBar.setVisibility(View.GONE);

        if (info.groupFormed) {
            Log.d(TAG, "Group formed. Group Owner: " + info.isGroupOwner);
            statusText.setText("Connected as " + (info.isGroupOwner ? "Group Owner" : "Client"));

            // Launch ChatActivity
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("isGroupOwner", info.isGroupOwner);
            intent.putExtra("groupOwnerAddress", info.groupOwnerAddress.getHostAddress());
            startActivity(intent);
        }
    }

    public void setIsWifiP2pEnabled(boolean enabled) {
        isWifiP2pEnabled = enabled;
        if (enabled) {
            statusText.setText("WiFi Direct enabled");
        } else {
            statusText.setText("WiFi Direct disabled");
        }
    }

    private String getErrorMessage(int reason) {
        switch (reason) {
            case WifiP2pManager.ERROR:
                return "Internal error";
            case WifiP2pManager.P2P_UNSUPPORTED:
                return "P2P unsupported on this device";
            case WifiP2pManager.BUSY:
                return "System busy, try again";
            default:
                return "Unknown error (" + reason + ")";
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (receiver != null && intentFilter != null) {
            registerReceiver(receiver, intentFilter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wifiP2pManager != null && channel != null) {
            wifiP2pManager.removeGroup(channel, null);
        }
    }
}
