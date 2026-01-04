package com.zedox.meshtalk;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Main Activity for MeshTalk
 * Home screen with navigation to Chat, Find Devices, and Settings
 * Team ZEDOX - Imagine Cup 2025
 */
public class MainActivity extends AppCompatActivity {
    
    private Button btnStartChat;
    private Button btnFindDevices;
    private Button btnSettings;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        
        // Initialize buttons
        btnStartChat = findViewById(R.id.btnStartChat);
        btnFindDevices = findViewById(R.id.btnFindDevices);
        btnSettings = findViewById(R.id.btnSettings);
        
        // Start Chat button - Opens ChatActivity
        btnStartChat.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            intent.putExtra("CONTACT_ID", "demo_contact");
            intent.putExtra("CONTACT_NAME", "John Doe");
            startActivity(intent);
        });
        
        // Find Devices button
        btnFindDevices.setOnClickListener(v -> {
            Toast.makeText(this, "Device discovery coming soon!", Toast.LENGTH_SHORT).show();
            // TODO: Open FindDevicesActivity
        });
        
        // Settings button - Opens SettingsActivity
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }
}