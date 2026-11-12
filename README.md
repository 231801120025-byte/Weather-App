# Weather-App

Android Weather Application built with Kotlin, Firebase Authentication, Fused Location Provider, and Open-Meteo API.

## Features
- **Current Location & 7-Day Forecast**: Open-Meteo Forecast API integration.
- **Firebase Authentication**: Email/Password & Anonymous Guest authentication.
- **Fused Location Provider**: Fetch location using `getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)`.
- **Offline Mode**: Local caching of weather data in `SharedPreferences`.
- **Favourites Management**: Add/Remove favourite cities.
- **Unit Conversion**: °C / °F toggle.
