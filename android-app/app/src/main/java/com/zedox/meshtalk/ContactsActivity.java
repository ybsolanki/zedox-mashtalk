package com.zedox.meshtalk;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.zedox.meshtalk.adapters.ContactsAdapter;
import com.zedox.meshtalk.utils.Constants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Contacts Activity for MeshTalk
 * Displays contacts saved by QR code scans.
 * Team ZEDOX - Imagine Cup 2025
 */
public class ContactsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewContacts;
    private TextView tvNoContacts;
    private ImageButton btnBackContacts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        recyclerViewContacts = findViewById(R.id.recyclerViewContacts);
        tvNoContacts = findViewById(R.id.tvNoContacts);
        btnBackContacts = findViewById(R.id.btnBackContacts);

        btnBackContacts.setOnClickListener(v -> finish());

        loadContacts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh in case a QR scan added a contact while this screen was in the back stack
        loadContacts();
    }

    private void loadContacts() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        String raw = prefs.getString(Constants.KEY_CONTACTS, "");

        List<String> contacts = new ArrayList<>();
        if (!raw.isEmpty()) {
            for (String c : raw.split(",")) {
                String trimmed = c.trim();
                if (!trimmed.isEmpty()) {
                    contacts.add(trimmed);
                }
            }
        }

        if (contacts.isEmpty()) {
            tvNoContacts.setVisibility(View.VISIBLE);
            recyclerViewContacts.setVisibility(View.GONE);
        } else {
            tvNoContacts.setVisibility(View.GONE);
            recyclerViewContacts.setVisibility(View.VISIBLE);

            ContactsAdapter adapter = new ContactsAdapter(contacts, username -> {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("CONTACT_NAME", username);
                intent.putExtra("CONTACT_ID", username);
                startActivity(intent);
            });
            recyclerViewContacts.setLayoutManager(new LinearLayoutManager(this));
            recyclerViewContacts.setAdapter(adapter);
        }
    }
}
