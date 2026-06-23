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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * WiFi Direct Service for MeshTalk
 * Handles peer discovery, connection, and messaging
 * Team ZEDOX - Imagine Cup 2025
 */
public class WiFiDirectService {

    private static final String TAG = "WiFiDirectService";
    private static final int SERVER_PORT = 8888;
    private static final int SOCKET_TIMEOUT = 5000;
    private static final int RECONNECT_DELAY_MS = 1500;

    private Context context;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;
    
    private List<WifiP2pDevice> peers = new ArrayList<>();
    private boolean isConnected = false;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private BufferedWriter messageWriter;
    private Thread socketThread;
    private Thread readThread;
    private volatile boolean socketLayerRunning = false;
    private volatile boolean reconnectScheduled = false;

    private boolean desiredSession = false;
    private boolean desiredIsGroupOwner = false;
    private InetAddress desiredGroupOwnerAddress;
    
    private WiFiDirectListener listener;

    /**
     * Listener interface for WiFi Direct events
     */
    public interface WiFiDirectListener {
        void onPeersDiscovered(List<WifiP2pDevice> peerList);
        void onConnectionChanged(boolean connected);
        void onMessageReceived(String message);
        void onError(String error);
    }

    /**
     * Constructor
     */
    public WiFiDirectService(Context context, WiFiDirectListener listener) {
        this.context = context;
        this.listener = listener;
        initialize();
    }

    /**
     * Initialize WiFi Direct
     */
    private void initialize() {
        wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(context, Looper.getMainLooper(), null);
        
        // Setup broadcast receiver
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        
        receiver = new WiFiDirectBroadcastReceiver();
        
        Log.d(TAG, "WiFi Direct initialized");
    }

    /**
     * Start peer discovery
     */
    public void discoverPeers() {
        try {
            wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Peer discovery started");
                }

                @Override
                public void onFailure(int reasonCode) {
                    String error = "Discovery failed: " + getErrorMessage(reasonCode);
                    Log.e(TAG, error);
                    if (listener != null) {
                        listener.onError(error);
                    }
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for peer discovery", e);
            if (listener != null) {
                listener.onError("Permission denied. Please grant location permission.");
            }
        }
    }

    /**
     * Connect to a peer device
     */
    public void connectToPeer(WifiP2pDevice device) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;

        try {
            wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Connection initiated to " + device.deviceName);
                }

