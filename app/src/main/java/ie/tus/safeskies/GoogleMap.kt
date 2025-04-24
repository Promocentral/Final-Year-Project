package ie.tus.safeskies

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import ie.tus.finalyearproject.Bars.MyBottomBar
import ie.tus.finalyearproject.Bars.MyTopBar
import ie.tus.finalyearproject.ViewModels.ViewModel

@Composable
fun googleMapsPage(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        MyTopBar(
            navController = navController,
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            //not an error just stating that it requires a certain API level - Luke Kelly
            ViewModel().googleMapsDisplaying()
        }

        MyBottomBar(
            navController = navController,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
