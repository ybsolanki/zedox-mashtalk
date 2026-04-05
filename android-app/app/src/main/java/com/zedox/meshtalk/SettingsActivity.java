package com.zedox.meshtalk;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;
import com.zedox.meshtalk.utils.Constants;

/**
 * Settings Activity for MeshTalk
 * Profile management, QR code generation, and app info
 * Team ZEDOX - Imagine Cup 2025
 */
public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = Constants.PREFS_NAME;
    private static final String KEY_USERNAME = Constants.KEY_USERNAME;
    private static final String KEY_STATUS = Constants.KEY_STATUS;
    private static final String KEY_LANGUAGE = Constants.KEY_LANGUAGE;

    /** Display names shown in the Spinner, parallel to LANGUAGE_CODES */
    private static final String[] LANGUAGE_NAMES = {
        "English (en)", "Spanish (es)", "Hindi (hi)", "Chinese (zh)",
        "Arabic (ar)", "French (fr)", "Portuguese (pt)", "Russian (ru)"
    };
    private static final String[] LANGUAGE_CODES = {
        "en", "es", "hi", "zh", "ar", "fr", "pt", "ru"
    };

    private EditText etUsername;
    private EditText etStatus;
    private Spinner spinnerLanguage;
    private Button btnSaveProfile;
    private ImageView ivQRCode;
    private Button btnScanQR;
    private Button btnViewContacts;
    private TextView tvAppVersion;
    private TextView tvTeamInfo;

    private SharedPreferences sharedPreferences;

    /** Launcher for the ZXing QR scanner (Activity Result API) */
    private final ActivityResultLauncher<ScanOptions> qrScanLauncher =
            registerForActivityResult(new ScanContract(), this::handleScanResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Initialize views
        etUsername = findViewById(R.id.etUsername);
        etStatus = findViewById(R.id.etStatus);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        ivQRCode = findViewById(R.id.ivQRCode);
        btnScanQR = findViewById(R.id.btnScanQR);
        btnViewContacts = findViewById(R.id.btnViewContacts);
        tvAppVersion = findViewById(R.id.tvAppVersion);
        tvTeamInfo = findViewById(R.id.tvTeamInfo);

        // Populate language spinner
        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, LANGUAGE_NAMES);
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(langAdapter);

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
            ScanOptions options = new ScanOptions();
            options.setPrompt("Scan a MeshTalk QR code");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            options.setBarcodeImageEnabled(false);
            qrScanLauncher.launch(options);
        });

        // View contacts button
        btnViewContacts.setOnClickListener(v ->
                startActivity(new Intent(this, ContactsActivity.class)));

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
        String language = sharedPreferences.getString(KEY_LANGUAGE, "en");

        etUsername.setText(username);
        etStatus.setText(status);

        // Select the matching language in the Spinner
        for (int i = 0; i < LANGUAGE_CODES.length; i++) {
            if (LANGUAGE_CODES[i].equals(language)) {
                spinnerLanguage.setSelection(i);
                break;
            }
        }
    }

    /**
     * Save profile to SharedPreferences
     */
    private void saveProfile() {
        String username = etUsername.getText().toString().trim();
        String status = etStatus.getText().toString().trim();
        int langPos = spinnerLanguage.getSelectedItemPosition();
        String language = (langPos >= 0 && langPos < LANGUAGE_CODES.length)
                ? LANGUAGE_CODES[langPos] : "en";

        if (username.isEmpty()) {
            username = "User" + System.currentTimeMillis() % 10000;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_STATUS, status);
        editor.putString(KEY_LANGUAGE, language);
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
        String qrData = Constants.QR_PREFIX + username;

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

    /**
     * Called by the QR scanner launcher with the scan result.
     */
    private void handleScanResult(ScanIntentResult result) {
        if (result.getContents() == null) {
            Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
            return;
        }
        String scanned = result.getContents();
        if (scanned.startsWith(Constants.QR_PREFIX)) {
            String scannedUsername = scanned.substring(Constants.QR_PREFIX.length()).trim();
            if (!scannedUsername.isEmpty()) {
                saveContact(scannedUsername);
                Toast.makeText(this, "Contact saved: " + scannedUsername, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "QR code has no username", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Not a MeshTalk QR code", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Persist a scanned contact username to SharedPreferences (comma-separated list).
     * Uses exact-match check against split tokens to avoid substring false-positives.
     */
    private void saveContact(String username) {
        String existing = sharedPreferences.getString(Constants.KEY_CONTACTS, "");
        boolean alreadySaved = false;
        if (!existing.isEmpty()) {
            for (String saved : existing.split(",")) {
                if (saved.trim().equals(username)) {
                    alreadySaved = true;
                    break;
                }
            }
        }
        if (!alreadySaved) {
            String updated = existing.isEmpty() ? username : existing + "," + username;
            sharedPreferences.edit().putString(Constants.KEY_CONTACTS, updated).apply();
        }
    }
}
