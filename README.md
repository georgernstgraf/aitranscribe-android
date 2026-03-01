# AITranscribe Android App

Native Android version of AITranscribe for F-Droid distribution.

## 🚀 Quick Start

### Prerequisites

**System Requirements:**
- JDK 21+
- Android Studio Hedgehog (2023.1.1+) or latest
- Android SDK 34 (Android 14)
- Gradle 8.2+

**Required Tools:**
```bash
# Ubuntu/Debian
sudo apt install openjdk-21-jdk
sudo apt install ffmpeg

# Install Android Studio (Snap recommended)
sudo snap install android-studio --classic
```

### Building the App

```bash
cd android

# Clean build
./gradlew clean

# Debug build (faster, includes debug info)
./gradlew assembleDebug

# Release build (optimized for production)
./gradlew assembleRelease
```

**Output Locations:**
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`

### Running Tests

```bash
cd android

# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.georgernstgraf.aitranscribe.domain.usecase.TranscribeAudioUseCaseTest"

# Run with code coverage
./gradlew testDebugUnitTestCoverageJacocoReport

# Run UI/instrumented tests (requires emulator or device)
./gradlew connectedAndroidTest

# Run specific instrumented test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.georgernstgraf.aitranscribe.runner.AndroidJUnitRunner
```

## 📱 Installation

### Development Installation

```bash
# Install debug APK via ADB
./gradlew installDebug

# Install release APK
adb install app/build/outputs/apk/release/app-release.apk

# Launch app
adb shell am start -n com.georgernstgraf.aitranscribe/.ui.screen.MainActivity
```

### Production Installation (F-Droid)

1. **Build Release AAB:**
   ```bash
   ./gradlew bundleRelease
   ```

2. **Sign AAB:**
   ```bash
   jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
     -keystore your-keystore.jks \
     -signed-aab output.aab \
     app/build/outputs/bundle/release/app-release.aab
   ```

3. **Upload to F-Droid:**
   - Upload signed AAB to F-Droid repo
   - Add metadata and screenshots

## 🧪 Testing Guide

### Unit Tests

**Location:** `app/src/test/java/com/georgernstgraf/aitranscribe/`

**Categories:**
- **Use Case Tests:** Business logic testing
- **ViewModel Tests:** UI state management
- **Repository Tests:** Data layer testing
- **Utility Tests:** Audio chunking, network monitoring

**Running Unit Tests:**
```bash
# All unit tests
./gradlew test

# Specific package
./gradlew test --tests "*.usecase.*"

# With verbose output
./gradlew test --info

# Run tests continuously
./gradlew test --continuous
```

**Coverage Reports:**
```bash
# Generate HTML coverage report
./gradlew jacocoTestReport

# View in browser
open app/build/reports/jacoco/jacocoTestReport/html/index.html
```

### UI/Instrumented Tests

**Location:** `app/src/androidTest/java/com/georgernstgraf.aitranscribe/`

**Categories:**
- **Component Tests:** Compose UI interactions
- **Service Tests:** Recording service, transcription worker
- **Integration Tests:** End-to-end workflows

**Running Instrumented Tests:**

**Prerequisites:**
- Emulator running OR
- Physical device connected with USB debugging enabled

```bash
# Start emulator (if not running)
emulator -avd Pixel_6_API_34 -no-window &

# Check connected devices
adb devices

# Run all instrumented tests
./gradlew connectedAndroidTest

# Run specific test class
./gradlew connectedAndroidTest -Pandroid.testClass=com.georgernstgraf.aitranscribe.ui.components.AudioRecordingButtonTest

# Run with specific instrumentation runner
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.georgernstgraf.aitranscribe.runner.AndroidJUnitRunner
```

### Manual Testing Checklist

**Core Features:**
- [ ] Push-to-talk recording
- [ ] Recording duration display
- [ ] Recording cancellation
- [ ] Transcription with GROQ
- [ ] Post-processing with OpenRouter
- [ ] Offline queue when no network
- [ ] Auto-retry when network returns
- [ ] Background transcription with WorkManager
- [ ] Progress notifications
- [ ] Completion notifications

**Search & Filter:**
- [ ] Date range filtering
- [ ] Text search
- [ ] View status toggle (All/Only Unviewed)
- [ ] Results display correctly

**View Status:**
- [ ] Blue dot indicator for unviewed
- [ ] Gray indicator for viewed
- [ ] Auto-mark as viewed when opening
- [ ] Mark as unread functionality
- [ ] View status persists correctly

**Delete Functionality:**
- [ ] Single delete
- [ ] Delete old by date range
- [ ] Delete old with view filter
- [ ] Count confirmation before deletion
- [ ] Delete from settings screen

**Export/Import:**
- [ ] Export to JSON
- [ ] Export to CSV
- [ ] Import from JSON
- [ ] Import from CSV
- [ ] Format validation
- [ ] Error handling

**Settings:**
- [ ] API key entry (GROQ)
- [ ] API key entry (OpenRouter)
- [ ] Model selection (STT)
- [ ] Model selection (LLM)
- [ ] Settings persistence
- [ ] Delete old transcriptions
- [ ] Export functionality

**UI/UX:**
- [ ] Dark/Light theme switching
- [ ] Navigation between screens
- [ ] Back button behavior
- [ ] Loading states
- [ ] Error messages
- [ ] Empty states

### Debugging

**Enable USB Debugging:**
```bash
# Check connected devices
adb devices

