package ie.tus.safeskies

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import ie.tus.finalyearproject.Bars.MyBottomBar
import ie.tus.finalyearproject.Bars.MyTopBar
import ie.tus.finalyearproject.ViewModels.ViewModel

@Composable
fun settingsPage(navController: NavController) {
    var isDarkModeEnabled by remember { mutableStateOf(false) }
    var isVibrationEnabled by remember { mutableStateOf(true) }
    var isPushNotificationsEnabled by remember { mutableStateOf(true) }
    var isLocationAccessEnabled by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showChangeProfilePictureDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var currentProfilePictureUrl by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        MyTopBar(
            navController = navController,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 110.dp, bottom = 56.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                settingProfilePicture(
                    title = "Profile Picture",
                    description = "Change your profile picture",
                    onClick = { showChangeProfilePictureDialog = true }
                )
            }
            item {
                settingTextButton(
                    title = "Change Password",
                    description = "Update your password",
                    onClick = { showChangePasswordDialog = true }
                )
            }
            item {
                settingTextButton(
                    title = "Sign Out",
                    description = "Sign out of your account",
                    onClick = { ViewModel().signOut(navController) },
                    showEditButton = false,
                    buttonText = "Sign Out"
                )
            }
            item {
                settingTextButton(
                    title = "Delete Account",
                    description = "Permanently delete your account",
                    onClick = { showDeleteDialog = true },
                    showEditButton = false,
                    buttonText = "Delete Account"
                )
            }
            item {
                settingSwitch(
                    title = "Dark Mode",
                    description = "Enable or disable dark mode.",
                    isChecked = isDarkModeEnabled,
                    onToggle = { isDarkModeEnabled = it },
                    context = LocalContext.current,
                    settingKey = "dark_mode_enabled"
                )
            }
            item {
                settingSwitch(
                    title = "Vibration",
                    description = "Enable or disable vibration.",
                    isChecked = isVibrationEnabled,
                    onToggle = { isVibrationEnabled = it },
                    context = LocalContext.current,
                    settingKey = "vibration_enabled"
                )
            }
            item {
                settingSwitch(
                    title = "Location Access",
                    description = "Allow this app to access your location",
                    isChecked = isLocationAccessEnabled,
                    onToggle = { isLocationAccessEnabled = it },
                    context = LocalContext.current,
                    settingKey = "location_access_enabled"
                )
            }
            item {
                settingSwitch(
                    title = "Push Notifications",
                    description = "Enable or disable push notifications",
                    isChecked = isPushNotificationsEnabled,
                    onToggle = { isPushNotificationsEnabled = it },
                    context = LocalContext.current,
                    isNotificationSetting = true,
                    settingKey = "push_notifications_enabled"
                )
            }
        }

        MyBottomBar(
            navController = navController,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        )

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(text = "Delete Account") },
                text = { Text("Are you sure you want to permanently delete your account? This action cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        ViewModel().deleteAccount(navController)
                        showDeleteDialog = false
                    }) {
                        Text("Delete", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    if (showChangePasswordDialog) {
        changePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onPasswordChanged = {
                Toast.makeText(context, "Password updated successfully", Toast.LENGTH_SHORT).show()
                showChangePasswordDialog = false
            },
            onError = { errorMsg ->
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showChangeProfilePictureDialog) {
        AlertDialog(
            onDismissRequest = { showChangeProfilePictureDialog = false },
            title = { Text("Change Profile Picture") },
            text = {
                Column {
                    clickableProfileIcon(
                        onProfilePictureChanged = { newUrl ->
                            currentProfilePictureUrl = newUrl
                            Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
                            showChangeProfilePictureDialog = false
                        },
                        onError = { errorMsg ->
                            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showChangeProfilePictureDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun changePasswordDialog(
    onDismiss: () -> Unit,
    onPasswordChanged: () -> Unit,
    onError: (String) -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Change Password",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "Enter your new password:",
                    style = MaterialTheme.typography.bodyMedium
                )
                androidx.compose.material3.OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user != null && newPassword.isNotBlank()) {
                        user.updatePassword(newPassword)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    onPasswordChanged()
                                } else {
                                    onError(task.exception?.message ?: "Password update failed")
                                }
                            }
                    } else {
                        onError("Please enter a valid password.")
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = "Change", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = MaterialTheme.colorScheme.onSurface)
            }
        },
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
fun clickableProfileIcon(
    onProfilePictureChanged: (String) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val firebaseUser = FirebaseAuth.getInstance().currentUser

    var profilePictureUrl by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null && firebaseUser != null) {
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("profilePictures/${firebaseUser.uid}.jpg")
                storageRef.putFile(uri)
                    .continueWithTask { task ->
                        if (!task.isSuccessful) {
                            task.exception?.let { throw it }
                        }
                        storageRef.downloadUrl
                    }
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val downloadUrl = task.result.toString()
                            profilePictureUrl = downloadUrl
                            FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(firebaseUser.uid)
                                .update("profilePictureUrl", downloadUrl)
                                .addOnSuccessListener {
                                    onProfilePictureChanged(downloadUrl)
                                }
                                .addOnFailureListener { e ->
                                    onError(e.message ?: "Failed to update profile picture")
                                }
                        } else {
                            onError("Image upload failed")
                        }
                    }
            } else {
                onError("No image selected")
            }
        }
    )

    IconButton(
        onClick = { imagePickerLauncher.launch("image/*") },
        modifier = Modifier
            .size(48.dp)
            .background(
                color = Color.LightGray,
                shape = CircleShape
            )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (profilePictureUrl != null) {
                Image(
                    painter = rememberImagePainter(profilePictureUrl),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Default Profile Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun settingProfilePicture(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
            }
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile Picture"
                )
            }
        }
        Divider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
fun settingTextButton(
    title: String,
    description: String,
    onClick: () -> Unit,
    showEditButton: Boolean = true,
    buttonText: String = "Edit"
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
            }
            if (showEditButton) {
                TextButton(
                    onClick = onClick,
                    modifier = Modifier.background(Color.Transparent)
                ) {
                    Text(buttonText, color = Color.Black)
                }
            } else {
                TextButton(onClick = onClick) {
                    Text(buttonText, color = Color.Black)
                }
            }
        }
        Divider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
fun settingSwitch(
    title: String,
    description: String,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit,
    context: Context,
    isNotificationSetting: Boolean = false,
    settingKey: String
) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
        }
    )

    val sharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    var localIsChecked by remember { mutableStateOf(sharedPreferences.getBoolean(settingKey, isChecked)) }

    var isFirstLoad by remember { mutableStateOf(true) }

    LaunchedEffect(localIsChecked) {
        if (isFirstLoad) {
            isFirstLoad = false
            return@LaunchedEffect
        }

        onToggle(localIsChecked)

        if (localIsChecked != isChecked) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(100)
            }
        }

        sharedPreferences.edit().putBoolean(settingKey, localIsChecked).apply()

        if (localIsChecked && isNotificationSetting && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                }
                else -> {
                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
            }
            Switch(
                checked = localIsChecked,
                onCheckedChange = { newState ->
                    localIsChecked = newState
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF4285F4),
                    checkedTrackColor = Color(0xFF4285F4).copy(alpha = 0.5f),
                    uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
            )
        }
        Divider(modifier = Modifier.padding(vertical = 8.dp))
    }
}