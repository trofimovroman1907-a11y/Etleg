# Nexora Android

Nexora is an independent Android messenger project.

## Current stage

- Native Android application shell.
- Guest entry flow with an explicit channels-only notice.
- Local sample channel catalogue for UI verification.
- Separate Gradle project under `Nexora/`, so the existing Etleg Telegram-derived project remains untouched.
- Backend, real account registration, remote channels, messaging, media, calls and end-to-end encryption are not enabled yet. They will be added and tested in separate stages; the app must not claim they work before integration.

## Build

From repository root:

```bash
./gradlew -p Nexora :app:assembleDebug
```

APK output:

`Nexora/app/build/outputs/apk/debug/app-debug.apk`

The GitHub Actions workflow `.github/workflows/build-nexora-apk.yml` builds and uploads the debug APK.