# View logs
adb logcat | grep aitranscribe

# Clear logs
adb logcat -c

# View crash reports
adb logcat AndroidRuntime:E *:FATAL
```

**Studio Debugging:**
1. Open `Run` > `Edit Configurations`
2. Add Android configuration
3. Set module: `android.app`
4. Set app: `com.georgernstgraf.aitranscribe.AITranscribeApp`
5. Click Debug

### Performance Profiling

```bash
# Generate method tracing profile
./gradlew assembleRelease \
  -Pandroid.enableR8.fullMode=false \
  -Pandroid.enableR8=true

# Analyze with Android Studio Profiler
# Tools -> Profile -> Import trace file
```

## ⚙️ Configuration

### Build Configuration

**build.gradle.kts:**
- Min SDK: 26 (Android 8.0)
- Target SDK: 34 (Android 14)
- Compile SDK: 34
- Build Tools: 34.0.0

**ProGuard:** Enabled for release builds
- Rules in `app/proguard-rules.pro`
- Minifies and obfuscates code

### Dependencies

**Core Libraries:**
- `androidx.core:core-ktx:1.12.0` - Core KTX extensions
- `androidx.lifecycle:lifecycle-runtime-ktx:2.7.0` - Lifecycle components
- `androidx.compose.*:2024.02.00` - Jetpack Compose
- `androidx.room:*:2.6.1` - Room database
- `com.google.dagger:hilt-android:2.50` - Dependency injection

**Networking:**
- `com.squareup.retrofit2:retrofit:2.9.0` - HTTP client
- `com.squareup.okhttp3:okhttp:4.12.0` - OkHttp
- `com.squareup.okhttp3:logging-interceptor:4.12.0` - HTTP logging

**Background:**
- `androidx.work:work-runtime-ktx:2.9.0` - WorkManager
- `androidx.hilt:hilt-work:1.1.0` - Hilt for WorkManager

**Audio:**
- `com.arthenica:ffmpeg-kit:6.0-2` - FFmpeg for Android (FOSS)

**Crash Reporting (ACRA):**
- `ch.acra:acra-http:5.11.3` - HTTP crash reporter
- `ch.acra:acra-dialog:5.11.3` - Dialog crash reporter

### Testing Libraries:
- `org.junit:junit:4.13.2` - Unit testing
- `org.mockito:*:5.7.0` - Mocking
- `org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3` - Coroutines testing
- `app.cash.turbine:turbine:1.0.0` - Flow testing
- `androidx.test:*` - Android testing
- `androidx.compose.ui:ui-test-junit4` - Compose testing

## 🐛 Troubleshooting

### Common Build Issues

**Gradle Sync Failures:**
```bash
# Clean Gradle cache
rm -rf ~/.gradle/caches/
./gradlew --refresh-dependencies

# Force refresh
./gradlew --refresh-dependencies --rerun-tasks
```

**Build Failures:**
```bash
# Clean build
./gradlew clean

# Incremental build
./gradlew assembleDebug --no-daemon

# Check for dependency conflicts
./gradlew dependencies
```

**Test Failures:**
```bash
# Run single test with verbose output
./gradlew test --tests "SpecificTest" --info

# Check test class path
./gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.georgernstgraf.aitranscribe.runner.AndroidJUnitRunner \
  -Pandroid.testClass=com.georgernstgraf.aitranscribe.ui.components.AudioRecordingButtonTest

# Reinstall app (if tests fail)
adb uninstall com.georgernstgraf.aitranscribe
./gradlew installDebug
```

### Runtime Issues

**Crash on Start:**
```bash
# Check device logs
adb logcat | grep -E "FATAL|AndroidRuntime"

# View crash reports
# ACRA reports sent to server
```

**Recording Issues:**
```bash
# Check microphone permission
adb shell pm dump com.georgernstgraf.aitranscribe | grep permission

