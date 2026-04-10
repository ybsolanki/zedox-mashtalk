package com.zedox.meshtalk;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.zedox.meshtalk.utils.Constants;

/**
 * IncomingCallActivity – shown when a remote peer initiates a voice call.
 *
 * The user can either accept (→ InCallActivity opens and CALL_ACCEPT is signalled
 * back to the caller via ChatActivity) or decline (→ CALL_REJECT is signalled).
 *
 * Communication with ChatActivity is done through LocalBroadcastManager so that
 * ChatActivity – which owns the Wi-Fi Direct connection – can send the actual
 * signalling messages without opening a second socket.
 * Team ZEDOX – Imagine Cup 2025
 */
public class IncomingCallActivity extends AppCompatActivity {

    private String callerName;
    private String contactId;
    private boolean isGroupOwner;
    private String groupOwnerAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);

        callerName        = getIntent().getStringExtra("CALLER_NAME");
        contactId         = getIntent().getStringExtra("CONTACT_ID");
        isGroupOwner      = getIntent().getBooleanExtra("isGroupOwner", true);
        groupOwnerAddress = getIntent().getStringExtra("groupOwnerAddress");

        if (callerName == null || callerName.isEmpty()) callerName = getString(R.string.unknown_caller);

        TextView tvName    = findViewById(R.id.tvCallerName);
        TextView tvInitial = findViewById(R.id.tvCallerInitial);
        tvName.setText(callerName);
        tvInitial.setText(String.valueOf(callerName.charAt(0)).toUpperCase());

        ImageButton btnAccept = findViewById(R.id.btnAcceptCall);
        ImageButton btnReject = findViewById(R.id.btnRejectCall);

        btnAccept.setOnClickListener(v -> acceptCall());
        btnReject.setOnClickListener(v -> rejectCall());

        // Treat back-press the same as rejecting the call.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                rejectCall();
            }
        });
    }

    private void acceptCall() {
        // Tell ChatActivity to send CALL_ACCEPT to the peer.
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(new Intent(Constants.ACTION_CALL_ACCEPT));

        // Open the in-call screen.
        Intent intent = new Intent(this, InCallActivity.class);
        intent.putExtra("CONTACT_NAME",      callerName);
        intent.putExtra("CONTACT_ID",        contactId);
        intent.putExtra("isGroupOwner",      isGroupOwner);
        intent.putExtra("groupOwnerAddress", groupOwnerAddress);
        startActivity(intent);
        finish();
    }

    private void rejectCall() {
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(new Intent(Constants.ACTION_CALL_REJECT));
        finish();
    }

    /** If the user presses Back, treat it as a rejection. */
    // onBackPress handled via OnBackPressedDispatcher (see onCreate).
}
