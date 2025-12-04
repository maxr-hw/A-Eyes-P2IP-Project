# A-Eyes: P2IP Project for Visual Assistance

**Android app for real-time scene description, designed for people with visual impairments.**

Note that this app only has one page, to simplify the use of the app, for the visually impaired. Apart from a button that is redirecting to this repository.

[![GitHub](https://img.shields.io/badge/GitHub-Repository-blue)](https://github.com/maxr-hw/A-Eyes-P2IP-Project)

---

## 📌 Overview

**A-Eyes** is an Android application developed as part of the **P2IP project** at **ESILV Engineering School**. The app leverages the device's camera and **Mistral's LLM APIs (Picstral and Mini LLM)** to provide real-time audio descriptions of the surrounding environment. Users can switch between **French** and **English** for descriptions.

---

## 🔧 Features

- **Real-time Scene Description**: Uses the phone camera to capture and describe scenes.
- **Multi-language Support**: Descriptions available in **French** and **English**.
- **LLM Integration**: Powered by **Mistral's Picstral and Mini LLM** for accurate, context-aware descriptions.
- **Accessibility Focus**: Designed for users with visual impairments.

---

## 🛠️ Setup & Installation

### Prerequisites
- Android Studio (latest version)
- Android device (API level 24+)
- Mistral API key (for LLM integration)

### Installation Steps
1. **Clone the Repository**:
   ```bash
   git clone https://github.com/maxr-hw/A-Eyes-P2IP-Project.git
   ```

2. **Open in Android Studio**:

  Import the project into Android Studio.
  Sync Gradle dependencies.


3. **Configure API Key**:
  
  Add your Mistral API key in the project's configuration file (e.g., local.properties or secrets.gradle).


4. **Build & Run**:
  
  Connect an Android device or use an emulator.
  Build and run the app.

## 📱 Usage

1. **Launch the App**: Open **A-Eyes** on your Android device.
2. **Grant Permissions**: Allow camera and microphone access.
3. **Point the Camera**: Aim the camera at the scene you want described.
4. **Listen to Descriptions**: The app will audibly describe the scene in your chosen language.

---

## 🔍 Technical Details

- **LLM Models**: Uses Mistral's **Picstral** and **Ministral** for scene interpretation.
- **Camera Integration**: Real-time image capture and processing.
- **Text-to-Speech**: Converts LLM-generated descriptions into audio.

---

## 📧 Contact

For questions or feedback, reach out to **[Maxime Hombreux-Wang](https://github.com/maxr-hw)**.

