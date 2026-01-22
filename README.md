# Genesis Empathic Cockpit
**Automotive World 2026 Demo Application**

## 1. Executive Summary
This application is a next-generation **Smart Cockpit** concept that combines **Touch-Free Gesture Control** with **Non-Invasive Biometric Monitoring**. It transforms the driver's interaction from passive inputs to an active, empathetic relationship where the vehicle understands the driver's intent, health, and cognitive state without physical contact.

---

## 2. Technology Stack & AI Models

### A. Computer Vision & Interaction (Powered by MediaPipe)
The system uses **Google MediaPipe** for all geometric understanding of the driver.

#### 1. Hand Gesture Interface
*   **Library**: `com.google.mediapipe:tasks-vision:0.10.x`
*   **Model**: `hand_landmarker.task`
*   **Specific Signals Used**:
    *   **Wrist (Landmark 0)**: Used as the root anchor for relative coordinates.
    *   **Index Finger Tip (8) & Thumb Tip (4)**: Distance calculated for "Pinch" interactions.
    *   **Palm Size**: Calculated as Euclidean distance between Wrist(0) and Middle Finger MCP(9) to normalize gesture thresholds dynamically (Scale Invariance).

#### 2. Driver Monitoring System (DMS)
*   **Library**: `com.google.mediapipe:tasks-vision:0.10.x`
*   **Model**: `face_landmarker.task` with `Blendshapes` enabled.
*   **Specific Signals Used (Blendshape coefficients 0.0 - 1.0)**:
    *   **Drowsiness**: `eyeBlinkLeft` + `eyeBlinkRight`. Logic: `IF (avg > 0.5) AND (duration > 1s) -> DROWSY`.
    *   **Distraction**: `eyeLookInLeft`, `eyeLookOutLeft`, etc. Logic: Vector sum of gaze direction.
    *   **Cognitive Load**: `browInnerUp`.
    *   **Pain/Discomfort**: `noseSneerLeft` + `noseSneerRight`.
    *   **Frustration**: `cheekPuff`.
    *   **Skepticism**: `eyeSquintLeft` + `eyeSquintRight` (Squinting without smiling).
    *   **Winking**: `eyeBlinkLeft` vs `eyeBlinkRight` (Asymmetric blinking).

---

### B. Biometric Health Cockpit (Custom Vision Algorithms)
**Note:** This module does NOT use a pre-built "Health API". It uses raw computer vision signal processing built from scratch in Kotlin.

#### 1. rPPG Heart Rate Engine
**Remote Photoplethysmography (rPPG)** extracts pulse signals from standard RGB video.

**Implementation Status:**
- ✅ **Suitable for Demo/Prototype** - Shows concept effectively
- ⚠️ **Not Medically Accurate** - Should not be used for actual health monitoring
- ✅ **Demonstrates rPPG Technology** - Proof of concept for stakeholder presentations

**Signal Processing Pipeline (`HealthProcessor.kt`):**
1.  **ROI Selection**: Uses **MediaPipe Face Mesh** Landmark 101 (Left Cheek) to target skin regions. Fallback: Center-crop if face tracking is lost.
2.  **Channel Extraction**: Extracts the **Green Channel (G)** from RGB pixels (Green light is best absorbed by hemoglobin).
3.  **Spatial Averaging**: Calculates mean Green intensity of 20x20px ROI region.
4.  **Temporal Buffering**: Stores last 150 frames (approx 5 sec at 30 FPS) in circular buffer.
5.  **AC Analysis**: Subtracts DC component (Mean) to isolate AC component (Pulse wave).
6.  **Zero-Crossing Detection**: Counts signal oscillations with ±0.5 threshold for hysteresis.
7.  **BPM Calculation**: `BPM = (zero_crossings / 2) * (60 / 5_seconds)`
8.  **Smoothing**: 50/50 exponential blend between current and estimated BPM.
9.  **Clamping**: Final BPM constrained to 50-120 range for display.

