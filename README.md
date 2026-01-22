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
*   **Input**: Raw `android.graphics.Bitmap` from Camera Stream.
*   **ROI Selection**:
    *   Uses **MediaPipe Face Mesh** Landmark 101, 123 (Cheek Centers) to dynamically target skin regions.
    *   *Fallback*: Center-crop if face tracking is lost.
*   **Signal Processing Pipeline (`HealthProcessor.kt`)**:
    1.  **Channel Extraction**: Extracts the **Green Channel (G)** from the RGB pixel array (Green light is best absorbed by hemoglobin).
    2.  **Spatial Averaging**: Calculates mean Green intensity of the 20x20px ROI region.
    3.  **Temporal Buffering**: Stores last 150 frames (approx 5 sec) in a circular buffer.
    4.  **AC Analysis**: Subtracts the DC component (Mean) to isolate the AC component (Pulse wave).
    5.  **Frequency Estimation**: Uses **Zero-Crossing Algorithm** with hysteresis to count peaks and calculate Beats Per Minute (BPM).

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
*   **State Management**: `LiveData` shared via `activityViewModels`.

---

## 3. Core Functionalities & Logic

### A. Gesture Control System
*   **Input**: Camera Frames -> MediaPipe Hands -> 21 Landmarks.
*   **Logic**:
    *   **Dynamic Thresholding**: Adapts recognition sensitivity based on `Palm Size` implies the system works reliably whether the hand is near the wheel or the dash.
    *   **Vector Analysis**: Calculates relative distance between specific nodes (e.g., Thumb Tip vs. Index Tip) to distinguish "Pinch" from "Open".
    *   **Mapping**: "Two Fingers Right" -> **Next Track**, "Open Palm" -> **Play/Pause**.

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
**Developed for TCS Automotive Demo 2026**
