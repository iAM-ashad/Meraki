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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.iamashad.meraki.navigation.Home
import com.iamashad.meraki.navigation.Register
import com.iamashad.meraki.navigation.Settings
import com.iamashad.meraki.screens.chatbot.ChatViewModel
import com.iamashad.meraki.screens.register.AuthUiEvent
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
    var showCheckInTimePickerDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var selectedTime by remember { mutableStateOf("9:00 AM") }
    var showNotificationPrompt by remember { mutableStateOf(false) }
    var userPassword by remember { mutableStateOf("") }

    // ── Phase 5: notification preference state ────────────────────────────────
    val dailyCheckInEnabled  by settingsViewModel.dailyCheckInEnabled.collectAsStateWithLifecycle()
    val weeklyInsightsEnabled by settingsViewModel.weeklyInsightsEnabled.collectAsStateWithLifecycle()
    val smartNudgesEnabled   by settingsViewModel.smartNudgesEnabled.collectAsStateWithLifecycle()
    val preferredCheckInTime by settingsViewModel.preferredCheckInTime.collectAsStateWithLifecycle()
    val shouldShowNudgePrompt by settingsViewModel.shouldShowNudgePrompt.collectAsStateWithLifecycle()

    val isDynamicColorEnabled by ThemePreference.isDynamicColorEnabled(context)
        .collectAsState(initial = false)
    var isCheckedLocal by remember { mutableStateOf(isDynamicColorEnabled) }
    LaunchedEffect(isDynamicColorEnabled) { isCheckedLocal = isDynamicColorEnabled }

    val user by settingsViewModel.user.collectAsState()
    val profilePicRes by settingsViewModel.profilePicRes.collectAsState()
    val isAvatarUpdating by settingsViewModel.isLoading.collectAsState()
    val avatarUpdateComplete by settingsViewModel.avatarUpdateComplete.collectAsState()

    // Navigate to Home once the Firestore avatar write confirms success.
    LaunchedEffect(avatarUpdateComplete) {
        if (avatarUpdateComplete) {
            settingsViewModel.consumeAvatarUpdateComplete()
            navController.navigate(Home) {
                popUpTo<Settings> { inclusive = false }
            }
        }
    }

    val registerUiState by registerViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(registerUiState.toastMessage) {
        registerUiState.toastMessage?.let {
            showToast(context, it)
            registerViewModel.clearToastMessage()
        }
    }

    LaunchedEffect(Unit) {
        registerViewModel.events.collect { event ->
            when (event) {
                is AuthUiEvent.NavigateToLogin -> navController.navigate(Register) {
                    popUpTo<Settings> { inclusive = true }
                }
                is AuthUiEvent.ShowToast -> showToast(context, event.message)
                else -> Unit
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // ── Profile Section ───────────────────────────────────────────────────
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
                        .clickable { showAvatarDialog = true }
                        .padding(8.dp)
                )
                Spacer(modifier = Modifier.height(dimens.paddingSmall))
                Text(text = user?.displayName ?: "User", fontWeight = FontWeight.Bold)
                Button(onClick = { showAvatarDialog = true }) { Text("Change Avatar") }
            }
        }

        // ── General Settings ──────────────────────────────────────────────────
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

        // ── Phase 5: Notification Preferences section ─────────────────────────
        SettingsSection(title = "Notification Preferences") {

            // Toggle 1 — Daily Check-ins (Default: ON)
            SettingToggle(
                icon     = R.drawable.ic_notifications,
                title    = "Daily Check-ins",
                subtitle = "A gentle daily check-in at your preferred time",
                isChecked = dailyCheckInEnabled,
                onToggle  = { enabled ->
                    settingsViewModel.setDailyCheckInEnabled(enabled, context, preferredCheckInTime)
                }
            )

            // Toggle 2 — Weekly Insights (Default: ON)
            SettingToggle(
                icon     = R.drawable.ic_insights,
                title    = "Weekly Insights",
                subtitle = "A Sunday summary of your emotional week",
                isChecked = weeklyInsightsEnabled,
                onToggle  = { enabled ->
                    settingsViewModel.setWeeklyInsightsEnabled(enabled, context)
                }
            )

            // Toggle 3 — Smart Nudges (Default: OFF; auto-prompted after 4 sessions)
            SettingToggle(
                icon     = R.drawable.ic_dynamic_colors,
                title    = "Smart Nudges",
                subtitle = "Personalised evening check-ins based on your patterns — you can turn this off any time",
                isChecked = smartNudgesEnabled,
                onToggle  = { enabled ->
                    settingsViewModel.setSmartNudgesEnabled(enabled)
                }
            )

            // Preferred Check-in Time picker
            SettingItem(
                icon  = R.drawable.ic_notifications,
                title = "Preferred Check-in Time  ·  $preferredCheckInTime"
            ) {
                showCheckInTimePickerDialog = true
            }
        }

        // ── Phase 4: Privacy & Memory section ─────────────────────────────────
        SettingsSection(title = "Privacy & Memory") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_delete_history),
                    contentDescription = "Memory",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "How Meraki remembers you",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Meraki saves a short summary at the end of each conversation — " +
                        "your key themes, emotional patterns, and what kind of support helped most. " +
                        "Summaries from your last 14 sessions are used to personalise the start " +
                        "of each new conversation. All memory is stored only on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "To permanently delete all conversation history and memory summaries, " +
                        "tap \"Clear Chat History\" in General Settings above.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Account Settings ──────────────────────────────────────────────────
        SettingsSection(title = "Account Settings") {
            SettingItem(icon = R.drawable.ic_password, title = "Update Password") {
                showResetPasswordDialog = true
            }
            SettingItem(icon = R.drawable.ic_logout, title = "Log Out") {
                showLogoutDialog = true
            }
            SettingItem(icon = R.drawable.ic_delete_account, title = "Close Account") {
                showPasswordDialog = true
            }
        }
    } // end Column

    // Full-screen loading overlay while avatar is being saved to Firestore.
    // Navigation to Home is gated behind avatarUpdateComplete, not triggered here.
    if (isAvatarUpdating) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    // Full-screen loading overlay during account deletion (reauth → Firestore → auth delete).
    // The multi-step chain can take a few seconds; this prevents double-tap on "Delete".
    if (registerUiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
    } // end Box

    // ── Dialogs ───────────────────────────────────────────────────────────────

    if (showChatDeleteDialog) ConfirmationDialog(
        "Clear Chat History",
        "Delete all chats?",
        "Clear",
        { showChatDeleteDialog = false }
    ) { chatViewModel.clearChatHistory(); showChatDeleteDialog = false }

    if (showAvatarDialog) AvatarSelectionDialog(navController, {
        settingsViewModel.updateUserAvatar(it); showAvatarDialog = false
    }) { showAvatarDialog = false }

    if (showPasswordDialog) {
        PasswordInputDialog(
            password = userPassword,
            onPasswordChange = { userPassword = it },
            onDismiss = { showPasswordDialog = false },
            onConfirm = {
                showPasswordDialog = false
                showDeleteDialog = true
            }
        )
    }

    if (showDeleteDialog) {
        ConfirmationDialog(
            title       = "Delete Account",
            text        = "Are you sure you want to permanently delete your account? This action cannot be undone.",
            confirmText = "Delete",
            onConfirm   = { registerViewModel.deleteUserAccount(password = userPassword) },
            onDismiss   = { showDeleteDialog = false }
        )
    }

    if (showResetPasswordDialog) {
        ResetPasswordDialog(
            userEmail = user?.email ?: "",
            onDismiss = { showResetPasswordDialog = false },
            onConfirm = {
                registerViewModel.resetPassword(user?.email ?: "")
                showResetPasswordDialog = false
            }
        )
    }

    if (showLogoutDialog) ConfirmationDialog(
        "Log Out",
        "Are you sure?",
        "Log Out",
        { showLogoutDialog = false }
    ) {
        auth.signOut()
        showToast(context, "Logged out successfully!")
        navController.navigate(Register)
    }

    if (showNotificationPrompt) PromptEnableNotifications(context)

    // Existing general reminder time picker
    if (showTimePickerDialog) CustomTimePickerDialog(
        selectedTime, {
            selectedTime = it
            showTimePickerDialog = false
            scheduleDailyReminderAt(context, it)
        }
    ) { showTimePickerDialog = false }

    // Phase 5: Preferred check-in time picker
    if (showCheckInTimePickerDialog) CustomTimePickerDialog(
        initialTime = preferredCheckInTime,
        onTimeSelected = { newTime ->
            showCheckInTimePickerDialog = false
            settingsViewModel.setPreferredCheckInTime(newTime, context)
            showToast(context, "Check-in time set to $newTime")
        },
        onDismiss = { showCheckInTimePickerDialog = false }
    )

    // Phase 5: Smart-nudge auto-prompt dialog (shown once after 4 sessions)
    if (shouldShowNudgePrompt) {
        AlertDialog(
            onDismissRequest = { settingsViewModel.markNudgePromptShown() },
            title = { Text("Enable Smart Nudges?") },
            text = {
                Text(
                    "You've had 4 or more conversations with Meraki. Based on what you've " +
                    "shared, Meraki can now send you a gentle evening nudge — something warm " +
                    "and personal, timed between 6 pm and 11 pm. You can turn this off any " +
                    "time in Settings. Would you like to enable it?"
                )
            },
            confirmButton = {
                Button(onClick = {
                    settingsViewModel.setSmartNudgesEnabled(true)
                    settingsViewModel.markNudgePromptShown()
                    showToast(context, "Smart Nudges enabled!")
                }) { Text("Enable") }
            },
            dismissButton = {
                TextButton(onClick = { settingsViewModel.markNudgePromptShown() }) {
                    Text("Not Now")
                }
            }
        )
    }
}

