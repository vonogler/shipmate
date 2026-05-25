# ShipMate Admin

Internal administrator platform for ShipMate upload verification.

## Current Scope

The first screen verifies Android call recording uploads:

```text
Android -> shipmate-api -> MinIO -> shipmate-api -> shipmate-admin
```

## Stack

- React
- TypeScript
- Vite
- Ant Design

## Local Setup

```powershell
Copy-Item .env.example .env
npm install
npm run dev
```

Default URL:

```text
http://localhost:3000
```

Default API URL:

```text
http://localhost:8080
```

Change `.env` when the API runs on another machine or IP:

```text
VITE_SHIPMATE_API_BASE_URL=http://192.168.0.25:8080
```

## Current Screen

```text
Call Recordings
```

It calls:

```http
GET /api/call-recordings/recent
```

Displayed fields:

- recordingId
- fileName
- status
- source
- fileSize
- uploadedAt / createdAt
- objectKey
- errorCode
