# Piper Voice ZIP Importer, Architecture Info Guide & Advanced Flag Customizer

This plan details the implementation of a single-file `.ZIP` package importer for Piper TTS voice models, a clear architecture info guide popup, and a comprehensive two-tier voice tuning interface that exposes both core sliders and deep Piper runtime flags.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user-confirmed requirements and technical specifications have been finalized:
> - **Confirmed: Single `.ZIP` File Import**: Instead of picking two separate files manually, users select a single `.zip` file containing both the Piper model (`<model>.onnx`) and its matching config (`<model>.onnx.json`). The app unpacks the archive, validates the files, extracts metadata (sample rate, speaker list), and stores them cleanly in internal app storage.
> - **Confirmed: Architecture Info Popup ("i" Circle Icon)**: A circular information icon (`Icons.Default.Info`) is placed prominently next to the voice importer. Tapping it opens an informative dialog explaining:
>   - Supported model architecture: VITS / Piper ONNX
>   - Required file pair: `<name>.onnx` and `<name>.onnx.json` inside the `.zip`
>   - Compatible sample rates: 16,000 Hz, 22,050 Hz, and 24,000 Hz
>   - Phoneme compatibility: espeak-ng phonemizer
> - **Confirmed: Two-Tier Tuning Controls**:
>   - **Tier 1 (Core Controls)**: Speed (`length_scale`), Expressiveness (`noise_scale`), Noise Width (`noise_w`), and Speaker Selection (for multi-speaker models).
>   - **Tier 2 (Expandable Advanced Flags)**: An "Advanced Options" toggle that exposes the actual underlying Piper synthesis flags:
>     - Sentence Pause (`sentence_silence_seconds`): pause length after `.`, `!`, `?`
>     - Sub-sentence Pause (`phoneme_silence` / comma pause): pause length after `,`, `;`, `:`
>     - Output Gain (`volume_scale`): speech amplitude multiplier
>     - CPU Inference Threads: 1 vs 2 threads for battery vs latency optimization
> - **Confirmed: UI & Theme Preservation**: Retains the current chat-first layout, minimal floating input pill, hamburger navigation drawer, and cozy espresso dark / cream light theme tokens in `com.example.ui`. No deleted files are restored.

---

## 1. Overview & Core Concept

- **What It Does**:
  Streamlines custom Piper voice installation by accepting a single `.zip` archive containing both the `.onnx` neural model and `.onnx.json` config. Users can inspect the exact architectural requirements via an on-screen info dialog and fine-tune their active voice through an intuitive dual-level tuning panel.
- **Target Audience / Persona**:
  Users running high-quality, fully offline local voice synthesis on Android who want effortless voice model imports and granular control over TTS prosody, cadence, and pause pacing.
- **Key Value**:
  Eliminates the friction and confusion of multi-file selection while providing transparent model requirements and full customization over Piper's native inference flags.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Viewing Architecture Info**:
   - The user opens the **Custom Voices & System TTS** bottom sheet from the hamburger menu.
   - Next to the **Add Voice** button, an information icon (`Icons.Default.Info`) is visible.
   - Tapping the icon displays the **Piper Model Requirements** dialog outlining file formats, sample rates, and packaging guidance.

2. **Importing a Voice Model (.ZIP)**:
   - The user taps **Add Voice**.
   - An inline import card prompts for a friendly voice name and a **Select .ZIP Package** action.
   - The user selects `en_US-lessac-medium.zip` via the Android file picker.
   - The app extracts the ZIP stream via `ZipInputStream`, validates that both `.onnx` and `.onnx.json` exist, reads sample rate and speaker count from the JSON config, and saves the new profile into the Room database.
   - The imported voice immediately appears in the list and can be activated with a single tap.

3. **Customizing Voice Flags & Advanced Parameters**:
   - The user taps the **Tune** icon (`Icons.Default.Build`) on any voice card.
   - **Primary Sliders** are presented immediately:
     - Speed / Pace (`length_scale` from 0.5x to 2.0x)
     - Expressiveness (`noise_scale` from 0.1 to 1.2)
     - Noise Width / Rhythm (`noise_w` from 0.1 to 1.2)
     - Speaker ID selector (if multi-speaker model)
   - Tapping **Advanced Options (Piper Flags)** reveals:
     - Sentence Pause duration (`0.0s` to `1.0s`, default `0.2s`)
     - Comma / Sub-phrase Pause duration (`0.0s` to `0.5s`, default `0.08s`)
     - Volume Gain (`0.5x` to `1.5x`, default `1.0x`)
     - CPU Inference Thread Count (1 or 2 threads)
   - Parameter adjustments take effect immediately in both in-app streaming playback and Android's `SystemTtsEngine`.

### Visual Identity & Theme Alignment

- **Theme Consistency**: Uses `Theme.AuraVoice` tokens:
  - Dark Mode: `EspressoBlack` (`#0C0907`), `CozyMochaSurface` (`#221A14`), `ToastedCaramel` (`#D6A271`)
  - Light Mode: `AlabasterWhite` (`#FFFDF9`), `CozyCreamSurface` (`#F6F0E4`), `RichEspressoPrimary` (`#5C3A21`)
