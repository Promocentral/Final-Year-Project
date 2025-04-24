package ie.tus.finalyearproject.Bars

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sos
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.DarkGray
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun MyBottomBar(navController: NavController, modifier: Modifier){
    val homePageActive = navController.currentDestination?.route?.startsWith("homePage") == true
    val googleMapsPageActive = navController.currentDestination?.route == "googleMapsPage"
    val emergencyPageActive = navController.currentDestination?.route == "emergencyPage"
    val settingsPageActive = navController.currentDestination?.route == "settingsPage"
    BottomAppBar(
        modifier = modifier
            .background(color = Color.LightGray)
            .height(70.dp),
        containerColor = DarkGray,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    navController.navigate("homePage?justLoggedIn=true")
                },
                modifier = Modifier
                    .weight(1f)
            ) {
                val backgroundColor by animateColorAsState(
                    if (homePageActive) Color.Black.copy(alpha = 0.2f) else Color.Transparent
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(width = 36.dp, height = 22.dp)
                            .background(color = backgroundColor, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Home,
                            contentDescription = "Home Page",
                            tint = Color.White
                        )
                    }
                    Text(
                        "Home",
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    navController.navigate("googleMapsPage")
                },
                modifier = Modifier
                    .weight(1f)
            ) {
                val backgroundColor by animateColorAsState(
                    if (googleMapsPageActive) Color.Black.copy(alpha = 0.2f) else Color.Transparent
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(width = 36.dp, height = 22.dp)
                            .background(color = backgroundColor, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = "Maps Page",
                            tint = Color.White
                        )
                    }
                    Text(
                        "Google Maps",
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    navController.navigate("emergencyPage")
                },
                modifier = Modifier
                    .weight(1f)
            ) {
                val backgroundColor by animateColorAsState(
                    if (emergencyPageActive) Color.Black.copy(alpha = 0.2f) else Color.Transparent
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(width = 36.dp, height = 22.dp)
                            .background(color = backgroundColor, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Sos,
                            contentDescription = "Emergency Page",
                            tint = Color.White
                        )
                    }
                    Text(
                        "Emergency",
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        color = Color.White
                    )
                }
            }

            IconButton(
                onClick = {
                    navController.navigate("settingsPage")
                },
                modifier = Modifier
                    .weight(1f)
            ) {
                val backgroundColor by animateColorAsState(
                    if (settingsPageActive) Color.Black.copy(alpha = 0.2f) else Color.Transparent
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(width = 36.dp, height = 22.dp)
                            .background(color = backgroundColor, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings Page",
                            tint = Color.White
                        )
                    }
                    Text(
                        "Settings",
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        color = Color.White
                    )
                }
            }
        }
    }
}