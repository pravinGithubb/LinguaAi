package com.aitranslator.app.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aitranslator.app.R;

/**
 * Dropdown adapter showing a flag emoji + language name.
 *
 * Drop-in replacement for {@code new ArrayAdapter<>(ctx,
 * simple_dropdown_item_1line, languages)} — same constructor signature,
 * same getItem semantics, just prefixes each row with the country flag.
 *
 * The selected text fed back into the {@link android.widget.AutoCompleteTextView}
 * (after picking a row) is still just the language name, so existing
 * code reading {@code spinner.getText().toString()} keeps working
 * unchanged.
 */
public class FlagLanguageAdapter extends ArrayAdapter<String> {

    private final String[] languages;
    private final LayoutInflater inflater;

    public FlagLanguageAdapter(@NonNull Context context, @NonNull String[] languages) {
        super(context, R.layout.item_language_dropdown, languages);
        this.languages = languages;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return bindRow(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView,
                                 @NonNull ViewGroup parent) {
        return bindRow(position, convertView, parent);
    }

    private View bindRow(int position, View convertView, ViewGroup parent) {
        View row = convertView != null
                ? convertView
                : inflater.inflate(R.layout.item_language_dropdown, parent, false);
        String name = languages[position];
        String code = LanguageUtils.getCode(name);
        ((TextView) row.findViewById(R.id.tv_flag)).setText(LanguageUtils.getFlagEmoji(code));
        ((TextView) row.findViewById(R.id.tv_name)).setText(name);
        return row;
    }
}
