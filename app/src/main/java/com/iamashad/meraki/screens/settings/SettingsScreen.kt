package com.iamashad.meraki.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.TimePicker
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.iamashad.meraki.R
import com.iamashad.meraki.components.showToast
import com.iamashad.meraki.navigation.Screens
import com.iamashad.meraki.screens.chatbot.ChatViewModel
import com.iamashad.meraki.ui.theme.ThemePreference
import com.iamashad.meraki.utils.LocalDimens
import com.iamashad.meraki.utils.PromptEnableNotifications
import com.iamashad.meraki.utils.scheduleDailyReminderAt
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SettingsScreen(
    navController: NavController,
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    val auth = FirebaseAuth.getInstance()
    var selectedTime by remember { mutableStateOf("9:00 AM") }
    val context = LocalContext.current
    val isDynamicColorEnabled by ThemePreference.isDynamicColorEnabled(context)
        .collectAsState(initial = false)
    val coroutineScope = rememberCoroutineScope()
    val dimens = LocalDimens.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showChatDeleteDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var isCheckedLocal by remember { mutableStateOf(isDynamicColorEnabled) }
    LaunchedEffect(isDynamicColorEnabled) {
        isCheckedLocal = isDynamicColorEnabled
    }
    var showNotificationPrompt by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.onBackground)
                .padding(vertical = dimens.paddingMedium),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = Modifier.height(dimens.paddingMedium))

        SettingsSection(title = "General Settings") {
            SettingToggle(
                icon = R.drawable.ic_dynamic_colors,
                title = "Dynamic Color Mode",
                isChecked = isCheckedLocal
            ) { isDynamic ->
                isCheckedLocal = isDynamic
                coroutineScope.launch {
                    ThemePreference.setDynamicColor(context, isDynamic)
                }
            }
            SettingItem(icon = R.drawable.ic_notifications, title = "Set Reminder") {
                showNotificationPrompt = true
                showTimePickerDialog = true
            }
            SettingItem(icon = R.drawable.ic_share, title = "Invite Friends") {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Check out this amazing app! Download it from: [App Link Placeholder Until App is Published]"
                    )
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share App"))
            }

            SettingItem(icon = R.drawable.ic_feedback, title = "Submit Feedback") {
                // TODO: Implement feedback functionality
            }
        }

        Spacer(modifier = Modifier.height(dimens.paddingMedium))

        SettingsSection(title = "Security & Privacy") {
            SettingItem(icon = R.drawable.ic_connect, title = "Connect With Us") {
                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:wevolveapps.inc@gmail.com")
                    putExtra(Intent.EXTRA_SUBJECT, "Feedback or Query")
                }
                context.startActivity(Intent.createChooser(emailIntent, "Send Email"))
            }
            SettingItem(icon = R.drawable.ic_delete_history, title = "Clear Chat History") {
                showChatDeleteDialog = true
            }
        }

        Spacer(modifier = Modifier.height(dimens.paddingMedium))

        SettingsSection(title = "Danger Zone", danger = true) {
            SettingItem(icon = R.drawable.ic_delete_account, title = "Close Account") {
                showDeleteDialog = true
            }
        }

        Spacer(modifier = Modifier.height(dimens.paddingMedium))

        SettingsSection(title = "Log Out") {
            SettingItem(icon = R.drawable.ic_logout, title = "Log Out") {
                showLogoutDialog = true
            }
        }
    }

    if (showNotificationPrompt) {
        PromptEnableNotifications(context)
    }

    if (showLogoutDialog) {
        ConfirmationDialog(
            title = "Log Out",
            text = "Are you sure you want to log out?",
            confirmText = "Log Out",
            onConfirm = {
                auth.signOut()
                navController.navigate(Screens.REGISTER.name)
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    if (showDeleteDialog) {
        ConfirmationDialog(
            title = "Delete Account",
            text = "Are you sure you want to permanently delete your account? This action cannot be undone.",
            confirmText = "Delete",
            onConfirm = {
                performAccountDeletion(auth, context, navController)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showChatDeleteDialog) {
        ConfirmationDialog(
            title = "Clear Chat History",
            text = "Are you sure you want to permanently clear your chats? This action cannot be undone.",
            confirmText = "Clear",
            onConfirm = {
                chatViewModel.clearChatHistory()
                navController.popBackStack()
            },
            onDismiss = { showChatDeleteDialog = false }
        )
    }

    if (showTimePickerDialog) {
        CustomTimePickerDialog(
            initialTime = selectedTime,
            onTimeSelected = { time ->
                selectedTime = time
                showTimePickerDialog = false

                scheduleDailyReminderAt(
                    context = context,
                    time = time
                )
            },
            onDismiss = { showTimePickerDialog = false }
        )
    }
}

@Composable
fun ConfirmationDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text(title) },
        text = { Text(text) }
    )
}

fun performAccountDeletion(auth: FirebaseAuth, context: Context, navController: NavController) {
    val googleSignInClient = GoogleSignIn.getClient(
        context,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    )

    googleSignInClient.silentSignIn().addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val googleAccount = task.result
            val idToken = googleAccount?.idToken
            if (idToken != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.currentUser?.let { user ->
                    user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                        if (reauthTask.isSuccessful) {
                            user.delete().addOnCompleteListener { deleteTask ->
                                if (deleteTask.isSuccessful) {
                                    showToast(
                                        context,
                                        "Account successfully deleted."
                                    )
                                    googleSignInClient.signOut()
                                    navController.navigate(Screens.REGISTER.name) {
                                        popUpTo(Screens.SETTINGS.name) {
                                            inclusive = true
                                        }
                                    }
                                } else {
                                    showToast(
                                        context,
                                        "Failed to delete account: ${deleteTask.exception?.localizedMessage}"
                                    )
                                }
                            }
                        } else {
                            showToast(
                                context,
                                "Re-authentication failed: ${reauthTask.exception?.localizedMessage}"
                            )
                        }
                    }
                }
            } else {
                showToast(context, "Failed to retrieve ID token.")
            }
        } else {
            showToast(
                context,
                "Silent sign-in failed: ${task.exception?.localizedMessage}"
            )
        }
    }
}

