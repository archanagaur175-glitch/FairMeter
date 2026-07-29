# FairMeter

An independent, unofficial auto-rickshaw fare meter for Indian cities. Built with Kotlin + Jetpack Compose.

**No affiliation** with any RTA, State Transport Department, or Government of India.

---

## Fare Tariff Sources

| City | Source | Effective | Status |
|------|--------|-----------|--------|
| **Bengaluru** | Karnataka DTA notification | Aug 1, 2025 | Verified Jul 2025 |
| **Mumbai** | Maharashtra Motor Vehicle Dept [tariff card PDF](https://transport.maharashtra.gov.in/Site/Upload/GR/Auto%20Rickshaw%20Tariff%20Card.pdf) | Feb 1, 2025 | Verified Feb 2025 |
| **Delhi** | STA Delhi notification | Jan 9, 2025 | Verified Jan 2025 |
| **Chennai** | TN Transport Dept (2013 — **NOT REVISED SINCE**) | 2013 (last official) | **UNVERIFIED — TODO** |
| **Hyderabad** | Telangana Transport Dept | 2025 | Verified 2025 |

> **Chennai note**: The last official TN government notification dates to 2013 (₹25/1.8km, ₹12/km). Union-unilateral rates from Feb 2025 (₹50/1.8km, ₹18/km) have not been government-notified. The app defaults to 2013 official rates with a loud TODO. **Verify before real-world use.**

All fare figures are **reference estimates subject to official revision**. This app does not process payments or enforce fares.

## Architecture

- **MVVM** with `ViewModel` + `StateFlow` + `SavedStateHandle`
- **Foreground Service** (`START_STICKY`) for GPS tracking via `FusedLocationProviderClient`
- **Kalman filter** path smoothing to reject GPS jitter
- **Accelerometer + GPS fusion** for waiting-time auto-detection
- **Per-tick fare calculation** — night surcharge evaluated each tick, correctly handling boundary crossings
- **6-language TTS** (English, Hindi, Kannada, Tamil, Telugu, Marathi)
- **OSMDroid** for map display (free, no API key)
- **Nominatim** for geocoding (1 req/sec throttled)

## Build

```bash
./gradlew assembleDebug
./gradlew lint
./gradlew testDebugUnitTest
```

CI runs on GitHub Actions (push/PR to `main`).

## Disclaimer

FairMeter is an independent, unofficial tool with no affiliation to any RTA, State Transport Department, or Government of India. All fare figures are reference estimates based on publicly notified tariffs and are subject to official revision. This app does not process payments or enforce fares.
