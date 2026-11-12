package com.example.practical

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.practical.model.FavoriteCity
import com.example.practical.model.WeatherData
import com.example.practical.ui.FavoritesAdapter
import com.example.practical.ui.ForecastAdapter
import com.example.practical.utils.FirebaseManager
import com.example.practical.utils.PreferencesManager
import com.example.practical.utils.WeatherRepository
import com.example.practical.utils.WeatherUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var firebaseManager: FirebaseManager

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var tvCityName: TextView
    private lateinit var tvCoords: TextView
    private lateinit var tvCurrentEmoji: TextView
    private lateinit var tvCurrentTemp: TextView
    private lateinit var tvCurrentCondition: TextView
    private lateinit var tvWindSpeed: TextView
    private lateinit var tvOfflineBanner: TextView
    private lateinit var btnToggleFavorite: ImageButton
    private lateinit var btnUnitToggle: MaterialButton
    private lateinit var btnAuthStatus: MaterialButton
    private lateinit var btnCurrentLocation: MaterialButton
    private lateinit var btnSearchCity: ImageButton

    private lateinit var rvForecast: RecyclerView
    private lateinit var rvFavorites: RecyclerView
    private lateinit var forecastAdapter: ForecastAdapter
    private lateinit var favoritesAdapter: FavoritesAdapter

    private var currentWeatherData: WeatherData? = null
    private var selectedLat: Double? = null
    private var selectedLon: Double? = null
    private var selectedCityName: String? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            fetchDeviceLocationAndLoadWeather()
        } else {
            Toast.makeText(this, "Location permission denied. Using default location.", Toast.LENGTH_SHORT).show()
            // Default location: London (51.5074, -0.1278)
            loadWeatherForLocation(51.5074, -0.1278, "London (Default)")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        weatherRepository = WeatherRepository(this)
        preferencesManager = PreferencesManager(this)
        firebaseManager = FirebaseManager(this)

        initViews()
        setupAdapters()
        setupListeners()
        updateAuthButtonText()
        updateUnitButtonText()
        refreshFavoritesList()

        checkLocationPermissionAndFetch()
    }

    private fun initViews() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        tvCityName = findViewById(R.id.tvCityName)
        tvCoords = findViewById(R.id.tvCoords)
        tvCurrentEmoji = findViewById(R.id.tvCurrentEmoji)
        tvCurrentTemp = findViewById(R.id.tvCurrentTemp)
        tvCurrentCondition = findViewById(R.id.tvCurrentCondition)
        tvWindSpeed = findViewById(R.id.tvWindSpeed)
        tvOfflineBanner = findViewById(R.id.tvOfflineBanner)
        btnToggleFavorite = findViewById(R.id.btnToggleFavorite)
        btnUnitToggle = findViewById(R.id.btnUnitToggle)
        btnAuthStatus = findViewById(R.id.btnAuthStatus)
        btnCurrentLocation = findViewById(R.id.btnCurrentLocation)
        btnSearchCity = findViewById(R.id.btnSearchCity)

        rvForecast = findViewById(R.id.rvForecast)
        rvFavorites = findViewById(R.id.rvFavorites)
    }

    private fun setupAdapters() {
        forecastAdapter = ForecastAdapter(emptyList(), preferencesManager.isCelsius)
        rvForecast.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvForecast.adapter = forecastAdapter

        favoritesAdapter = FavoritesAdapter(
            favorites = emptyList(),
            onCitySelected = { favorite ->
                selectedCityName = favorite.name
                loadWeatherForLocation(favorite.lat, favorite.lon, favorite.name)
            },
            onRemoveFavorite = { favorite ->
                preferencesManager.removeFavorite(favorite)
                refreshFavoritesList()
                updateFavoriteButtonState()
                Toast.makeText(this, "Removed ${favorite.name} from favourites", Toast.LENGTH_SHORT).show()
            }
        )
        rvFavorites.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvFavorites.adapter = favoritesAdapter
    }

    private fun setupListeners() {
        swipeRefreshLayout.setOnRefreshListener {
            val lat = selectedLat
            val lon = selectedLon
            if (lat != null && lon != null) {
                loadWeatherForLocation(lat, lon, selectedCityName)
            } else {
                checkLocationPermissionAndFetch()
            }
        }

        btnUnitToggle.setOnClickListener {
            preferencesManager.isCelsius = !preferencesManager.isCelsius
            updateUnitButtonText()
            currentWeatherData?.let { displayWeatherData(it) }
        }

        btnCurrentLocation.setOnClickListener {
            selectedCityName = null
            checkLocationPermissionAndFetch()
        }

        btnToggleFavorite.setOnClickListener {
            val data = currentWeatherData ?: return@setOnClickListener
            val city = FavoriteCity(data.cityName, data.lat, data.lon)
            if (preferencesManager.isFavorite(data.cityName, data.lat, data.lon)) {
                preferencesManager.removeFavorite(city)
                Toast.makeText(this, "Removed ${data.cityName} from favourites", Toast.LENGTH_SHORT).show()
            } else {
                val added = preferencesManager.addFavorite(city)
                if (added) {
                    Toast.makeText(this, "Added ${data.cityName} to favourites", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "${data.cityName} is already in favourites", Toast.LENGTH_SHORT).show()
                }
            }
            refreshFavoritesList()
            updateFavoriteButtonState()
        }

        btnAuthStatus.setOnClickListener {
            showAuthDialog()
        }

        btnSearchCity.setOnClickListener {
            showSearchCityDialog()
        }
    }

    private fun checkLocationPermissionAndFetch() {
        val finePerm = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarsePerm = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (finePerm == PackageManager.PERMISSION_GRANTED || coarsePerm == PackageManager.PERMISSION_GRANTED) {
            fetchDeviceLocationAndLoadWeather()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchDeviceLocationAndLoadWeather() {
        swipeRefreshLayout.isRefreshing = true

        val fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            swipeRefreshLayout.isRefreshing = false
            Toast.makeText(this, "Location permission is required to fetch device location.", Toast.LENGTH_SHORT).show()
            loadWeatherForLocation(51.5074, -0.1278, "London (Default)")
            return
        }

        val locationManager = getSystemService(LOCATION_SERVICE) as? android.location.LocationManager
        val isGpsEnabled = locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true

        if (!isGpsEnabled) {
            Toast.makeText(this, "Location/GPS is disabled on device. Please enable GPS.", Toast.LENGTH_LONG).show()
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    selectedCityName = null
                    loadWeatherForLocation(location.latitude, location.longitude, null)
                } else {
                    // Fallback to last known location if getCurrentLocation returns null
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                selectedCityName = null
                                loadWeatherForLocation(lastLoc.latitude, lastLoc.longitude, null)
                            } else {
                                Toast.makeText(this, "Unable to detect GPS location. Using default location.", Toast.LENGTH_SHORT).show()
                                loadWeatherForLocation(51.5074, -0.1278, "London (Default)")
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Location error. Using default location.", Toast.LENGTH_SHORT).show()
                            loadWeatherForLocation(51.5074, -0.1278, "London (Default)")
                        }
                }
            }
            .addOnFailureListener { exception ->
                // Fallback to last known location on failure
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            selectedCityName = null
                            loadWeatherForLocation(lastLoc.latitude, lastLoc.longitude, null)
                        } else {
                            Toast.makeText(this, "Location error: ${exception.localizedMessage}. Using default.", Toast.LENGTH_SHORT).show()
                            loadWeatherForLocation(51.5074, -0.1278, "London (Default)")
                        }
                    }
                    .addOnFailureListener {
                        loadWeatherForLocation(51.5074, -0.1278, "London (Default)")
                    }
            }
    }

    private fun loadWeatherForLocation(lat: Double, lon: Double, customCityName: String?) {
        selectedLat = lat
        selectedLon = lon
        swipeRefreshLayout.isRefreshing = true

        lifecycleScope.launch {
            val result = weatherRepository.fetchWeather(lat, lon, customCityName)
            swipeRefreshLayout.isRefreshing = false
            result.onSuccess { data ->
                currentWeatherData = data
                displayWeatherData(data)
            }.onFailure { exception ->
                Toast.makeText(this@MainActivity, exception.localizedMessage ?: "Failed to load weather", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun displayWeatherData(data: WeatherData) {
        tvCityName.text = data.cityName
        tvCoords.text = "Lat: ${String.format(Locale.US, "%.2f", data.lat)}, Lon: ${String.format(Locale.US, "%.2f", data.lon)}"
        tvCurrentEmoji.text = WeatherUtils.getWeatherEmoji(data.weatherCode)
        tvCurrentTemp.text = WeatherUtils.formatTemperature(data.currentTempC, preferencesManager.isCelsius)
        tvCurrentCondition.text = WeatherUtils.getWeatherDescription(data.weatherCode)
        tvWindSpeed.text = "${data.windSpeed} km/h"

        tvOfflineBanner.visibility = if (data.isCached) View.VISIBLE else View.GONE

        forecastAdapter.updateData(data.dailyForecasts, preferencesManager.isCelsius)
        updateFavoriteButtonState()
    }

    private fun updateFavoriteButtonState() {
        val data = currentWeatherData ?: return
        val isFav = preferencesManager.isFavorite(data.cityName, data.lat, data.lon)
        btnToggleFavorite.setImageResource(
            if (isFav) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off
        )
    }

    private fun refreshFavoritesList() {
        val favorites = preferencesManager.getFavorites()
        favoritesAdapter.updateFavorites(favorites)
    }

    private fun updateUnitButtonText() {
        btnUnitToggle.text = if (preferencesManager.isCelsius) "°C" else "°F"
    }

    private fun updateAuthButtonText() {
        val user = firebaseManager.getCurrentUser()
        if (user != null) {
            val display = if (user.isAnonymous) "Guest User" else user.email ?: "Logged In"
            btnAuthStatus.text = display
        } else {
            btnAuthStatus.text = "Sign In"
        }
    }

    private fun showAuthDialog() {
        val user = firebaseManager.getCurrentUser()
        if (user != null) {
            val display = if (user.isAnonymous) "Signed in as Guest" else "Signed in as ${user.email}"
            AlertDialog.Builder(this)
                .setTitle("Firebase Account")
                .setMessage(display)
                .setPositiveButton("Sign Out") { dialog, _ ->
                    firebaseManager.signOut()
                    updateAuthButtonText()
                    Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .setNegativeButton("Close", null)
                .show()
        } else {
            val options = arrayOf("Sign In with Email", "Register New Account", "Sign In Anonymously (Guest)")
            AlertDialog.Builder(this)
                .setTitle("Firebase Authentication")
                .setItems(options) { dialog, which ->
                    when (which) {
                        0 -> showEmailPasswordDialog(isSignUp = false)
                        1 -> showEmailPasswordDialog(isSignUp = true)
                        2 -> {
                            firebaseManager.signInAnonymously { success, error ->
                                if (success) {
                                    updateAuthButtonText()
                                    Toast.makeText(this, "Signed in anonymously", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this, error ?: "Auth failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showEmailPasswordDialog(isSignUp: Boolean) {
        val layout = layoutInflater.inflate(R.layout.dialog_auth, null)
        val etEmail = layout.findViewById<EditText>(R.id.etEmail)
        val etPassword = layout.findViewById<EditText>(R.id.etPassword)

        val title = if (isSignUp) "Register Account" else "Sign In"

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(layout)
            .setPositiveButton(title) { dialog, _ ->
                val email = etEmail.text.toString().trim()
                val pass = etPassword.text.toString().trim()

                if (email.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(this, "Email and password cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (isSignUp) {
                    firebaseManager.signUpWithEmail(email, pass) { success, error ->
                        if (success) {
                            updateAuthButtonText()
                            Toast.makeText(this, "Account registered & signed in!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, error ?: "Sign up failed", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    firebaseManager.signInWithEmail(email, pass) { success, error ->
                        if (success) {
                            updateAuthButtonText()
                            Toast.makeText(this, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, error ?: "Sign in failed", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSearchCityDialog() {
        val etCity = EditText(this).apply {
            hint = "Enter city name (e.g. Paris, Tokyo)"
            setPadding(32, 24, 32, 24)
        }

        AlertDialog.Builder(this)
            .setTitle("Search & Add City")
            .setView(etCity)
            .setPositiveButton("Search") { dialog, _ ->
                val query = etCity.text.toString().trim()
                if (query.isNotEmpty()) {
                    performCitySearch(query)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performCitySearch(query: String) {
        swipeRefreshLayout.isRefreshing = true
        lifecycleScope.launch {
            val result = weatherRepository.searchCity(query)
            swipeRefreshLayout.isRefreshing = false
            result.onSuccess { cities ->
                if (cities.isEmpty()) {
                    Toast.makeText(this@MainActivity, "No cities found for '$query'", Toast.LENGTH_SHORT).show()
                } else {
                    showCitySearchResults(cities)
                }
            }.onFailure { error ->
                Toast.makeText(this@MainActivity, error.localizedMessage ?: "Search failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCitySearchResults(cities: List<FavoriteCity>) {
        val cityNames = cities.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select a City")
            .setItems(cityNames) { dialog, which ->
                val selected = cities[which]
                selectedCityName = selected.name
                loadWeatherForLocation(selected.lat, selected.lon, selected.name)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
