# Real-Time System Hardware Monitor & 100% Pop Transition to Library

This plan details the integration of a real-time hardware telemetry HUD on the voice training screen and a celebratory 100% completion animation. During F5-TTS sample generation and Piper ONNX fine-tuning, the monitor provides real-time visibility into CPU utilization percentage, used RAM out of total system RAM, overall completion percentage, live animated gauges, and an immediate training abort/cancel button. When training reaches 100%, an animated resizing and fading "pop" spring transition plays, automatically carrying the user straight into the **Voices Library** tab with their newly trained voice ready.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product behaviors and requested refinements have been incorporated:
> - **Confirmed: 100% Completion Pop Animation & Auto-Navigation**: When overall training reaches 100%, the completion card plays a spring-loaded scale and fade "pop" animation (`scaleIn(spring(dampingRatio = Bouncy))` + `fadeIn()` followed by `scaleOut()` + `fadeOut()`), seamlessly switching the main tab to `VoiceViewModel.StudioTab.LIBRARY` and closing the training modal.
> - **Confirmed: Live Display During Training**: The hardware monitor is actively displayed directly on `TrainingScreen` during all active training stages (`GENERATING_300_SAMPLES`, `RECLAIMING_MEMORY`, and `PIPER_FINE_TUNING`).
> - **Confirmed: Live Hardware Gauges & Metrics**:
>   1. **Live CPU Percentage**: Displays real-time compute load with a color-coded strain gauge (Cyan, Amber, Red).
>   2. **Used RAM Out of Total System RAM**: Formatted as `X.XX GB / Y.YY GB (Z%)` with peak consumption tracking via Android `ActivityManager.MemoryInfo`.
>   3. **Overall Completion Percentage**: Unified 0%–100% gauge tracking sample generation (0%–65%), memory reclamation (65%–75%), and Piper epoch training (75%–100%).
> - **Confirmed: Immediate Cancel & Abort Action**: A prominent Cancel button in the telemetry card that immediately halts running coroutines, unloads heavy model memory, deletes temporary batch files, and reverts state to `AUDIO_READY`.
> - **Confirmed: Strictly No Emojis**: All metrics, badges, gauges, and animations use Material Design vector icons (`Icons.Default.Memory`, `Icons.Default.Speed`, `Icons.Default.Timer`, `Icons.Default.CheckCircle`, `Icons.Default.Cancel`) with clean monospace figures and zero emojis.

---

## 1. Overview & Core Concept

- **What It Does**:
  Provides an on-screen hardware telemetry dashboard during voice model fine-tuning. Users can observe real-time strain on their device processor (CPU %) and physical memory (Used RAM / Total RAM), track overall progress toward completion, cancel at any time, and enjoy a smooth, bouncy "pop" transition straight to the Voices Library when training finishes.
- **Target Audience / Persona**:
  Users and creators running on-device generative voice synthesis who want clear hardware visibility and an effortless transition into testing their newly created voice model.
- **Key Value**:
  Combines industrial hardware transparency with delight: users see hardware strain during generation, and upon reaching 100%, are smoothly propelled straight into their voice library.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Initiating Training & HUD Activation**:
   - The user taps **Commit & Train Model** in the Voice Cloner.
   - `TrainingScreen` opens as a full-screen workstation dialog.
   - The **Real-Time Hardware Monitor HUD** appears at the top, immediately sampling hardware metrics.

2. **Monitoring Live Gauges During Generation**:
   - **CPU Gauge**: Displays fluctuating load (e.g. `82.4%`) with a horizontal gauge bar that shifts from Cyan (`#00E5FF`) to Amber (`#FFB300`) under high compute strain.
   - **RAM Gauge**: Displays physical RAM utilization (e.g. `3.28 GB / 6.00 GB (54%)`) alongside JVM app heap usage.
   - **Overall Progress**: Displays total progress percentage (e.g. `45% Complete`) along with estimated time remaining and batch index.

3. **Reaching 100% & Pop Transition to Library**:
   - As Piper fine-tuning hits the final epoch, overall progress hits `100%`.
   - The dialog triggers the **Completion Pop Sequence**:
     - A celebratory badge expands with a spring overshoot bounce (`scaleIn(spring(dampingRatio = 0.55f, stiffness = 400f))`).
     - A brief 900ms visual celebration displays "Training Complete! Model Ready".
     - The modal smoothly scales down and fades out, automatically selecting `StudioTab.LIBRARY` in the main container.
     - The user arrives in the Voices Library with their new model active and selected.

