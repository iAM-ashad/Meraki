package com.iamashad.meraki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.iamashad.meraki.navigation.MerakiNavigation
import com.iamashad.meraki.screens.register.RegisterScreen
import com.iamashad.meraki.ui.theme.MerakiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MerakiTheme {
                MerakiNavigation()
//                val navController = rememberNavController()
//                RegisterScreen(navController)
            }
        }
    }
}