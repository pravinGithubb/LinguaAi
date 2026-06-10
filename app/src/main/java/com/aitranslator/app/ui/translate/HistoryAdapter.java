package com.aitranslator.app.ui.translate;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.ListAdapter;
import com.aitranslator.app.R;
import com.aitranslator.app.data.local.entity.TranslationHistory;

public class HistoryAdapter extends ListAdapter<TranslationHistory, HistoryAdapter.HistoryVH> {
    public interface OnFavoriteClick { void onClick(TranslationHistory item); }
    public interface OnDeleteClick { void onClick(TranslationHistory item); }
    private final OnFavoriteClick favClick;
    private final OnDeleteClick delClick;

    public HistoryAdapter(OnFavoriteClick favClick, OnDeleteClick delClick) {
        super(DIFF); this.favClick = favClick; this.delClick = delClick;
    }

    private static final DiffUtil.ItemCallback<TranslationHistory> DIFF =
            new DiffUtil.ItemCallback<TranslationHistory>() {
                @Override public boolean areItemsTheSame(@NonNull TranslationHistory a, @NonNull TranslationHistory b) { return a.id == b.id; }
                @Override public boolean areContentsTheSame(@NonNull TranslationHistory a, @NonNull TranslationHistory b) { return a.isFavorite == b.isFavorite && a.translatedText.equals(b.translatedText); }
            };

    @NonNull @Override
    public HistoryVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new HistoryVH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull HistoryVH h, int pos) {
        TranslationHistory item = getItem(pos);
        h.tvOriginal.setText(item.originalText);
        h.tvTranslated.setText(item.translatedText);
        h.tvLangs.setText(item.fromLanguage + "  →  " + item.toLanguage);
        h.btnFavorite.setImageResource(item.isFavorite ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
        h.btnFavorite.setOnClickListener(v -> favClick.onClick(item));
        h.btnDelete.setOnClickListener(v -> delClick.onClick(item));
    }

    static class HistoryVH extends RecyclerView.ViewHolder {
        TextView tvOriginal, tvTranslated, tvLangs;
        ImageButton btnFavorite, btnDelete;
        HistoryVH(@NonNull View v) {
            super(v);
            tvOriginal = v.findViewById(R.id.tv_original); tvTranslated = v.findViewById(R.id.tv_translated);
            tvLangs = v.findViewById(R.id.tv_langs); btnFavorite = v.findViewById(R.id.btn_favorite);
            btnDelete = v.findViewById(R.id.btn_delete);
        }
    }
}