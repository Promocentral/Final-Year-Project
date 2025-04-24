package ie.tus.finalyearproject.ViewModels

import android.Manifest
import android.provider.Settings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class ViewModel {

    @Composable
    fun staySignedIn(navController: NavController) {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        var hasNavigated by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay(2000)
            if (currentUser != null) {
                navController.navigate("homePage?justLoggedIn=true") {
                    popUpTo("staySignedIn") { inclusive = true }
                }
            } else {
                navController.navigate("login") {
                    popUpTo("staySignedIn") { inclusive = true }
                }
            }
            hasNavigated = true
        }

        if (!hasNavigated) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    fun validLoginEmail(email: String): Boolean {
        val emailRegex = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        return email.isNotEmpty() && email.matches(emailRegex)
    }

    fun validLoginPassword(password: String): Boolean {
        val loginPasswordRegex =
            Regex("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@\\\$!%*?&\\-])[A-Za-z\\d@\\\$!%*?&\\-]{8,}$")
        //password requires th following
        // 8 characters minimum
        // 1 digit minimum
        // 1 special character minimum
        // 1 letter minimum
        return loginPasswordRegex.matches(password)
    }

    fun validLoginAttempt(email: String, password: String): Boolean {
        return validLoginEmail(email) && validLoginPassword(password)
    }

    fun signInWithFirebase(email: String, password: String, navController: NavController) {
        val firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    navController.navigate("homePage")
                } else {
                }
            }
    }

    fun validSignUpEmail(email: String): Boolean {
        val emailRegex = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        return email.isNotEmpty() && email.matches(emailRegex)
    }

    fun validSignUpPassword(password: String): Boolean {
        val signUpPasswordRegex =
            Regex("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@\\\$!%*?&\\-])[A-Za-z\\d@\\\$!%*?&\\-]{8,}$")
        //password requires th following
        // 8 characters minimum
        // 1 digit minimum
        // 1 special character minimum
        // 1 letter minimum
        return signUpPasswordRegex.matches(password)
    }

    fun validSignUp(email: String, password: String, confirmPassword: String): Boolean {
        return validSignUpEmail(email) && validSignUpPassword(password) && password == confirmPassword
    }

    fun signUpWithFirebase(
        email: String,
        password: String,
        fullName: String,
        phoneNumber: String,
        dateOfBirth: String,
        selectedProfilePicture: Uri?,
        navController: NavController
    ) {
        val firebaseAuth = FirebaseAuth.getInstance()

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser

                    var profilePictureUrl: String? = null
                    if (selectedProfilePicture != null) {
                        val storageRef = FirebaseStorage.getInstance().reference
                        val profilePictureRef = storageRef.child("profile_pictures/${user?.uid}")
                        val uploadTask = profilePictureRef.putFile(selectedProfilePicture)

                        uploadTask.addOnSuccessListener {
                            profilePictureRef.downloadUrl.addOnSuccessListener { uri ->
                                profilePictureUrl = uri.toString()

                                Log.d("ProfilePictureUrl", "Profile picture URL: $profilePictureUrl")

                                saveUserDataToFirestore(
                                    user?.uid ?: "",
                                    fullName,
                                    email,
                                    phoneNumber,
                                    dateOfBirth,
                                    profilePictureUrl,
                                    navController
                                )
                            }
                        }.addOnFailureListener { exception ->
                            Log.e("SignUp", "Error uploading profile picture: ${exception.message}")
                            saveUserDataToFirestore(
                                user?.uid ?: "",
                                fullName,
                                email,
                                phoneNumber,
                                dateOfBirth,
                                null,
                                navController
                            )
                        }
                    } else {
                        saveUserDataToFirestore(
                            user?.uid ?: "",
                            fullName,
                            email,
                            phoneNumber,
                            dateOfBirth,
                            null,
                            navController
                        )
                    }
                } else {
                    Log.e("SignUp", "Sign up failed: ${task.exception?.message}")
                }
            }
    }

    private fun saveUserDataToFirestore(
        userId: String,
        fullName: String,
        email: String,
        phoneNumber: String,
        dateOfBirth: String,
        profilePictureUrl: String?,
        navController: NavController
    ) {
        val userData = hashMapOf(
            "userId" to userId,
            "fullName" to fullName,
            "email" to email,
            "phoneNumber" to phoneNumber,
            "dateOfBirth" to dateOfBirth,
            "profilePictureUrl" to profilePictureUrl,
        )

        val database = FirebaseFirestore.getInstance()
        database.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener {
                Log.d("Firestore", "User data saved successfully")
                navController.navigate("login")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error saving user data: $e")
            }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @Composable
    fun googleMapsDisplaying() {
        val context = LocalContext.current
        val cameraPos = rememberCameraPositionState {
            position = CameraPosition(LatLng(0.0, 0.0), 17f, 0f, 0f)
        }
        val currentLocation = remember { mutableStateOf(LatLng(0.0, 0.0)) }
        val currentAddress = remember { mutableStateOf("Fetching address...") }
        val isLocationPermissionGranted = remember { mutableStateOf(false) }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                isLocationPermissionGranted.value = true
                startLocationUpdates(context, currentLocation, cameraPos) { address ->
                    currentAddress.value = address
                }
            } else {
                isLocationPermissionGranted.value = false
            }
        }

        LaunchedEffect(Unit) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (isLocationPermissionGranted.value) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPos,
                    properties = MapProperties(
                        isMyLocationEnabled = true,
                        mapType = MapType.NORMAL
                    )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-80).dp)
                ) {
                    addressCard(address = currentAddress.value)
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Location permission is required to display the map.")
                }
            }
        }
    }

    @Composable
    fun addressCard(address: String) {
        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .wrapContentSize()
                .background(Color.Transparent)
        ) {
            Row(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val lines = address.split(", ")
                Column {
                    for (line in lines) {
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun startLocationUpdates(
        context: Context,
        currentLocation: MutableState<LatLng>,
        cameraPos: CameraPositionState,
        onAddressFound: (String) -> Unit
    ) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        val locationRequest = com.google.android.gms.location.LocationRequest().apply {
            interval = 1000L
            fastestInterval = 500L
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    currentLocation.value = latLng
                    Handler(Looper.getMainLooper()).post {
                        CoroutineScope(Dispatchers.Main).launch {
                            cameraPos.animate(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
                        }
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val geocoder = Geocoder(context, Locale.getDefault())
                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            val addressLine = if (!addresses.isNullOrEmpty()) {
                                addresses[0].getAddressLine(0)
                            } else {
                                "Address not found"
                            }
                            withContext(Dispatchers.Main) {
                                onAddressFound(addressLine)
                            }
                        } catch (e: Exception) {
                            Log.e("Geocoder", "Error fetching address: ${e.message}")
                            withContext(Dispatchers.Main) {
                                onAddressFound("Address unavailable")
                            }
                        }
                    }
                }
            }
        }

        //again, not an error just requiring a permission - Luke Kelly
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun signOut(navController: NavController) {
        val firebaseCurrentUser = FirebaseAuth.getInstance()
        firebaseCurrentUser.signOut()
        navController.navigate("login")
    }

    fun deleteAccount(navController: NavController) {
        val firebaseCurrentUser = FirebaseAuth.getInstance().currentUser
        firebaseCurrentUser?.delete()
        navController.navigate("login")
    }

    fun changePassword(navController: NavController) {

    }

    fun forgottenPassword(navController: NavController, email: String) {
        if (email.isBlank()) {
            Toast.makeText(navController.context, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(navController.context, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
            return
        }

        val auth = FirebaseAuth.getInstance()
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Toast.makeText(navController.context, "Password reset email sent to $email.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(navController.context, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    fun signInWithGoogle(navController: NavController){

    }

    fun signInWithPhone(navController: NavController){

    }

    fun sendEmailLink(navController: NavController){

    }

    fun handleNotificationPermission(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) {
            guideToNotificationSettings(context)
        }
    }

    fun guideToNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        context.startActivity(intent)
    }

    fun notifyContactsForDangerousWeather(context: Context, userID: String, message: String) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("emergencyContacts")
            .whereEqualTo("userID", userID)
            .get()
            .addOnSuccessListener { docs ->
                val smsManager = SmsManager.getDefault()
                for (doc in docs) {
                    val phoneNumber = doc.getString("phoneNumber")
                    if (!phoneNumber.isNullOrEmpty()) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                            Log.e("notifyContacts", "SEND_SMS permission not granted!")
                        } else {
                            try {
                                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                                Log.d("notifyContacts", "SMS sent to $phoneNumber")
                            } catch (e: Exception) {
                                Log.e("notifyContacts", "Error sending SMS: ${e.message}")
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("notifyContacts", "Failed to fetch contacts: ${e.message}")
            }
    }
}