// ── Composable components ─────────────────────────────────────────────────────

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
            Button(onClick = onConfirm) { Text("Send Reset Email") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
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
            Button(onClick = onConfirm) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AvatarSelectionDialog(
    navController: NavController,
    onAvatarSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val avatars = listOf(
        R.drawable.avatar1, R.drawable.avatar2, R.drawable.avatar3,
        R.drawable.avatar4,  R.drawable.avatar5, R.drawable.avatar6,
        R.drawable.avatar7,  R.drawable.avatar8, R.drawable.avatar9,
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
                            // Navigation to Home is now deferred until Firestore confirms
                            // the write (via settingsViewModel.avatarUpdateComplete).
                            onAvatarSelected(it)
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
        text  = { Text(text) })
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
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(icon), contentDescription = title, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * A toggle row for a settings item.
 *
 * Phase 5 addition: optional [subtitle] shown below [title] in secondary style
 * to describe the feature concisely.
 */
@Composable
fun SettingToggle(
    @DrawableRes icon: Int,
    title: String,
    isChecked: Boolean,
    subtitle: String? = null,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(icon), contentDescription = title, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(
                    text  = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = isChecked, onCheckedChange = onToggle)
    }
}

@Composable
fun CustomTimePickerDialog(
    initialTime: String, onTimeSelected: (String) -> Unit, onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedHour   by remember { mutableIntStateOf(9) }
    var selectedMinute by remember { mutableIntStateOf(0) }
    val dimens = LocalDimens.current

    LaunchedEffect(initialTime) {
        val parts = initialTime.split(":").mapNotNull { it.toIntOrNull() }
        if (parts.size == 2) {
            selectedHour   = parts[0]
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
                    text  = "Set Reminder Time",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                AndroidView(
                    factory = { ctx ->
                        TimePicker(ctx).apply {
                            setIs24HourView(true)
                            hour   = selectedHour
                            minute = selectedMinute
                            setOnTimeChangedListener { _, h, m ->
                                selectedHour   = h
                                selectedMinute = m
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
