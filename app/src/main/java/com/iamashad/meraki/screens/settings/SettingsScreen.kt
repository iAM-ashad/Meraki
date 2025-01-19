package com.iamashad.meraki.screens.settings

import android.util.Log
import android.widget.TimePicker
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.iamashad.meraki.R
import com.iamashad.meraki.navigation.Screens
import com.iamashad.meraki.screens.chatbot.ChatViewModel
import com.iamashad.meraki.utils.LocalDimens
import com.iamashad.meraki.utils.PromptEnableNotifications
import com.iamashad.meraki.utils.ProvideDimens
import com.iamashad.meraki.utils.scheduleDailyReminderAt


@Composable
fun SettingsScreen(
    navController: NavController,
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    val firstName = user?.displayName?.split(" ")?.firstOrNull()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    var selectedTime by remember { mutableStateOf("9:00 AM") }
    val context = LocalContext.current

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showChatDeleteDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    PromptEnableNotifications(context)

    ProvideDimens(screenWidth, screenHeight) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.inversePrimary)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Account Settings",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            SettingsSection(title = "General Settings") {
                SettingToggle(icon = R.drawable.ic_dark_mode, title = "Dark Mode") {
                    //TODO
                }
                SettingItem(icon = R.drawable.ic_notifications, title = "Set Reminder") {
                    showTimePickerDialog = true
                }
                SettingItem(icon = R.drawable.ic_share, title = "Invite Friends") {
                    //TODO
                }
                SettingItem(icon = R.drawable.ic_feedback, title = "Submit Feedback") {
                    //TODO
                }
            }

            SettingsSection(title = "Security & Privacy") {
                SettingItem(icon = R.drawable.ic_support, title = "Help Center") {
                    //TODO
                }
                SettingItem(icon = R.drawable.ic_delete_history, title = "Clear Chat History") {
                    showChatDeleteDialog = true
                }
            }

            SettingsSection(title = "Danger Zone", danger = true) {
                SettingItem(
                    icon = R.drawable.ic_delete_account,
                    title = "Close Account"
                ) {
                    showDeleteDialog = true
                }
            }

            SettingsSection(title = "Log Out") {
                SettingItem(icon = R.drawable.ic_logout, title = "Log Out") {
                    showLogoutDialog = true
                }
            }
        }
    }
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            confirmButton = {
                Button(onClick = {
                    auth.signOut()
                    navController.navigate(Screens.REGISTER.name)
                }) {
                    Text("Log Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Log Out") },
            text = { Text("Are you sure you want to log out?") })
    }

    if (showDeleteDialog) {
        AlertDialog(onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                Button(onClick = {
                    user?.delete()?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = user.uid
                            FirebaseFirestore.getInstance().collection("users")
                                .document(userId)
                                .delete()
                            navController.navigate(Screens.REGISTER.name)
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
    if (showChatDeleteDialog) {
        AlertDialog(onDismissRequest = { showChatDeleteDialog = false },
            confirmButton = {
                Button(onClick = {
                    chatViewModel.clearChatHistory()
                    navController.popBackStack()
                }) {
                    Text("Clear Chats")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChatDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Clear Chat History") },
            text = { Text("Are you sure you want to permanently clear your chats? This action cannot be undone.") })
    }
    if (showTimePickerDialog) {
        TimePickerDialog(
            initialTime = selectedTime,
            onTimeSelected = { time ->
                Log.d("SettingsScreen", "Time selected: $time") // Log before calling the function
                selectedTime = time
                showTimePickerDialog = false

                // Schedule notification at the selected time
                scheduleDailyReminderAt(
                    context = context,
                    time = time
                )
            },
            onDismiss = {
                showTimePickerDialog = false
            }
        )
    }

}

@Composable
fun TimePickerDialog(
    initialTime: String,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val is24HourFormat = android.text.format.DateFormat.is24HourFormat(context)

    // State to track the selected hour and minute
    var selectedHour by remember { mutableStateOf(9) }
    var selectedMinute by remember { mutableStateOf(0) }

    // Parse the initialTime string
    LaunchedEffect(initialTime) {
        val parts = initialTime.split(":").mapNotNull { it.toIntOrNull() }
        if (parts.size == 2) {
            selectedHour = parts[0]
            selectedMinute = parts[1]
        }
    }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Select Reminder Time") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { context ->
                        TimePicker(context).apply {
                            setIs24HourView(is24HourFormat)
                            hour = selectedHour
                            minute = selectedMinute
                        }
                    },
                    modifier = Modifier.wrapContentSize(),
                    update = { timePicker ->
                        timePicker.hour = selectedHour
                        timePicker.minute = selectedMinute
                        timePicker.setOnTimeChangedListener { _, hour, minute ->
                            selectedHour = hour
                            selectedMinute = minute
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                    Log.d("TimePickerDialog", "Time selected: $formattedTime")
                    onTimeSelected(formattedTime)
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Cancel")
            }
        }
    )
}


@Composable
fun SettingsSection(
    title: String,
    danger: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val dimens = LocalDimens.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimens.paddingSmall, horizontal = dimens.paddingMedium)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(vertical = dimens.paddingSmall)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(dimens.cornerRadius)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inversePrimary),
            elevation = CardDefaults.cardElevation(dimens.elevation)
        ) {
            Column(
                modifier = Modifier.padding(vertical = dimens.paddingSmall),
                content = content
            )
        }
    }
}

@Composable
fun SettingItem(
    @DrawableRes icon: Int,
    title: String,
    additionalInfo: String? = null,
    onClick: () -> Unit
) {
    val dimens = LocalDimens.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = dimens.paddingMedium)
            .height(dimens.paddingSmall * 7),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(dimens.avatarSize / 12)
        )

        Spacer(modifier = Modifier.width(dimens.paddingMedium))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.weight(1f)
        )

        if (additionalInfo != null) {
            Text(
                text = additionalInfo,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Gray
                )
            )
        }
    }
}

@Composable
fun SettingToggle(
    @DrawableRes icon: Int,
    title: String,
    onToggle: (Boolean) -> Unit
) {
    var toggled by remember { mutableStateOf(false) }
    val dimens = LocalDimens.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.paddingMedium)
            .height(dimens.paddingSmall * 7),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(dimens.avatarSize / 12)
        )

        Spacer(modifier = Modifier.width(dimens.paddingMedium))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = toggled,
            onCheckedChange = {
                toggled = it
                onToggle(it)
            },
            modifier = Modifier.scale(.75f),
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.background)
        )
    }
}


