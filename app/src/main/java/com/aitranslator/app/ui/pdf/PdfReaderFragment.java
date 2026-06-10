package com.aitranslator.app.ui.pdf;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.aitranslator.app.data.repository.AppRepository;
import com.aitranslator.app.databinding.FragmentPdfReaderBinding;
import com.aitranslator.app.utils.AnimUtils;
import com.aitranslator.app.utils.PrefsManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PDF Reader screen.
 *
 * Two parallel pipelines on the same PDF:
 *  1. {@link PdfRenderer}  — built-in Android renderer, used to display the
 *                            current page as a bitmap so the user can see it.
 *  2. {@link PDDocument} via PDFBox-Android — extracts the actual text content
 *                            of each page so we can ship it to Gemini for a
 *                            real translation rather than a generic prompt.
 *
 * Text extraction is cached per page so flipping back to a previously-translated
 * page is instant and doesn't re-hit the AI.
 */
public class PdfReaderFragment extends Fragment {

    private FragmentPdfReaderBinding binding;
    private AppRepository repository;
    private PrefsManager prefs;

    // Render pipeline (bitmap display)
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor fileDescriptor;

    // Text-extraction pipeline (PDFBox)
    private PDDocument pdfBoxDoc;
    /** Cache of page index → extracted plain text, populated on demand. */
    private final Map<Integer, String> pageTextCache = new HashMap<>();
    /** Cache of page index → AI translation, so flipping pages doesn't re-translate. */
    private final Map<Integer, String> translationCache = new HashMap<>();

