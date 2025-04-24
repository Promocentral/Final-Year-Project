package ie.tus.safeskies.Bars

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountAppBar(modifier: Modifier = Modifier, navController: NavController) {
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    var userProfilePictureUrl by remember { mutableStateOf<String?>(null) }

    if (firebaseUser != null) {
        val userId = firebaseUser.uid
        val db = FirebaseFirestore.getInstance()

        LaunchedEffect(userId) {
            db.collection("users").document(userId).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    userProfilePictureUrl = document.getString("profilePictureUrl")
                }
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
                imageVector = Icons.Outlined.ArrowBackIosNew,
                contentDescription = "Hamburger Icon",
                tint = Color.White,
                modifier = Modifier.size(35.dp).clickable { navController.popBackStack() },
            )
        },
        modifier = modifier.fillMaxWidth()
    )
}
