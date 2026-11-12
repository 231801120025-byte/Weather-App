package com.example.practical.model

data class DailyForecast(
    val date: String,
    val maxTempC: Double,
    val minTempC: Double,
    val weatherCode: Int
)

data class WeatherData(
    val cityName: String,
    val lat: Double,
    val lon: Double,
    val currentTempC: Double,
    val windSpeed: Double,
    val weatherCode: Int,
    val dailyForecasts: List<DailyForecast>,
    val isCached: Boolean = false
)

data class FavoriteCity(
    val name: String,
    val lat: Double,
    val lon: Double
)
