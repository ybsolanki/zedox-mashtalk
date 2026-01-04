package com.zedox.meshtalk;

import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Message Service for WiFi Direct Communication
 * Handles sending and receiving messages over WiFi Direct
 * Team ZEDOX - Imagine Cup 2025
 */
public class MessageService {

    private static final String TAG = "MessageService";
    private static final int SERVER_PORT = 8888;

    private ExecutorService executorService;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private BufferedWriter writer;
    private BufferedReader reader;
    private Handler mainHandler;
    private Gson gson;

    private MessageListener messageListener;
    private boolean isRunning = false;
    private boolean isGroupOwner = false;
    private InetAddress groupOwnerAddress;

    public MessageService() {
        executorService = Executors.newCachedThreadPool();
        mainHandler = new Handler(Looper.getMainLooper());
        gson = new Gson();
    }

    public void start(WifiP2pInfo connectionInfo) {
        if (connectionInfo == null || !connectionInfo.groupFormed) {
            Log.e(TAG, "No valid connection info");
            return;
        }

        isGroupOwner = connectionInfo.isGroupOwner;
        groupOwnerAddress = connectionInfo.groupOwnerAddress;

        if (isGroupOwner) {
            startServer();
        } else {
            startClient(groupOwnerAddress);
        }
    }

    private void startServer() {
        executorService.execute(() -> {
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
                isRunning = true;
                Log.d(TAG, "Server started on port " + SERVER_PORT);

                while (isRunning) {
                    Socket socket = serverSocket.accept();
                    Log.d(TAG, "Client connected: " + socket.getInetAddress().getHostAddress());
                    handleClient(socket);
                }
            } catch (IOException e) {
                Log.e(TAG, "Server error", e);
            }
        });
    }

    private void startClient(InetAddress serverAddress) {
        executorService.execute(() -> {
            try {
                clientSocket = new Socket(serverAddress, SERVER_PORT);
                Log.d(TAG, "Connected to server: " + serverAddress.getHostAddress());
                handleClient(clientSocket);
            } catch (IOException e) {
                Log.e(TAG, "Client connection error", e);
            }
        });
    }

    private void handleClient(Socket socket) {
        try {
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            mainHandler.post(() -> {
                if (messageListener != null) {
                    messageListener.onConnectionEstablished();
                }
            });

            String line;
            while ((line = reader.readLine()) != null) {
                final String message = line;
                Log.d(TAG, "Message received: " + message);

                mainHandler.post(() -> {
                    if (messageListener != null) {
                        try {
                            Message msg = gson.fromJson(message, Message.class);
                            messageListener.onMessageReceived(msg);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to parse message", e);
                        }
                    }
                });
            }
        } catch (IOException e) {
            Log.e(TAG, "Client handler error", e);
        }
    }

    public void sendMessage(Message message) {
        executorService.execute(() -> {
            try {
                if (writer != null) {
                    String json = gson.toJson(message);
                    writer.write(json + "\n");
                    writer.flush();
                    Log.d(TAG, "Message sent: " + json);

                    mainHandler.post(() -> {
                        if (messageListener != null) {
                            messageListener.onMessageSent(message);
                        }
                    });
                } else {
                    Log.e(TAG, "Writer is null, cannot send message");
                }
            } catch (IOException e) {
                Log.e(TAG, "Send message error", e);
            }
        });
    }

    public void stop() {
        isRunning = false;

        try {
            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (clientSocket != null) clientSocket.close();
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing connections", e);
        }

        executorService.shutdown();
        Log.d(TAG, "Service stopped");
    }

    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    public interface MessageListener {
        void onConnectionEstablished();
        void onMessageReceived(Message message);
        void onMessageSent(Message message);
    }
}
