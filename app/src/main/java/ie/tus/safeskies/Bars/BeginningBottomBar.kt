package ie.tus.finalyearproject.Bars

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun beginningBar(navController: NavController, modifier: Modifier) {
    val loginPageActive = navController.currentDestination?.route == "login"
    val signUpPageActive = navController.currentDestination?.route == "signUp"

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val isPhone = screenWidth < 600

    if (isPhone && !isLandscape) {
        BottomAppBar(
            modifier = modifier
                .background(color = Color.DarkGray)
                .height(70.dp),
            containerColor = Color.DarkGray,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        navController.navigate("login")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clickable {  }
                        .fillMaxHeight()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val iconModifier = if (loginPageActive) {
                            Modifier.size(40.dp)
                        } else {
                            Modifier.size(35.dp)
                        }

                        Icon(
                            imageVector = Icons.Default.AccountBox,
                            contentDescription = "Account Box Icon",
                            tint = Color.White,
                            modifier = iconModifier
                        )
                        Text(
                            "Login",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        navController.navigate("signUp")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(70.dp)
                        .clickable {  }
                        .fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {

                        val iconModifier = if (signUpPageActive) {
                            Modifier.size(40.dp)
                        } else {
                            Modifier.size(35.dp)
                        }

                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Account Circle Icon",
                            tint = Color.White,
                            modifier = iconModifier
                        )
                        Text(
                            "Sign Up",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}