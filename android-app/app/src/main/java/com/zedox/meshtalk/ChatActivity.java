package com.zedox.meshtalk;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.zedox.meshtalk.adapters.MessageAdapter;
import com.zedox.meshtalk.models.Message;
import java.util.ArrayList;
import java.util.List;

/**
 * Chat Activity for MeshTalk
 * Displays chat interface with message sending/receiving
 * Team ZEDOX - Imagine Cup 2025
 */
public class ChatActivity extends AppCompatActivity {
    
    private RecyclerView recyclerViewMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private ImageButton btnBack;
    private ImageButton btnVoiceCall;
    private ImageButton btnVideoCall;
    private ImageButton btnMenu;
    private ImageButton btnEmoji;
    private TextView tvContactName;
    private TextView tvContactStatus;
    
    private MessageAdapter messageAdapter;
    private String currentUserId;
    private String contactId;
    private String contactName;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        
        // Get contact info from intent
        contactId = getIntent().getStringExtra("CONTACT_ID");
        contactName = getIntent().getStringExtra("CONTACT_NAME");
        
        if (contactName == null) {
            contactName = "Unknown Contact";
        }
        
        // Initialize current user ID (from SharedPreferences)
        currentUserId = getSharedPreferences("MeshTalkPrefs", MODE_PRIVATE)
                .getString("username", "ybsolanki");
        
        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        loadDemoMessages();
    }
    
    private void initializeViews() {
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        btnVoiceCall = findViewById(R.id.btnVoiceCall);
        btnVideoCall = findViewById(R.id.btnVideoCall);
        btnMenu = findViewById(R.id.btnMenu);
        btnEmoji = findViewById(R.id.btnEmoji);
        tvContactName = findViewById(R.id.tvContactName);
        tvContactStatus = findViewById(R.id.tvContactStatus);
        
        // Set contact name
        tvContactName.setText(contactName);
        tvContactStatus.setText("🟢 Online");
    }
    
    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Start from bottom
        recyclerViewMessages.setLayoutManager(layoutManager);
        recyclerViewMessages.setAdapter(messageAdapter);
    }
    
    private void setupClickListeners() {
        // Back button
        btnBack.setOnClickListener(v -> finish());
        
        // Send button
        btnSend.setOnClickListener(v -> sendMessage());
        
        // Voice call button
        btnVoiceCall.setOnClickListener(v -> {
            Toast.makeText(this, "Voice call coming soon!", Toast.LENGTH_SHORT).show();
            // TODO: Implement WiFi Direct voice call
        });
        
        // Video call button
        btnVideoCall.setOnClickListener(v -> {
            Toast.makeText(this, "Video call coming soon!", Toast.LENGTH_SHORT).show();
            // TODO: Implement WiFi Direct video call
        });
        
        // Menu button
        btnMenu.setOnClickListener(v -> {
            Toast.makeText(this, "Menu options coming soon!", Toast.LENGTH_SHORT).show();
            // TODO: Show popup menu with options
        });
        
        // Emoji button
        btnEmoji.setOnClickListener(v -> {
            Toast.makeText(this, "Emoji picker coming soon!", Toast.LENGTH_SHORT).show();
            // TODO: Implement emoji picker
        });
    }
    
    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        
        if (TextUtils.isEmpty(messageText)) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create new message
        Message message = new Message(
            currentUserId,
            currentUserId,
            contactId != null ? contactId : "contact_001",
            messageText
        );
        
        // Mark as sent (in real app, this would happen after WiFi Direct transmission)
        message.setSent(true);
        
        // Add to adapter
        messageAdapter.addMessage(message);
        
        // Clear input
        etMessage.setText("");
        
        // Scroll to bottom
        recyclerViewMessages.scrollToPosition(messageAdapter.getItemCount() - 1);
        
        // TODO: Send via WiFi Direct
        sendViaWiFiDirect(message);
        
        // Simulate received response after 2 seconds
        simulateReceivedMessage(messageText);
    }
    
    private void sendViaWiFiDirect(Message message) {
        // TODO: Implement WiFi Direct message transmission
        // This will be implemented in Phase 2 with WiFi Direct
        
        // For now, mark as delivered after 1 second
        recyclerViewMessages.postDelayed(() -> {
            message.setDelivered(true);
            messageAdapter.notifyDataSetChanged();
        }, 1000);
    }
    
    private void simulateReceivedMessage(String originalMessage) {
        // Simulate a received response (for demo purposes)
        recyclerViewMessages.postDelayed(() -> {
            Message receivedMessage = new Message(
                contactId != null ? contactId : "contact_001",
                contactName,
                currentUserId,
                "Thanks for your message: \"" + originalMessage + "\" 👍"
            );
            
            receivedMessage.setSent(true);
            receivedMessage.setDelivered(true);
            
            messageAdapter.addMessage(receivedMessage);
            recyclerViewMessages.scrollToPosition(messageAdapter.getItemCount() - 1);
        }, 2000);
    }
    
    private void loadDemoMessages() {
        // Load some demo messages for testing
        List<Message> demoMessages = new ArrayList<>();
        
        // Received message
        Message msg1 = new Message(
            "contact_001",
            contactName,
            currentUserId,
            "Hey! How are you? 😊"
        );
        msg1.setSent(true);
        msg1.setDelivered(true);
        demoMessages.add(msg1);
        
        // Sent message
        Message msg2 = new Message(
            currentUserId,
            currentUserId,
            "contact_001",
            "I'm great! Thanks for asking! 🔥"
        );
        msg2.setSent(true);
        msg2.setDelivered(true);
        msg2.setRead(true);
        demoMessages.add(msg2);
        
        // Received message
        Message msg3 = new Message(
            "contact_001",
            contactName,
            currentUserId,
            "Want to test the mesh network? 📱"
        );
        msg3.setSent(true);
        msg3.setDelivered(true);
        demoMessages.add(msg3);
        
        // Sent message
        Message msg4 = new Message(
            currentUserId,
            currentUserId,
            "contact_001",
            "Absolutely! Let's do it! 💪"
        );
        msg4.setSent(true);
        msg4.setDelivered(true);
        demoMessages.add(msg4);
        
        messageAdapter.setMessages(demoMessages);
        recyclerViewMessages.scrollToPosition(demoMessages.size() - 1);
    }
}
