package com.zedox.meshtalk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

/**
 * WiFi Direct Broadcast Receiver for MeshTalk
 * Listens for WiFi P2P state changes and updates MainActivity
 * Team ZEDOX - Imagine Cup 2025
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "WiFiDirectReceiver";

    private final WifiP2pManager wifiP2pManager;
    private final WifiP2pManager.Channel channel;
    private final MainActivity mainActivity;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager,
                                        WifiP2pManager.Channel channel,
                                        MainActivity activity) {
        this.wifiP2pManager = manager;
        this.channel = channel;
        this.mainActivity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        switch (action) {
            case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                boolean isEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED;
                Log.d(TAG, "WiFi P2P state: " + (isEnabled ? "enabled" : "disabled"));
                mainActivity.setIsWifiP2pEnabled(isEnabled);
                break;

            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                try {
                    wifiP2pManager.requestPeers(channel, mainActivity::updatePeerList);
                } catch (SecurityException e) {
                    Log.e(TAG, "Permission denied requesting peers", e);
                }
                break;

            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo != null && networkInfo.isConnected()) {
                    try {
                        wifiP2pManager.requestConnectionInfo(channel, mainActivity::onConnectionInfoAvailable);
                    } catch (SecurityException e) {
                        Log.e(TAG, "Permission denied requesting connection info", e);
                    }
                }
                break;

            case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                Log.d(TAG, "This device changed: " + (device != null ? device.deviceName : "unknown"));
                break;

            default:
                break;
        }
    }
}
