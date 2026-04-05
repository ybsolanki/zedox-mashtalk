package com.zedox.meshtalk;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Home Activity – main entry screen for MeshTalk
 * Shows three action buttons: Start Chat, Find Devices, Settings
 * Team ZEDOX - Imagine Cup 2025
 */
public class HomeActivity extends AppCompatActivity {

    private Button btnStartChat;
    private Button btnFindDevices;
    private Button btnSettings;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        btnStartChat   = findViewById(R.id.btnStartChat);
        btnFindDevices = findViewById(R.id.btnFindDevices);
        btnSettings    = findViewById(R.id.btnSettings);
        tvStatus       = findViewById(R.id.tvStatus);

        btnStartChat.setOnClickListener(v -> {
            // Open chat with a placeholder contact – real contact comes from
            // WiFi Direct peer selection in MainActivity
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("CONTACT_NAME", "MeshTalk Peer");
            startActivity(intent);
        });

        btnFindDevices.setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));

        btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update status indicator whenever the user returns to this screen
        if (tvStatus != null) {
            tvStatus.setVisibility(android.view.View.VISIBLE);
            tvStatus.setText("Ready – tap Find Devices to connect ⚡");
        }
    }
}
