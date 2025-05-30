# OpenDrive: Open-Source Vehicle Monitoring System 🚗💨

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Build Status](https://img.shields.io/github/actions/workflow/status/1719pankaj/OpenDrive/android.yml?branch=main&label=Build)](https://github.com/1719pankaj/OpenDrive/actions)
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/1719pankaj/OpenDrive?label=Latest%20Release)](https://github.com/1719pankaj/OpenDrive/releases)
[![GitHub contributors](https://img.shields.io/github/contributors/1719pankaj/OpenDrive)](https://github.com/1719pankaj/OpenDrive/graphs/contributors)
[![GitHub last commit](https://img.shields.io/github/last-commit/1719pankaj/OpenDrive)](https://github.com/1719pankaj/OpenDrive/commits/main)
[![Code size](https://img.shields.io/github/languages/code-size/1719pankaj/OpenDrive)](https://github.com/1719pankaj/OpenDrive)

**OpenDrive** is an open-source Android application designed to interface with standard **ELM327 OBD-II** adapters via Bluetooth. It aims to provide real-time vehicle diagnostics, performance monitoring, and trouble code reading capabilities directly on your Android device.

This project is currently in active development, focusing on building a stable, extensible platform for vehicle data visualization and analysis.

<!--
**[Optional: Insert a Screenshot or GIF here once you have basic UI]**
![App Screenshot Placeholder](link_to_your_screenshot_or_gif_here.png)
-->

## ✨ Key Features

*   ✅ **Real-time Data Monitoring:** Display core vehicle parameters like:
    *   Engine RPM
    *   Vehicle Speed
    *   Engine Coolant Temperature
    *   *(More PIDs planned!)*
*   ✅ **Bluetooth Connectivity:** Connects to standard ELM327 Bluetooth OBD-II adapters.
*   ⏳ **Diagnostic Trouble Codes (DTCs):** Read and clear engine fault codes (Check Engine Light).
*   ⏳ **Fuel Efficiency Tracking:** Basic calculation and display of fuel consumption metrics.
*   📐 **Modular Architecture:** Designed with extensibility in mind, aiming to support community-developed plugins or extensions for different vehicle manufacturers or custom PIDs in the future.
*   ⚙️ **Optimized Communication:** Focus on reliable and efficient data transmission over Bluetooth.
*   📝 **Data Logging:** Future support for logging sessions to a file.

## 🎯 Target Hardware

This application requires a standard **ELM327-based OBD-II adapter** with **Bluetooth** connectivity. These are widely available online (e.g., on Amazon, eBay). Ensure your adapter supports the standard ELM327 command set.

*Note: USB and Wi-Fi adapters are not currently supported.*

## 🏗️ Architecture Overview

OpenDrive follows the **MVVM (Model-View-ViewModel)** architecture pattern, utilizing:

*   **Kotlin:** For modern, concise, and safe code.
*   **Coroutines:** For managing background threads and asynchronous operations (Bluetooth I/O, data processing).
*   **LiveData / StateFlow:** For reactive UI updates from the ViewModel.
*   **Repository Pattern:** To abstract data sources (Bluetooth OBD Service).
*   **Dedicated Bluetooth Service:** To handle connection management and OBD-II command execution.

For a more detailed breakdown, see the [Architecture Documentation](docs/architecture.md).
A brief overview of the target hardware interface can be found in [Hardware Notes](docs/hardware.md).

## 🚀 Getting Started

### Prerequisites

*   An **ELM327 Bluetooth OBD-II Adapter**.
*   An Android device (API level 23+ recommended).
*   Android Studio (latest stable version recommended).

### Installation & Setup

1.  **Pair Adapter:** Before launching the app, pair your ELM327 adapter with your Android device via the system Bluetooth settings.
2.  **Clone the Repository:**
    ```bash
    git clone https://github.com/1719pankaj/OpenDrive.git
    cd OpenDrive
    ```
3.  **Build the App:** Open the project in Android Studio and build using Gradle:
    ```bash
    ./gradlew assembleDebug
    ```
    Alternatively, build and run directly from Android Studio onto your connected device or emulator (emulator requires simulated Bluetooth connection).
4.  **Run the App:**
    *   Install the generated APK (`app/build/outputs/apk/debug/app-debug.apk`).
    *   Launch OpenDrive.
    *   Grant necessary Bluetooth permissions (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`).
    *   *(Development WIP)* The app should allow you to select your paired ELM327 adapter to establish a connection.

## 💻 Technology Stack

*   **Language:** Kotlin
*   **Architecture:** MVVM
*   **Async:** Kotlin Coroutines
*   **UI:** Android Views (XML Layouts), Material Design Components (May explore Jetpack Compose later)
*   **Connectivity:** Android Bluetooth Classic API (SPP Profile)
*   **Build System:** Gradle

## 🤝 Contributing

Contributions are welcome! Whether it's reporting bugs, suggesting features, or submitting pull requests, your input is valuable.

1.  **Fork** the Project
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your Changes (`git commit -m 'feat: Add some AmazingFeature'`)
4.  Push to the Branch (`git push origin feature/AmazingFeature`)
5.  Open a **Pull Request** against the `main` branch.
