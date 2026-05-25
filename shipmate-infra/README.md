# ShipMate Infra

Local development infrastructure for ShipMate.

## Services

- MinIO API: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`
- PostgreSQL: `localhost:5432`

For phone testing on the same Wi-Fi, use the PC Wi-Fi IP instead of `localhost`.

```text
MinIO API: http://{PC_WIFI_IP}:9000
shipmate-api: http://{PC_WIFI_IP}:8080
shipmate-admin: http://{PC_WIFI_IP}:3000
```

## Start

```powershell
Copy-Item .env.example .env
docker compose -f docker-compose.local.yml --env-file .env up -d
```

The `minio-init` service creates this bucket:

```text
shipmate-call-recordings
```

## Object Key Rule

```text
call-recordings/{tenantId}/{userId}/{recordingId}/original.{ext}
```

Example:

```text
call-recordings/local-tenant/local-user/rec_20260525_001/original.m4a
```
