package com.zedox.meshtalk.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.zedox.meshtalk.R;
import com.zedox.meshtalk.models.Message;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Message Adapter for Chat RecyclerView
 * Displays sent and received messages
 * Team ZEDOX - Imagine Cup 2025
 */
public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    
    private List<Message> messages;
    private String currentUserId;
    
    public MessageAdapter(String currentUserId) {
        this.messages = new ArrayList<>();
        this.currentUserId = currentUserId;
    }
    
    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if (message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        
        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }
    
    @Override
    public int getItemCount() {
        return messages.size();
    }
    
    public void addMessage(Message message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }
    
    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }
    
    // ViewHolder for sent messages
    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        TextView tvTimestamp;
        TextView tvStatus;
        
        SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessageSent);
            tvTimestamp = itemView.findViewById(R.id.tvTimestampSent);
            tvStatus = itemView.findViewById(R.id.tvMessageStatus);
        }
        
        void bind(Message message) {
            tvMessage.setText(message.getMessageText());
            tvTimestamp.setText(formatTimestamp(message.getTimestamp()));
            
            // Set message status
            if (message.isRead()) {
                tvStatus.setText("✓✓");
                tvStatus.setTextColor(0xFF4CAF50); // Green
            } else if (message.isDelivered()) {
                tvStatus.setText("✓✓");
            } else if (message.isSent()) {
                tvStatus.setText("✓");
            } else {
                tvStatus.setText("⏱");
            }
        }
    }
    
    // ViewHolder for received messages
    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        TextView tvTimestamp;
        TextView tvSenderName;
        
        ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessageReceived);
            tvTimestamp = itemView.findViewById(R.id.tvTimestampReceived);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
        }
        
        void bind(Message message) {
            tvMessage.setText(message.getMessageText());
            tvTimestamp.setText(formatTimestamp(message.getTimestamp()));
            tvSenderName.setText(message.getSenderName());
        }
    }
    
    private static String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}