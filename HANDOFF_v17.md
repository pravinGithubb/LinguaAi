# LinguaAI — Development Handoff Document (v17)

**Last updated:** 2026-05-06
**Latest build:** `LinguaAI_v17_raster_swap_ready.zip`
**Use this document to continue development in a new chat with Claude.**

> **Important:** This supersedes the original `HANDOFF_FOR_NEXT_CHAT.pdf`
> from v10. The original is still accurate for sections 1-3 and 11 but
> sections 4-10 are out of date. Read this document first.

---

## 1. Project Overview

- **App name:** LinguaAI — AI-powered language learning Android app
- **Developer:** Retired Android developer (Java + Groovy background)
- **Package:** `com.aitranslator.app`
- **Build environment:** Android Studio
- **Language:** Java (NOT Kotlin)

### Build configuration

- AGP 9.2.0 · Gradle 9.4.1
- minSdk 30 · targetSdk 37 · compileSdk 37
- Java 17, ViewBinding enabled

### Credentials

- **Gemini API key:** in `app/build.gradle` as `GEMINI_API_KEY`
- **Model:** `gemini-2.5-flash`
- **Google Translate API:** placeholder (Gemini is the fallback)

### Design system (current — Lernix-inspired refresh as of v14)

- **Primary brand:** cornflower blue `#5B7DD8` (was deep indigo in v10)
- **Accent:** coral `#FF6B6B`
- **Background:** soft mint→pale-blue gradient (`bg_app_gradient.xml`)
  applied as `windowBackground` so every screen inherits
- **Surface (cards):** white `#FFFFFF`
- **Text primary:** near-black navy `#0E1632`
- **Cards:** flat (0dp elevation) with 1dp soft border (`@color/divider`)
- **All entrance animations** via `AnimUtils` (slideInFromLeft/Right,
  fadeInUp, fadeInScale, pulse)

---

## 2. Architecture

**Pattern:** MVVM + Repository.

### Key components
- `AppRepository` — single facade for Room + Gemini API + dictionary
- Room DB v4 with **7 entities** (handoff v10 said 6 — `MediaBookmark`
  was added at some point), `fallbackToDestructiveMigration`
- Retrofit 3.0.0 + OkHttp 4.12 for Gemini API
- Navigation Component 2.9.0
- LiveData + ViewModel
- **Bottom nav: 3 tabs** (changed from 5 in v13)
  — Home, History, Settings

### Working directory
`/home/claude/AITranslatorApp/` (full unzipped project)

### Key utility classes
- `PrefsManager` — language prefs, streak, sessions, XP, weekly goals
- `LanguageUtils` — flag emoji, language list (Hindi included)
- `AnimUtils` — animation helpers
- `GeminiResponseUtils` — robust JSON + section extraction (added v10)
- `TtsManager(context, callback)` — text-to-speech
- `SpeechRecognitionManager` — STT wrapper
- **`PronunciationLocalScorer`** (NEW v11) — Levenshtein scorer
- **`SpeakingQuizSentences`** (NEW v11/v12) — bundled sentence corpus
  in English + Hindi, 50 per level
- **`XpBarChartView`** (NEW v16) — custom-drawn weekly XP bar chart
- **`FeatureBottomSheet`** (NEW v13) — secondary feature catalog

---

## 3. Room Database (Version 4)

Entities:
1. `ConversationMessage` — chat messages
2. `TranslationHistory` — translation records
3. `VocabularyWord` — saved vocabulary, includes Word of the Day
4. `FlashcardSession` — flashcard practice sessions
5. `QuizResult` — quiz history (Batch 3) — **also reused for Speaking
   Quiz session aggregates as of v11**
6. `Phrase` — phrasebook entries (Batch 5)
7. `MediaBookmark` — YouTube/Podcast bookmarks (Batch 6, undocumented in
   the v10 handoff)

**No new entity has been added since v10.** The Speaking Quiz uses the
existing `QuizResult` table — zero migration risk.