@Composable
fun CustomTimePickerDialog(
    initialTime: String,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedHour by remember { mutableIntStateOf(9) }
    var selectedMinute by remember { mutableIntStateOf(0) }
    val dimens = LocalDimens.current

    LaunchedEffect(initialTime) {
        val parts = initialTime.split(":").mapNotNull { it.toIntOrNull() }
        if (parts.size == 2) {
            selectedHour = parts[0]
            selectedMinute = parts[1]
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimens.paddingLarge)
            .background(Color.Transparent)
            .clickable(
                onClick = onDismiss,
                indication = null,
                interactionSource = remember { MutableInteractionSource() })
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(dimens.cornerRadius))
                .background(MaterialTheme.colorScheme.background)
                .align(Alignment.Center)
                .padding(dimens.paddingMedium)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimens.paddingMedium)
            ) {
                Text(
                    text = "Set Reminder Time",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                AndroidView(
                    factory = { context ->
                        TimePicker(context).apply {
                            setIs24HourView(true)
                            hour = selectedHour
                            minute = selectedMinute
                            setOnTimeChangedListener { _, hour, minute ->
                                selectedHour = hour
                                selectedMinute = minute
                            }
                        }
                    },
                    modifier = Modifier.wrapContentSize()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            "Cancel",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                    TextButton(
                        onClick = {
                            val formattedTime = String.format(
                                Locale("in", "IN"),
                                "%02d:%02d",
                                selectedHour,
                                selectedMinute
                            )
                            onTimeSelected(formattedTime)
                            showToast(context, "Reminder set at $formattedTime")
                        }
                    ) {
                        Text(
                            "Set",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
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
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit
) {
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
            checked = isChecked,
            onCheckedChange = onToggle,
            modifier = Modifier.scale(.75f),
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.background)
        )
    }
}


