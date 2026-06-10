package com.aitranslator.app.ui.camera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.*;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.*;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.aitranslator.app.data.repository.AppRepository;
import com.aitranslator.app.databinding.FragmentCameraOcrBinding;
import com.aitranslator.app.utils.AnimUtils;
import com.aitranslator.app.utils.PrefsManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraOcrFragment extends Fragment {
    private FragmentCameraOcrBinding binding;
    private AppRepository repository;
    private PrefsManager prefs;
    private TextRecognizer textRecognizer;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    // "Select from Gallery" launcher
    private final ActivityResultLauncher<String> galleryLauncher =
        registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) processGalleryImage(uri);
        });

    // Camera permission launcher
    private final ActivityResultLauncher<String> permissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) startCamera();
            else {
                Toast.makeText(requireContext(), "Camera permission required for live OCR", Toast.LENGTH_LONG).show();
                showGalleryMode();
            }
        });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCameraOcrBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new AppRepository(requireActivity().getApplication());
        prefs = PrefsManager.getInstance(requireContext());
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Animate entrance
        AnimUtils.fadeInUp(binding.cardOcrResult, 200);
        AnimUtils.fadeInUp(binding.btnGallery, 300);

        // Request camera permission → start camera
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }

        binding.btnCapture.setOnClickListener(v -> captureAndRecognize());
        binding.btnGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        binding.btnTranslateOcr.setOnClickListener(v -> translateExtractedText());
        binding.btnCopyOcr.setOnClickListener(v -> copyToClipboard());
        binding.btnClearOcr.setOnClickListener(v -> clearResult());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
            ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build();
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageCapture);
                binding.btnCapture.setEnabled(true);
                binding.tvCameraStatus.setText("Point camera at text, then tap 📷 to scan");
            } catch (Exception e) {
                showGalleryMode();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void showGalleryMode() {
        binding.previewView.setVisibility(View.GONE);
        binding.btnCapture.setVisibility(View.GONE);
        binding.tvCameraStatus.setText("Camera unavailable — use 🖼 Gallery to pick an image");
    }

    private void captureAndRecognize() {
        if (imageCapture == null) return;
        binding.btnCapture.setEnabled(false);
        binding.tvOcrText.setText("Scanning text…");
        binding.progressOcr.setVisibility(View.VISIBLE);

        imageCapture.takePicture(ContextCompat.getMainExecutor(requireContext()),
            new ImageCapture.OnImageCapturedCallback() {
                @Override
                public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                    try {
                        @SuppressWarnings("UnsafeOptInUsageError")
                        InputImage inputImage = InputImage.fromMediaImage(
                            imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());
                        runRecognition(inputImage, imageProxy);
                    } catch (Exception e) {
                        imageProxy.close();
                        showError("Could not process captured image");
                    }
                }
                @Override
                public void onError(@NonNull ImageCaptureException e) {
                    showError("Capture failed: " + e.getMessage());
                    binding.btnCapture.setEnabled(true);
                    binding.progressOcr.setVisibility(View.GONE);
                }
            });
    }

    private void processGalleryImage(Uri uri) {
        binding.progressOcr.setVisibility(View.VISIBLE);
        binding.tvOcrText.setText("Recognising text…");
        try {
            InputImage image = InputImage.fromFilePath(requireContext(), uri);
            runRecognition(image, null);
        } catch (Exception e) {
            showError("Could not load image: " + e.getMessage());
        }
    }

    private void runRecognition(InputImage image, @Nullable ImageProxy proxyToClose) {
        textRecognizer.process(image)
            .addOnSuccessListener(visionText -> {
                if (proxyToClose != null) proxyToClose.close();
                binding.progressOcr.setVisibility(View.GONE);
                String text = visionText.getText().trim();
                if (text.isEmpty()) {
                    binding.tvOcrText.setText("No text detected. Try better lighting or a clearer image.");
                } else {
                    binding.tvOcrText.setText(text);
                    binding.cardOcrResult.setVisibility(View.VISIBLE);
                    binding.btnTranslateOcr.setVisibility(View.VISIBLE);
                    binding.btnCopyOcr.setVisibility(View.VISIBLE);
                    binding.btnClearOcr.setVisibility(View.VISIBLE);
                    AnimUtils.fadeInScale(binding.cardOcrResult, 0);
                }
                binding.btnCapture.setEnabled(true);
            })
            .addOnFailureListener(e -> {
                if (proxyToClose != null) proxyToClose.close();
                showError("Recognition failed: " + e.getMessage());
                binding.btnCapture.setEnabled(true);
            });
    }

    private void translateExtractedText() {
        String text = binding.tvOcrText.getText().toString().trim();
        if (text.isEmpty()) return;
        binding.btnTranslateOcr.setEnabled(false);
        binding.tvTranslationResult.setText("Translating…");
        binding.tvTranslationResult.setVisibility(View.VISIBLE);

        String targetLang = prefs.getTargetLanguage();
        repository.translateText(text, "auto", targetLang, new AppRepository.TranslateCallback() {
            @Override public void onSuccess(String translatedText) {
                requireActivity().runOnUiThread(() -> {
                    binding.tvTranslationResult.setText(translatedText);
                    binding.btnTranslateOcr.setEnabled(true);
                    AnimUtils.fadeInUp(binding.tvTranslationResult, 0);
                });
            }
            @Override public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    binding.tvTranslationResult.setText("Translation error: " + error);
                    binding.btnTranslateOcr.setEnabled(true);
                });
            }
        });
    }

    private void copyToClipboard() {
        String text = binding.tvOcrText.getText().toString();
        if (text.isEmpty()) return;
        android.content.ClipboardManager cm = (android.content.ClipboardManager)
            requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(android.content.ClipData.newPlainText("OCR Text", text));
        Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void clearResult() {
        binding.tvOcrText.setText("Extracted text will appear here…");
        binding.tvTranslationResult.setVisibility(View.GONE);
        binding.btnTranslateOcr.setVisibility(View.GONE);
        binding.btnCopyOcr.setVisibility(View.GONE);
        binding.btnClearOcr.setVisibility(View.GONE);
    }

    private void showError(String msg) {
        requireActivity().runOnUiThread(() -> {
            binding.progressOcr.setVisibility(View.GONE);
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraExecutor != null) cameraExecutor.shutdown();
        if (textRecognizer != null) textRecognizer.close();
        binding = null;
    }
}
