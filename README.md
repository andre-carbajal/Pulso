# Pulso Backend (Telemetry API)

Reactive backend to collect telemetry events, expose aggregated metrics, and stream new events in real time via
WebSocket.

## What You Can Do With This Service

- Send telemetry events through `POST /ingest`.
- Query aggregated metrics for the last 24 hours with `GET /metrics`.
- Query feature analytics with `GET /feature-stats`.
- Subscribe to live events through `ws://<host>/ws`.
- Check service health with `GET /health`.

## Stack

- Kotlin + Spring Boot WebFlux
- Spring Data R2DBC + PostgreSQL
- TimescaleDB (hypertable for `events`)
- Docker / Docker Compose

## Requirements

### Recommended

- Docker
- Docker Compose

### Local Runtime

- JDK 25
- PostgreSQL with TimescaleDB extension enabled
- Gradle Wrapper (`./gradlew`)

## Configuration

1. Copy the environment template:

```bash
cp .env.example .env
```

2. Available variables:

- `DB_HOST` (default: `localhost`)
- `DB_PORT` (default: `5432`)
- `DB_NAME` (default: `telemetry`)
- `DB_USER` (default: `telemetry`)
- `DB_PASS` (default: `telemetry`)
- `PORT` (default: `8080`)
- `CORS_ALLOWED_ORIGINS` (default: `http://localhost:4173`) - comma-separated origins allowed to call the API from
  browsers.

3. CORS origin examples:

- Single origin: `CORS_ALLOWED_ORIGINS=http://localhost:4173`
- Multiple origins: `CORS_ALLOWED_ORIGINS=http://localhost:4173,http://localhost:3000`

## Run the Project

### Docker (recommended)

```bash
docker compose up --build
```

Service URL: `http://localhost:8080`

Health check:

```bash
curl http://localhost:8080/health
```

### Development Compose (dev container)

```bash
docker compose -f docker-compose.dev.yml up
```

### Local without backend container

1. Start PostgreSQL/TimescaleDB.
2. Export environment variables.
3. Run:

```bash
./gradlew bootRun
```

## API Usage

Local base URL:

- `http://localhost:8080`

### 1) Ingest Event - `POST /ingest`

Registers a telemetry event.

#### Required fields

- `appId`
- `appVersion`
- `os`
- `eventType`
- `sessionId`

#### Example payload

```json
{
  "appId": "desktop-client",
  "appVersion": "1.4.2",
  "os": "macOS",
  "arch": "arm64",
  "eventType": "feature_used",
  "feature": "sync_notes",
  "durationMs": 420,
  "errorType": null,
  "errorMessage": null,
  "sessionId": "sess-abc-123"
}
```

#### cURL

```bash
curl -X POST http://localhost:8080/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "appId":"desktop-client",
    "appVersion":"1.4.2",
    "os":"macOS",
    "arch":"arm64",
    "eventType":"feature_used",
    "feature":"sync_notes",
    "durationMs":420,
    "sessionId":"sess-abc-123"
  }'
```

#### Responses

- `202 Accepted`

```json
{
  "status": "accepted"
}
```

- `400 Bad Request` (validation)

```json
{
  "errors": [
    "appId: must not be blank"
  ]
}
```

### 2) Query Metrics - `GET /metrics`

Returns aggregates for the last 24 hours:

- `executions24h`: total event count.
- `topFeatures`: top `feature` values for `feature_used` events.
- `errors`: count by `errorType` for `error` events.
- `byOs`: distribution by operating system.

#### cURL

```bash
curl http://localhost:8080/metrics
```

#### Example response

```json
{
  "executions24h": 1280,
  "topFeatures": [
    {
      "feature": "sync_notes",
      "total": 320
    }
  ],
  "errors": [
    {
      "errorType": "TimeoutError",
      "total": 12
    }
  ],
  "byOs": [
    {
      "os": "macOS",
      "total": 700
    },
    {
      "os": "Windows",
      "total": 580
    }
  ]
}
```

### 3) Service Status - `GET /health`

Returns API health and connected WebSocket clients.