- **Component Geometry**: Rounded corners (`16.dp` to `20.dp`) for cards and dialogs matching the chat bubbles and search pill.
- **Iconography**: Clean filled Material 3 vector icons (`Icons.Default.Info`, `Icons.Default.Build`, `Icons.Default.CheckCircle`, `Icons.Default.PlayArrow`).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: ZIP Archive Extraction vs. Dual File Pickers**
  - *Chosen Approach*: Accept a single `.zip` file, extracting its contents into `context.filesDir/voices/<voice_id>/` in a background I/O coroutine.
  - *Why*: Piper voices distributed on HuggingFace and GitHub are typically packaged as `.zip` or `.tar.gz` archives containing both `<model>.onnx` and `<model>.onnx.json`. A single ZIP picker prevents mismatched file pairing and simplifies the import flow.
  - *Alternatives Considered*: Requiring separate picks for `.onnx` and `.json` was confusing to users and prone to mismatched configurations.

- **Decision 2: Expandable "Advanced Options" Section**
  - *Chosen Approach*: Keep the primary card clean with 3 core sliders and a speaker picker, and place pause durations, volume scale, and thread count behind an expandable section.
  - *Why*: Prevents visual clutter for everyday users while giving power users access to all actual Piper runtime parameters.

- **Decision 3: Room Database Schema Evolution**
  - *Chosen Approach*: Update `VoiceProfile` entity with default values for new fields (`sentenceSilenceSec`, `phonemeSilenceSec`, `volumeScale`) and increment Room database version with destructive fallback for seamless local upgrades.

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────────┐
│                     VoicesLibraryPane.kt                        │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ Header: "Custom .ONNX Voices"  [ (i) Info ]  [ Add Voice ] │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ Architecture Info Dialog (triggered by (i) icon)          │  │
│  │   • VITS / Piper ONNX requirements                        │  │
│  │   • .onnx + .onnx.json packaging guide                    │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ Inline Zip Importer (ZipInputStream -> filesDir)          │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ OnnxVoiceCard                                             │  │
│  │   ├── Tier 1: Speed, Expressiveness, Noise Width, Speaker │  │
│  │   └── Tier 2: [▼ Advanced Piper Flags]                   │  │
│  │         • Sentence Silence (sec)                          │  │
│  │         • Comma Silence (sec)                             │  │
│  │         • Volume Gain                                     │  │
│  │         • CPU Thread Allocation (1 or 2)                  │  │
│  └───────────────────────────────────────────────────────────┘  │
└────────────────────────────────┬────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                     VoiceViewModel.kt                           │
│  • importVoiceFromZip(context, name, zipUri)                    │
│  • updateVoiceParameters(profile, length, noise, noiseW, ...)   │
│  • updateAdvancedVoiceFlags(profile, sentenceSilence, ...)      │
└────────────────────────────────┬────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│              AppDatabase (Room) & PiperOnnxEngine               │
│  • VoiceProfile (schema updated with advanced pause flags)      │
│  • PiperOnnxEngine applies flags to synthesis & sentence split  │
└─────────────────────────────────────────────────────────────────┘
```

### Component & File Mapping

1. **`com.example.data.VoiceProfile.kt`**:
   - Add fields:
     - `sentenceSilenceSec: Float = 0.2f`
     - `phonemeSilenceSec: Float = 0.08f`
     - `volumeScale: Float = 1.0f`
2. **`com.example.data.AppDatabase.kt`**:
   - Increment database version to 7 (with `fallbackToDestructiveMigration(dropAllTables = true)`).
3. **`com.example.data.VoiceRepository.kt` & `VoiceProfileDao.kt`**:
   - Maintain queries and update methods to persist updated parameters.
4. **`com.example.engine.PiperOnnxEngine.kt`**:
   - Apply `sentenceSilenceSec` and `phonemeSilenceSec` in sentence parsing and PCM playback pacing.
5. **`com.example.ui.VoiceViewModel.kt`**:
   - Implement `importVoiceFromZip(context, name, zipUri)` using `ZipInputStream`:
     - Creates destination folder `context.filesDir/voices/<timestamp>/`.
     - Extracts all entries; finds `.onnx` and `.json`.
     - Parses JSON to read `audio.sample_rate`, `num_speakers`, and defaults.
     - Saves new `VoiceProfile` record to Room.
   - Implement `updateVoiceParameters(...)` supporting both core and advanced flags.
6. **`com.example.ui.VoicesLibraryPane.kt`**:
   - Add circular `(i)` info icon button next to "Add Voice".
   - Implement `PiperArchitectureInfoDialog` explaining the VITS architecture and packaging specs.
   - Update `InlineOnnxImporter` to accept `.zip` files via `ActivityResultContracts.OpenDocument()`.
   - Update `OnnxVoiceCard` with primary sliders and an expandable **Advanced Options** drawer with actual Piper flags.