    private int currentPage = 0;
    private int totalPages = 0;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String[]> pdfPicker =
        registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) openPdf(uri);
        });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPdfReaderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new AppRepository(requireActivity().getApplication());
        prefs = PrefsManager.getInstance(requireContext());

        // Initialise PDFBox resources (font loader, etc.) on first use.
        // Safe to call repeatedly; it short-circuits internally.
        PDFBoxResourceLoader.init(requireContext().getApplicationContext());

        AnimUtils.fadeInUp(binding.btnOpenPdf, 200);
        AnimUtils.fadeInUp(binding.cardPdfInfo, 300);

        binding.btnOpenPdf.setOnClickListener(v ->
            pdfPicker.launch(new String[]{"application/pdf"}));

        binding.btnPrevPage.setOnClickListener(v -> navigatePage(-1));
        binding.btnNextPage.setOnClickListener(v -> navigatePage(1));
        binding.btnTranslatePage.setOnClickListener(v -> confirmAndTranslatePage());
    }

    // ────────────────────────────────────────────────────────────────────────
    //  PDF OPEN / CLOSE
    // ────────────────────────────────────────────────────────────────────────

    private void openPdf(Uri uri) {
        closePdf();
        binding.tvPdfStatus.setText("Loading PDF…");
        binding.progressPdf.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            try {
                // Open file descriptor for the renderer
                fileDescriptor = requireContext().getContentResolver()
                        .openFileDescriptor(uri, "r");
                if (fileDescriptor == null) {
                    postError("Could not open file");
                    return;
                }
                pdfRenderer = new PdfRenderer(fileDescriptor);
                totalPages = pdfRenderer.getPageCount();

                // Open a *separate* InputStream for PDFBox — we can't share the FD
                try (InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
                    if (in != null) {
                        pdfBoxDoc = PDDocument.load(in);
                    }
                }

                currentPage = 0;
                pageTextCache.clear();
                translationCache.clear();

                requireActivity().runOnUiThread(() -> {
                    binding.pdfContainer.setVisibility(View.VISIBLE);
                    binding.btnOpenPdf.setText("📂  Open Different PDF");
                    binding.tvPdfStatus.setText("PDF loaded — " + totalPages + " pages");
                    binding.tvTranslationPdf.setVisibility(View.GONE);
                });
                renderPage(currentPage);
            } catch (Exception e) {
                postError("Failed to open PDF: " + e.getMessage());
            }
        });
    }

    private void closePdf() {
        try {
            if (pdfRenderer != null) { pdfRenderer.close(); pdfRenderer = null; }
            if (fileDescriptor != null) { fileDescriptor.close(); fileDescriptor = null; }
            if (pdfBoxDoc != null) { pdfBoxDoc.close(); pdfBoxDoc = null; }
        } catch (IOException ignored) { /* best effort */ }
        pageTextCache.clear();
        translationCache.clear();
    }

    private void postError(String msg) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            binding.progressPdf.setVisibility(View.GONE);
            binding.tvPdfStatus.setText("");
            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
        });
    }

    // ────────────────────────────────────────────────────────────────────────
    //  PAGE RENDER (visual)
    // ────────────────────────────────────────────────────────────────────────

    private void renderPage(int pageIndex) {
        if (pdfRenderer == null || pageIndex < 0 || pageIndex >= totalPages) return;
        binding.progressPdf.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            try {
                PdfRenderer.Page page = pdfRenderer.openPage(pageIndex);
                int width = page.getWidth() * 2;   // 2× scale for readability
                int height = page.getHeight() * 2;
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bitmap.eraseColor(android.graphics.Color.WHITE);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();

                requireActivity().runOnUiThread(() -> {
                    binding.ivPdfPage.setImageBitmap(bitmap);
                    binding.progressPdf.setVisibility(View.GONE);
                    binding.tvPageIndicator.setText((pageIndex + 1) + " / " + totalPages);
                    binding.btnPrevPage.setEnabled(pageIndex > 0);
                    binding.btnNextPage.setEnabled(pageIndex < totalPages - 1);
                    binding.btnTranslatePage.setVisibility(View.VISIBLE);

                    // If we already translated this page once, show it instantly
                    String cached = translationCache.get(pageIndex);
                    if (cached != null) {
                        binding.tvTranslationPdf.setText(cached);
                        binding.tvTranslationPdf.setVisibility(View.VISIBLE);
                    } else {
                        binding.tvTranslationPdf.setVisibility(View.GONE);
                    }
                });
            } catch (Exception e) {
                postError("Could not render page: " + e.getMessage());
            }
        });
    }

    private void navigatePage(int delta) {
        int next = currentPage + delta;
        if (next >= 0 && next < totalPages) {
            currentPage = next;
            renderPage(currentPage);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  TEXT EXTRACTION + TRANSLATION
    // ────────────────────────────────────────────────────────────────────────

    private void confirmAndTranslatePage() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Translate Page " + (currentPage + 1))
            .setMessage("Extract the text from this page and translate it to "
                + prefs.getTargetLanguage() + "?\n\n"
                + "Note: Image-based PDFs (scans) won't have extractable text.")
            .setPositiveButton("Translate", (d, w) -> doTranslateCurrentPage())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void doTranslateCurrentPage() {
        if (pdfBoxDoc == null) {
            Toast.makeText(requireContext(),
                "PDF text layer is not available", Toast.LENGTH_SHORT).show();
            return;
        }
        binding.btnTranslatePage.setEnabled(false);
        binding.tvTranslationPdf.setText("📑 Extracting text from page…");
        binding.tvTranslationPdf.setVisibility(View.VISIBLE);
        AnimUtils.fadeInUp(binding.tvTranslationPdf, 0);

        final int pageIdx = currentPage;
        executor.execute(() -> {
            String text = pageTextCache.get(pageIdx);
            if (text == null) {
                text = extractPageText(pageIdx);
                if (text != null && !text.isEmpty()) pageTextCache.put(pageIdx, text);
            }
            final String pageText = text;

            if (pageText == null || pageText.trim().isEmpty()) {
                requireActivity().runOnUiThread(() -> {
                    binding.tvTranslationPdf.setText(
                        "ℹ️ No extractable text on this page.\n\n"
                      + "This usually means the PDF is a scanned image. "
                      + "Use the Camera Translator instead — it can read text from images.");
                    binding.btnTranslatePage.setEnabled(true);
                });
                return;
            }

            // Truncate very long pages so we stay well within Gemini's input limits
            final String toTranslate = pageText.length() > 8000
                ? pageText.substring(0, 8000) + "\n\n[…page truncated for translation]"
                : pageText;

            requireActivity().runOnUiThread(() ->
                binding.tvTranslationPdf.setText("🤖 Translating " + toTranslate.length()
                    + " characters with AI…"));

            String prompt =
                "Translate the following text to " + prefs.getTargetLanguage() + ".\n" +
                "Preserve paragraph breaks. Do not add commentary, headers, or quotation marks " +
                "around the result — return only the translation.\n\n" +
                "TEXT TO TRANSLATE:\n" + toTranslate;

            repository.askAi(prompt, new AppRepository.AiResponseCallback() {
                @Override public void onSuccess(String response) {
                    if (!isAdded()) return;
                    String translation = response == null ? "" : response.trim();
                    translationCache.put(pageIdx, translation);
                    requireActivity().runOnUiThread(() -> {
                        if (currentPage == pageIdx) {
                            binding.tvTranslationPdf.setText(translation);
                            AnimUtils.fadeInUp(binding.tvTranslationPdf, 0);
                        }
                        binding.btnTranslatePage.setEnabled(true);
                    });
                }
                @Override public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        binding.tvTranslationPdf.setText("Translation failed: " + error);
                        binding.btnTranslatePage.setEnabled(true);
                    });
                }
            });
        });
    }

    /** Extract plain text from a single PDF page using PDFBox.
     *  Page is 0-indexed externally; PDFBox is 1-indexed. */
    @Nullable
    private String extractPageText(int pageIndex) {
        try {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setStartPage(pageIndex + 1);
            stripper.setEndPage(pageIndex + 1);
            return stripper.getText(pdfBoxDoc);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        closePdf();
        executor.shutdown();
        binding = null;
    }
}
