package com.iamashad.meraki.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.iamashad.meraki.R
import com.iamashad.meraki.ui.theme.bodyFontFamily
import com.iamashad.meraki.ui.theme.displayFontFamily
import com.iamashad.meraki.utils.LoadImageWithGlide

@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeScreenViewModel = hiltViewModel()
) {
    // Observe user data (FirebaseUser) using collectAsState for the StateFlow
    val user by homeViewModel.user.collectAsState()

    // Observe advice LiveData using observeAsState for the LiveData
    val advice by homeViewModel.advice.observeAsState("Loading advice...")

    val photoUrl by homeViewModel.photoUrl.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        MaterialTheme.colorScheme.onPrimaryContainer,
                        MaterialTheme.colorScheme.primaryContainer,
                    )
                )
            )
    ) {
        // Other content is added on top of the gradient background
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(R.drawable.meditation_bg),
                    contentScale = ContentScale.Inside,
                    contentDescription = null,
                    alignment = Alignment.Center
                )
                Card(
                    shape = RoundedCornerShape(corner = CornerSize(20.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    elevation = CardDefaults.cardElevation(10.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        LoadImageWithGlide(
                            imageUrl = photoUrl.toString(),
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable {
                                    // TODO
                                }
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 5.dp))
                        Text(
                            text = user?.displayName ?: "User",
                            fontSize = 16.sp,
                            fontFamily = bodyFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Card(
                        shape = RoundedCornerShape(topStart = 25.dp, bottomEnd = 25.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(10.dp),
                        modifier = Modifier
                            .padding(10.dp)
                            .align(Alignment.Center)
                    ) {
                        Text(
                            text = advice,
                            fontSize = 16.sp,
                            fontFamily = displayFontFamily,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

