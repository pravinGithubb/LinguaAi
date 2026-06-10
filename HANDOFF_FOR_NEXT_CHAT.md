# LinguaAI — Project Handoff Document
# Paste this entire document at the START of your new Claude conversation

---

## 🧑‍💻 About This Project

I am a retired Android developer (Java + Groovy). I am building an AI language learning app
called **LinguaAI** in Android Studio using Java.

The project ZIP attached is named: **LinguaAI_v6_batch2.zip**
Please unzip and use it as the base for all further work.

---

## ✅ What Has Already Been Built (DO NOT REBUILD)

### Core Config
- AGP 9.2.0, Gradle 9.4.1, minSdk 30, targetSdk 37, compileSdk 37
- Java 17, ViewBinding enabled
- Gemini API key already in app/build.gradle: AIzaSyCbB7yyXtaVvWeLiGg6R-zt0LZZzb4KbQ0
- Model: gemini-2.5-flash

### Architecture
- MVVM + Repository pattern
- Room Database v2 (4 tables: messages, translation_history, vocabulary, flashcard_sessions)
- Retrofit 3.0.0 + OkHttp 4.12
- Navigation Component 2.9.0
- LiveData + ViewModel

### Screens / Features DONE
1. **Onboarding** — language selection with animations (OnboardingActivity)
2. **Home Dashboard** — stats cards, quick action cards, animated entrance (HomeFragment)
   - Navigation wired to: Chat, Voice, Translate, Word of Day, Vocabulary, Camera OCR, PDF Reader
3. **AI Chat** — Gemini 2.5 Flash tutor conversation, chat bubbles, typing indicator, TTS (ChatFragment)
4. **Translate** — text translation with Gemini fallback, history, favorites, copy (TranslateFragment)
   - Export History button → HistoryExportFragment
5. **Voice Translate** — mic → STT → Gemini translate → TTS, pulse rings, waveform animation (VoiceFragment)
6. **Progress** — streak, sessions, language pair, daily tips + LIVE vocabulary stats from Room DB (ProgressFragment)
7. **Dictionary** — word lookup via Free Dictionary API + Gemini fallback, bottom sheet UI (DictionaryBottomSheet)
8. **Vocabulary List** — saved words, mastery levels, search, delete → navigates to FlashcardFragment (VocabularyListFragment)
9. **Flashcards** — flip animation, known/unknown, spaced repetition, session summary (FlashcardFragment)
10. **Word of the Day** — Gemini-generated daily word, save to vocabulary (WordOfDayFragment)
11. **Camera OCR Translator** — CameraX live preview + ML Kit text recognition (offline) → Gemini translation (CameraOcrFragment)
12. **PDF Reader** — Android PdfRenderer, page navigation, per-page Gemini translation (PdfReaderFragment)
13. **History Export** — exports translation history as PDF (Android PdfDocument API) or .txt, with share sheet (HistoryExportFragment)

### Services
- TtsManager.java — Android TextToSpeech wrapper
- SpeechRecognitionManager.java — Android SpeechRecognizer wrapper
- AnimUtils.java — fadeInUp, fadeInScale, slideInFromLeft/Right, popIn, pulse, startTypingAnimation

### Data Layer
- AppRepository.java — all data access, Gemini API, translate, dictionary, vocabulary, word of day
  - New: askAi(prompt, callback) — simple Gemini prompt
  - New: translateText(text, fromLang, toLang, callback) — simplified 3-arg translate via Gemini
- RetrofitClient.java — Gemini + Google Translate + Free Dictionary API clients
- AppDatabase.java — Room DB with 4 entities, version 2, fallbackToDestructiveMigration

