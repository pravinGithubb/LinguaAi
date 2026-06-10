# Raster Illustration Swap-In Guide

The home screen and chat screen currently use four hand-drawn vector
illustrations (`illus_ai_tutor`, `illus_books`, `illus_globe`,
`illus_practice`) as placeholders. This document explains exactly how to
replace them with AI-generated PNGs (or any raster art) without touching
any code.

---

## TL;DR

1. Generate four PNG illustrations matching the prompts/specs below.
2. Export each at five density-bucket sizes (the densitypixels table is
   in section 3).
3. Drop the PNGs into the matching `app/src/main/res/drawable-*/` folders
   using the exact filenames listed in section 2.
4. Rebuild. Done.

Android picks the right-density PNG over the existing `.xml` vector
automatically. **You don't have to delete the vectors** — they remain as
fallback for any density bucket you skip.

---

## 1. The four illustrations and where they appear

| File stem        | Where it shows up                                  | Composition                                                     |
| ---------------- | -------------------------------------------------- | --------------------------------------------------------------- |
| `illus_ai_tutor` | Home → AI Chat hero card · Chat screen avatar tile | Friendly female AI tutor, wand, glasses, cornflower-blue accents |
| `illus_books`    | Home → Vocabulary card                             | Stack of 2–3 colourful books with a bookmark                    |
| `illus_globe`    | Home → Translate card                              | Stylised globe with a small "translate" badge or arrow           |
| `illus_practice` | Home → Practice card                               | Mortarboard cap above a microphone, suggesting speaking practice |

Visual style guidance:

- **Flat illustration** style (not 3D, not photorealistic). Think Notion,
  Storyset, unDraw.
- **Background = transparent**. The illustrations sit on coloured cards;
  any solid background will look wrong.
- **Palette consistent with the app**:
  - Cornflower blue `#5B7DD8` (primary brand)
  - Coral red  `#FF6B6B`
  - Sunny yellow `#FFD568`
  - Mint green `#5DB075`
  - Warm chestnut for hair / wood `#8B4513`
- **No copyrighted characters**. (Don't generate Disney princesses or
  movie likenesses — keep it generic.)

---

## 2. Filenames — must match exactly

Drop your PNGs in with these exact names (no extension prefix changes,
no underscores swapped to hyphens). Android resolves resources by stem,
so `illus_ai_tutor.png` overrides `illus_ai_tutor.xml` automatically.

```
illus_ai_tutor.png
illus_books.png
illus_globe.png
illus_practice.png
```

---

## 3. Density buckets — what size to export at

Android serves the closest density bucket for the user's screen. To
ship a polished result on every device, export each illustration at
**five sizes** and drop each into the matching folder:

| Folder              | Density | Multiplier | `illus_ai_tutor` | Other three |
| ------------------- | ------- | ---------- | ---------------- | ----------- |
| `drawable-mdpi`     | 160 dpi | ×1.0       | 120 × 120 px     | 80 × 80 px  |
| `drawable-hdpi`     | 240 dpi | ×1.5       | 180 × 180 px     | 120 × 120 px|
| `drawable-xhdpi`    | 320 dpi | ×2.0       | 240 × 240 px     | 160 × 160 px|
| `drawable-xxhdpi`   | 480 dpi | ×3.0       | 360 × 360 px     | 240 × 240 px|
| `drawable-xxxhdpi`  | 640 dpi | ×4.0       | 480 × 480 px     | 320 × 320 px|

**If you only have time for one bucket: use `drawable-xxhdpi`** — that's
the modal phone density today. Android scales down for `xxxhdpi` and up
for everything below acceptably. The other buckets become a polish pass
later.

> **The reason `illus_ai_tutor` is bigger than the others:** the home
> hero card displays it at 120 dp on phones (vs 80 dp for the side
> cards). To stay crisp at xxhdpi you need 360 px source.

---

## 4. Recommended AI prompts

Tested prompt skeletons that produce on-style results in DALL-E 3,
Midjourney, and Stable Diffusion. Tweak the wording to taste.

### `illus_ai_tutor.png`

> Flat illustration of a friendly young female AI tutor. Long auburn
> hair tied back. Round glasses. White lab coat over a cornflower-blue
> blouse. Holding a small magic wand with a yellow glowing star. Warm
> smile. **Transparent background. No text. No logos.** Soft shadows,
> Storyset / Notion illustration style. Square crop with the figure
> centred. Cornflower-blue (#5B7DD8) and coral-red (#FF6B6B) accents.

### `illus_books.png`

> Flat illustration of a stack of three colourful textbooks: bottom
> book red, middle book blue, top book yellow with a red bookmark
> tassel hanging out. Slight perspective, gentle drop shadow.
> **Transparent background. No text on the books.** Storyset / Notion
> illustration style. Square crop, books centred.

### `illus_globe.png`

> Flat illustration of a stylised earth globe in pale ocean blue with
> mint-green continent silhouettes. A small yellow circular badge floats
> in the upper-right corner with a black left-to-right arrow inside,
> suggesting translation. **Transparent background. No text on the
> badge.** Storyset / Notion illustration style. Square crop.

### `illus_practice.png`

> Flat illustration of a black mortarboard graduation cap floating
> above a white-and-blue stylised microphone on a small black stand.
> The cap has a yellow tassel. Cornflower-blue (#5B7DD8) accents on
> the microphone grille. **Transparent background.** Storyset / Notion
> illustration style. Square crop.

---

## 5. Workflow checklist

After generating the four PNGs:

- [ ] Verify each has a transparent background (open in a viewer that
      shows the checkerboard pattern, like macOS Preview or GIMP).
- [ ] Resize to the five density-bucket dimensions in section 3.
- [ ] Drop each density's file into the matching `drawable-<density>/`
      folder.
- [ ] Optimise the PNGs (TinyPNG, ImageOptim, or `pngquant`) — vital
      for `xxxhdpi` where uncompressed art can balloon the APK.
- [ ] In Android Studio, run **Build → Clean Project** then re-run.
- [ ] On home screen: confirm illustrations look crisp on the cards.
- [ ] On chat screen: confirm `illus_ai_tutor` appears in the avatar
      tile.

---

## 6. Reverting

If the raster art doesn't land well, you can revert in seconds:
delete the PNG files (or just the folders for the buckets you don't
want) and Android will fall back to the vector `.xml` again. No code
change. The vectors live in `app/src/main/res/drawable/illus_*.xml`
and are never modified by the swap-in.

---

## 7. Future illustrations

To add a new illustration to the project, follow the same recipe:

1. Pick a stem name like `illus_<thing>`.
2. Reference it from a layout: `android:src="@drawable/illus_<thing>"`.
3. Optionally provide an XML vector at `drawable/illus_<thing>.xml`
   as a fallback.
4. Export PNGs at the five densities, drop in matching folders.
