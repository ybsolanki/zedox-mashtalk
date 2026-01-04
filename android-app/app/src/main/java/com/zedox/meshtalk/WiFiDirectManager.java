package com.zedox.meshtalk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * WiFi Direct P2P Manager for MeshTalk
 * Handles device discovery, connection, and peer management
 * Team ZEDOX - Imagine Cup 2025
 */
public class WiFiDirectManager {

    private static final String TAG = "WiFiDirectManager";

    private Context context;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;

    private List<WifiP2pDevice> peers = new ArrayList<>();
    private WifiP2pDevice connectedDevice;
    private WifiP2pInfo connectionInfo;

    private PeerDiscoveryListener discoveryListener;
    private ConnectionListener connectionListener;

    /**
     * Initialize WiFi Direct Manager
     */
    public WiFiDirectManager(Context context) {
        this.context = context;
        wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(context, Looper.getMainLooper(), null);

        // Setup intent filter for WiFi Direct broadcasts
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        receiver = createBroadcastReceiver();
    }

    /**
     * Register broadcast receiver
     */
    public void register() {
        context.registerReceiver(receiver, intentFilter);
    }

    /**
     * Unregister broadcast receiver
     */
    public void unregister() {
        try {
            context.unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Receiver not registered", e);
        }
    }

    /**
     * Start peer discovery
     */
    public void discoverPeers() {
        wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Peer discovery started");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Peer discovery failed: " + reason);
            }
        });
    }

    /**
     * Stop peer discovery
     */
    public void stopPeerDiscovery() {
        wifiP2pManager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Peer discovery stopped");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Stop discovery failed: " + reason);
            }
        });
    }

    /**
     * Connect to a peer device
     */
    public void connect(WifiP2pDevice device) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;

        wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Connection initiated to " + device.deviceName);
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Connection failed: " + reason);
                if (connectionListener != null) {
                    connectionListener.onConnectionFailed(reason);
                }
            }
        });
    }

    /**
     * Disconnect from current peer
     */
    public void disconnect() {
        wifiP2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Disconnected");
                connectedDevice = null;
                connectionInfo = null;
                if (connectionListener != null) {
                    connectionListener.onDisconnected();
                }
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Disconnect failed: " + reason);
            }
        });
    }

    /**
     * Create broadcast receiver for WiFi Direct events
     */
    private BroadcastReceiver createBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                    // WiFi P2P state changed
                    int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                    boolean isEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED;
                    Log.d(TAG, "WiFi P2P state: " + (isEnabled ? "enabled" : "disabled"));

                } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                    // Peer list changed
                    wifiP2pManager.requestPeers(channel, peerListListener);

                } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                    // Connection state changed
                    wifiP2pManager.requestConnectionInfo(channel, connectionInfoListener);

                } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                    // This device's details changed
                    WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                    Log.d(TAG, "This device: " + (device != null ? device.deviceName : "unknown"));
                }
            }
        };
    }

    /**
     * Peer list listener
     */
    private WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            peers.clear();
            peers.addAll(peerList.getDeviceList());
            Log.d(TAG, "Peers found: " + peers.size());

            if (discoveryListener != null) {
                discoveryListener.onPeersDiscovered(peers);
            }
        }
    };

    /**
     * Connection info listener
     */
    private WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            connectionInfo = info;

            if (info.groupFormed) {
                Log.d(TAG, "Group formed. Is group owner: " + info.isGroupOwner);
                Log.d(TAG, "Group owner address: " + (info.groupOwnerAddress != null ? info.groupOwnerAddress.getHostAddress() : "null"));

                if (connectionListener != null) {
                    connectionListener.onConnected(info);
                }
            } else {
                Log.d(TAG, "Group not formed");
                if (connectionListener != null) {
                    connectionListener.onDisconnected();
                }
            }
        }
    };

    /**
     * Get discovered peers
     */
    public List<WifiP2pDevice> getPeers() {
        return peers;
    }

    /**
     * Get connection info
     */
    public WifiP2pInfo getConnectionInfo() {
        return connectionInfo;
    }

    /**
     * Check if connected
     */
    public boolean isConnected() {
        return connectionInfo != null && connectionInfo.groupFormed;
    }

    /**
     * Set peer discovery listener
     */
    public void setPeerDiscoveryListener(PeerDiscoveryListener listener) {
        this.discoveryListener = listener;
    }

    /**
     * Set connection listener
     */
    public void setConnectionListener(ConnectionListener listener) {
        this.connectionListener = listener;
    }

    /**
     * Peer discovery listener interface
     */
    public interface PeerDiscoveryListener {
        void onPeersDiscovered(List<WifiP2pDevice> peers);
    }

    /**
     * Connection listener interface
     */
    public interface ConnectionListener {
        void onConnected(WifiP2pInfo info);
        void onDisconnected();
        void onConnectionFailed(int reason);
    }
}
