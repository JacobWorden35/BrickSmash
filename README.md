# BrickSmash - A Modern Brick Breaker Game for Android

**Developer:** Jacob Worden (jrw5859@utexas.edu)  
**Course:** Android Programming (UTAP Spring 2025)

## Overview
BrickSmash is a modern take on the classic brick breaker arcade game, built natively for Android using Kotlin. Features include 15+ built-in levels, 4 power-up types, a custom level editor, and a Firebase-backed online leaderboard.

## Setup Instructions

### 1. Clone & Open in Android Studio
```bash
git clone <your-repo-url>
```
Open the project in Android Studio (Ladybug or newer recommended).

### 2. Firebase Setup (Required)
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project named "BrickSmash"
3. Add an Android app with package name: `com.bricksmash`
4. Download `google-services.json` and place it in `app/`
5. Enable **Authentication** → Email/Password sign-in method
6. Enable **Cloud Firestore** → Start in test mode, then deploy `firestore.rules`
7. (Optional) Enable **Cloud Storage** for user avatars

### 3. Deploy Firestore Security Rules
```bash
firebase deploy --only firestore:rules
```
Or copy the contents of `firestore.rules` into the Firebase Console Rules tab.

### 4. Build & Run
- Target: API 31+ device or emulator
- Build with Android Studio or `./gradlew assembleDebug`

## Project Structure
```
app/src/main/
├── java/com/bricksmash/
│   ├── MainActivity.kt          # Single-activity host
│   ├── game/                     # Game engine
│   │   ├── Ball.kt              # Ball physics & rendering
│   │   ├── Paddle.kt            # Player paddle
│   │   ├── Brick.kt             # Brick types & destruction
│   │   ├── PowerUp.kt           # 4 power-up types
│   │   ├── ParticleEffect.kt    # Visual effects
│   │   ├── CollisionDetector.kt # AABB collision system
│   │   ├── GameEngine.kt        # Main game logic
│   │   ├── GameView.kt          # SurfaceView renderer
│   │   ├── GameThread.kt        # 60fps game loop
│   │   └── LevelManager.kt      # Level loading
│   ├── ui/                       # Fragments
│   │   ├── MainMenuFragment.kt
│   │   ├── LevelSelectFragment.kt
│   │   ├── GameFragment.kt
│   │   ├── LeaderboardFragment.kt
│   │   ├── LevelEditorFragment.kt
│   │   ├── LoginFragment.kt
│   │   ├── RegisterFragment.kt
│   │   └── SettingsFragment.kt
│   ├── data/                     # Firebase repositories
│   │   ├── UserRepository.kt
│   │   ├── LeaderboardRepository.kt
│   │   └── LevelRepository.kt
│   └── model/                    # Data classes
│       ├── LevelData.kt
│       ├── ScoreEntry.kt
│       └── UserProfile.kt
├── res/                          # Layouts, navigation, values
└── assets/levels/                # Built-in level JSON files
```

## Technologies Used
- **Kotlin** + Android SDK (API 31-36)
- **SurfaceView / Canvas** for 60fps game rendering
- **Firebase Auth** for email/password authentication
- **Cloud Firestore** for leaderboard & community levels (mutable shared state)
- **Material Design 3** for UI components
- **Kotlin Serialization** for JSON level data
- **Navigation Component** for screen management
- **Coroutines** for async Firebase operations
