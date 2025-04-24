package ie.tus.safeskies

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import ie.tus.safeskies.Bars.AccountAppBar

@Composable
fun accountDetailsPage(navController: NavController){
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
    AccountAppBar(
        navController = navController,
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
    )
        }
}