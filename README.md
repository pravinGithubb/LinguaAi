# 🌍 LinguaAI — AI Language Tutor

A Java/Android language-learning app powered by Google Gemini.
Cornflower-blue Lernix-inspired UI · 3-tab bottom nav · Java 17 ·
MaterialComponents 1.12.0 · Room v4.

Built on AGP 9.2.0 · Gradle 9.4.1 · minSdk 30 · targetSdk 37.

---

## Setup (2 minutes)

### 1. Get a free Gemini API key
1. Visit https://aistudio.google.com
2. Sign in with Google → "Get API key" → "Create API key"
3. Copy the key (starts with `AIzaSy…`)

### 2. Paste it into `app/build.gradle`
Find:
```
buildConfigField "String", "GEMINI_API_KEY", '"YOUR_GEMINI_API_KEY_HERE"'
```
Replace with your key:
```
buildConfigField "String", "GEMINI_API_KEY", '"AIzaSy…"'
```

Open the folder in Android Studio → Sync Gradle → ▶ Run.

---

## What's in here

### Bottom nav (3 tabs)
- **Home** — Lernix-style hero card (AI Chat) + Vocabulary + Translate
  + Practice (Speaking Quiz) + a "More features" pill that opens a
  bottom sheet with the other 17 features.
- **History** — quick stats (streak, sessions, XP) + recent activity
  feed combining quiz results and translations + button to the full
  Export History screen.
- **Settings** — Change Language, Streak Challenge, Offline Language
  Packs, Export History, About.

### Full feature catalogue (reachable from Home > More features)
Voice translate · Word of the Day · My Vocabulary · Camera OCR · PDF
Reader · AI Quiz · Daily Lesson · Streak Challenge · Grammar Checker ·
Pronunciation Scorer · AI Story · Conversation Mode · Phrasebook ·
Language Packs · YouTube Lessons · Podcasts · History & Export.

### Speaking Quiz (offline, scored locally)
- Three difficulty levels: Beginner / Intermediate / Advanced
- 50 sentences per level, bundled offline
- English + Hindi corpus included; schema is language-aware so adding
  Spanish / Tamil / etc. is just dropping another corpus block
- Levenshtein-based pronunciation scoring on-device — zero AI cost
- TTS reads each sentence at user-selected speed; STT scores user's
  pronunciation against the expected text

### Change Language anywhere
The native + target language are normally set during onboarding.
Settings → Change Language lets the user update either at any time
without restarting the app.

---

## Architecture (one-liner)

MVVM + Repository · Java only (no Kotlin) · Navigation Component 2.9 ·
Retrofit 3 + OkHttp 4 · Room v4 with 7 entities · ViewBinding · Material
**Components** 1.12 (NOT Material3 — see "gotchas" below).

---

## Where things live

```
app/src/main/
├── java/com/aitranslator/app/
│   ├── data/
│   │   ├── local/        Room DB, DAOs, entities
│   │   ├── remote/       Retrofit interfaces + Gemini DTOs
│   │   └── repository/   AppRepository — single facade
│   ├── service/          TtsManager, SpeechRecognitionManager
│   ├── ui/
│   │   ├── home/         MainActivity, HomeFragment, FeatureBottomSheet
│   │   ├── history/      HistoryFragment, XpBarChartView
│   │   ├── settings/     SettingsFragment, ChangeLanguageFragment
│   │   ├── speakingquiz/ SpeakingQuizLevelFragment, SpeakingQuizFragment,
│   │   │                 SpeakingQuizSentences, PronunciationLocalScorer
│   │   ├── chat/         Chat with avatar tile
│   │   └── …             one package per other feature
│   └── utils/            PrefsManager, LanguageUtils, AnimUtils, ...
└── res/
    ├── layout/           38 fragment + activity layouts
    ├── drawable/         vector illustrations + chip / card backgrounds
    ├── drawable-mdpi/    raster swap-in folders (see RASTER_ASSETS.md)
    ├── drawable-hdpi/    …
    ├── drawable-xhdpi/   …
    ├── drawable-xxhdpi/  …
    ├── drawable-xxxhdpi/ …
    ├── values/           colors.xml, themes.xml, dimens.xml, …
    ├── values-sw600dp/   tablet dimens override (wider side margins)
    └── navigation/       nav_graph.xml — 29 destinations
```

