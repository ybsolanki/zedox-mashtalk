package com.zedox.meshtalk;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Main Activity for MeshTalk
 * Team ZEDOX - Imagine Cup 2025
 */
public class MainActivity extends AppCompatActivity {
    
    private TextView tvStatus;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize views
        tvStatus = findViewById(R.id.tv_status);
        
        // Set welcome message
        tvStatus.setText("MeshTalk by Team ZEDOX\nImagine Cup 2025\n\nDay 2: Project Setup Complete! ✅");
    }
}
