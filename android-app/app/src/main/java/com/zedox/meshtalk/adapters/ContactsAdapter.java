package com.zedox.meshtalk.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.zedox.meshtalk.R;
import java.util.List;

/**
 * Adapter for the Contacts list screen.
 * Each item shows the contact's username with an initial avatar and a chat button.
 * Team ZEDOX - Imagine Cup 2025
 */
public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {

    /** Callback when the user taps the Chat button next to a contact. */
    public interface OnChatClickListener {
        void onChat(String username);
    }

    private final List<String> contacts;
    private final OnChatClickListener listener;

    public ContactsAdapter(List<String> contacts, OnChatClickListener listener) {
        this.contacts = contacts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String username = contacts.get(position);
        holder.tvContactName.setText(username);
        // Show first letter as avatar
        holder.tvContactInitial.setText(
                username.isEmpty() ? "?" : String.valueOf(Character.toUpperCase(username.charAt(0))));
        holder.btnChatContact.setOnClickListener(v -> listener.onChat(username));
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvContactName;
        TextView tvContactInitial;
        ImageButton btnChatContact;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContactName = itemView.findViewById(R.id.tvContactName);
            tvContactInitial = itemView.findViewById(R.id.tvContactInitial);
            btnChatContact = itemView.findViewById(R.id.btnChatContact);
        }
    }
}