### Navigation Graph (nav_graph.xml) — ALL actions wired
- homeFragment → chatFragment (action_home_to_chat)
- homeFragment → translateFragment (action_home_to_translate)
- homeFragment → voiceFragment (action_home_to_voice)
- homeFragment → wordOfDayFragment (action_home_to_word_of_day)  ← NEW
- homeFragment → vocabularyListFragment (action_home_to_vocabulary)  ← NEW
- homeFragment → cameraOcrFragment (action_home_to_camera_ocr)  ← NEW
- homeFragment → pdfReaderFragment (action_home_to_pdf_reader)  ← NEW
- translateFragment → historyExportFragment (action_translate_to_history_export)  ← NEW
- vocabularyListFragment → flashcardFragment (action_vocabulary_to_flashcard)  ← FIXED

### UI Design
- Color palette: Deep indigo (#3D52A0) + warm coral (#FF6B6B) + soft cream background
- Gradient header on all screens
- Material3-inspired cards with rounded corners (20dp)
- Staggered entrance animations on all screens
- Bottom navigation with 5 tabs: Home, Chat, Voice (mic icon), Translate, Progress

### Permissions
- INTERNET, RECORD_AUDIO, VIBRATE (original)
- CAMERA  ← NEW (for Camera OCR)
- READ_EXTERNAL_STORAGE / READ_MEDIA_IMAGES  ← NEW (for gallery picker)

### Dependencies (app/build.gradle)
All original deps PLUS:
- com.google.mlkit:text-recognition:16.0.1
- androidx.camera:camera-core:1.4.1
- androidx.camera:camera-camera2:1.4.1
- androidx.camera:camera-lifecycle:1.4.1
- androidx.camera:camera-view:1.4.1

### Package Structure (new additions)
```
com.aitranslator.app/
├── ui/
│   ├── camera/     CameraOcrFragment   ← NEW
│   ├── pdf/        PdfReaderFragment   ← NEW
│   └── export/     HistoryExportFragment  ← NEW
└── res/
    ├── layout/
    │   ├── fragment_camera_ocr.xml   ← NEW
    │   ├── fragment_pdf_reader.xml   ← NEW
    │   └── fragment_history_export.xml  ← NEW
    └── xml/
        └── file_provider_paths.xml   ← NEW (FileProvider)
```

---

## ⚠️ Known Pending Items / Notes

- PdfReader page translation uses Gemini context prompt (PdfRenderer doesn't extract text in Java).
  For better text extraction, Batch 3+ could add iText or pdfbox-android.
- Camera OCR uses ML Kit Latin script. For CJK/Arabic add the respective ML Kit modules.
- FileProvider authority: `com.aitranslator.app.provider` (declared in AndroidManifest.xml)

---

## 🔜 Next Batches To Build

### Batch 3 — Quiz Mode + Daily Lessons + Streak Challenges
- Quiz: multiple choice from vocabulary list, XP points system
- Daily Lessons: 5-min Gemini-generated structured lesson
- Streak Challenges: weekly goals, badge system

### Batch 4 — Grammar Checker + Pronunciation Scorer + AI Story Generator
- Grammar Checker: Gemini analyzes pasted text, color-coded corrections
- Pronunciation Scorer: compare STT output vs correct text, give score 0-100
- AI Story: Gemini generates reading passage at user's level

### Batch 5 — Conversation Mode + Audio Phrasebook + Offline Language Packs
### Batch 6 — YouTube Integration + Podcast Player
### Batch 7 — AdMob Ads + Google Play Billing subscription

---

## 🔑 API Keys In Use

- **Gemini 2.5 Flash**: `AIzaSyCbB7yyXtaVvWeLiGg6R-zt0LZZzb4KbQ0`
- **Free Dictionary API**: `https://api.dictionaryapi.dev` — no key needed
- **ML Kit Text Recognition**: free, on-device, no key needed

---

## 📱 App Name & Branding

- App name: **LinguaAI**
- Package: `com.aitranslator.app`
- Primary color: `#3D52A0` (deep indigo)
- Accent color: `#FF6B6B` (warm coral)
- Background: `#F5F6FA` (soft cream)
- Supported languages: 24
