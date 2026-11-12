package com.example.practical.utils

import android.content.Context
import android.location.Geocoder
import com.example.practical.model.DailyForecast
import com.example.practical.model.FavoriteCity
import com.example.practical.model.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class WeatherRepository(private val context: Context) {

    private val prefsManager = PreferencesManager(context)

    suspend fun fetchWeather(lat: Double, lon: Double, customCityName: String? = null): Result<WeatherData> {
        return withContext(Dispatchers.IO) {
            val cacheKey = "${String.format(Locale.US, "%.2f", lat)}_${String.format(Locale.US, "%.2f", lon)}"
            try {
                val urlString = "https://api.openmeteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min,weather_code&timezone=auto"
                val url = URL(urlString)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.use { it.readText() }
                    connection.disconnect()

                    // Save cache
                    prefsManager.saveCachedWeather(cacheKey, response)

                    val weatherData = parseWeatherData(response, lat, lon, customCityName, isCached = false)
                    Result.success(weatherData)
                } else {
                    connection.disconnect()
                    loadFromCache(cacheKey, lat, lon, customCityName, "HTTP error $responseCode")
                }
            } catch (e: Exception) {
                loadFromCache(cacheKey, lat, lon, customCityName, e.localizedMessage)
            }
        }
    }

    private fun loadFromCache(
        cacheKey: String,
        lat: Double,
        lon: Double,
        customCityName: String?,
        errorMessage: String?
    ): Result<WeatherData> {
        val cachedJson = prefsManager.getCachedWeather(cacheKey)
        return if (cachedJson != null) {
            try {
                val cachedData = parseWeatherData(cachedJson, lat, lon, customCityName, isCached = true)
                Result.success(cachedData)
            } catch (_: Exception) {
                Result.failure(Exception("Failed to fetch weather: $errorMessage and cache is invalid"))
            }
        } else {
            Result.failure(Exception("Offline & no cached weather available for location"))
        }
    }

    private fun parseWeatherData(
        jsonString: String,
        lat: Double,
        lon: Double,
        customCityName: String?,
        isCached: Boolean
    ): WeatherData {
        val json = JSONObject(jsonString)

        val currentObj = json.getJSONObject("current")
        val currentTemp = currentObj.getDouble("temperature_2m")
        val weatherCode = currentObj.getInt("weather_code")
        val windSpeed = currentObj.getDouble("wind_speed_10m")

        val dailyObj = json.getJSONObject("daily")
        val datesArray = dailyObj.getJSONArray("time")
        val maxTempArray = dailyObj.getJSONArray("temperature_2m_max")
        val minTempArray = dailyObj.getJSONArray("temperature_2m_min")
        val codesArray = if (dailyObj.has("weather_code")) dailyObj.getJSONArray("weather_code") else null

        val dailyForecasts = mutableListOf<DailyForecast>()
        val count = datesArray.length()
        for (i in 0 until count) {
            val date = datesArray.getString(i)
            val maxTemp = maxTempArray.getDouble(i)
            val minTemp = minTempArray.getDouble(i)
            val code = codesArray?.optInt(i, weatherCode) ?: weatherCode

            dailyForecasts.add(DailyForecast(date, maxTemp, minTemp, code))
        }

        val cityName = customCityName ?: getCityNameFromCoords(lat, lon)

        return WeatherData(
            cityName = cityName,
            lat = lat,
            lon = lon,
            currentTempC = currentTemp,
            windSpeed = windSpeed,
            weatherCode = weatherCode,
            dailyForecasts = dailyForecasts,
            isCached = isCached
        )
    }

    private fun getCityNameFromCoords(lat: Double, lon: Double): String {
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val city = address.locality ?: address.subAdminArea ?: address.adminArea
                    val country = address.countryName
                    if (city != null && country != null) {
                        return "$city, $country"
                    } else if (city != null) {
                        return city
                    } else if (country != null) {
                        return country
                    }
                }
            }
        } catch (_: Exception) {
            // Fallthrough to formatted location
        }
        return "Location (${String.format(Locale.US, "%.2f", lat)}, ${String.format(Locale.US, "%.2f", lon)})"
    }

    suspend fun searchCity(query: String): Result<List<FavoriteCity>> {
        return withContext(Dispatchers.IO) {
            try {
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                val urlString = "https://geocoding-api.openmeteo.com/v1/search?name=$encodedQuery&count=5&language=en&format=json"
                val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val jsonStr = reader.use { it.readText() }
                    connection.disconnect()

                    val json = JSONObject(jsonStr)
                    val list = mutableListOf<FavoriteCity>()
                    if (json.has("results")) {
                        val results = json.getJSONArray("results")
                        for (i in 0 until results.length()) {
                            val item = results.getJSONObject(i)
                            val name = item.getString("name")
                            val country = if (item.has("country")) ", " + item.getString("country") else ""
                            val latitude = item.getDouble("latitude")
                            val longitude = item.getDouble("longitude")
                            list.add(FavoriteCity(name + country, latitude, longitude))
                        }
                    }
                    Result.success(list)
                } else {
                    connection.disconnect()
                    Result.failure(Exception("City search failed"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
