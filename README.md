# Booking concurrency study

Backend-oriented **final-year (licență) project**: a minimal appointment booking API used as the workload for a **Java concurrency and threading performance study**.

**Spring Boot 3**, **Java 21**, **PostgreSQL**. Booking is implemented with multiple locking strategies and threading models; results are measured with an in-process race harness and graphed with **JMeter**.

---

## Study overview

### Part A — Locking / correctness

| Endpoint | Strategy |
|----------|----------|
| `POST /api/appointments/book/unsafe` | Check-then-act (racy baseline) |
| `POST /api/appointments/book/synchronized` | Per-provider `synchronized` |
| `POST /api/appointments/book/reentrant-lock` | Per-provider `ReentrantLock` |
| `POST /api/appointments/book/pessimistic` | JPA `PESSIMISTIC_WRITE` on provider |
| `POST /api/appointments/book/optimistic` | `@Version` on provider |

### Part B — Threading / performance

Correct booking underneath (`ReentrantLock`):

| Endpoint | Model |
|----------|-------|
| `POST /api/appointments/book/blocking` | Servlet thread |
| `POST /api/appointments/book/executor` | Fixed `ExecutorService` |
| `POST /api/appointments/book/completable-future` | `CompletableFuture` |
| `POST /api/appointments/book/virtual-thread` | Virtual threads (Java 21) |

### Measurement helpers

| Endpoint | Purpose |
|----------|---------|
| `GET /api/concurrency/metrics` | Latency / conflict counters |
| `POST /api/concurrency/metrics/reset` | Clear metrics |
| `GET /api/concurrency/double-bookings?providerId=` | Overlapping `BOOKED` pairs |
| `POST /api/concurrency/race?strategy=...&concurrency=50` | Same-slot correctness race |

Supporting auth (for JWT / JMeter): `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/provider/register`.

---

## Request body (book endpoints)

```json
{
  "providerId": 5,
  "startTime": "2026-08-03T10:00:00",
  "endTime": "2026-08-03T10:30:00",
  "type": "GENERAL"
}
```

JWT required (`CLIENT`). Slot must be inside provider working hours. Conflicts → **HTTP 409**.

---

## Experiments

### Correctness

```http
POST /api/concurrency/race?strategy=UNSAFE&concurrency=50
Authorization: Bearer <token>
Content-Type: application/json

{ "providerId": 5, "startTime": "2026-08-03T10:00:00", "endTime": "2026-08-03T10:30:00", "type": "GENERAL" }
```

Expect double-bookings mainly for `UNSAFE`.

### Performance (JMeter)

See [benchmarks/jmeter/README.md](benchmarks/jmeter/README.md).

```bash
cd benchmarks/jmeter
.\run-jmeter.ps1 locking
.\run-jmeter.ps1 threading
```

---

## Tech stack

| Layer | Technology |
|-------|------------|
| Backend | Spring Boot 3.3, Java 21 |
| Security | Spring Security, JWT |
| Database | PostgreSQL |
| Load / graphs | Apache JMeter |
| Build | Maven |

---

## Run locally

1. PostgreSQL with database `booking_concurrency_study`
2. Configure `src/main/resources/application.yml`
3. `mvn spring-boot:run` (JDK 21)
4. Register a CLIENT + PROVIDER and insert working hours before experiments