#### cURL

```bash
curl http://localhost:8080/health
```

#### Example response

```json
{
  "status": "up",
  "ws_clients": 2
}
```

### 4) Feature Analytics - `GET /feature-stats`

Returns aggregated usage stats by feature for a selected time range, optionally filtered by project (`appId`).

#### Query params

| Param   | Type   | Required | Default           | Allowed values / example |
|---------|--------|---------:|-------------------|--------------------------|
| `appId` | string |       No | `null` (all apps) | `desktop-client`         |
| `range` | string |       No | `24h`             | `1h`, `6h`, `24h`, `7d`  |

#### cURL

```bash
curl "http://localhost:8080/feature-stats?range=24h&appId=desktop-client"
```

#### Example response

```json
{
  "range": "24h",
  "appId": "desktop-client",
  "summary": {
    "totalCalls": 8241,
    "avgDurationMs": 384.2,
    "activeFeatures": 11,
    "totalFeatures": 14
  },
  "features": [
    {
      "feature": "sync_notes",
      "calls": 2340,
      "avgDurationMs": 420.1,
      "p95DurationMs": 980.0,
      "errorRate": 0.004,
      "uniqueSessions": 841,
      "trendPct": 18.2,
      "hourly": [
        20,
        15,
        18,
        40,
        85,
        120,
        140,
        160,
        155,
        130,
        100,
        80
      ]
    }
  ]
}
```

#### Response semantics

- `summary.totalCalls`: sum of all `feature_used` events in the selected range/filter.
- `summary.avgDurationMs`: weighted average using only events with non-null `durationMs`.
- `summary.activeFeatures`: features with at least one event in the selected range/filter.
- `summary.totalFeatures`: distinct features in the selected range/filter.
- `features[].errorRate`: `errors / calls` (0 to 1).
- `features[].trendPct`: percent change vs previous period of same duration; can be `null` when previous period has zero
  calls.
- `features[].hourly`: fixed-length time series:
    - `1h`: 12 points (5-minute buckets)
    - `6h`: 12 points (30-minute buckets)
    - `24h`: 12 points (2-hour buckets)
    - `7d`: 7 points (1-day buckets)

#### Error response

- `400 Bad Request` for invalid `range`:

```json
{
  "error": "Invalid range '2h'. Allowed values: 1h, 6h, 24h, 7d"
}
```

### 5) Real-Time Stream - WebSocket `/ws`

Connect a WebSocket client to:

- `ws://localhost:8080/ws`

When a new event arrives through `/ingest`, the backend emits:

```json
{
  "type": "new_event",
  "payload": {
    "id": 123,
    "appId": "desktop-client",
    "appVersion": "1.4.2",
    "os": "macOS",
    "arch": "arm64",
    "eventType": "feature_used",
    "feature": "sync_notes",
    "durationMs": 420,
    "errorType": null,
    "errorMessage": null,
    "sessionId": "sess-abc-123",
    "time": "2026-05-03T18:20:11.123Z"
  }
}
```

Example with `wscat`:

```bash
npx wscat -c ws://localhost:8080/ws
```

## Recommended Usage Flow

1. Start the backend.
2. Open a WebSocket connection to `/ws`.
3. Send events to `/ingest`.
4. Query `/metrics` to validate aggregations.
5. Use `/health` for operational checks.

## Observability Endpoints

Spring Actuator exposes:

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`

## Database Initialization

Database initialization runs through Flyway migrations from `classpath:db/migration`.
The current base migration is `src/main/resources/db/migration/V1__init_events_schema.sql`, which creates:

- Create the `timescaledb` extension (if missing).
- Create the `events` table.
- Create a hypertable on the `time` column.
- Create indexes used by metrics queries.

## Quick Troubleshooting

- If DB connection fails, verify `DB_HOST`, `DB_PORT`, `DB_USER`, and `DB_PASS`.
- If Timescale setup fails, ensure the extension is installed in PostgreSQL.
- If no WebSocket messages appear, verify `/ingest` returns `202`.
- If port `8080` is busy, change `PORT` or your compose mapping.
