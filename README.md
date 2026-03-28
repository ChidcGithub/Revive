# Revive

[![Platform](https://img.shields.io/badge/Platform-Android-green?style=flat-square)](https://www.android.com)
[![MinSDK](https://img.shields.io/badge/MinSDK-24%20(Android%207.0)-blue?style=flat-square)](https://developer.android.com/about/versions/nougat)
[![TargetSDK](https://img.shields.io/badge/TargetSDK-36%20(Android%2016)-blue?style=flat-square)](https://developer.android.com/about/versions)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple?style=flat-square&logo=kotlin)](https://kotlinlang.org)

[![Build](https://img.shields.io/github/actions/workflow/status/ChidcGithub/Revive/release.yml?branch=main&style=flat-square&label=Build)](https://github.com/ChidcGithub/Revive/actions)
[![Release](https://img.shields.io/github/v/release/ChidcGithub/Revive?style=flat-square&label=Release)](https://github.com/ChidcGithub/Revive/releases)
[![License](https://img.shields.io/github/license/ChidcGithub/Revive?style=flat-square)](LICENSE)
[![Code Size](https://img.shields.io/github/languages/code-size/ChidcGithub/Revive?style=flat-square)](https://github.com/ChidcGithub/Revive)

A modern local music player for Android built with Jetpack Compose and Material 3, featuring Apple Music-inspired design.

---

## Features

### Playback
- Multiple audio format support: MP3, M4A, FLAC, WAV, OGG, etc.
- Playback controls: Play/Pause, Next/Previous, Seek
- Shuffle and repeat modes (Off, One, All)
- Queue management with drag-to-reorder
- Background playback with notification controls
- Lock screen media controls
- Audio focus handling
- Sleep timer with fade-out effect

### Library Management
- Browse by **Albums**, **Artists**, **Folders**
- Global search with fuzzy matching
- Favorites management
- Recently played history
- Custom playlists with drag-to-reorder
- Smart collections (Top Picks, New Releases)

### User Interface
- Apple Music-inspired design system
- Material You dynamic colors (Android 12+)
- Light/Dark theme with system follow
- Smooth animations and transitions
- Immersive full-screen player with ambient backgrounds
- "Listen Now" home screen with personalized sections
- Audio quality badges (HI-RES, LOSSLESS)

---

## Tech Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin 2.0 |
| UI Framework | Jetpack Compose |
| Design System | Material 3 + Apple Music Style |
| Architecture | MVVM + Repository |
| DI | Hilt |
| Media Playback | ExoPlayer (Media3) |
| Database | Room |
| Preferences | DataStore |
| Image Loading | Coil |
| Async | Coroutines & Flow |
| Navigation | Compose Navigation |
| Color Extraction | AndroidX Palette |

---

## Project Structure

```
app/src/main/java/com/music/revive/
├── data/
│   ├── datasource/      # MediaStore data source
│   ├── color/           # Color extraction services
│   ├── local/           # Room database
│   │   ├── dao/         # DAO interfaces
│   │   └── entity/      # Database entities
│   └── repository/      # Repository implementations
├── domain/
│   ├── model/           # Domain models
│   └── utils/           # Domain utilities (SleepTimer)
├── presentation/
│   ├── components/      # Reusable Compose components
│   │   ├── GradientOrb.kt          # Ambient background orbs
│   │   ├── ImmersivePlayerComponents.kt  # Player UI
│   │   └── QualityBadges.kt        # Audio quality badges
│   ├── navigation/      # Navigation setup
│   ├── screen/          # Screen UIs
│   │   ├── home/        # "Listen Now" home screen
│   │   ├── player/      # Immersive player screen
│   │   ├── playlist/    # Playlist screens
│   │   ├── search/      # Search screen
│   │   └── settings/    # Settings screen
│   └── theme/           # Theme configuration
│       ├── Color.kt     # Apple Music color system
│       ├── Type.kt      # SF Pro-style typography
│       └── Theme.kt     # Shape and animation system
├── service/             # Background services
│   ├── MusicService.kt  # MediaSession service
│   └── MusicPlayer.kt   # ExoPlayer wrapper
├── di/                  # Hilt modules
└── MainActivity.kt      # Main entry point
```

---

## Build

### Requirements

| Requirement | Version |
|-------------|---------|
| Android Studio | Hedgehog (2023.1.1) or newer |
| JDK | 17 |
| Android SDK | 36 |
| Gradle | 8.7 |

### Build Steps

```bash
# Clone the repository
git clone https://github.com/ChidcGithub/Revive.git
cd revive

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

### IDE Setup

1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to the cloned directory
4. Wait for Gradle sync to complete
5. Run on device or emulator

---

## Version Naming

The application uses automatic version naming via GitHub Actions:

```
[Version].[Type][BuildNumber]-[GitSHA]
```

**Examples:**
- Release: `1.0.0.r1083-21zv882nef`
- Beta: `1.0.0.b1083-21zv882nef`

| Component | Description |
|-----------|-------------|
| `1.0.0` | Semantic version (major.minor.patch) |
| `r` / `b` | Build type: **r**elease or **b**eta |
| `1083` | GitHub Actions run number |
| `21zv882nef` | Git commit short SHA |

### Branch Detection

| Branch | Build Type |
|--------|------------|
| `main` | Release (`r`) |
| `beta` | Beta (`b`) |

### Manual Release

Trigger a release via GitHub Actions:

1. Go to Actions > Build and Release
2. Click "Run workflow"
3. Enter version name (e.g., `1.2.0`)
4. (Optional) Select release type to override branch detection
5. The workflow will create a tagged release (only for release type)

---

## GitHub Actions

### CI Workflow (develop branch)
- Triggered on push to `develop` and PRs
- Runs lint checks
- Builds debug APK with `b` (beta) type

### Release Workflow (main/beta branches)
- Triggered on push to `main` or `beta` branch
- `main` branch: Build type `r` (release)
- `beta` branch: Build type `b` (beta)
- Auto-increments version patch number
- Creates GitHub Release only when manually triggered with version and release type

### Required Secrets

| Secret | Description |
|--------|-------------|
| `KEYSTORE_BASE64` | Base64 encoded keystore file |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |

Generate keystore Base64:
```bash
base64 -w 0 keystore.jks > keystore_base64.txt
```

---

## Permissions

| Permission | Purpose |
|------------|---------|
| `READ_MEDIA_AUDIO` | Read audio files (Android 13+) |
| `READ_EXTERNAL_STORAGE` | Read storage (Android 12-) |
| `POST_NOTIFICATIONS` | Show notifications (Android 13+) |
| `FOREGROUND_SERVICE` | Background playback service |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Media playback foreground service |
| `WAKE_LOCK` | Keep playback running |

---

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style
- Follow Kotlin official coding conventions
- Use ktlint for formatting
- Write meaningful commit messages

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Acknowledgments

- [ExoPlayer](https://github.com/google/ExoPlayer) - Media playback engine
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - UI toolkit
- [Material Design 3](https://m3.material.io/) - Design system
- [Coil](https://coil-kt.github.io/coil/) - Image loading
- [Hilt](https://dagger.dev/hilt/) - Dependency injection
- [AndroidX Palette](https://developer.android.com/jetpack/androidx/releases/palette) - Color extraction
