# Skinwalker

Skinwalker is an Android app-cloning prototype built around a bundled virtual runtime. It lets you place supported installed apps into an isolated in-app space and launch them with separate app data, so you can sign in with a different account inside the cloned copy.

This project is a sideloaded prototype. It is not a Play Store release and it is not using Android work-profile provisioning. The current implementation uses the bundled BlackBox-based virtual environment to host cloned apps inside Skinwalker.

## What it does

- Creates a home space for cloned apps inside Skinwalker
- Supports multiple local user profiles inside Skinwalker
- Launches cloned apps in isolated per-profile virtual users
- Lets you add and remove apps from the cloned home screen
- Shows per-profile app slots and storage usage
- Supports light and dark mode
- Checks GitHub Releases for new APK updates

## Current product shape

Main screens:

- Home screen
  Shows the active profile, cloned apps, and add button
- Add Apps
  Full-screen searchable app list for adding new clones
- Profiles
  Switch, create, and edit local Skinwalker profiles
- Settings
  Appearance, storage, current version, and update check

## How cloning works

Skinwalker does not duplicate the original APK package name on the device launcher. Instead, it installs and launches apps inside the bundled virtual runtime. That gives each Skinwalker profile its own isolated app data space.

High-level flow:

1. User adds an installed app to Skinwalker
2. Skinwalker installs that package into the virtual environment for the active Skinwalker profile
3. Skinwalker launches the app through the virtual runtime
4. The cloned app stores its own login state inside Skinwalker’s virtual data, separate from the original app

## Project structure

- [app](C:/Users/harsh/AndroidStudioProjects/Skinwalker/app)
  Android application module
- [third_party/NewBlackbox](C:/Users/harsh/AndroidStudioProjects/Skinwalker/third_party/NewBlackbox)
  Bundled virtual runtime and support modules
- [updates](C:/Users/harsh/AndroidStudioProjects/Skinwalker/updates)
  Update-related helper files

Important app files:

- [MainActivity.kt](C:/Users/harsh/AndroidStudioProjects/Skinwalker/app/src/main/java/com/example/skinwalker/MainActivity.kt)
- [AddAppsActivity.kt](C:/Users/harsh/AndroidStudioProjects/Skinwalker/app/src/main/java/com/example/skinwalker/AddAppsActivity.kt)
- [UpdateManager.kt](C:/Users/harsh/AndroidStudioProjects/Skinwalker/app/src/main/java/com/example/skinwalker/UpdateManager.kt)
- [BlackBoxVirtualEngine.kt](C:/Users/harsh/AndroidStudioProjects/Skinwalker/app/src/main/java/com/example/skinwalker/BlackBoxVirtualEngine.kt)
- [ProfileRepository.kt](C:/Users/harsh/AndroidStudioProjects/Skinwalker/app/src/main/java/com/example/skinwalker/ProfileRepository.kt)
- [CloneRepository.kt](C:/Users/harsh/AndroidStudioProjects/Skinwalker/app/src/main/java/com/example/skinwalker/CloneRepository.kt)

## Requirements

- Windows with Android Studio
- Android SDK installed
- Gradle wrapper support
- A physical Android device is recommended for validation

## Build

Debug build:

```powershell
.\gradlew.bat assembleDebug
```

Run unit tests and build:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

APK output:

- [app-debug.apk](C:/Users/harsh/AndroidStudioProjects/Skinwalker/app/build/outputs/apk/debug/app-debug.apk)

## Install on device

Using ADB:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Versioning

Skinwalker uses standard Android versioning:

- `versionName`
  Human-facing version such as `1.1.0`
- `versionCode`
  Internal numeric build number such as `2`

Current values live in [app/build.gradle.kts](C:/Users/harsh/AndroidStudioProjects/Skinwalker/app/build.gradle.kts).

## GitHub Releases updates

Skinwalker is configured to check this public repository for updates:

- [X-DIABLO-X/Skinwalker](https://github.com/X-DIABLO-X/Skinwalker)

Update behavior:

1. On startup, Skinwalker checks the latest GitHub Release
2. If the release version is newer than the installed app, Skinwalker shows an update prompt
3. Tapping update downloads the APK release asset
4. Android opens the package installer
5. After installation, the app reopens on package replacement

Notes:

- The repo is public, so no GitHub token is needed
- Android still requires the user to approve the APK install
- The updater expects the latest release to include an APK asset

## Release workflow

Build the APK:

```powershell
.\gradlew.bat assembleDebug
```

Commit and push source:

```powershell
git add .
git commit -m "Your change"
git push
```

Create or update a release with the APK:

```powershell
gh release create v3 "app\build\outputs\apk\debug\app-debug.apk#app-debug.apk" --repo X-DIABLO-X/Skinwalker --title "1.2.0 (3)" --notes "Release notes"
```

If the tag already exists, replace the asset:

```powershell
gh release upload v3 "app\build\outputs\apk\debug\app-debug.apk#app-debug.apk" --repo X-DIABLO-X/Skinwalker --clobber
```

Recommended release pattern:

- Tag name: `v3`
- Release title: `1.2.0 (3)`
- APK asset: `app-debug.apk`

## Data and storage

Skinwalker stores:

- App-level settings
- Local profile metadata
- Clone metadata
- Virtualized app runtime data under Skinwalker’s app storage

The Settings screen shows estimated storage used by:

- Skinwalker app data
- Cloned app data
- Total local usage

## Known limitations

- Some apps may refuse to run properly in virtual environments
- Some apps may detect virtualization and limit login or feature access
- This is still a prototype, not a hardened production container
- Updates are sideload-based, not Play Store in-app updates

## Repository

- Repository: [X-DIABLO-X/Skinwalker](https://github.com/X-DIABLO-X/Skinwalker)
- Releases: [GitHub Releases](https://github.com/X-DIABLO-X/Skinwalker/releases)

## License and ownership

Review bundled third-party code licensing separately before distributing builds beyond personal or prototype use. The `third_party/NewBlackbox` subtree is included as a dependency source base and should be audited according to its upstream license terms.