# Grant permission manually
adb shell pm grant com.georgernstgraf.aitranscribe android.permission.RECORD_AUDIO
```

**API Connection Issues:**
```bash
# Check network connectivity
adb shell dumpsys connectivity

# Test network from emulator
adb shell ping -c 1 google.com
```

## 📊 Code Coverage

### Target Coverage
- **Unit Tests:** 80%+ coverage
- **UI Tests:** 70%+ coverage
- **Overall:** 75%+ coverage

### Generating Coverage Reports

```bash
# Unit test coverage
./gradlew testDebugUnitTestCoverageJacocoReport

# Combined coverage (unit + instrumented)
./gradlew jacocoTestReport

# View reports
open app/build/reports/jacoco/test/html/index.html
```

## 🚀 Deployment

### Pre-Release Checklist

- [ ] All tests passing
- [ ] Code coverage >75%
- [ ] No compiler warnings
- [ ] ProGuard rules tested
- [ ] Release build successful
- [ ] APK/AAB signed
- [ ] Screenshots captured
- [ ] F-Droid metadata prepared

### Release Process

1. **Version Bump:**
   ```kotlin
   // In build.gradle.kts
   versionName = "1.0.0"
   versionCode = 1
   ```

2. **Build Release:**
   ```bash
   ./gradlew bundleRelease
   ```

3. **Sign AAB:**
   ```bash
   jarsigner -verbose -sigalg SHA256withRSA \
     -keystore release-key.jks \
     -signed-aab aitranscribe-release.aab \
     app/build/outputs/bundle/release/app-release.aab
   ```

4. **Verify AAB:**
   ```bash
   bundletool build-apps --mode=install \
     --aabs=aitranscribe-release.aab \
     --output-dir=build/verified
   ```

5. **Upload to F-Droid:**
   - Upload `aitranscribe-release.aab` to F-Droid repo
   - Add app metadata:
     - Description
     - Screenshots
     - Feature graphic (512x512)
     - Promo graphic (1024x500)

## 📝 Development Workflow

### Feature Development Workflow

1. **Write Tests FIRST**
   ```bash
   # Create test file
   touch app/src/test/java/com/georgernstgraf/aitranscribe/feature/NewFeatureTest.kt
   ```

2. **Implement Feature**
   ```bash
   # Create implementation file
   touch app/src/main/java/com/georgernstgraf/aitranscribe/feature/NewFeature.kt
   ```

3. **Run Tests**
   ```bash
   ./gradlew test --tests "NewFeatureTest"
   ```

4. **Refactor**
   ```bash
   # Run tests again
   ./gradlew test
   
   # Commit changes
   git add .
   git commit -m "feat: Add new feature"
   ```

### Git Workflow

```bash
# Create feature branch
git checkout -b feature/new-feature

# Develop and test
./gradlew test
./gradlew connectedAndroidTest

# Commit changes
git add .
git commit -m "feat: Add new feature"

# Push to remote
git push origin feature/new-feature

# Create pull request (if applicable)
gh pr create --base main --head feature/new-feature
```

### Code Style

**Kotlin:**
- Follow Android Kotlin style guide
- Use Kotlin idioms
- Prefer immutability
- Use data classes for DTOs
- Use sealed classes for states

**Compose:**
- Follow Compose best practices
- Use state hoisting
- Prefer stateless components
- Use material3 components

## 📚 Resources

### Documentation
- [Android Developers Guide](https://developer.android.com)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Hilt Documentation](https://dagger.dev/hilt/)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)

### F-Droid
- [F-Droid Documentation](https://f-droid.org/docs/Build_Tools_IDs)
- [Inclusion Policy](https://f-droid.org/docs/All_About_F-Droid_Web_Client#Inclusion_Policy)
- [Metadata](https://f-droid.org/docs/Build_Tools_IDs#Metadata)
- [Build Signatures](https://f-droid.org/docs/Build_Tools_IDs#Reproducible_Builds)

---

## 🎯 Quick Reference

### Key Commands

```bash
# Build
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew bundleRelease

# Test
./gradlew test
./gradlew connectedAndroidTest

# Install
./gradlew installDebug
adb install app/build/outputs/apk/release/app-release.apk

# Debug
adb logcat | grep aitranscribe
adb shell am start -n com.georgernstgraf.aitranscribe/.ui.screen.MainActivity
```

### Package Information

- **Package Name:** `com.georgernstgraf.aitranscribe`
- **Application ID:** `com.georgernstgraf.aitranscribe`
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34 (Android 14)
- **Version:** 1.0.0
- **Version Code:** 1

---

**For implementation details, see `Architecture.txt`**