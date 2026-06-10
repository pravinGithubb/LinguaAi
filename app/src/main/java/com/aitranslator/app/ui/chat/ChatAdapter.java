package com.aitranslator.app.ui.chat;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.ListAdapter;
import com.aitranslator.app.R;
import com.aitranslator.app.data.local.entity.ConversationMessage;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatAdapter extends ListAdapter<ConversationMessage, RecyclerView.ViewHolder> {
    private static final int TYPE_USER = 1, TYPE_AI = 2;
    public interface SpeakListener { void onSpeak(String text); }
    private final String targetLangCode;
    private final SpeakListener speakListener;

    public ChatAdapter(String targetLangCode, SpeakListener speakListener) {
        super(DIFF); this.targetLangCode = targetLangCode; this.speakListener = speakListener;
    }

    private static final DiffUtil.ItemCallback<ConversationMessage> DIFF =
            new DiffUtil.ItemCallback<ConversationMessage>() {
                @Override public boolean areItemsTheSame(@NonNull ConversationMessage a, @NonNull ConversationMessage b) { return a.id == b.id; }
                @Override public boolean areContentsTheSame(@NonNull ConversationMessage a, @NonNull ConversationMessage b) { return a.content.equals(b.content); }
            };

    @Override public int getItemViewType(int pos) { return getItem(pos).sender.equals("user") ? TYPE_USER : TYPE_AI; }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_USER) return new UserVH(inf.inflate(R.layout.item_message_user, parent, false));
        return new AiVH(inf.inflate(R.layout.item_message_ai, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
        ConversationMessage msg = getItem(pos);
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(msg.timestamp));
        if (holder instanceof UserVH) { ((UserVH) holder).tvMessage.setText(msg.content); ((UserVH) holder).tvTime.setText(time); }
        else {
            AiVH h = (AiVH) holder;
            h.tvMessage.setText(msg.content); h.tvTime.setText(time);
            h.btnSpeak.setOnClickListener(v -> { if (speakListener != null) speakListener.onSpeak(msg.content); });
        }
    }

    static class UserVH extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        UserVH(@NonNull View v) { super(v); tvMessage = v.findViewById(R.id.tv_message); tvTime = v.findViewById(R.id.tv_time); }
    }

    static class AiVH extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime; MaterialButton btnSpeak;
        AiVH(@NonNull View v) { super(v); tvMessage = v.findViewById(R.id.tv_message); tvTime = v.findViewById(R.id.tv_time); btnSpeak = v.findViewById(R.id.btn_speak); }
    }
}