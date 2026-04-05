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
import com.zedox.meshtalk.ai.EmergencyDetector;
import com.zedox.meshtalk.ai.TranslationService;
import com.zedox.meshtalk.database.AppDatabase;
import com.zedox.meshtalk.models.Message;
import com.zedox.meshtalk.utils.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Chat Activity for MeshTalk
 * Displays the chat interface and wires together:
 *   - WiFi Direct messaging (MessageService)
 *   - On-device AI translation (TranslationService / ML Kit)
 *   - Emergency detection (EmergencyDetector)
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

    // WiFi Direct real messaging
    private MessageService messageService;
    private boolean isGroupOwner;
    private String groupOwnerAddress;

    // AI services
    private EmergencyDetector emergencyDetector;
    private TranslationService translationService;
    private String userLanguage;

    // Local persistence
    private AppDatabase db;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Contact info from intent (peer discovery or home screen)
        contactId   = getIntent().getStringExtra("CONTACT_ID");
        contactName = getIntent().getStringExtra("CONTACT_NAME");
        if (contactName == null) contactName = "Unknown Contact";

        // WiFi Direct connection params (set when launched from MainActivity)
        isGroupOwner      = getIntent().getBooleanExtra("isGroupOwner", true);
        groupOwnerAddress = getIntent().getStringExtra("groupOwnerAddress");

        currentUserId = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
                .getString(Constants.KEY_USERNAME, "ybsolanki");
        userLanguage  = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
                .getString(Constants.KEY_LANGUAGE, "en");

        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        initializeServices();
        loadConversationHistory();
    }

    private void initializeViews() {
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        etMessage      = findViewById(R.id.etMessage);
        btnSend        = findViewById(R.id.btnSend);
        btnBack        = findViewById(R.id.btnBack);
        btnVoiceCall   = findViewById(R.id.btnVoiceCall);
        btnVideoCall   = findViewById(R.id.btnVideoCall);
        btnMenu        = findViewById(R.id.btnMenu);
        btnEmoji       = findViewById(R.id.btnEmoji);
        tvContactName  = findViewById(R.id.tvContactName);
        tvContactStatus = findViewById(R.id.tvContactStatus);

        tvContactName.setText(contactName);
        tvContactStatus.setText("🟢 Online");
    }

    private void showDemoModeBanner() {
        tvContactStatus.setText("⚠️ Demo mode – no WiFi Direct connection");
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewMessages.setLayoutManager(layoutManager);
        recyclerViewMessages.setAdapter(messageAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());

        btnVoiceCall.setOnClickListener(v ->
                Toast.makeText(this, "Voice call coming soon!", Toast.LENGTH_SHORT).show());

        btnVideoCall.setOnClickListener(v ->
                Toast.makeText(this, "Video call coming soon!", Toast.LENGTH_SHORT).show());

        btnMenu.setOnClickListener(v ->
                Toast.makeText(this, "Menu options coming soon!", Toast.LENGTH_SHORT).show());

        btnEmoji.setOnClickListener(v ->
                Toast.makeText(this, "Emoji picker coming soon!", Toast.LENGTH_SHORT).show());
    }

    /**
     * Initialize AI services and WiFi Direct MessageService.
     * If connection params are present (real WiFi Direct session), start the socket service.
     */
    private void initializeServices() {
        db = AppDatabase.getInstance(this);
        emergencyDetector = new EmergencyDetector();
        translationService = new TranslationService(this);

        messageService = new MessageService();
        messageService.setMessageListener(new MessageService.MessageListener() {
            @Override
            public void onConnectionEstablished() {
                runOnUiThread(() ->
                        tvContactStatus.setText("🟢 Connected via WiFi Direct"));
            }

            @Override
            public void onMessageReceived(Message message) {
                runOnUiThread(() -> handleIncomingMessage(message));
            }

            @Override
            public void onMessageSent(Message message) {
                runOnUiThread(() -> {
                    message.setDelivered(true);
                    messageAdapter.notifyDataSetChanged();
                });
            }
        });

        // Start real WiFi Direct comms if we came from a peer connection
        if (groupOwnerAddress != null) {
            messageService.start(isGroupOwner, groupOwnerAddress);
        } else {
            // No real connection – tell the user they are in demo mode
            showDemoModeBanner();
        }
    }

    private void sendMessage() {
        String rawText = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(rawText)) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for emergency keywords before sending
        EmergencyDetector.EmergencyResult emergencyResult = emergencyDetector.analyze(rawText);
        if (emergencyResult.isEmergency) {
            showEmergencyAlert(rawText, emergencyResult);
        }

        // Build message
        Message message = new Message(
                currentUserId,
                currentUserId,
                contactId != null ? contactId : "contact_001",
                rawText
        );
        message.setSent(true);

        messageAdapter.addMessage(message);
        etMessage.setText("");
        scrollToBottom();

        // Persist to local DB
        if (contactId != null) {
            dbExecutor.execute(() -> db.messageDao().insertMessage(message));
        }

        // Transmit via WiFi Direct socket (or simulate if not connected)
        if (groupOwnerAddress != null) {
            messageService.sendMessage(message);
        } else {
            simulateDelivery(message);
            simulateReceivedMessage(rawText);
        }
    }

    /**
     * Handle a message received over WiFi Direct.
     * Runs emergency check on the original text, then auto-translates if needed.
     */
    private void handleIncomingMessage(Message message) {
        // Emergency check on the original (source-language) text immediately
        if (emergencyDetector.isEmergency(message.getMessageText())) {
            runOnUiThread(() ->
                    Toast.makeText(this,
                            "🚨 EMERGENCY message received! Prioritizing...",
                            Toast.LENGTH_LONG).show());
        }

        // Detect language and translate if it differs from the user's preference
        translationService.detectLanguage(message.getMessageText(),
                detectedLang -> {
                    if (!detectedLang.equals(userLanguage)) {
                        translationService.translate(
                                message.getMessageText(),
                                detectedLang,
                                userLanguage,
                                new TranslationService.TranslationCallback() {
                                    @Override
                                    public void onSuccess(String translated) {
                                        runOnUiThread(() -> {
                                            message.setMessageText(translated);
                                            // Second emergency check on translated text
                                            if (emergencyDetector.isEmergency(translated)) {
                                                Toast.makeText(ChatActivity.this,
                                                        "🚨 EMERGENCY message (translated)!",
                                                        Toast.LENGTH_LONG).show();
                                            }
                                            addReceivedMessage(message);
                                        });
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        runOnUiThread(() -> addReceivedMessage(message));
                                    }
                                });
                    } else {
                        runOnUiThread(() -> addReceivedMessage(message));
                    }
                });
    }

    private void addReceivedMessage(Message message) {
        messageAdapter.addMessage(message);
        scrollToBottom();
        // Persist to local DB
        if (contactId != null) {
            dbExecutor.execute(() -> db.messageDao().insertMessage(message));
        }
    }

    private void showEmergencyAlert(String text, EmergencyDetector.EmergencyResult result) {
        Toast.makeText(this,
                "🚨 Emergency detected! Sending with priority.",
                Toast.LENGTH_LONG).show();
    }

    /** Simulate delivery tick for demo mode (no real WiFi Direct). */
    private void simulateDelivery(Message message) {
        recyclerViewMessages.postDelayed(() -> {
            message.setDelivered(true);
            messageAdapter.notifyDataSetChanged();
        }, 1000);
    }

    /** Simulate an echo response for demo/testing mode. */
    private void simulateReceivedMessage(String originalMessage) {
        recyclerViewMessages.postDelayed(() -> {
            Message reply = new Message(
                    contactId != null ? contactId : "contact_001",
                    contactName,
                    currentUserId,
                    "Got your message: \"" + originalMessage + "\" 👍"
            );
            reply.setSent(true);
            reply.setDelivered(true);
            messageAdapter.addMessage(reply);
            scrollToBottom();
        }, 2000);
    }

    /**
     * Load conversation history from the local Room database.
     * Falls back to demo messages when no contactId is available (pure demo mode).
     */
    private void loadConversationHistory() {
        if (contactId == null) {
            showDemoModeBanner();
            loadDemoMessages();
            return;
        }
        dbExecutor.execute(() -> {
            List<Message> history = db.messageDao().getConversation(currentUserId, contactId);
            // Mark incoming messages as read now that the user opened the chat
            db.messageDao().markConversationRead(currentUserId, contactId);
            runOnUiThread(() -> {
                if (!history.isEmpty()) {
                    messageAdapter.setMessages(history);
                    scrollToBottom();
                }
                // If empty, do nothing – the user will start fresh
            });
        });
    }

    private void loadDemoMessages() {
        List<Message> demos = new ArrayList<>();

        Message m1 = new Message("contact_001", contactName, currentUserId,
                "Hey! How are you? 😊");
        m1.setSent(true); m1.setDelivered(true);
        demos.add(m1);

        Message m2 = new Message(currentUserId, currentUserId, "contact_001",
                "I'm great! Thanks for asking! 🔥");
        m2.setSent(true); m2.setDelivered(true); m2.setRead(true);
        demos.add(m2);

        Message m3 = new Message("contact_001", contactName, currentUserId,
                "Want to test the mesh network? 📱");
        m3.setSent(true); m3.setDelivered(true);
        demos.add(m3);

        Message m4 = new Message(currentUserId, currentUserId, "contact_001",
                "Absolutely! Let's do it! 💪");
        m4.setSent(true); m4.setDelivered(true);
        demos.add(m4);

        messageAdapter.setMessages(demos);
        recyclerViewMessages.scrollToPosition(demos.size() - 1);
    }

    private void scrollToBottom() {
        recyclerViewMessages.scrollToPosition(messageAdapter.getItemCount() - 1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageService != null) messageService.stop();
        if (translationService != null) translationService.close();
        dbExecutor.shutdown();
    }
}
