package com.aitranslator.app.ui.export;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import com.aitranslator.app.data.local.entity.TranslationHistory;
import com.aitranslator.app.data.repository.AppRepository;
import com.aitranslator.app.databinding.FragmentHistoryExportBinding;
import com.aitranslator.app.utils.AnimUtils;
import com.aitranslator.app.utils.PrefsManager;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryExportFragment extends Fragment {
    private FragmentHistoryExportBinding binding;
    private AppRepository repository;
    private PrefsManager prefs;
    private List<TranslationHistory> historyItems = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHistoryExportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new AppRepository(requireActivity().getApplication());
        prefs = PrefsManager.getInstance(requireContext());

        AnimUtils.slideInFromLeft(binding.cardExportOptions, 200);
        AnimUtils.slideInFromRight(binding.cardPreview, 300);

        // Observe history to populate preview and enable export
        repository.getAllHistory().observe(getViewLifecycleOwner(), items -> {
            historyItems = items;
            updatePreview();
        });

        binding.btnExportText.setOnClickListener(v -> exportAsText());
        binding.btnExportPdf.setOnClickListener(v -> exportAsPdf());
        binding.btnShareText.setOnClickListener(v -> shareAsText());
    }

    private void updatePreview() {
        int count = historyItems.size();
        binding.tvHistoryCount.setText(count + " translation" + (count != 1 ? "s" : "") + " ready to export");
        binding.btnExportText.setEnabled(count > 0);
        binding.btnExportPdf.setEnabled(count > 0);
        binding.btnShareText.setEnabled(count > 0);

        if (count == 0) {
            binding.tvPreviewContent.setText("No translation history yet.\nGo translate something first! 🌍");
            return;
        }

        // Build preview string (last 5 items)
        StringBuilder sb = new StringBuilder();
        int preview = Math.min(5, count);
        for (int i = 0; i < preview; i++) {
            TranslationHistory h = historyItems.get(i);
            sb.append("• ").append(h.originalText.length() > 40
                    ? h.originalText.substring(0, 40) + "…" : h.originalText)
              .append("\n  → ").append(h.translatedText.length() > 40
                    ? h.translatedText.substring(0, 40) + "…" : h.translatedText)
              .append("\n\n");
        }
        if (count > 5) sb.append("… and ").append(count - 5).append(" more");
        binding.tvPreviewContent.setText(sb.toString().trim());
    }

    private String buildTextContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("LinguaAI — Translation History\n");
        sb.append("Exported: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date())).append("\n");
        sb.append("User: ").append(prefs.getNativeLanguage()).append(" → ").append(prefs.getTargetLanguage()).append("\n");
        sb.append("Total entries: ").append(historyItems.size()).append("\n");
        sb.append("─".repeat(50)).append("\n\n");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        for (TranslationHistory h : historyItems) {
            sb.append("📅 ").append(sdf.format(new Date(h.timestamp))).append("\n");
            sb.append("🔤 Original: ").append(h.originalText).append("\n");
            sb.append("🌍 Translation: ").append(h.translatedText).append("\n");
            if (h.isFavorite) sb.append("⭐ Favorited\n");
            sb.append("\n");
        }
        return sb.toString();
    }

    private void exportAsText() {
        binding.btnExportText.setEnabled(false);
        executor.execute(() -> {
            try {
                String content = buildTextContent();
                File dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                if (dir == null) dir = requireContext().getFilesDir();
                String filename = "LinguaAI_History_"
                    + new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date())
                    + ".txt";
                File file = new File(dir, filename);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(content.getBytes("UTF-8"));
                }
                Uri uri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".provider", file);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Saved: " + filename, Toast.LENGTH_LONG).show();
                    binding.btnExportText.setEnabled(true);
                    openFile(uri, "text/plain");
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    binding.btnExportText.setEnabled(true);
                });
            }
        });
    }

    private void exportAsPdf() {
        binding.btnExportPdf.setEnabled(false);
        executor.execute(() -> {
            try {
                PdfDocument pdf = new PdfDocument();
                Paint titlePaint = new Paint();
                titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                titlePaint.setTextSize(18);
                titlePaint.setColor(0xFF3D52A0); // primary color

                Paint bodyPaint = new Paint();
                bodyPaint.setTextSize(12);
                bodyPaint.setColor(Color.DKGRAY);

                Paint subPaint = new Paint();
                subPaint.setTextSize(11);
                subPaint.setColor(Color.GRAY);

                Paint accentPaint = new Paint();
                accentPaint.setTextSize(11);
                accentPaint.setColor(0xFF3D52A0);

                int pageW = 595, pageH = 842; // A4
                int margin = 48, lineH = 18;
                int y = margin + 40;
                int pageNum = 1;

                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create();
                PdfDocument.Page page = pdf.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                // Title block
                canvas.drawText("LinguaAI — Translation History", margin, y, titlePaint); y += lineH + 6;
                String dateLine = "Exported: " + new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
                canvas.drawText(dateLine, margin, y, subPaint); y += lineH;
                canvas.drawText(historyItems.size() + " entries  |  "
                    + prefs.getNativeLanguage() + " → " + prefs.getTargetLanguage(), margin, y, subPaint); y += lineH + 4;

                // Divider line
                Paint linePaint = new Paint(); linePaint.setColor(0xFF3D52A0); linePaint.setStrokeWidth(1.5f);
                canvas.drawLine(margin, y, pageW - margin, y, linePaint); y += lineH;

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                for (TranslationHistory h : historyItems) {
                    // Check if we need a new page
                    if (y > pageH - margin - 60) {
                        pdf.finishPage(page);
                        pageNum++;
                        pageInfo = new PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create();
                        page = pdf.startPage(pageInfo);
                        canvas = page.getCanvas();
                        y = margin;
                    }
                    canvas.drawText(sdf.format(new Date(h.timestamp))
                        + (h.isFavorite ? "  ⭐" : ""), margin, y, subPaint); y += lineH;
                    // Word-wrap original (basic)
                    canvas.drawText("Original:", margin, y, accentPaint); y += lineH;
                    for (String line : wrapText(h.originalText, 80)) {
                        canvas.drawText(line, margin + 10, y, bodyPaint); y += lineH;
                        if (y > pageH - margin - 30) break;
                    }
                    canvas.drawText("Translation:", margin, y, accentPaint); y += lineH;
                    for (String line : wrapText(h.translatedText, 80)) {
                        canvas.drawText(line, margin + 10, y, bodyPaint); y += lineH;
                        if (y > pageH - margin - 30) break;
                    }
                    y += 8; // spacing between entries
                }
                pdf.finishPage(page);

                File dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                if (dir == null) dir = requireContext().getFilesDir();
                String filename = "LinguaAI_History_"
                    + new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date()) + ".pdf";
                File file = new File(dir, filename);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    pdf.writeTo(fos);
                }
                pdf.close();

                Uri uri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".provider", file);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "PDF saved: " + filename, Toast.LENGTH_LONG).show();
                    binding.btnExportPdf.setEnabled(true);
                    openFile(uri, "application/pdf");
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "PDF export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    binding.btnExportPdf.setEnabled(true);
                });
            }
        });
    }

    private List<String> wrapText(String text, int maxChars) {
        List<String> lines = new ArrayList<>();
        while (text.length() > maxChars) {
            int cut = text.lastIndexOf(' ', maxChars);
            if (cut <= 0) cut = maxChars;
            lines.add(text.substring(0, cut));
            text = text.substring(cut).trim();
        }
        if (!text.isEmpty()) lines.add(text);
        return lines;
    }

    private void shareAsText() {
        String content = buildTextContent();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "LinguaAI Translation History");
        intent.putExtra(Intent.EXTRA_TEXT, content);
        startActivity(Intent.createChooser(intent, "Share Translation History"));
    }

    private void openFile(Uri uri, String mimeType) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (Exception e) {
            // No app to open — that's fine, the file is saved
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
        binding = null;
    }
}
