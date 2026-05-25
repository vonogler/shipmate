# ShipMate Android

ShipMate AI Android MVP.

## Current Scope

- Call recording upload only.
- Shipment screens are postponed.
- Upload flow is designed for MinIO presigned URL direct upload.

## Stack

- Kotlin
- Jetpack Compose
- Material 3
- MVVM
- ViewModel + StateFlow
- Coroutines
- Retrofit + OkHttp
- Gradle Kotlin DSL
- compileSdk 36
- targetSdk 36
- minSdk 26

## First Screen

`CallUploadScreen` contains:

- Upload and AI analysis consent checkbox
- Audio file picker
- Upload button
- Upload progress
- Recent upload list
- Analysis status display

## API Flow

```text
Android -> API server: create upload session
Android -> MinIO: PUT audio file to presigned URL
Android -> API server: complete upload
API server -> analysis job queue
```

The current repository implementation is wired to `shipmate-api` for upload session creation, MinIO presigned PUT upload, upload completion, and recent upload refresh.

## Local API URL

Default API URL:

```text
http://10.0.2.2:8080/
```

This default is for Android emulator testing.

For a real phone on the same Wi-Fi, build with the PC Wi-Fi IP:

```powershell
.\gradlew.bat assembleDebug -PSHIPMATE_API_BASE_URL=http://192.168.0.25:8080/
```

You can also set the environment variable:

```powershell
$env:SHIPMATE_API_BASE_URL='http://192.168.0.25:8080/'
.\gradlew.bat assembleDebug
```
