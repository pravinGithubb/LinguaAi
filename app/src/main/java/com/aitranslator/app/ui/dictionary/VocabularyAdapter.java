package com.aitranslator.app.ui.dictionary;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.ListAdapter;
import com.aitranslator.app.R;
import com.aitranslator.app.data.local.entity.VocabularyWord;

public class VocabularyAdapter extends ListAdapter<VocabularyWord, VocabularyAdapter.VH> {
    public interface OnWordClick { void onClick(VocabularyWord word); }
    public interface OnDeleteClick { void onClick(VocabularyWord word); }
    private final OnWordClick wordClick;
    private final OnDeleteClick deleteClick;

    public VocabularyAdapter(OnWordClick wordClick, OnDeleteClick deleteClick) {
        super(DIFF); this.wordClick = wordClick; this.deleteClick = deleteClick;
    }

    private static final DiffUtil.ItemCallback<VocabularyWord> DIFF = new DiffUtil.ItemCallback<VocabularyWord>() {
        @Override public boolean areItemsTheSame(@NonNull VocabularyWord a, @NonNull VocabularyWord b) { return a.id == b.id; }
        @Override public boolean areContentsTheSame(@NonNull VocabularyWord a, @NonNull VocabularyWord b) { return a.masteryLevel == b.masteryLevel && a.word.equals(b.word); }
    };

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vocabulary_word, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        VocabularyWord w = getItem(pos);
        h.tvWord.setText(w.word);
        h.tvDefinition.setText(w.definition);
        h.tvPhonetic.setText(w.phonetic);
        h.tvPos.setText(w.partOfSpeech);
        // Mastery badge
        String[] labels = {"New", "Learning", "Familiar", "Mastered"};
        int[] colors = {0xFFFF6B6B, 0xFFFFB347, 0xFF7091E6, 0xFF2ECC71};
        h.tvMastery.setText(labels[Math.min(w.masteryLevel, 3)]);
        h.tvMastery.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colors[Math.min(w.masteryLevel, 3)]));
        h.itemView.setOnClickListener(v -> wordClick.onClick(w));
        h.btnDelete.setOnClickListener(v -> deleteClick.onClick(w));
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvWord, tvDefinition, tvPhonetic, tvPos, tvMastery;
        ImageButton btnDelete;
        VH(@NonNull View v) {
            super(v);
            tvWord = v.findViewById(R.id.tv_word); tvDefinition = v.findViewById(R.id.tv_definition);
            tvPhonetic = v.findViewById(R.id.tv_phonetic); tvPos = v.findViewById(R.id.tv_pos);
            tvMastery = v.findViewById(R.id.tv_mastery); btnDelete = v.findViewById(R.id.btn_delete);
        }
    }
}