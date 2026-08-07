# Concurrency study with JMeter

## Prerequisites

1. App running on `http://localhost:8080` (Java 21)
2. PostgreSQL + Redis up
3. A CLIENT user (e.g. `client1` / `password`) and a provider with working hours covering the test slot
4. Apache JMeter 5.5+ installed (`jmeter` on PATH)

Edit [user.properties](user.properties) with your credentials, `providerId`, and slot times.

## Locking study (graphs)

```bash
mkdir -p results
jmeter -n -t booking-locking-study.jmx -q user.properties -l results/locking.jtl -e -o results/locking-report
```

Open `results/locking-report/index.html` for throughput / latency charts.

## Threading study (graphs)

```bash
jmeter -n -t booking-threading-study.jmx -q user.properties -l results/threading.jtl -e -o results/threading-report
```

Open `results/threading-report/index.html`.

## Tips for thesis figures

- Run each plan with increasing `threads` (10, 50, 100, 200) by editing `user.properties`
- Same-slot requests exercise lock contention: **HTTP 409 = booking conflict (expected)**, not an auth failure
- Plans treat **200 and 409 as successful samples** so the dashboard error % reflects real failures only
- After locking runs, call `GET /api/concurrency/double-bookings?providerId=...` (with JWT) for correctness — especially for `unsafe`
- Clear overlapping bookings between runs, or use `POST /api/concurrency/race` for an in-process correctness experiment
