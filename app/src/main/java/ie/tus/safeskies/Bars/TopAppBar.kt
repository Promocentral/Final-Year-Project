package ie.tus.finalyearproject.Bars

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopBar(modifier: Modifier = Modifier, navController: NavController) {
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    var userProfilePictureUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(firebaseUser) {
        if (firebaseUser == null) {
            isLoading = false
        } else {
            try {
                val document = db.collection("users")
                    .document(firebaseUser.uid)
                    .get()
                    .await()
                if (document.exists()) {
                    userProfilePictureUrl = document.getString("profilePictureUrl")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.DarkGray),
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 40.dp),
                contentAlignment = Alignment.Center
            ) {
            }
        },
        navigationIcon = {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = "Hamburger Icon",
                tint = Color.White,
                modifier = Modifier.size(35.dp)
            )
        },
        actions = {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(6.dp)
                    )
                }
                userProfilePictureUrl != null -> {
                    Image(
                        painter = rememberImagePainter(userProfilePictureUrl),
                        contentDescription = "User Profile Picture",
                        modifier = Modifier
                            .size(35.dp)
                            .clip(CircleShape)
                            .clickable {
                                navController.navigate("accountDetailsPage")
                            },
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = "Account Icon",
                        tint = Color.White,
                        modifier = Modifier
                            .size(35.dp)
                            .clickable {
                                navController.navigate("accountDetailsPage")
                            }
                    )
                }
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}