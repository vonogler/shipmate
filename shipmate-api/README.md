# ShipMate API

Java Spring Boot API server for local upload testing.

## Current Scope

This server provides the first call recording upload test flow:

```text
Android -> shipmate-api -> MinIO -> shipmate-api -> shipmate-admin
```

The current implementation uses an in-memory repository. PostgreSQL will be connected later.

## Java / Build

The local machine currently has Java 25 installed, so the project targets Java 25.

The project uses Gradle Kotlin DSL.

This project does not include a Gradle wrapper yet. Use an installed Gradle distribution or import the project in IntelliJ as a Gradle project.

## Local Environment

Start MinIO first from `shipmate-infra`.

Environment example:

```text
.env.example
```

For phone testing on the same Wi-Fi, replace `localhost` with your PC Wi-Fi IP:

```powershell
$env:SHIPMATE_MINIO_ENDPOINT='http://192.168.0.25:9000'
$env:SHIPMATE_MINIO_PUBLIC_ENDPOINT='http://192.168.0.25:9000'
gradle bootRun
```

For Android emulator testing, keep the API server's internal MinIO endpoint on the host and expose presigned URLs through the emulator host alias:

```powershell
$env:SHIPMATE_MINIO_ENDPOINT='http://localhost:9000'
$env:SHIPMATE_MINIO_PUBLIC_ENDPOINT='http://10.0.2.2:9000'
gradle bootRun
```

`SHIPMATE_MINIO_ENDPOINT` is used by `shipmate-api` itself. `SHIPMATE_MINIO_PUBLIC_ENDPOINT` is used when generating presigned URLs for Android.

## APIs

```http
POST /api/call-recordings/upload-sessions
POST /api/call-recordings/{recordingId}/complete
GET  /api/call-recordings/recent
GET  /api/health
```

Request samples are available in:

```text
requests/call-recordings.local.http
```

## MinIO Bucket

```text
shipmate-call-recordings
```
