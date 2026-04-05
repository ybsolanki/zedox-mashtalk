package com.zedox.meshtalk.mesh;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.util.Log;

import com.zedox.meshtalk.WiFiDirectService;
import com.zedox.meshtalk.models.Device;
import com.zedox.meshtalk.models.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Connection Manager for MeshTalk
 * Manages the WiFi Direct connection lifecycle and coordinates between
 * WiFiDirectService, MessageRouter, and the rest of the app.
 * Team ZEDOX - Imagine Cup 2025
 */
public class ConnectionManager implements WiFiDirectService.WiFiDirectListener {

    private static final String TAG = "ConnectionManager";

    private final Context context;
    private final WiFiDirectService wifiDirectService;
    private final MessageRouter messageRouter;

    private boolean connected = false;
    private ConnectionManagerListener listener;

    public ConnectionManager(Context context) {
        this.context = context.getApplicationContext();
        this.messageRouter = new MessageRouter();
        this.wifiDirectService = new WiFiDirectService(context, this);

        // Set routing listener once so it is reused for all messages
        this.messageRouter.setRoutingListener(new MessageRouter.MessageRoutingListener() {
            @Override
            public void onMessageDelivered(Message msg) {
                Log.d(TAG, "Message delivered locally: " + msg.getMessageId());
            }

            @Override
            public void onForwardMessage(Message msg, Device nextHop) {
                wifiDirectService.sendMessage(serializeMessage(msg));
            }

            @Override
            public void onMessageDropped(Message msg, String reason) {
                Log.w(TAG, "Message dropped: " + reason);
                if (listener != null) {
                    listener.onError("Message dropped: " + reason);
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /** Call from Activity.onResume() */
    public void start() {
        wifiDirectService.register();
        Log.d(TAG, "ConnectionManager started");
    }

    /** Call from Activity.onPause() */
    public void stop() {
        wifiDirectService.cleanup();
        Log.d(TAG, "ConnectionManager stopped");
    }

    // -------------------------------------------------------------------------
    // Peer discovery & connection
    // -------------------------------------------------------------------------

    /** Begin scanning for nearby WiFi Direct peers. */
    public void discoverPeers() {
        wifiDirectService.discoverPeers();
    }

    /** Connect to a discovered peer. */
    public void connectTo(WifiP2pDevice device) {
        wifiDirectService.connectToPeer(device);
    }

    /** Send a message to the connected peer (or route through the mesh). */
    public void sendMessage(Message message, String localDeviceId) {
        if (!connected) {
            Log.w(TAG, "sendMessage called but not connected");
            if (listener != null) {
                listener.onError("Not connected to any device");
            }
            return;
        }
        messageRouter.routeMessage(message, localDeviceId);
    }

    // -------------------------------------------------------------------------
    // WiFiDirectListener callbacks
    // -------------------------------------------------------------------------

    @Override
    public void onPeersDiscovered(List<WifiP2pDevice> peerList) {
        Log.d(TAG, "Peers discovered: " + peerList.size());
        List<Device> devices = new ArrayList<>();
        for (WifiP2pDevice p : peerList) {
            Device d = new Device(p.deviceAddress, p.deviceName, p.deviceAddress);
            d.setConnected(false);
            devices.add(d);
        }
        if (listener != null) {
            listener.onPeersUpdated(devices);
        }
    }

    @Override
    public void onConnectionChanged(boolean isConnected) {
        this.connected = isConnected;
        Log.d(TAG, "Connection changed: " + isConnected);
        if (listener != null) {
            listener.onConnectionStatusChanged(isConnected);
        }
    }

    @Override
    public void onMessageReceived(String rawMessage) {
        Log.d(TAG, "Raw message received: " + rawMessage);
        if (listener != null) {
            listener.onRawMessageReceived(rawMessage);
        }
    }

    @Override
    public void onError(String error) {
        Log.e(TAG, "WiFiDirect error: " + error);
        if (listener != null) {
            listener.onError(error);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String serializeMessage(Message message) {
        return new com.google.gson.Gson().toJson(message);
    }

    public boolean isConnected() {
        return connected;
    }

    public void setListener(ConnectionManagerListener listener) {
        this.listener = listener;
    }

    // -------------------------------------------------------------------------
    // Listener interface
    // -------------------------------------------------------------------------

    public interface ConnectionManagerListener {
        void onPeersUpdated(List<Device> peers);
        void onConnectionStatusChanged(boolean connected);
        void onRawMessageReceived(String rawMessage);
        void onError(String error);
    }
}
