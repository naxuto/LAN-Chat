# LAN Chat

Serverless peer-to-peer messenger for Android that works on a local Wi-Fi network with no Internet.

- Discovery: UDP broadcast on port 45679
- Messages: TCP on port 45678, ECDSA challenge-response auth + ECDH/AES-256-GCM encryption
- Storage: Room (SQLite), offline queue for pending messages
- UI: Jetpack Compose, Material 3
- Requires Android 12 (API 31) or newer, JDK 17

## Build
Open the folder in Android Studio (it generates the Gradle wrapper), or run
`gradle wrapper --gradle-version 8.4` once, then:

    gradlew.bat assembleDebug
    gradlew.bat installDebug

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Testing with two phones
Connect both phones to the same router (Internet not needed), open the app on both.
If devices don't see each other, disable AP / Client Isolation on the router.
