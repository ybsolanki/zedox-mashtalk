package com.zedox.meshtalk;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.zedox.meshtalk.database.AppDatabase;
import com.zedox.meshtalk.utils.Constants;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private TextView tvUnreadBadge;

    private AppDatabase db;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        btnStartChat   = findViewById(R.id.btnStartChat);
        btnFindDevices = findViewById(R.id.btnFindDevices);
        btnSettings    = findViewById(R.id.btnSettings);
        tvStatus       = findViewById(R.id.tvStatus);
        tvUnreadBadge  = findViewById(R.id.tvUnreadBadge);

        db = AppDatabase.getInstance(this);

        btnStartChat.setOnClickListener(v ->
                // Real chat requires a WiFi Direct connection first.
                // Take the user to the Find Devices screen where they pick a peer;
                // ChatActivity opens automatically once a connection is established.
                startActivity(new Intent(this, MainActivity.class)));

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
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText("Ready – tap Find Devices to connect ⚡");
        }
        refreshUnreadBadge();
    }

    /** Query unread message count from the DB and update the badge on the Start Chat button. */
    private void refreshUnreadBadge() {
        String userId = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
                .getString(Constants.KEY_USERNAME, null);
        if (userId == null || tvUnreadBadge == null) return;

        dbExecutor.execute(() -> {
            int count = db.messageDao().countUnreadMessages(userId);
            runOnUiThread(() -> {
                if (count > 0) {
                    tvUnreadBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                    tvUnreadBadge.setVisibility(View.VISIBLE);
                } else {
                    tvUnreadBadge.setVisibility(View.GONE);
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }
}