### DAOs
`MessageDao`, `TranslationHistoryDao`, `VocabularyDao`,
`FlashcardSessionDao`, `QuizResultDao`, `PhraseDao`, `MediaBookmarkDao`

---

## 4. Feature Inventory (cumulative — v1 through v17)

### Batches 1-6 (pre-v10, unchanged)
Home, Chat, Voice, Translate, Progress, Vocabulary, Flashcards,
Word of Day, Camera OCR, PDF Reader, History Export, Quiz, Daily Lesson,
Streak Challenge, Grammar Checker, Pronunciation Scorer, AI Story,
Conversation Mode, Phrasebook, Language Packs, YouTube, Podcasts.

### v11 — Speaking Quiz (NEW package `ui.speakingquiz`)
- Lernix-style level picker (Beginner / Intermediate / Advanced)
- Quiz screen with 60s timer, progress bar, sentence card with speed
  chips, score circle, mic button, Next button
- 50 English sentences per level bundled (offline)
- Levenshtein-based local scoring (no Gemini calls)
- Reuses `QuizResult` for session aggregates + `PrefsManager.addXp`
- TTS reads sentences aloud, STT scores user's pronunciation
- Speed chips currently visual-only — `TtsManager` doesn't expose
  setSpeechRate. One-line addition when needed.

### v12 — Hindi support
- 50 Hindi sentences per level added to `SpeakingQuizSentences`
- `forLevel(level, langCode)` overload selects the right corpus
- "hi" → Hindi corpus, anything else → English fallback
- New **Change Language** screen (`ChangeLanguageFragment`) accessible
  from the Settings tab, lets user update native + target language
  any time after onboarding

### v13 — Lernix redesign (major)
- Bottom nav reduced from 5 tabs to 3: Home / History / Settings
- New Lernix-style home screen: hero AI Chat card + Vocabulary +
  Translate row + Practice card + "More features" pill
- New **History tab** with stat cards + recent activity feed
- New **Settings tab** as a hub of preference cards
- `FeatureBottomSheet` shows the 17 secondary features as a tap-up
  grid, replacing the old long Home grid
- All existing fragments still reachable — nothing was deleted

### v14 — Palette refresh & chat redesign
- Cornflower-blue primary, mint→pale-blue gradient backdrop
- Chat screen rewritten with Lernix-style avatar tile (no more gradient
  toolbar), ✕ close button, sparse top action row, greeting bubble that
  hides on first user message
- Vector AI tutor portrait at `drawable/ai_tutor_avatar.xml`
- All hard-coded indigos replaced with palette tokens

### v15 — Audit pass
- `Card.Elevated` style flattened from 8dp → 0dp + 1dp border
  (cascaded across 9 screens)
- 22 layout backgrounds switched from `@color/background` to transparent
  so the gradient shows through
- 3 high-elevation card stragglers fixed individually

### v16 — Charts + responsiveness
- `XpBarChartView` custom view added to History tab — weekly XP
  visualisation, no chart-library dependency
- `values/dimens.xml` + `values-sw600dp/dimens.xml` system added so
  tablets get wider side margins (120dp) instead of phone (20dp)
- Chose proper responsive overrides over SDP/SSP libraries

### v17 — Raster swap-in prep
- Five density-bucket folders created (`drawable-mdpi` through
  `drawable-xxxhdpi`) ready for AI-generated PNGs
- `RASTER_ASSETS.md` at project root with per-density dimensions,
  filenames, and AI prompt skeletons for the 4 illustrations
- Vectors stay as fallbacks — Android picks density-specific PNG
  automatically over the XML

---

## 5. Dependencies

Unchanged since v10. **No new dependencies added across v11-v17.**
(See original handoff PDF section 5 for the full list.)