**Known Limitations:**
- No bandpass filtering (should isolate 0.7-4 Hz for 42-240 BPM range)
- Zero-crossing method is sensitive to noise
- Assumes fixed 5-second window (actual FPS varies)
- Small ROI size (20px) may be prone to noise
- No motion artifact detection or removal
- No skin detection to validate ROI quality

**For Production Use, Would Require:**
- Proper bandpass filter (Butterworth/Chebyshev)
- FFT-based frequency analysis instead of zero-crossing
- Motion artifact detection and compensation
- Skin detection to validate ROI is on skin
- Multi-ROI averaging for robustness
- Timestamp-based windowing instead of frame count
- Medical-grade validation and calibration
- FDA/regulatory compliance for health monitoring

#### 2. Stress & Adaptive Mood
*   **Fusion Logic**: Combines inputs from **Module A** (Face) and **Module B** (Heart).
*   **Formula**: `Stress = (BPM > 90) + (Face_Expression == ANGRY/SAD)`.
*   **Output**: High Stress state triggers the "Calm Mode" system event.

### B. Android Architecture
The application follows a robust **MVVM (Model-View-ViewModel)** pattern coupled with a **Foreground Service** architecture to ensure safety-critical features run continuously.

*   **Language**: Kotlin
*   **Concurrency**: Kotlin Coroutines (Async signal processing)
*   **Service Layer**: `GestureService` (Foreground Service) - Keeps AI logic alive even when UI is backgrounded.
*   **UI Toolkit**: XML Layouts, `ViewPager2`, Custom Views (`LineGraphView`).
### B. Advanced Driver Monitoring System (DMS)
*   **Input**: Camera Frames -> MediaPipe Face Blendshapes.
*   **States Detected**:
    *   **Drowsiness**: `eyeBlinkLeft/Right` > 0.5 for > 1.0 second.
    *   **Distraction**: `eyeLookOut/In` > 0.5 (Gaze aversion) for > 2.0 seconds.
    *   **Cognitive Load**: `browInnerUp` (Confusion).
    *   **Discomfort**: `noseSneer` (Physical pain/wince).
*   **Response**: Triggers "Wake Up" alarms and visual alerts in the "Driver Safety" tab.

### C. Biometric Health Cockpit (Adaptive Mood)
*   **Input**: Raw Pixel Data from Facial Regions.
*   **Logic**:
    1.  **Signal Extraction**: Spatial averaging of Green Channel in cheek regions.
    2.  **Filtering**: Bandpass filtering to remove noise and motion artifacts.
    3.  **BPM Calculation**: Zero-crossing / Peak detection on the AC component of the signal.
    4.  **Stress Estimation**: Fusion of High BPM (>90) and Negative Facial Expressions.
*   **Adaptive Response**:
    *   **Scenario**: If High Stress is detected.
    *   **Action**: Broadasts `com.android.design.intent.action.APPLY_THEME` (Mock) to switch the entire Cockpit UI to **"Calm Blue"** and creates a toast notification about "Lowering Music / Cooling Cabin".

---

## 4. Hardware Requirements
*   **Camera**: Standard RGB Camera (Webcam or Built-in). No IR required.
*   **Compute**: Standard Android Mobile CPU (Qualcomm/Exynos/Tensor). NPU acceleration utilized via MediaPipe Delegate where available.
*   **OS**: Android 10+ (API 29+).

## 5. Integration Points (Automotive)
This demo simulates a Tier-1 Supplier integration into Android Automotive OS (AAOS):
*   **VHAL Mocking**: Logs simulated Vehicle HAL calls (e.g., `HVAC_SET_TEMP`, `AUDIO_VOLUME`).
*   **System Intents**: Broadcasts standard Android Intents for interaction with Cluster and Launcher.

---

## 6. Medical Disclaimer
⚠️ **IMPORTANT**: The biometric monitoring features in this application are for **demonstration and research purposes only**. They are **NOT medically validated** and should **NOT be used** for actual health monitoring, diagnosis, or treatment decisions. The heart rate values displayed are estimates based on simplified algorithms and may not accurately reflect actual physiological measurements.

---

**Developed for TCS Automotive Demo 2026**
