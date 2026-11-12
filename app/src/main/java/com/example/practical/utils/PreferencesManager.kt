package com.example.practical.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.practical.model.FavoriteCity
import org.json.JSONArray
import org.json.JSONObject

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_CELSIUS = "key_is_celsius"
        private const val KEY_FAVORITES = "key_favorites"
        private const val KEY_CACHE_PREFIX = "key_cache_"
    }

    var isCelsius: Boolean
        get() = prefs.getBoolean(KEY_IS_CELSIUS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_IS_CELSIUS, value).apply()
        }

    fun getFavorites(): List<FavoriteCity> {
        val jsonStr = prefs.getString(KEY_FAVORITES, null) ?: return emptyList()
        val list = mutableListOf<FavoriteCity>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    FavoriteCity(
                        name = obj.getString("name"),
                        lat = obj.getDouble("lat"),
                        lon = obj.getDouble("lon")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun addFavorite(city: FavoriteCity): Boolean {
        val current = getFavorites().toMutableList()
        if (current.any { it.name.equals(city.name, ignoreCase = true) || (Math.abs(it.lat - city.lat) < 0.05 && Math.abs(it.lon - city.lon) < 0.05) }) {
            return false // already exists
        }
        current.add(city)
        saveFavorites(current)
        return true
    }

    fun removeFavorite(city: FavoriteCity) {
        val current = getFavorites().filterNot {
            it.name.equals(city.name, ignoreCase = true) || (Math.abs(it.lat - city.lat) < 0.05 && Math.abs(it.lon - city.lon) < 0.05)
        }
        saveFavorites(current)
    }

    fun isFavorite(cityName: String, lat: Double, lon: Double): Boolean {
        return getFavorites().any {
            it.name.equals(cityName, ignoreCase = true) || (Math.abs(it.lat - lat) < 0.05 && Math.abs(it.lon - lon) < 0.05)
        }
    }

    private fun saveFavorites(list: List<FavoriteCity>) {
        val array = JSONArray()
        for (city in list) {
            val obj = JSONObject()
            obj.put("name", city.name)
            obj.put("lat", city.lat)
            obj.put("lon", city.lon)
            array.put(obj)
        }
        prefs.edit().putString(KEY_FAVORITES, array.toString()).apply()
    }

    fun saveCachedWeather(cacheKey: String, jsonResponse: String) {
        prefs.edit().putString(KEY_CACHE_PREFIX + cacheKey, jsonResponse).apply()
    }

    fun getCachedWeather(cacheKey: String): String? {
        return prefs.getString(KEY_CACHE_PREFIX + cacheKey, null)
    }
}