Notable choices:
- Speaking Quiz is fully offline — no extra Gemini cost
- History chart is custom-drawn — no MPAndroidChart dependency
- Responsive layout via `dimens.xml` + `sw600dp` qualifier — no SDP/SSP
- Bottom sheet uses Material's `BottomSheetDialogFragment` (already in
  `material:1.12.0`)

---

## 6. AndroidManifest Permissions

Unchanged from v10:
`INTERNET`, `RECORD_AUDIO`, `VIBRATE`, `CAMERA`,
`READ_EXTERNAL_STORAGE` (≤sdk32), `READ_MEDIA_IMAGES`.

FileProvider authority: `com.aitranslator.app.provider`.

---

## 7. Navigation Graph

29 destinations. New since v10:
- `speakingQuizLevelFragment` + `speakingQuizFragment` (v11)
- `changeLanguageFragment` (v12)
- `historyFragment` + `settingsFragment` (v13)

All Home → feature actions still exist as nav graph entries even though
the home screen no longer references most of them — they're called by
the bottom sheet via `Navigation.findNavController(...).navigate(id)`.

Bottom nav menu IDs in `res/menu/bottom_nav_menu.xml` must match nav
graph fragment IDs exactly. They currently align:
`homeFragment`, `historyFragment`, `settingsFragment`.

---

## 8. Home Screen (current — v13+)

**Lernix-style, four primary actions only:**
1. **AI Chat hero card** (full-width, blue gradient, illustration)
   → `chatFragment`
2. **Vocabulary** (yellow card, half-width)
   → `vocabularyListFragment`
3. **Translate** (green card, half-width)
   → `translateFragment`
4. **Practice** (full-width, "New" badge, blue→cyan gradient)
   → `speakingQuizLevelFragment`
5. **More features pill** → opens `FeatureBottomSheet`

The bottom sheet contains the other 17 features.

---

## 9. Output Files (in `/mnt/user-data/outputs/`)

Build progression:
- `LinguaAI_v6_batch2.zip` — through Batch 2
- `LinguaAI_v7_batch3_4.zip` — adds Batches 3 + 4
- `LinguaAI_v8_batch5.zip` — adds Batch 5
- `LinguaAI_v9_batch6.zip` — adds Batch 6
- `LinguaAI_v10_batch4_fixes.zip` — original handoff baseline
- `LinguaAI_v11_speaking_quiz.zip` — Speaking Quiz feature
- `LinguaAI_v12_change_language_hindi.zip` — Hindi corpus + Change Language
- `LinguaAI_v13_lernix_redesign.zip` — bottom nav, home, history, settings
- `LinguaAI_v14_lernix_palette_chat.zip` — palette refresh + chat redesign
- `LinguaAI_v15_audit_pass.zip` — card flattening + background sweep
- `LinguaAI_v16_responsive_polish.zip` — XP chart wired + sw600dp dimens
- **`LinguaAI_v17_raster_swap_ready.zip`** — current state

---

## 10. Pending Roadmap

### Immediate (when you have time)
- **Generate the 4 raster illustrations** following `RASTER_ASSETS.md`
  and drop into `drawable-xxhdpi/` (minimum) or all 5 density buckets
  (full polish). Vectors will fall back automatically until then.

### Batch 7 — Monetization (NOT STARTED)
- AdMob banner + interstitial ads
- Google Play Billing — subscription tiers (free / premium)
- Premium feature gating (e.g. unlimited quizzes, all phrasebook
  categories)

### Possible future work
- Push notifications for daily reminders (FCM)
- Widget for Word of the Day on home screen
- Apple Watch / Wear OS companion
- Cloud backup of vocabulary + progress
- Social features (compete with friends on streaks)
- Image translation (live AR overlay)
- More languages in Speaking Quiz corpus (Spanish, French, etc. —
  schema is already language-aware, just paste another corpus block)
- Wire `TtsManager.setSpeechRate` so the Speaking Quiz speed chips
  actually slow down playback

---

## 11. Known Architecture Decisions

