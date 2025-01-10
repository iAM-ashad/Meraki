package com.iamashad.meraki.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.iamashad.meraki.R
import com.iamashad.meraki.navigation.Screens
import com.iamashad.meraki.utils.LoadImageWithGlide

@Composable
fun SettingsScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    val firstName = user?.displayName?.split(" ")?.firstOrNull()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.onBackground
                        )
                    )
                )
                .padding(bottom = 70.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(bottomStart = 175.dp, bottomEnd = 175.dp),
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LoadImageWithGlide(
                        imageUrl = user?.photoUrl.toString(),
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Hey $firstName! \uD83D\uDC4B",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold, color = Color.White
                        ),
                        textAlign = TextAlign.Center,
                        letterSpacing = 2.sp,
                        overflow = TextOverflow.Clip,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SettingsCard(
                    title = "Log Out",
                    description = "Sign out from your account",
                    gradientColors = listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
                ) {
                    showLogoutDialog = true
                }

                SettingsCard(
                    title = "Delete Account",
                    description = "Permanently delete your account and data",
                    gradientColors = listOf(Color(0xFFFF416C), Color(0xFFFF4B2B))
                ) {
                    showDeleteDialog = true
                }

                SettingsCard(
                    title = "Rate Us",
                    description = "Share your feedback on the Play Store",
                    gradientColors = listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                ) {
                    val playStoreIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.iamashad.meraki")
                    )
                    navController.context.startActivity(playStoreIntent)
                }

                SettingsCard(
                    title = "About Us",
                    description = "Learn more about our mission",
                    gradientColors = listOf(Color(175, 7, 65, 255), Color(210, 152, 172, 255))
                ) {
                    navController.navigate(Screens.ABOUT.name)
                }
            }
        }

        FloatingActionButton(
            onClick = { navController.navigate(Screens.HOME.name) },
            containerColor = Color.Transparent,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(top = 24.dp)
                .scale(.2f)
        ) {
            Icon(
                painter = painterResource(R.drawable.home_123),
                contentDescription = "Go to Home",
                tint = MaterialTheme.colorScheme.background
            )
        }

        if (showLogoutDialog) {
            AlertDialog(onDismissRequest = { showLogoutDialog = false }, confirmButton = {
                Button(onClick = {
                    auth.signOut()
                    navController.navigate(Screens.REGISTER.name)
                }) {
                    Text("Log Out")
                }
            }, dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }, title = { Text("Log Out") }, text = { Text("Are you sure you want to log out?") })
        }

        if (showDeleteDialog) {
            AlertDialog(onDismissRequest = { showDeleteDialog = false },
                confirmButton = {
                    Button(onClick = {
                        user?.delete()?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val userId = user.uid
                                FirebaseFirestore.getInstance().collection("users").document(userId)
                                    .delete()
                                navController.popBackStack()
                            }
                        }
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Delete Account") },
                text = { Text("Are you sure you want to permanently delete your account? This action cannot be undone.") })
        }
    }
}


@Composable
fun SettingsCard(
    title: String, description: String, gradientColors: List<Color>, onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    topEnd = 96.dp, bottomEnd = 96.dp
                )
            )
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(gradientColors)
                )
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title, style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold, color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description, style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.8f)
                    ), modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

