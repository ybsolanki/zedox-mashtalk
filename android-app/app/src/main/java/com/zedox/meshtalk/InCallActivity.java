package com.zedox.meshtalk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.zedox.meshtalk.utils.Constants;

/**
 * InCallActivity – shown during an active voice call.
 *
 * Owns a {@link VoiceCallService} instance that streams audio over TCP.
 * Provides Mute and Hang-Up controls.
 *
 * Listens for {@link Constants#ACTION_PEER_CALL_END} via LocalBroadcastManager
 * (sent by ChatActivity when a CALL_END signal arrives from the peer) so it can
 * end the call gracefully from either side.
 * Team ZEDOX – Imagine Cup 2025
 */
public class InCallActivity extends AppCompatActivity {

    private String contactName;
    private boolean isGroupOwner;
    private String groupOwnerAddress;

    private VoiceCallService voiceCallService;
    private boolean isMuted = false;

    private TextView tvStatus;
    private TextView tvMuteLabel;
    private ImageButton btnMute;

    // Call timer
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private int secondsElapsed = 0;
    private boolean timerRunning = false;
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            secondsElapsed++;
            int minutes = secondsElapsed / 60;
            int secs    = secondsElapsed % 60;
            if (tvStatus != null) {
                tvStatus.setText(String.format("%02d:%02d", minutes, secs));
            }
            timerHandler.postDelayed(this, 1000);
        }
    };

    // Receives CALL_END signal forwarded from ChatActivity.
    private final BroadcastReceiver peerEndedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            endCall(false /* don't notify ChatActivity again */);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_call);

        contactName       = getIntent().getStringExtra("CONTACT_NAME");
        isGroupOwner      = getIntent().getBooleanExtra("isGroupOwner", true);
        groupOwnerAddress = getIntent().getStringExtra("groupOwnerAddress");

        if (contactName == null || contactName.isEmpty()) contactName = getString(R.string.unknown_caller);

        // Views
        TextView tvName = findViewById(R.id.tvContactNameCall);
        TextView tvInit = findViewById(R.id.tvContactInitial);
        tvStatus        = findViewById(R.id.tvCallStatus);
        btnMute         = findViewById(R.id.btnMute);
        tvMuteLabel     = findViewById(R.id.tvMuteLabel);
        ImageButton btnHangUp = findViewById(R.id.btnHangUp);

        tvName.setText(contactName);
        tvInit.setText(String.valueOf(contactName.charAt(0)).toUpperCase());
        tvStatus.setText(R.string.connecting);

        btnMute.setOnClickListener(v -> toggleMute());
        btnHangUp.setOnClickListener(v -> endCall(true));

        // Listen for peer-initiated call end.
        LocalBroadcastManager.getInstance(this).registerReceiver(
                peerEndedReceiver,
                new IntentFilter(Constants.ACTION_PEER_CALL_END));

        startVoiceCall();
    }

    private void startVoiceCall() {
        voiceCallService = new VoiceCallService(isGroupOwner, groupOwnerAddress);
        voiceCallService.setListener(new VoiceCallService.CallListener() {
            @Override
            public void onCallConnected() {
                runOnUiThread(() -> {
                    tvStatus.setText(R.string.call_connected);
                    startTimer();
                });
            }

            @Override
            public void onCallEnded() {
                runOnUiThread(() -> endCall(true));
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(InCallActivity.this,
                            "Call error: " + error, Toast.LENGTH_LONG).show();
                    endCall(false);
                });
            }
        });
        voiceCallService.start();
    }

    private void toggleMute() {
        isMuted = !isMuted;
        voiceCallService.setMuted(isMuted);
        btnMute.setImageResource(isMuted ? R.drawable.ic_mic_off : R.drawable.ic_mic_on);
        tvMuteLabel.setText(isMuted ? R.string.unmute : R.string.mute);
    }

    /**
     * End the call.
     *
     * @param notifyChatActivity when {@code true}, sends ACTION_CALL_END_LOCAL via
     *                           LocalBroadcastManager so ChatActivity can forward a
     *                           CALL_END signal to the peer over Wi-Fi Direct.
     */
    private void endCall(boolean notifyChatActivity) {
        stopTimer();
        if (voiceCallService != null) {
            voiceCallService.stop();
            voiceCallService = null;
        }
        if (notifyChatActivity) {
            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(new Intent(Constants.ACTION_CALL_END_LOCAL));
        }
        finish();
    }

    // -------------------------------------------------------------------------
    // Timer
    // -------------------------------------------------------------------------

    private void startTimer() {
        timerRunning = true;
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void stopTimer() {
        timerRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(peerEndedReceiver);
        stopTimer();
        if (voiceCallService != null) {
            voiceCallService.stop();
        }
    }

    @Override
    public void onBackPressed() {
        endCall(true);
    }
}
