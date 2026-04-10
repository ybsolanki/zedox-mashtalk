package com.zedox.meshtalk;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.zedox.meshtalk.adapters.MessageAdapter;
import com.zedox.meshtalk.ai.EmergencyDetector;
import com.zedox.meshtalk.ai.TranslationService;
import com.zedox.meshtalk.database.AppDatabase;
import com.zedox.meshtalk.mesh.ConnectionManager;
import com.zedox.meshtalk.models.Device;
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

    // Mesh connection manager (wires WiFiDirectService + MessageRouter)
    private ConnectionManager connectionManager;
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
        btnVoiceCall.setOnClickListener(v -> showCallNotSupportedDialog("Voice"));
        btnVideoCall.setOnClickListener(v -> showCallNotSupportedDialog("Video"));
        btnMenu.setOnClickListener(v -> showChatMenu());
        btnEmoji.setOnClickListener(v -> showEmojiPicker());
    }

    /** Show an informative dialog for call features (not available without VoIP stack). */
    private void showCallNotSupportedDialog(String callType) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(callType + " Call")
                .setMessage(callType + " calls require a VoIP stack (e.g. WebRTC) and a data connection, "
                        + "which is not available in offline mesh mode.\n\n"
                        + "MeshTalk currently supports text messaging over WiFi Direct.")
                .setPositiveButton("OK", null)
                .show();
    }

    /** Show a popup menu with chat management actions. */
    private void showChatMenu() {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(this, btnMenu);
        popup.getMenu().add(0, 1, 0, "Clear Chat");
        popup.getMenu().add(0, 2, 1, "Copy Last Message");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                clearChat();
                return true;
            } else if (item.getItemId() == 2) {
                copyLastMessage();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void clearChat() {
        messageAdapter.setMessages(new ArrayList<>());
        if (contactId != null) {
            dbExecutor.execute(() -> db.messageDao().deleteAllMessages());
        }
        Toast.makeText(this, "Chat cleared", Toast.LENGTH_SHORT).show();
    }

    private void copyLastMessage() {
        int count = messageAdapter.getItemCount();
        if (count == 0) {
            Toast.makeText(this, "No messages to copy", Toast.LENGTH_SHORT).show();
            return;
        }
        // MessageAdapter exposes getItem via getMessages; use reflection-free approach
        String text = messageAdapter.getLastMessageText();
        if (text != null) {
            android.content.ClipboardManager cm = (android.content.ClipboardManager)
                    getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(android.content.ClipData.newPlainText("message", text));
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    /** Show the emoji picker bottom sheet. */
    private void showEmojiPicker() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_emoji, null);
        dialog.setContentView(sheetView);

        int[] emojiIds = {
            R.id.emoji1, R.id.emoji2, R.id.emoji3, R.id.emoji4, R.id.emoji5,
            R.id.emoji6, R.id.emoji7, R.id.emoji8, R.id.emoji9, R.id.emoji10,
            R.id.emoji11, R.id.emoji12, R.id.emoji13, R.id.emoji14, R.id.emoji15,
            R.id.emoji16, R.id.emoji17, R.id.emoji18, R.id.emoji19, R.id.emoji20,
            R.id.emoji21
        };

        for (int id : emojiIds) {
            Button btn = sheetView.findViewById(id);
            if (btn != null) {
                btn.setOnClickListener(v -> {
                    String emoji = btn.getText().toString();
                    int cursor = etMessage.getSelectionEnd();
                    etMessage.getText().insert(Math.max(cursor, 0), emoji);
                    dialog.dismiss();
                });
            }
        }

        dialog.show();
    }

    /**
     * Initialize AI services and the mesh ConnectionManager.
     * If connection params are present (real WiFi Direct session), start the socket layer.
     */
    private void initializeServices() {
        db = AppDatabase.getInstance(this);
        emergencyDetector = new EmergencyDetector();
        translationService = new TranslationService(this);

        connectionManager = new ConnectionManager(this);
        connectionManager.setListener(new ConnectionManager.ConnectionManagerListener() {
            @Override
            public void onPeersUpdated(List<Device> peers) {
                // Peer discovery is handled in MainActivity; nothing to do here.
            }

            @Override
            public void onConnectionStatusChanged(boolean isConnected) {
                runOnUiThread(() -> tvContactStatus.setText(
                        isConnected ? "🟢 Connected via WiFi Direct" : "🔴 Disconnected"));
            }

            @Override
            public void onRawMessageReceived(String rawMessage) {
                try {
                    Message msg = new com.google.gson.Gson().fromJson(rawMessage, Message.class);
                    runOnUiThread(() -> handleIncomingMessage(msg));
                } catch (Exception e) {
                    android.util.Log.e("ChatActivity", "Failed to parse message: " + rawMessage, e);
                }
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(ChatActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show());
            }
        });

        if (groupOwnerAddress != null) {
            // Register peer in routing table so MessageRouter can forward messages.
            String peerId = contactId != null ? contactId : "peer";
            connectionManager.setConnectedPeer(peerId, contactName, groupOwnerAddress);
            connectionManager.startSocket(isGroupOwner, groupOwnerAddress);
        } else {
            // No real connection – tell the user they are in demo mode.
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

        // Transmit via ConnectionManager (routes through MessageRouter + WiFiDirectService)
        if (groupOwnerAddress != null) {
            connectionManager.sendMessage(message, currentUserId);
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
        if (connectionManager != null) connectionManager.stop();
        if (translationService != null) translationService.close();
        dbExecutor.shutdown();
    }
}
