package com.zedox.meshtalk;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import com.zedox.meshtalk.utils.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * VoiceCallService – full-duplex audio call over Wi-Fi Direct (TCP).
 *
 * Architecture:
 *   • Group owner  → acts as the TCP server, listens on {@link Constants#CALL_PORT}.
 *   • Non-group-owner → acts as the TCP client, connects to the group owner's IP.
 *
 * Once the single TCP connection is established, both sides run two threads:
 *   1. captureThread  – reads PCM from AudioRecord and writes to the socket.
 *   2. playbackThread – reads PCM from the socket and writes to AudioTrack.
 *
 * Audio format: 8 kHz, 16-bit PCM, mono (standard narrow-band voice).
 * Team ZEDOX – Imagine Cup 2025
 */
public class VoiceCallService {

    private static final String TAG = "VoiceCallService";

    private static final int SAMPLE_RATE  = 8000;
    private static final int CHANNEL_IN   = AudioFormat.CHANNEL_IN_MONO;
    private static final int CHANNEL_OUT  = AudioFormat.CHANNEL_OUT_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    /** How long (ms) the client retries connecting to the server before giving up. */
    private static final int CONNECT_TIMEOUT_MS = 12_000;

    private final boolean isGroupOwner;
    private final String  peerAddress;

    private volatile boolean running = false;
    private volatile boolean muted   = false;

    private ServerSocket serverSocket;
    private Socket       callSocket;
    private AudioRecord  audioRecord;
    private AudioTrack   audioTrack;

    private Thread connectThread;
    private Thread captureThread;
    private Thread playbackThread;

    private CallListener listener;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public interface CallListener {
        void onCallConnected();
        void onCallEnded();
        void onError(String error);
    }

    public VoiceCallService(boolean isGroupOwner, String peerAddress) {
        this.isGroupOwner = isGroupOwner;
        this.peerAddress  = peerAddress;
    }

    public void setListener(CallListener listener) {
        this.listener = listener;
    }

    /** Start the call.  Must be called from any thread. */
    public void start() {
        running = true;
        if (isGroupOwner) {
            startServer();
        } else {
            startClient();
        }
    }

    /** Toggle the microphone mute state. Thread-safe. */
    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isMuted() {
        return muted;
    }

    /** Stop the call and release all resources. */
    public void stop() {
        running = false;
        closeQuietly(callSocket);
        closeQuietly(serverSocket);
        releaseAudio();
    }

    // -------------------------------------------------------------------------
    // Connection
    // -------------------------------------------------------------------------

    private void startServer() {
        connectThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(Constants.CALL_PORT);
                Log.d(TAG, "Audio server listening on port " + Constants.CALL_PORT);
                callSocket = serverSocket.accept();
                Log.d(TAG, "Audio client connected");
                onSocketReady();
            } catch (IOException e) {
                if (running) {
                    Log.e(TAG, "Audio server error", e);
                    notifyError("Audio connection failed");
                }
            }
        }, "VoiceCall-Server");
        connectThread.start();
    }

    private void startClient() {
        connectThread = new Thread(() -> {
            try {
                callSocket = new Socket();
                callSocket.connect(
                        new InetSocketAddress(InetAddress.getByName(peerAddress), Constants.CALL_PORT),
                        CONNECT_TIMEOUT_MS);
                Log.d(TAG, "Connected to audio server at " + peerAddress);
                onSocketReady();
            } catch (IOException e) {
                if (running) {
                    Log.e(TAG, "Audio client error", e);
                    notifyError("Cannot connect to audio stream");
                }
            }
        }, "VoiceCall-Client");
        connectThread.start();
    }

    // -------------------------------------------------------------------------
    // Audio
    // -------------------------------------------------------------------------

    private void onSocketReady() {
        if (listener != null) listener.onCallConnected();
        startAudioThreads();
    }

    private void startAudioThreads() {
        int minBuf = Math.max(
                AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, AUDIO_FORMAT),
                AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_OUT, AUDIO_FORMAT));
        // Use a buffer twice the minimum for reliability.
        final int bufSize = minBuf * 2;

        // AudioRecord (microphone)
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE, CHANNEL_IN, AUDIO_FORMAT, bufSize);

        // AudioTrack (speaker / earpiece)
        audioTrack = new AudioTrack(
                AudioManager.STREAM_VOICE_CALL,
                SAMPLE_RATE, CHANNEL_OUT, AUDIO_FORMAT, bufSize,
                AudioTrack.MODE_STREAM);

        audioTrack.play();
        audioRecord.startRecording();

        // Capture thread: mic → socket
        captureThread = new Thread(() -> {
            byte[] buf = new byte[bufSize];
            OutputStream out;
            try {
                out = callSocket.getOutputStream();
            } catch (IOException e) {
                return;
            }
            while (running) {
                int read = audioRecord.read(buf, 0, bufSize);
                if (read > 0) {
                    if (muted) {
                        // Send silence instead of real audio when muted.
                        java.util.Arrays.fill(buf, 0, read, (byte) 0);
                    }
                    try {
                        out.write(buf, 0, read);
                        out.flush();
                    } catch (IOException e) {
                        break;
                    }
                }
            }
        }, "VoiceCall-Capture");

        // Playback thread: socket → speaker
        playbackThread = new Thread(() -> {
            byte[] buf = new byte[bufSize];
            InputStream in;
            try {
                in = callSocket.getInputStream();
            } catch (IOException e) {
                return;
            }
            while (running) {
                try {
                    int read = in.read(buf, 0, bufSize);
                    if (read > 0) {
                        audioTrack.write(buf, 0, read);
                    } else {
                        // Remote side closed the connection.
                        break;
                    }
                } catch (IOException e) {
                    break;
                }
            }
            if (running) {
                // Peer disconnected unexpectedly.
                notifyEnded();
            }
        }, "VoiceCall-Playback");

        captureThread.start();
        playbackThread.start();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void releaseAudio() {
        try {
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error releasing AudioRecord", e);
        }
        try {
            if (audioTrack != null) {
                audioTrack.stop();
                audioTrack.release();
                audioTrack = null;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error releasing AudioTrack", e);
        }
    }

    private static void closeQuietly(java.io.Closeable c) {
        if (c != null) {
            try { c.close(); } catch (IOException ignored) {}
        }
    }

    private void notifyError(String msg) {
        if (listener != null) listener.onError(msg);
    }

    private void notifyEnded() {
        if (listener != null) listener.onCallEnded();
    }
}