---

## Design system

- **Primary brand:** cornflower blue `#5B7DD8` (Lernix vibe — softer
  than the original deep indigo `#3D52A0`)
- **Background:** soft mint→pale-blue gradient
  (`drawable/bg_app_gradient.xml`) wired into `windowBackground`, so
  every screen inherits it
- **Cards:** flat 0dp elevation + 1dp soft border (`@color/divider`).
  Single source of truth: edit `Card.Elevated` in `themes.xml`.
- **Lernix card colours:** sunny yellow `#FFD568`, mint green `#5DB075`,
  blue gradients for hero cards
- **Text primary:** `#0E1632` (near-black navy)

---

## Gotchas worth knowing

### 1. Material**Components**, not Material3
The theme parent is `Theme.MaterialComponents.DayNight.NoActionBar`.
**Never** use `@style/Widget.Material3.*` in this project — it crashes
at inflate time. The active-pill indicator on the bottom nav is an M3
exclusive; we don't use it. The default Material highlight on tab
selection is what you'll see.

### 2. AppRepository is NOT a singleton
Instantiate directly:
```java
new AppRepository(requireActivity().getApplication());
```
There is no `getInstance()` method.

### 3. Bottom nav uses a custom listener (not `setupWithNavController`)
The standard `NavigationUI` helper has two bugs in this 3-tab setup:
the highlight stops tracking after the first tab switch, and getting
"stuck" on a secondary destination (e.g. Export History) makes the
parent tab tap a no-op. `MainActivity#wireBottomNav` and
`wireHighlightSync` fix both. **Don't** revert to the helper.

### 4. The bottom sheet uses the activity NavController, not its own
`FeatureBottomSheet` calls
`Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)`
because `BottomSheetDialogFragment` lives in its own dialog window.

### 5. `0dp` is correct in 220 places
Used for `weight=1` columns and `cardElevation=0`. Don't try to migrate
those to anything else — `0dp` is universally correct.

### 6. Per-fragment STT / TTS managers
Created in `onViewCreated`, destroyed in `onDestroyView`. Don't share
across fragments.

---

## Bundled docs

- **`HANDOFF_v17.md`** — full development history, batch-by-batch.
  Read this first if you're picking up the codebase fresh.
- **`RASTER_ASSETS.md`** — raster illustration swap-in guide.
  Drop AI-generated PNGs into `drawable-xxhdpi/` etc. with the right
  filenames and they override the existing vectors automatically.

---

## Roadmap

### Pending
- **Batch 7** — AdMob ads + Google Play Billing for premium tiers
  (NOT STARTED). Premium would unlock unlimited AI features + remove
  ads.
- **More languages** in the Speaking Quiz corpus (Spanish, French,
  Tamil — schema already supports it, just paste another block in
  `SpeakingQuizSentences.java`).
- **Wire `TtsManager.setSpeechRate`** so the Speaking Quiz speed chips
  (Normal / Slow / Very Slow) actually change the playback rate.
  Currently visual-only.

### Done in this session
- Speaking Quiz feature (offline, English + Hindi)
- Lernix-style 3-tab redesign (bottom nav, home, history, settings)
- Cornflower-blue palette refresh + gradient backdrop
- Chat screen redesign with avatar tile + sparse top row
- Custom XP weekly bar chart in History
- Tablet-responsive dimens (`values-sw600dp/`)
- Raster swap-in scaffolding (5 density folders + guide)
- Bottom-nav back-stack bug fixes (highlight + Export-stuck)
- Back buttons on Speaking Quiz Level / Speaking Quiz / Change Language
- M3 → MaterialComponents namespace fixes (TextInputLayout +
  BottomNavigationView.ActiveIndicator)

---

## License

Private project — not currently licensed for redistribution.
