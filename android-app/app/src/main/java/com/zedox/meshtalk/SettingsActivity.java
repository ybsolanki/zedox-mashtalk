package com.zedox.meshtalk;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * Settings Activity for MeshTalk
 * Profile management, QR code generation, and app info
 * Team ZEDOX - Imagine Cup 2025
 */
public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "MeshTalkPrefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_STATUS = "status";

    private EditText etUsername;
    private EditText etStatus;
    private Button btnSaveProfile;
    private ImageView ivQRCode;
    private Button btnScanQR;
    private Button btnViewContacts;
    private TextView tvAppVersion;
    private TextView tvTeamInfo;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Initialize views
        etUsername = findViewById(R.id.etUsername);
        etStatus = findViewById(R.id.etStatus);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        ivQRCode = findViewById(R.id.ivQRCode);
        btnScanQR = findViewById(R.id.btnScanQR);
        btnViewContacts = findViewById(R.id.btnViewContacts);
        tvAppVersion = findViewById(R.id.tvAppVersion);
        tvTeamInfo = findViewById(R.id.tvTeamInfo);

        // Load saved profile
        loadProfile();

        // Generate QR code with current username
        generateQRCode();

        // Save profile button
        btnSaveProfile.setOnClickListener(v -> {
            saveProfile();
            generateQRCode();
            Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show();
        });

        // Scan QR button
        btnScanQR.setOnClickListener(v -> {
            Toast.makeText(this, "QR Scanner coming soon!", Toast.LENGTH_SHORT).show();
            // TODO: Open QR Scanner Activity
        });

        // View contacts button
        btnViewContacts.setOnClickListener(v -> {
            Toast.makeText(this, "Contacts list coming soon!", Toast.LENGTH_SHORT).show();
            // TODO: Open Contacts Activity
        });

        // Set app version
        tvAppVersion.setText("MeshTalk v1.0 Beta");
        tvTeamInfo.setText("Team ZEDOX | Imagine Cup 2025");
    }

    /**
     * Load profile from SharedPreferences
     */
    private void loadProfile() {
        String username = sharedPreferences.getString(KEY_USERNAME, "User" + System.currentTimeMillis() % 10000);
        String status = sharedPreferences.getString(KEY_STATUS, "Available");
        
        etUsername.setText(username);
        etStatus.setText(status);
    }

    /**
     * Save profile to SharedPreferences
     */
    private void saveProfile() {
        String username = etUsername.getText().toString().trim();
        String status = etStatus.getText().toString().trim();

        if (username.isEmpty()) {
            username = "User" + System.currentTimeMillis() % 10000;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_STATUS, status);
        editor.apply();
    }

    /**
     * Generate QR code for current user
     */
    private void generateQRCode() {
        String username = etUsername.getText().toString().trim();
        if (username.isEmpty()) {
            username = sharedPreferences.getString(KEY_USERNAME, "Anonymous");
        }

        // QR code contains username for contact sharing
        String qrData = "MESHTALK:" + username;

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, 400, 400);
            
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            
            ivQRCode.setImageBitmap(bitmap);
            
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Get current username
     */
    public String getUsername() {
        return sharedPreferences.getString(KEY_USERNAME, "Anonymous");
    }
}
