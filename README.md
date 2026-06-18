# Markdown Vault

A secure, local-first Markdown editor with automatic GitHub synchronization, Obsidian/Logseq vault compatibility, LaTeX, and Mermaid.js support.

---

## System Prerequisites

To build and run this Android project, your development machine needs the following tools installed:

### 1. Java Development Kit (JDK)
* **Requirement**: JDK 17 (or newer). OpenJDK 17 is recommended.
* **Installation (macOS)**: 
  ```bash
  brew install openjdk@17
  ```
* **Configuration**: Set your Java environment by exporting `JAVA_HOME` or pinning it directly in Gradle. We have pinned it in your [gradle.properties](gradle.properties):
  ```properties
  org.gradle.java.home=/opt/homebrew/opt/openjdk@17
  ```

### 2. Android SDK
* **Requirement**: Android Software Development Kit (SDK) with API Platform 36 and matching Build-Tools.
* **Default SDK Locations**:
  * **macOS**: `/Users/<username>/Library/Android/sdk`
  * **Linux**: `/home/<username>/Android/Sdk`
  * **Windows**: `C:\Users\<username>\AppData\Local\Android\Sdk`
* **Configuration**: Specify your local SDK path in the [local.properties](local.properties) file at the project root:
  ```properties
  sdk.dir=/Users/kaushal/Library/Android/sdk
  ```

### 3. Gradle
* **Requirement**: Gradle is used to build the application. Although the project includes the Gradle wrapper (`gradlew`), you can install Gradle globally if needed.
* **Installation (macOS)**:
  ```bash
  brew install gradle
  ```

---

## Project Setup & Configuration

1. **Environment Variables**: Create a `.env` file in the root directory and define your `GEMINI_API_KEY`:
   ```properties
   GEMINI_API_KEY=your_gemini_api_key_here
   ```
2. **Signing Configuration**: A keystore is required to sign the APK.
   * **Debug Builds**: Generate a debug keystore using `keytool`:
     ```bash
     keytool -genkey -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
     ```
   * Ensure `debug.keystore` is located in the root of the project.

---

## Build Commands (Generating APKs)

Use the Gradle Wrapper (`gradlew` on macOS/Linux or `gradlew.bat` on Windows) to compile the project.

### 1. Build Debug APK
This compiles the application in debug mode using the local `debug.keystore`.
```bash
./gradlew assembleDebug
```
* **Output Location**: `app/build/outputs/apk/debug/app-debug.apk`

### 2. Build Release APK
This compiles an optimized, production-ready version of the application.
```bash
./gradlew assembleRelease
```
* **Output Location**: `app/build/outputs/apk/release/app-release.apk`
* *Note*: Ensure `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` environment variables are set before compiling a release build.

### 3. Clean Build Artifacts
If you encounter caching issues or want to perform a fresh compilation:
```bash
./gradlew clean
```
