package com.example.practical.utils

import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

object WeatherUtils {

    fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Clear Sky"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45 -> "Fog"
            48 -> "Depositing Rime Fog"
            51 -> "Light Drizzle"
            53 -> "Moderate Drizzle"
            55 -> "Dense Drizzle"
            56 -> "Light Freezing Drizzle"
            57 -> "Dense Freezing Drizzle"
            61 -> "Slight Rain"
            63 -> "Moderate Rain"
            65 -> "Heavy Rain"
            66 -> "Light Freezing Rain"
            67 -> "Heavy Freezing Rain"
            71 -> "Slight Snow Fall"
            73 -> "Moderate Snow Fall"
            75 -> "Heavy Snow Fall"
            77 -> "Snow Grains"
            80 -> "Slight Rain Showers"
            81 -> "Moderate Rain Showers"
            82 -> "Violent Rain Showers"
            85 -> "Slight Snow Showers"
            86 -> "Heavy Snow Showers"
            95 -> "Thunderstorm"
            96 -> "Thunderstorm with Slight Hail"
            99 -> "Thunderstorm with Heavy Hail"
            else -> "Unknown ($code)"
        }
    }

    fun getWeatherEmoji(code: Int): String {
        return when (code) {
            0 -> "☀️"
            1, 2 -> "🌤️"
            3 -> "☁️"
            45, 48 -> "🌫️"
            51, 53, 55, 56, 57 -> "🌧️"
            61, 63, 65, 66, 67 -> "🌧️"
            71, 73, 75, 77 -> "❄️"
            80, 81, 82 -> "🌦️"
            85, 86 -> "🌨️"
            95, 96, 99 -> "🌩️"
            else -> "🌡️"
        }
    }

    fun formatTemperature(tempInC: Double, isCelsius: Boolean): String {
        return if (isCelsius) {
            "${tempInC.roundToInt()}°C"
        } else {
            val f = tempInC * 9.0 / 5.0 + 32.0
            "${f.roundToInt()}°F"
        }
    }

    fun formatDate(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            if (date != null) {
                val outputFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                outputFormat.format(date)
            } else {
                dateStr
            }
        } catch (e: Exception) {
            dateStr
        }
    }
}