4. **Aborting Training**:
   - The user taps **Abort Training** on the HUD card.
   - A confirmation dialog prompts the user ("Discard current progress and reclaim memory?").
   - Confirming cancels the worker job, invokes `f5TtsEngine.terminateAndReclaimMemory()`, deletes intermediate audio cache, and safely returns to `AUDIO_READY`.

### Visual Identity & Theme

- **Aesthetic Direction**: High-tech audio synthesizer diagnostics console with dark slate container cards (`#121622`), glowing telemetry lines, and monospace numerical readings.
- **Animation Design**:
  - `scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))`
  - `fadeIn(animationSpec = tween(250))`
  - Auto-dismiss transition with `scaleOut(tween(300))` + `fadeOut(tween(250))`
- **Iconography (Strictly No Emojis)**:
  - `Icons.Default.Speed`: CPU processing load
  - `Icons.Default.Memory`: System physical RAM & heap
  - `Icons.Default.Timer`: Pipeline completion percentage
  - `Icons.Default.CheckCircle`: 100% success badge
  - `Icons.Default.Close` / `Icons.Default.Cancel`: Abort action
  - `Icons.Default.FolderShared`: Library destination badge

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: 100% Pop Animation & Auto-Navigation**
  - *Chosen Approach*: When progress reaches 100%, animate a spring scale/fade "pop" celebration for under a second, then immediately dismiss the dialog and navigate to `StudioTab.LIBRARY`.
  - *Why*: Eliminates tedious manual clicks to exit the training dialog and takes the user immediately to the destination where their voice is ready to be played, edited, or exported.
  - *Alternatives Considered*: Requiring the user to tap "Done" manually was considered, but disrupts the flow of creation.

- **Decision 2: System RAM vs. Process Heap**
  - *Chosen Approach*: Display both Total System RAM (`ActivityManager.MemoryInfo.availMem` & `totalMem`) and App Heap memory.
  - *Why*: Users asked specifically for "used ram out of total system ram" to know if their entire phone is under memory pressure or close to an Out-Of-Memory termination.

- **Decision 3: Unified Overall Percentage vs. Stage-only Percentage**
  - *Chosen Approach*: Compute a weighted composite progress percentage:
    - Stage 1 (300 Samples): 0%–65%
    - Stage 2 (RAM Reclaim): 65%–75%
    - Stage 3 (Piper Tuning): 75%–100%
  - *Why*: Provides the user with a single cohesive progress meter indicating when the entire session will reach 100%.

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────────┐
│                     TrainingScreen.kt                           │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ RealTimeSystemMonitorCard                                 │  │
│  │   • Live CPU Gauge: [====      ] 78.4%                    │  │
│  │   • RAM Gauge: [========    ] 3.2 GB / 6.0 GB (53%)       │  │
│  │   • Total Completion: [=====       ] 48%                  │  │
│  │   • [Cancel & Abort Training]                             │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ PopCelebrationOverlay (Triggers at 100%)                  │  │
│  │   • Bouncy Spring ScaleIn + FadeIn                        │  │
│  │   • Auto-dismiss -> switch to StudioTab.LIBRARY           │  │
│  └───────────────────────────────────────────────────────────┘  │
└────────────────────────────────┬────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                       VoiceViewModel                            │
│  • hardwareTelemetry: StateFlow<HardwareTelemetry>              │
│  • cancelTrainingPipeline() -> terminates and discards cache    │
│  • selectTab(StudioTab.LIBRARY) on 100% completion              │
└────────────────────────────────┬────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                 HardwareTelemetryMonitor.kt                     │
│  • ActivityManager.MemoryInfo (usedRam, totalRam)               │
│  • Process CPU sampling with EMA smoothing                      │
│  • Pipeline composite completion calculation (0..100%)          │
└─────────────────────────────────────────────────────────────────┘
```

### Data Model & State Updates

- **`HardwareTelemetry.kt`**:
  - `cpuPercentage: Float` (0f–100f)
  - `usedRamBytes: Long`, `totalRamBytes: Long`, `ramPercent: Float`
  - `heapUsedBytes: Long`
  - `overallProgressPercent: Float` (0f–100f)
  - `estimatedRemainingSec: Int`
- **`HardwareTelemetryMonitor.kt`**:
  - Lifecycle-aware sampling loop running in a background coroutine while training is active.
- **`VoiceViewModel.kt`**:
  - Exposes `hardwareTelemetry: StateFlow<HardwareTelemetry>`.
  - In `saveClonedVoice()`, upon model commit and reaching 100%, triggers the auto-transition to `StudioTab.LIBRARY`.
- **`TrainingScreen.kt`**:
  - Adds `RealTimeSystemMonitorCard` with CPU gauge, RAM gauge, overall completion progress, and cancel button.
  - Implements the pop animation transition sequence when completion hits 100%.
