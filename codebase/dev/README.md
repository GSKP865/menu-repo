# Android Development - Me'n'U App

## Project Setup

### 1. Create Android Project in Android Studio
```bash
# Open Android Studio
# File → New → New Project
# Select "Empty Activity"
# Name: MenuApp
# Package name: com.menu.app
# Language: Kotlin
# Minimum SDK: API 24 (Android 7.0)
# Click Finish
```

### 2. Project Structure
```
app/
├── src/
│   ├── main/
│   │   ├── java/com/menu/app/
│   │   │   ├── MainActivity.kt
│   │   │   ├── auth/
│   │   │   ├── profile/
│   │   │   ├── matching/
│   │   │   ├── audio/
│   │   │   └── utils/
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   ├── values/
│   │   │   └── drawable/
│   │   └── AndroidManifest.xml
│   └── test/
└── build.gradle
```

## Key Features to Implement

### 1. Authentication (Firebase Auth)
- Sign up with email/password
- Sign in with email/password
- Phone number authentication
- Profile creation
- **UI**: Pink/white minimalist login screen with clean input fields

### 2. User Profile
- Voice bio recording with duration display
- Profile picture upload (circular)
- Age selector
- Gender selection
- Interests selection (chips/tags)
- Voice sample playback
- **UI**: Profile creation screen with voice recording button and multi-select interests

### 3. Matching Algorithm
- Voice-based matching
- Location-based matching
- Interest-based filtering
- Real-time availability
- **UI**: Swipeable profile cards with voice bio play button, like/pass/super-like buttons

### 4. Audio Calling (WebRTC)
- Peer-to-peer audio calls
- Audio quality settings
- Call duration tracking
- End call functionality
- Voice waveform visualization
- **UI**: Audio call screen with caller profile, duration timer, mute/speaker/end call controls

### 5. Real-time Features
- Online status
- Push notifications
- Real-time matching
- Call signaling
- **UI**: Match celebration screen with "It's a Match!" animation

### 6. UI/UX Design (Based on Prototype)
- **Color Scheme**: Pink (#FF69B4) and White (#FFFFFF)
- **Design Style**: Minimalist, modern, mobile-first
- **Components**: Rounded corners (16dp), subtle shadows, Material Design
- **Navigation**: Bottom navigation bar (Home, Matches, Profile, Settings)
- **Typography**: Sans-serif (Roboto), bold headlines, regular body text
- **Animations**: Card swipe, match celebration, voice waveform, button feedback

## Dependencies to Add

### build.gradle (app level)
```gradle
dependencies {
    // Firebase
    implementation 'com.google.firebase:firebase-auth:22.3.0'
    implementation 'com.google.firebase:firebase-firestore:24.10.0'
    implementation 'com.google.firebase:firebase-storage:20.3.0'
    implementation 'com.google.firebase:firebase-messaging:23.4.0'

    // WebRTC for audio calling
    implementation 'io.getstream:stream-webrtc-android:1.3.10'

    // UI Components
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

    // Audio recording and playback
    implementation 'androidx.media:media:1.7.0'
    implementation 'com.google.android.exoplayer:exoplayer:2.19.1'

    // Card swipe functionality
    implementation 'com.yuyakaido.android:card-stack-view:2.3.4'

    // Image loading
    implementation 'com.github.bumptech.glide:glide:4.16.0'
}
```

### colors.xml (Pink/White Theme)
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="pink_primary">#FF69B4</color>
    <color name="pink_light">#FFB6C1</color>
    <color name="pink_dark">#FF1493</color>
    <color name="white">#FFFFFF</color>
    <color name="gray_light">#F5F5F5</color>
    <color name="gray_dark">#333333</color>
    <color name="text_primary">#000000</color>
    <color name="text_secondary">#666666</color>
</resources>
```

### styles.xml (Material Design Theme)
```xml
<resources>
    <style name="AppTheme" parent="Theme.MaterialComponents.Light.NoActionBar">
        <item name="colorPrimary">@color/pink_primary</item>
        <item name="colorPrimaryDark">@color/pink_dark</item>
        <item name="colorAccent">@color/pink_primary</item>
        <item name="android:windowBackground">@color/white</item>
    </style>
</resources>
```

## Development Steps

### Phase 1: Authentication & UI Setup (Week 1)
1. Set up Firebase project
2. Add Firebase to Android project
3. Implement pink/white theme (colors.xml, styles.xml)
4. Create login/signup screens with minimalist design
5. Implement sign-up/sign-in functionality
6. Test authentication flow

### Phase 2: User Profile (Week 2)
1. Design profile creation screen with voice recording
2. Implement voice bio recording with MediaRecorder
3. Add profile picture upload (circular)
4. Implement age/gender selectors
5. Create interests selection (chips/tags)
6. Add voice sample playback with ExoPlayer
7. Test profile management

### Phase 3: Matching System (Week 3)
1. Implement matching algorithm
2. Create swipeable card UI (CardStackView)
3. Add voice bio play button on cards
4. Implement like/pass/super-like buttons
5. Add filtering options
6. Real-time matching updates
7. Test matching flow

### Phase 4: Audio Calling (Week 4)
1. Set up WebRTC for audio calls
2. Implement call signaling with Firebase
3. Create audio call screen with pink/white theme
4. Add voice waveform visualization
5. Implement mute/speaker/end call controls
6. Add call duration timer
7. Test calling functionality

### Phase 5: Real-time Features & Polish (Week 5)
1. Implement match celebration screen
2. Add bottom navigation bar (Home, Matches, Profile, Settings)
3. Implement push notifications
4. Add online status indicators
5. Create settings screen
6. Add animations (card swipe, match celebration, button feedback)
7. Unit testing
8. Integration testing
9. UI testing
10. Performance optimization
11. Bug fixes

## Testing on Emulator

### Create AVD (Android Virtual Device)
```bash
# Android Studio → Tools → Device Manager
# Create Device → Select device (Pixel 6 recommended)
# Select system image (API 33 or 34)
# Finish
```

### Run App
```bash
# In Android Studio
# Click Run button (green triangle)
# Select AVD
# App will install and launch
```

## Firebase Setup
```bash
# Install Firebase CLI
npm install -g firebase-tools

# Login
firebase login

# Initialize
firebase init

# Add to Android project
# In Android Studio: Tools → Firebase
# Follow the assistant
```

## Common Commands
```bash
# Build project
./gradlew build

# Run tests
./gradlew test

# Generate APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

## Troubleshooting
- **Gradle sync failed**: File → Invalidate Caches → Restart
- **Emulator not starting**: Enable virtualization in BIOS
- **Firebase not connecting**: Check google-services.json
- **Audio not working**: Check microphone permissions

## Next Steps
1. Create Firebase project
2. Set up Android Studio project
3. Implement authentication first
4. Test on emulator regularly
5. Commit code to dev branch