                @Override
                public void onFailure(int reasonCode) {
                    String error = "Connection failed: " + getErrorMessage(reasonCode);
                    Log.e(TAG, error);
                    if (listener != null) {
                        listener.onError(error);
                    }
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for connection", e);
            if (listener != null) {
                listener.onError("Permission denied");
            }
        }
    }

    /**
     * Directly start the socket layer using known connection parameters.
     * Use this when the WiFi Direct P2P connection was already established
     * (e.g. by MainActivity) before this service was created.
     */
    public void startSocket(boolean isGroupOwner, String groupOwnerAddress) {
        desiredSession = true;
        desiredIsGroupOwner = isGroupOwner;
        if (!isGroupOwner) {
            try {
                desiredGroupOwnerAddress = InetAddress.getByName(groupOwnerAddress);
            } catch (java.net.UnknownHostException e) {
                Log.e(TAG, "Invalid group owner address: " + groupOwnerAddress, e);
                if (listener != null) {
                    listener.onError("Invalid peer address");
                }
                return;
            }
        }
        startSocketLayer();
    }

    /**
     * Send message to connected peer
     */
    public void sendMessage(final String message) {
        new Thread(() -> {
            try {
                if (clientSocket != null && clientSocket.isConnected() && messageWriter != null) {
                    synchronized (this) {
                        messageWriter.write(message);
                        messageWriter.newLine();
                        messageWriter.flush();
                    }
                    Log.d(TAG, "Message sent: " + message);
                } else {
                    Log.e(TAG, "No active connection to send message");
                    if (listener != null) {
                        new Handler(Looper.getMainLooper()).post(() -> 
                            listener.onError("Not connected to any device")
                        );
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error sending message", e);
                if (listener != null) {
                    new Handler(Looper.getMainLooper()).post(() -> 
                        listener.onError("Failed to send message")
                    );
                }
            }
        }).start();
    }

    /**
     * Start server socket to receive connections
     */
    private void startServer() {
        socketThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
                Log.d(TAG, "Server started on port " + SERVER_PORT);

                while (socketLayerRunning) {
                    clientSocket = serverSocket.accept();
                    Log.d(TAG, "Client connected");
                    handleClient(clientSocket);
                }
            } catch (IOException e) {
                if (socketLayerRunning) {
                    Log.e(TAG, "Server error", e);
                    notifyErrorOnMainThread("Connection error");
                }
            } finally {
                closeServerSocket();
            }
        }, "WiFiDirect-Server");
        socketThread.start();
    }

    /**
     * Connect to server as client
     */
    private void connectToServer(InetAddress serverAddress) {
        socketThread = new Thread(() -> {
            try {
                clientSocket = new Socket();
                clientSocket.connect(new InetSocketAddress(serverAddress, SERVER_PORT), SOCKET_TIMEOUT);
                Log.d(TAG, "Connected to server: " + serverAddress.getHostAddress());
                handleClient(clientSocket);
            } catch (IOException e) {
                if (socketLayerRunning) {
                    Log.e(TAG, "Client connection error", e);
                    socketLayerRunning = false;
                    closeClientSocket();
                    notifyConnectionState(false);
                    notifyErrorOnMainThread("Failed to connect to peer");
                    scheduleReconnect();
                }
            }
        }, "WiFiDirect-Client");
        socketThread.start();
    }

    /**
     * Handle client connection for reading messages
     */
    private void handleClient(Socket socket) {
        notifyConnectionState(true);
        readThread = new Thread(() -> {
            try {
                messageWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String message;
                while (socketLayerRunning && (message = reader.readLine()) != null) {
                    String finalMessage = message;
                    Log.d(TAG, "Message received: " + finalMessage);
                    if (listener != null) {
                        new Handler(Looper.getMainLooper()).post(() ->
                                listener.onMessageReceived(finalMessage));
                    }
                }
            } catch (IOException e) {
                if (socketLayerRunning) {
                    Log.e(TAG, "Error reading message", e);
                }
            } finally {
                messageWriter = null;
                closeClientSocket();
                notifyConnectionState(false);
                if (desiredSession && desiredIsGroupOwner) {
                    socketLayerRunning = true;
                } else {
                    socketLayerRunning = false;
                    if (desiredSession && !desiredIsGroupOwner) {
                        scheduleReconnect();
                    }
                }
            }
        }, "WiFiDirect-Read");
        readThread.start();

        try {
            readThread.join();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private synchronized void startSocketLayer() {
        if (!desiredSession || socketLayerRunning) return;
        socketLayerRunning = true;
        if (desiredIsGroupOwner) {
            startServer();
        } else if (desiredGroupOwnerAddress != null) {
            connectToServer(desiredGroupOwnerAddress);
        }
    }

    private void scheduleReconnect() {
        if (reconnectScheduled) return;
        reconnectScheduled = true;
        new Thread(() -> {
            try {
                Thread.sleep(RECONNECT_DELAY_MS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                reconnectScheduled = false;
                return;
            }
            synchronized (this) {
                reconnectScheduled = false;
                if (!desiredSession || desiredIsGroupOwner) return;
                if (isConnected) return;
                startSocketLayer();
            }
        }, "WiFiDirect-Reconnect").start();
    }

    private void notifyConnectionState(boolean connected) {
        isConnected = connected;
        if (listener != null) {
            new Handler(Looper.getMainLooper()).post(() -> listener.onConnectionChanged(connected));
        }
    }

    private void notifyErrorOnMainThread(String error) {
        if (listener != null) {
            new Handler(Looper.getMainLooper()).post(() -> listener.onError(error));
        }
    }

    private void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
                serverSocket = null;
            }
        } catch (IOException e) {
            Log.w(TAG, "Error closing server socket", e);
        }
    }

    private void closeClientSocket() {
        try {
            if (clientSocket != null) {
                clientSocket.close();
                clientSocket = null;
            }
        } catch (IOException e) {
            Log.w(TAG, "Error closing client socket", e);
        }
    }

    /**
     * Register broadcast receiver
     */
    public void register() {
        context.registerReceiver(receiver, intentFilter);
        Log.d(TAG, "Broadcast receiver registered");
    }

    /**
     * Unregister broadcast receiver
     */
    public void unregister() {
        try {
            context.unregisterReceiver(receiver);
            Log.d(TAG, "Broadcast receiver unregistered");
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Receiver already unregistered");
        }
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        try {
            desiredSession = false;
            socketLayerRunning = false;
            reconnectScheduled = false;
            messageWriter = null;
            closeClientSocket();
            closeServerSocket();
            if (channel != null) {
                wifiP2pManager.removeGroup(channel, null);
            }
            unregister();
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }

    /**
     * Get error message from reason code
     */
    private String getErrorMessage(int reasonCode) {
        switch (reasonCode) {
            case WifiP2pManager.ERROR:
                return "Internal error";
            case WifiP2pManager.P2P_UNSUPPORTED:
                return "WiFi Direct not supported";
            case WifiP2pManager.BUSY:
                return "System busy";
            default:
                return "Unknown error (" + reasonCode + ")";
        }
    }

    /**
     * Broadcast receiver for WiFi Direct events
     */
    private class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                // Peer list changed
                try {
                    wifiP2pManager.requestPeers(channel, peerList -> {
                        peers.clear();
                        peers.addAll(peerList.getDeviceList());
                        
                        Log.d(TAG, "Peers discovered: " + peers.size());
                        if (listener != null) {
                            listener.onPeersDiscovered(new ArrayList<>(peers));
                        }
                    });
                } catch (SecurityException e) {
                    Log.e(TAG, "Permission denied when requesting peers", e);
                }
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                // Connection state changed
                try {
                    wifiP2pManager.requestConnectionInfo(channel, info -> {
                        if (info.groupFormed) {
                            Log.d(TAG, "Connected. Group Owner: " + info.isGroupOwner);

                            desiredSession = true;
                            desiredIsGroupOwner = info.isGroupOwner;
                            desiredGroupOwnerAddress = info.groupOwnerAddress;

                            synchronized (WiFiDirectService.this) {
                                if (!socketLayerRunning) {
                                    startSocketLayer();
                                } else {
                                    notifyConnectionState(true);
                                }
                            }
                        } else {
                            Log.d(TAG, "Disconnected");
                            socketLayerRunning = false;
                            closeClientSocket();
                            notifyConnectionState(false);
                            if (desiredSession && !desiredIsGroupOwner) {
                                scheduleReconnect();
                            }
                        }
                    });
                } catch (SecurityException e) {
                    Log.e(TAG, "Permission denied when requesting connection info", e);
                }
            }
        }
    }

    /**
     * Check if connected to a peer
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Get list of discovered peers
     */
    public List<WifiP2pDevice> getPeers() {
        return new ArrayList<>(peers);
    }
}
