package com.iamashad.meraki.screens.settings

import android.widget.TimePicker
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.iamashad.meraki.R
import com.iamashad.meraki.components.showToast
import com.iamashad.meraki.navigation.Screens
import com.iamashad.meraki.screens.chatbot.ChatViewModel
import com.iamashad.meraki.screens.register.RegisterViewModel
import com.iamashad.meraki.ui.theme.ThemePreference
import com.iamashad.meraki.utils.LocalDimens
import com.iamashad.meraki.utils.PromptEnableNotifications
import com.iamashad.meraki.utils.scheduleDailyReminderAt
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SettingsScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    registerViewModel: RegisterViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    val auth = FirebaseAuth.getInstance()
    val dimens = LocalDimens.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAvatarDialog by remember { mutableStateOf(false) }
    var showChatDeleteDialog by remember { mutableStateOf(false) }
    var showResetPasswordDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }  // New: Show password dialog
    var selectedTime by remember { mutableStateOf("9:00 AM") }
    var showNotificationPrompt by remember { mutableStateOf(false) }
    var userPassword by remember { mutableStateOf("") }  // New: Store user password input

    val isDynamicColorEnabled by ThemePreference.isDynamicColorEnabled(context)
        .collectAsState(initial = false)
    var isCheckedLocal by remember { mutableStateOf(isDynamicColorEnabled) }
    LaunchedEffect(isDynamicColorEnabled) { isCheckedLocal = isDynamicColorEnabled }

    val user by settingsViewModel.user.collectAsState()
    val profilePicRes by settingsViewModel.profilePicRes.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Profile Section
        SettingsSection(title = "Profile") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = profilePicRes),
                    contentDescription = "User Avatar",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .clickable { showAvatarDialog = true })
                Spacer(modifier = Modifier.height(dimens.paddingSmall))
                Text(text = user?.displayName ?: "User", fontWeight = FontWeight.Bold)
                Button(onClick = { showAvatarDialog = true }) { Text("Change Avatar") }
            }
        }

        // General Settings
        SettingsSection(title = "General Settings") {
            SettingToggle(
                icon = R.drawable.ic_dynamic_colors,
                title = "Dynamic Color Mode",
                isChecked = isCheckedLocal
            ) {
                isCheckedLocal = it
                coroutineScope.launch { ThemePreference.setDynamicColor(context, it) }
            }
            SettingItem(icon = R.drawable.ic_notifications, title = "Set Reminder") {
                showNotificationPrompt = true
                showTimePickerDialog = true
            }
            SettingItem(
                icon = R.drawable.ic_delete_history, title = "Clear Chat History"
            ) { showChatDeleteDialog = true }
        }


        // Danger Zone
        SettingsSection(title = "Account Settings") {
            SettingItem(icon = R.drawable.ic_delete_account, title = "Close Account") {
                showPasswordDialog = true  // Show password input dialog
            }
            SettingItem(icon = R.drawable.ic_logout, title = "Log Out") {
                showLogoutDialog = true
            }
            SettingItem(icon = R.drawable.ic_password, title = "Update Password") {
                showResetPasswordDialog = true  // Show password reset dialog
            }
        }
    }

    if (showChatDeleteDialog) ConfirmationDialog(
        "Clear Chat History",
        "Delete all chats?",
        "Clear",
        {showChatDeleteDialog = false}
    ) { chatViewModel.clearChatHistory(); showChatDeleteDialog = false }

    // Dialogs
    if (showAvatarDialog) AvatarSelectionDialog(navController, {
        settingsViewModel.updateUserAvatar(it); showAvatarDialog = false
    }) { showAvatarDialog = false }

    if (showPasswordDialog) {  // New: Ask for password before deleting account
        PasswordInputDialog(
            password = userPassword,
            onPasswordChange = { userPassword = it },
            onDismiss = { showPasswordDialog = false },
            onConfirm = {
                showPasswordDialog = false
                showDeleteDialog = true  // Show final confirmation dialog
            }
        )
    }

    if (showDeleteDialog) {
        ConfirmationDialog(
            title = "Delete Account",
            text = "Are you sure you want to permanently delete your account? This action cannot be undone.",
            confirmText = "Delete",
            onConfirm = {
                registerViewModel.deleteUserAccount(
                    password = userPassword,
                    onComplete = { success, message ->
                        if (success) {
                            showToast(context, "Account successfully deleted.")
                            navController.navigate(Screens.REGISTER.name) {
                                popUpTo(Screens.SETTINGS.name) { inclusive = true }
                            }
                        } else {
                            showToast(context, message ?: "Unknown error occurred")
                        }
                    }
                )
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
    if (showResetPasswordDialog) { // New: Reset password confirmation dialog
        ResetPasswordDialog(
            userEmail = user?.email ?: "",
            onDismiss = { showResetPasswordDialog = false },
            onConfirm = {
                registerViewModel.resetPassword(user?.email ?: "") { success, message ->
                    if (success) {
                        showToast(context, "Password reset email sent!")
                    } else {
                        showToast(context, message ?: "Failed to send reset email")
                    }
                    showResetPasswordDialog = false
                }
            }
        )
    }
    if (showLogoutDialog) ConfirmationDialog(
        "Log Out",
        "Are you sure?",
        "Log Out",
        { showLogoutDialog = false }
    ) { auth.signOut(); navController.navigate(Screens.REGISTER.name) }
    if (showNotificationPrompt) PromptEnableNotifications(context)
    if (showTimePickerDialog) CustomTimePickerDialog(
        selectedTime, {
            selectedTime = it; showTimePickerDialog = false; scheduleDailyReminderAt(
            context, it
        )
        }) { showTimePickerDialog = false }
}

@Composable
fun ResetPasswordDialog(
    userEmail: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset Password") },
        text = {
            Column {
                Text("A password reset link will be sent to:")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = userEmail,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Check your email and follow the instructions to reset your password.")
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Send Reset Email")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PasswordInputDialog(
    password: String,
    onPasswordChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var inputPassword by remember { mutableStateOf(password) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Password") },
        text = {
            Column {
                Text("Please enter your password to confirm account deletion.")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = inputPassword,
                    onValueChange = {
                        inputPassword = it
                        onPasswordChange(it)
                    },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Composable
fun AvatarSelectionDialog(
    navController: NavController,
    onAvatarSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val avatars =
        listOf(
            R.drawable.avatar1,
            R.drawable.avatar2,
            R.drawable.avatar3,
            R.drawable.avatar4,
            R.drawable.avatar5,
            R.drawable.avatar6,
            R.drawable.avatar7,
            R.drawable.avatar8,
            R.drawable.avatar9,
            R.drawable.avatar10
        )
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Choose Avatar") }, text = {
        LazyRow {
            items(avatars) {
                Image(
                    painterResource(it),
                    "Avatar",
                    Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .clickable {
                            onAvatarSelected(it)
                            navController.navigate(Screens.HOME.name)
                        })
            }
        }
    }, confirmButton = {})
}

@Composable
fun ConfirmationDialog(
    title: String,
    text: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = { Button(onClick = onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text(title) },
        text = { Text(text) })
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(text = title, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth()) { Column(content = content) }
    }
}

@Composable
fun SettingItem(@DrawableRes icon: Int, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), contentDescription = title, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SettingToggle(
    @DrawableRes icon: Int, title: String, isChecked: Boolean, onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(icon), contentDescription = title, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Switch(checked = isChecked, onCheckedChange = onToggle)
    }
}

@Composable
fun CustomTimePickerDialog(
    initialTime: String, onTimeSelected: (String) -> Unit, onDismiss: () -> Unit
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
                    }, modifier = Modifier.wrapContentSize()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            "Cancel", style = MaterialTheme.typography.titleSmall.copy(
                                color = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                    TextButton(
                        onClick = {
                            val formattedTime = String.format(
                                Locale("in", "IN"), "%02d:%02d", selectedHour, selectedMinute
                            )
                            onTimeSelected(formattedTime)
                            showToast(context, "Reminder set at $formattedTime")
                        }) {
                        Text(
                            "Set", style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

