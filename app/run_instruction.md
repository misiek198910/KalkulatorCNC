# Build and Run Instructions

This repository contains a native Android application built with Kotlin. Follow these steps to build and execute the project locally.

## Prerequisites
- **Android Studio**: Jellyfish (2023.3.1) or newer recommended.
- **Java Development Kit (JDK)**: JDK 17 (embedded in modern Android Studio).
- **Android SDK**: Minimum SDK 24, Target SDK 34.

## Setup & Build
1. **Open the Project**: Launch Android Studio and select *File > Open...*, then navigate to the root directory of this repository.
2. **Gradle Sync**: Android Studio should automatically start syncing the Gradle files. If it doesn't, click *File > Sync Project with Gradle Files*.
3. **Clean & Rebuild**:
    - Go to *Build > Clean Project*.
    - Once finished, go to *Build > Rebuild Project* to ensure all dependencies and Compose compiler metrics are correctly resolved.

## Execution
1. Set up an Android Virtual Device (AVD) running API level 24+ or connect a physical Android device with Developer Options and USB Debugging enabled.
2. Select the `app` module from the run configuration dropdown in the top toolbar.
3. Click the **Run** button (green play icon) or press `Shift + F10` (Windows) / `Control + R` (macOS).

## Note on Architecture
- The application uses Jetpack Compose for the UI layer and follows the MVVM architectural pattern.
- Ensure you have an active internet connection on the first build to download the required Gradle dependencies.