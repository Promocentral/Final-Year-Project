package ie.tus.safeskies

import android.Manifest
import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import ie.tus.finalyearproject.Bars.MyBottomBar
import ie.tus.finalyearproject.Bars.MyTopBar
import ie.tus.finalyearproject.Notifications.NotificationAlerts
import ie.tus.finalyearproject.ViewModels.ViewModel
import kotlinx.coroutines.delay
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

data class WeatherData(
    val temperature: Int,
    val description: String,
    val humidity: Int,
    val windSpeed: Double,
    val cityName: String,
    val feelsLike: Int,
    val tempMin: Int,
    val tempMax: Int,
    val pressure: Int
)

data class ForecastData(
    val daily: List<DailyForecast>,
    val hourly: List<HourlyForecast>
)

data class DailyForecast(
    val dateTime: Long,
    val temperature: Int,
    val description: String
)

data class HourlyForecast(
    val dateTime: Long,
    val temperature: Int,
    val description: String,
    val pressure: Int
)

@Composable
fun homePage(navController: NavController, justLoggedIn: Boolean = false) {

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("SMSPermission", "SEND_SMS permission granted")
        } else {
            Log.e("SMSPermission", "SEND_SMS permission denied")
        }
    }
    LaunchedEffect(Unit) {
        smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
    }

    val context = LocalContext.current
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val userID = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val currentLocation = remember { mutableStateOf(LatLng(0.0, 0.0)) }
    val currentAddress = remember { mutableStateOf("Fetching address...") }

    val weatherData = remember { mutableStateOf<WeatherData?>(null) }
    val forecastData = remember { mutableStateOf<ForecastData?>(null) }
    val isLocationPermissionGranted = remember { mutableStateOf(false) }
    val isLoading = remember { mutableStateOf(true) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isLocationPermissionGranted.value = isGranted
    }

    LaunchedEffect(justLoggedIn) {
        if (justLoggedIn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            locationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    LaunchedEffect(isLocationPermissionGranted.value) {
        if (isLocationPermissionGranted.value) {
            //not an error just stating to remember this requires permission - Luke Kelly
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLocation.value = LatLng(it.latitude, it.longitude)
                    fetchWeatherData(
                        latitude = it.latitude,
                        longitude = it.longitude,
                        context = context
                    ) { weather ->
                        weatherData.value = weather
                        isLoading.value = false
                    }
                    fetchWeatherForecast(
                        latitude = it.latitude,
                        longitude = it.longitude,
                        apiKey = "ddcc1247455316987d35c01c6223435e"
                    ) { forecast ->
                        forecastData.value = forecast
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(5000)
        isLoading.value = false
    }

    LaunchedEffect(weatherData.value, forecastData.value, currentLocation.value) {
        val current = weatherData.value
        val nextForecast = forecastData.value?.hourly?.firstOrNull()
        if (current != null && nextForecast != null && userID.isNotEmpty()) {
            val address = getAddressForLatLng(context, currentLocation.value)
            currentAddress.value = address
            Log.d("HomePage", "Current address updated to: $address")
            if (shouldSendNotification(context)) {
                checkDangerousWeather(current, nextForecast, context, userID, currentLocation.value)
                updateLastNotifiedTime(context)
            } else {
                Log.d("checkDangerousWeather", "Notification suppressed; not enough time elapsed.")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (isLoading.value) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                weatherData.value?.let { data ->
                    currentWeatherCard(data)
                }
                Spacer(modifier = Modifier.height(24.dp))
                forecastData.value?.let { data ->
                    twoRowHourlyForecastSection(data.hourly)
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        MyTopBar(
            navController = navController,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        )
        MyBottomBar(
            navController = navController,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        )
    }
}

fun fetchWeatherData(
    latitude: Double,
    longitude: Double,
    context: Context,
    onResult: (WeatherData?) -> Unit
) {
    val apiKey = "ddcc1247455316987d35c01c6223435e"
    val url = "https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&appid=$apiKey&units=metric"
    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("WeatherData", "API call failed: ${e.message}")
            onResult(null)
        }
        override fun onResponse(call: Call, response: Response) {
            Log.d("WeatherData", "Current Weather response code: ${response.code}")
            if (response.isSuccessful) {
                val body = response.body?.string()
                Log.d("WeatherData", "API response body: $body")
                try {
                    val jsonObject = JSONObject(body ?: "")
                    val mainObj = jsonObject.getJSONObject("main")
                    val temperature = mainObj.getDouble("temp").roundToInt()
                    val feelsLike = mainObj.getDouble("feels_like").roundToInt()
                    val tempMin = mainObj.getDouble("temp_min").roundToInt()
                    val tempMax = mainObj.getDouble("temp_max").roundToInt()
                    val pressure = mainObj.getInt("pressure")
                    val humidity = mainObj.getInt("humidity")
                    val description = jsonObject.getJSONArray("weather")
                        .getJSONObject(0)
                        .getString("description")
                    val windSpeed = jsonObject.getJSONObject("wind")
                        .optDouble("speed", 0.0)
                    val cityName = jsonObject.optString("name", "Unknown")
                    val weatherData = WeatherData(
                        temperature = temperature,
                        description = description,
                        humidity = humidity,
                        windSpeed = windSpeed,
                        cityName = cityName,
                        feelsLike = feelsLike,
                        tempMin = tempMin,
                        tempMax = tempMax,
                        pressure = pressure
                    )
                    onResult(weatherData)
                } catch (e: Exception) {
                    Log.e("WeatherData", "JSON Parsing error: ${e.message}")
                    onResult(null)
                }
            } else {
                Log.e("WeatherData", "API response unsuccessful: ${response.code}")
                onResult(null)
            }
        }
    })
}

fun fetchWeatherForecast(
    latitude: Double,
    longitude: Double,
    apiKey: String,
    onResult: (ForecastData?) -> Unit
) {
    Log.d("ForecastData", "Fetching 3-hour forecast for lat=$latitude, lon=$longitude")
    val url = "https://api.openweathermap.org/data/2.5/forecast?lat=$latitude&lon=$longitude&appid=$apiKey&units=metric"
    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("ForecastData", "Forecast API call failed: ${e.message}")
            onResult(null)
        }
        override fun onResponse(call: Call, response: Response) {
            Log.d("ForecastData", "Forecast response code: ${response.code}")
            if (response.isSuccessful) {
                val body = response.body?.string()
                Log.d("ForecastData", "Forecast response body: $body")
                try {
                    val jsonObject = JSONObject(body ?: "")
                    val listArray = jsonObject.getJSONArray("list")
                    val hourlyList = mutableListOf<HourlyForecast>()
                    val limit = if (listArray.length() < 8) listArray.length() else 8
                    for (i in 0 until limit) {
                        val entry = listArray.getJSONObject(i)
                        val dt = entry.getLong("dt")
                        val temp = entry.getJSONObject("main").getDouble("temp").roundToInt()
                        val pressure = entry.getJSONObject("main").getInt("pressure")
                        val weatherArr = entry.getJSONArray("weather")
                        val description = weatherArr.getJSONObject(0).getString("description")
                        hourlyList.add(
                            HourlyForecast(
                                dateTime = dt,
                                temperature = temp,
                                description = description,
                                pressure = pressure
                            )
                        )
                    }
                    val forecastData = ForecastData(daily = listOf(), hourly = hourlyList)
                    onResult(forecastData)
                } catch (e: Exception) {
                    Log.e("ForecastData", "JSON Parsing error: ${e.message}")
                    onResult(null)
                }
            } else {
                Log.e("ForecastData", "Forecast API response unsuccessful: ${response.code}")
                onResult(null)
            }
        }
    })
}

