package com.iamashad.meraki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.iamashad.meraki.navigation.MerakiNavigation
import com.iamashad.meraki.ui.theme.MerakiTheme
import com.iamashad.meraki.ui.theme.ThemePreference
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val isDynamicColorEnabled by ThemePreference.isDynamicColorEnabled(context)
                .collectAsState(initial = false)
            MerakiTheme(
                dynamicColor = isDynamicColorEnabled
            ) {
                MerakiNavigation()
            }
        }
    }
}