package com.iamashad.meraki.screens.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.iamashad.meraki.R
import com.iamashad.meraki.navigation.Screens

@Composable
fun AboutScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            )
            .padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "About This App", style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground
            ), textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "A Message From the Developer",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = """
                        Hi there! 👋
                        
                        Thank you for using this app. It’s been a journey of passion and learning for me as a solo developer. My goal has always been to create a tool that makes a difference in your life, even in the smallest way.

                        I want to let you know that this app was built with good intentions and a lot of care, but as a one-person team, there might be areas where things could be improved. For that, I sincerely apologize and want to assure you that I’m always listening to your feedback and committed to making this app better.

                        If there’s something you love about the app, or something you think could be better, I’d love to hear from you. Together, we can make this app a more helpful companion on your journey.
                        
                        Thank you for your trust and support—it means the world to me. 🙏
                    """.trimIndent(), style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ), textAlign = TextAlign.Start, lineHeight = 24.sp
                )
            }
        }

        FloatingActionButton(
            onClick = { navController.navigate(Screens.HOME.name) },
            containerColor = Color.Transparent,
            modifier = Modifier
                .padding(top = 24.dp)
                .scale(.8f)
        ) {
            Icon(
                painter = painterResource(R.drawable.home_123),
                contentDescription = "Go to Home",
                tint = MaterialTheme.colorScheme.background
            )
        }
    }
}