@Composable
fun currentWeatherCard(data: WeatherData) {
    Card(
        modifier = Modifier
            .padding(start = 46.dp, end = 46.dp, top = 120.dp)
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp)
    ) {
        val cardBackgroundColor = when {
            data.description.contains("clear sky", true) -> Brush.verticalGradient(
                colors = listOf(Color(0xFFFFF59D), Color(0xFFFFB74D))
            )
            data.description.contains("rain", true) -> Brush.verticalGradient(
                colors = listOf(Color(0xFF2196F3), Color(0xFF64B5F6))
            )
            data.description.contains("cloud", true) -> Brush.verticalGradient(
                colors = listOf(Color(0xFFE0E0E0), Color(0xFFB0BEC5))
            )
            data.description.contains("snow", true) -> Brush.verticalGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFFBBDEFB))
            )
            data.description.contains("thunderstorm", true) -> Brush.verticalGradient(
                colors = listOf(Color(0xFF4A148C), Color(0xFF7B1FA2))
            )
            else -> Brush.verticalGradient(
                colors = listOf(Color(0xFF2E2E2E), Color(0xFF616161))
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBackgroundColor)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = data.cityName,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                val weatherIcon = when {
                    data.description.contains("clear sky", true) -> Icons.Default.WbSunny
                    data.description.contains("few clouds", true) -> Icons.Default.WbCloudy
                    data.description.contains("scattered clouds", true) -> Icons.Default.CloudQueue
                    data.description.contains("broken clouds", true) -> Icons.Default.Cloud
                    data.description.contains("overcast clouds", true) -> Icons.Default.Cloud
                    data.description.contains("drizzle", true) -> Icons.Default.Grain
                    data.description.contains("rain", true) -> Icons.Default.Grain
                    data.description.contains("thunderstorm", true) -> Icons.Default.Bolt
                    data.description.contains("snow", true) -> Icons.Default.AcUnit
                    data.description.contains("mist", true) -> Icons.Default.WaterDrop
                    data.description.contains("fog", true) -> Icons.Default.WaterDrop
                    data.description.contains("haze", true) -> Icons.Default.WaterDrop
                    data.description.contains("smoke", true) -> Icons.Default.SmokingRooms
                    data.description.contains("dust", true) -> Icons.Default.Landscape
                    data.description.contains("sand", true) -> Icons.Default.Landscape
                    data.description.contains("ash", true) -> Icons.Default.Warning
                    data.description.contains("tornado", true) -> Icons.Default.Tornado
                    data.description.contains("squall", true) -> Icons.Default.Air
                    else -> Icons.Default.HelpOutline
                }
                Icon(
                    imageVector = weatherIcon,
                    contentDescription = "Weather Icon",
                    modifier = Modifier
                        .size(64.dp)
                        .padding(end = 16.dp),
                    tint = Color.White
                )
                Column {
                    Text(
                        text = "${data.temperature}°C",
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White
                    )
                    Text(
                        text = "Feels like: ${data.feelsLike}°C",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }
            }
            Text(
                text = data.description.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Min: ${data.tempMin}°C",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Text(
                    text = "Max: ${data.tempMax}°C",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Humidity: ${data.humidity}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Text(
                    text = "Pressure: ${data.pressure} hPa",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
            Text(
                text = "Wind: ${(data.windSpeed * 3.6).roundToInt()} km/h",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

@Composable
fun twoRowHourlyForecastSection(hourlyList: List<HourlyForecast>) {
    if (hourlyList.isEmpty()) return

    val chunkedList = hourlyList.take(8).chunked(4)
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF2196F3), Color(0xFF64B5F6))
                    )
                )
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Next 24 Hours",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                chunkedList.forEachIndexed { index, chunk ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (index < chunkedList.lastIndex) 12.dp else 0.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        chunk.forEach { hourly ->
                            twoRowHourlyItem(hourly)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun twoRowHourlyItem(hourly: HourlyForecast) {
    val hourString = remember(hourly.dateTime) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(hourly.dateTime * 1000))
    }
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .size(width = 70.dp, height = 160.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = hourString,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            val weatherIcon = when {
                hourly.description.contains("clear", true) -> Icons.Default.WbSunny
                hourly.description.contains("cloud", true) -> Icons.Default.Cloud
                hourly.description.contains("rain", true) -> Icons.Default.Umbrella
                hourly.description.contains("thunderstorm", true) -> Icons.Default.Bolt
                hourly.description.contains("snow", true) -> Icons.Default.AcUnit
                else -> Icons.Default.HelpOutline
            }
            Icon(
                imageVector = weatherIcon,
                contentDescription = "Hourly Weather Icon",
                modifier = Modifier.size(36.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${hourly.temperature}°C",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = hourly.description.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }
    }
}

fun checkDangerousWeather(
    current: WeatherData,
    nextForecast: HourlyForecast,
    context: Context,
    userID: String,
    currentLocation: LatLng
) {
    try {
        Log.d("checkDangerousWeather", "Current: ${current.description}, Temp: ${current.temperature}°C, Wind: ${(current.windSpeed * 3.6).roundToInt()} km/h, Pressure: ${current.pressure} hPa")
        Log.d("checkDangerousWeather", "Forecast: ${nextForecast.description}, Temp: ${nextForecast.temperature}°C, Pressure: ${nextForecast.pressure} hPa")

        val dangerousKeywords = listOf("tornado", "thunderstorm", "hail", "blizzard", "extreme rain")
        val currentIsDangerous = dangerousKeywords.any { current.description.contains(it, ignoreCase = true) }

        val temperatureSpikeThreshold = 5
        val tempDifference = abs(nextForecast.temperature - current.temperature)
        val forecastSpike = tempDifference >= temperatureSpikeThreshold

        val currentWindKmh = (current.windSpeed * 3.6).roundToInt()
        val dangerousWindThreshold = 0
        val highWind = currentWindKmh >= dangerousWindThreshold

        val pressureDifference = abs(current.pressure - nextForecast.pressure)
        val pressureDropThreshold = 10
        val dangerousPressureDrop = pressureDifference >= pressureDropThreshold

        Log.d("checkDangerousWeather", "Conditions: currentIsDangerous=$currentIsDangerous, forecastSpike=$forecastSpike, highWind=$highWind, dangerousPressureDrop=$dangerousPressureDrop")

        val localMessages = mutableListOf<String>()
        if (currentIsDangerous) localMessages.add("Dangerous weather: ${current.description}")
        if (forecastSpike) localMessages.add("Temperature spike (${current.temperature}°C to ${nextForecast.temperature}°C)")
        if (highWind) localMessages.add("High wind speed: $currentWindKmh km/h")
        if (dangerousPressureDrop) localMessages.add("Pressure change: $pressureDifference hPa")

        if (localMessages.isNotEmpty()) {
            val localMessage = localMessages.joinToString(separator = ", ") + ". Please take precautions!"
            Log.d("checkDangerousWeather", "Triggering local notification with message: $localMessage")
            try {
                val notificationAlerts = NotificationAlerts(context)
                notificationAlerts.triggerNotification("Weather Alert", localMessage)
                Log.d("checkDangerousWeather", "Local notification sent successfully")
            } catch (nfe: Exception) {
                Log.e("checkDangerousWeather", "Error sending local notification: ${nfe.message}")
            }
        }

        val address = getAddressForLatLng(context, currentLocation)
        val userName = FirebaseAuth.getInstance().currentUser?.displayName ?: "User"
        val contactMessage = "$userName is currently experiencing dangerous weather at $address. Please contact them immediately."
        Log.d("checkDangerousWeather", "Contact notification message: $contactMessage")

        try {
            ViewModel().notifyContactsForDangerousWeather(context, userID, contactMessage)
            Log.d("checkDangerousWeather", "Contact notifications triggered successfully")
        } catch (e: Exception) {
            Log.e("checkDangerousWeather", "Error notifying contacts: ${e.message}")
        }
    } catch (e: Exception) {
        Log.e("checkDangerousWeather", "Error in checkDangerousWeather: ${e.message}")
    }
}

fun getAddressForLatLng(context: Context, latLng: LatLng): String {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
        if (addresses != null && addresses.isNotEmpty()) {
            addresses[0].getAddressLine(0)
        } else {
            "Address not found"
        }
    } catch (e: Exception) {
        Log.e("Geocoder", "Error fetching address: ${e.message}")
        "Address unavailable"
    }
}

private const val NOTIFICATION_PREFS = "weather_alerts_prefs"
private const val LAST_NOTIFIED_KEY = "last_notified_time"
private const val TEN_MINUTES_MILLIS = 0L

fun shouldSendNotification(context: Context): Boolean {
    val prefs = context.getSharedPreferences(NOTIFICATION_PREFS, Context.MODE_PRIVATE)
    val lastNotified = prefs.getLong(LAST_NOTIFIED_KEY, 0L)
    val currentTime = System.currentTimeMillis()
    return currentTime - lastNotified >= TEN_MINUTES_MILLIS
}

fun updateLastNotifiedTime(context: Context) {
    val prefs = context.getSharedPreferences(NOTIFICATION_PREFS, Context.MODE_PRIVATE)
    prefs.edit().putLong(LAST_NOTIFIED_KEY, System.currentTimeMillis()).apply()
}