(Unchanged from v10 except where noted)

- **No Kotlin** — Java throughout
- **No Hilt/Dagger** — repository instantiated directly in ViewModels:
  `new AppRepository(requireActivity().getApplication())`
  (NOT `AppRepository.getInstance(...)` — that doesn't exist)
- **No Compose** — XML + ViewBinding
- **Gemini-only AI** — no OpenAI/Anthropic SDK
- **Destructive Room migrations** — fine for development; bump version
  when adding entities
- **Bracket-walking JSON extraction** in `GeminiResponseUtils`
- **Per-fragment STT/TTS managers** — instantiated in `onViewCreated`,
  destroyed in `onDestroyView`
- **NEW v13:** Bottom sheet uses `Navigation.findNavController(activity,
  R.id.nav_host_fragment)` — NOT a fragment-scoped controller, because
  `BottomSheetDialogFragment` lives in its own dialog window
- **NEW v15:** Card style is the single source of truth — edit
  `Card.Elevated` in `themes.xml` to restyle all cards across 9+ screens
- **NEW v16:** Responsive overrides go in `values-sw600dp/dimens.xml`,
  not via SDP/SSP libraries — fewer dependencies, less churn

---

## 12. How to Resume in a New Chat

Paste this entire document plus the latest zip
(`LinguaAI_v17_raster_swap_ready.zip`) and an issue/feature you want
to work on. Claude can:
1. Read this doc to understand the codebase structure
2. Read the zip to see actual code
3. Build the next batch or fix issues

**Recommended opening prompt:**
> "Here's the LinguaAI handoff doc and latest zip. I want to [build
> Batch 7 / fix X / add Y feature]. Please use Opus-level care: audit
> existing code first, identify root causes for any bugs, then write
> robust, well-commented Java that fits the existing architecture."

---

## 13. Files Most Likely to Need Changes

When adding a new screen:
1. New `XxxFragment.java` + `XxxViewModel.java` in
   `app/src/main/java/com/aitranslator/app/ui/xxx/`
2. New `fragment_xxx.xml` in `app/src/main/res/layout/`
3. Add destination + action in
   `app/src/main/res/navigation/nav_graph.xml`
4. **NEW (v13):** Either add a card to `fragment_home.xml` (only for
   primary features) OR add a tile to `FeatureBottomSheet.FEATURES`
5. If new entity: add to `data/local/entity/`, add DAO, bump version
   in `AppDatabase`, wire in `AppRepository`
6. If new dependency: add to `app/build.gradle`, document why
7. **NEW (v15):** Use `style="@style/Card.Elevated"` for cards instead
   of duplicating cardCornerRadius/cardElevation/cardBackgroundColor
8. **NEW (v16):** Use `@dimen/screen_horizontal_padding` for outer
   padding so tablets get wider margins automatically
9. When making AI calls, always use `GeminiResponseUtils` for response
   parsing. Never use raw `JsonParser` directly.

---

## 14. Audit Status (as of v17)

A clean-slate audit was performed on 2026-05-06 covering:
- ✅ All 29 nav destinations have matching Java classes on disk
- ✅ All `R.layout.*` references resolve to actual layout files
- ✅ All `@drawable/...` references resolve (across all 6 drawable buckets)
- ✅ All `@color/...` references resolve (`values/colors.xml` + `color/`)
- ✅ All `@style/...` references resolve
- ✅ All `binding.xxx` calls in fragments resolve to layout `@+id`s
- ✅ All `R.id.*` references in Java exist in nav graph or menu
- ✅ Bottom nav menu IDs align with nav graph fragment IDs
- ✅ `AndroidManifest.xml` activities exist on disk

**No broken references. Project should compile cleanly.**

### Project size as of v17
- 86 Java files
- 39 XML layouts
- 68 vector drawables
- 5 density-bucket folders (empty, ready for raster swap-in)
- 29 nav destinations · 3 bottom-nav tabs
