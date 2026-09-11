# Water0 - Hydration Tracker

Open-source Android app for tracking hydration with personalized recommendations.

## Features (v1.0)
- Log water intake with quick-add buttons
- Daily progress ring visualization
- Personalized daily goal based on weight, activity, climate
- Local SQLite storage (Room)
- Offline-first, no tracking
- Adaptive notifications (v1: fixed interval)

## Tech Stack
- Kotlin, Jetpack Compose, Material3
- Room Database, Hilt DI, DataStore
- WorkManager for notifications
- Health Connect integration (API 34+)
- ComposeCharts for history visualization

## Building
```bash
./gradlew assembleDebug
```

## License
Apache 2.0 - see LICENSE file