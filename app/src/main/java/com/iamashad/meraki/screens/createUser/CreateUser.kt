package com.iamashad.meraki.screens.createUser

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.iamashad.meraki.R
import com.iamashad.meraki.components.PrivacyPolicyText
import com.iamashad.meraki.components.showToast
import com.iamashad.meraki.navigation.AvatarCelebration
import com.iamashad.meraki.navigation.CreateUser
import com.iamashad.meraki.screens.register.AuthUiEvent
import com.iamashad.meraki.screens.register.RegisterViewModel
import com.iamashad.meraki.utils.LocalDimens
import com.iamashad.meraki.utils.ProvideDimens
import com.iamashad.meraki.utils.rememberWindowAdaptiveInfo

/**
 * UI Composable for the Create User (Sign Up) screen.
 * Handles user input, form validation, avatar selection, and navigation.
 */
@Composable
fun CreateUserScreen(
    viewModel: RegisterViewModel = viewModel(),
    navController: NavController,
    onNavigateToLogin: () -> Unit
) {
    val dimens = LocalDimens.current

    // State variables for form fields
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    // Phase 2: selectedAvatar removed — avatar selection moved to AvatarCelebrationScreen.
    var acceptPolicy by remember { mutableStateOf(false) }

    // Phase 2: collect consolidated AuthUiState instead of separate errorMessage StateFlow
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // Phase 6: collect one-time navigation/toast events from AuthUiEvent channel.
    // Onboarding Overhaul (Phase 2): on success navigate to AvatarCelebration (step 2)
    // instead of Home — passes the new userId so the avatar can be saved to Firestore.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AuthUiEvent.NavigateToHome -> {
                    // Redirect to AvatarCelebration — the new step 2 of sign-up.
                    val userId = com.google.firebase.auth.FirebaseAuth
                        .getInstance().currentUser?.uid.orEmpty()
                    navController.navigate(AvatarCelebration(userId = userId)) {
                        popUpTo<CreateUser> { inclusive = true }
                    }
                }
                is AuthUiEvent.ShowToast -> showToast(context, event.message)
                else -> Unit
            }
        }
    }

    // Phase 2: avatars list removed — avatar selection moved to AvatarCelebrationScreen.

    val adaptiveInfo = rememberWindowAdaptiveInfo()

    ProvideDimens(adaptiveInfo) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Phase 1 fix: was hardcoded Color.White — broken in dark mode
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header image box
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_signup_register),
                    contentDescription = "Sign Up Illustration"
                )
            }

            Spacer(modifier = Modifier.height(dimens.paddingMedium))

            // Screen title and subtitle
            Text(
                text = "Sign Up",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.inversePrimary
            )
            Text(
                text = "Create an account to continue",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
            )

            Spacer(modifier = Modifier.height(dimens.paddingMedium))

            // Full Name Input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                shape = RoundedCornerShape(dimens.cornerRadius),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.inversePrimary,
                    unfocusedBorderColor = Color.Gray,
                    unfocusedTextColor = MaterialTheme.colorScheme.background,
                    focusedTextColor = MaterialTheme.colorScheme.inversePrimary,
                    focusedLabelColor = MaterialTheme.colorScheme.inversePrimary,
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                    capitalization = KeyboardCapitalization.Words
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.paddingMedium)
                    .focusRequester(focusRequester)
            )

            Spacer(modifier = Modifier.height(dimens.paddingSmall))

            // Email Input
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                shape = RoundedCornerShape(dimens.cornerRadius),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.inversePrimary,
                    unfocusedBorderColor = Color.Gray,
                    unfocusedTextColor = MaterialTheme.colorScheme.background,
                    focusedTextColor = MaterialTheme.colorScheme.inversePrimary,
                    focusedLabelColor = MaterialTheme.colorScheme.inversePrimary,
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.paddingMedium)
                    .focusRequester(focusRequester)
            )

            Spacer(modifier = Modifier.height(dimens.paddingSmall))

            // Password Input
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                shape = RoundedCornerShape(dimens.cornerRadius),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            painter = painterResource(id = if (passwordVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off),
                            contentDescription = "Toggle Password Visibility",
                            tint = MaterialTheme.colorScheme.inversePrimary,
                            modifier = Modifier.scale(.5f)
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.inversePrimary,
                    unfocusedBorderColor = Color.Gray,
                    unfocusedTextColor = MaterialTheme.colorScheme.background,
                    focusedTextColor = MaterialTheme.colorScheme.inversePrimary,
                    focusedLabelColor = MaterialTheme.colorScheme.inversePrimary,
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.paddingMedium)
                    .focusRequester(focusRequester)
            )

            // Phase 2: Avatar picker removed from this screen.
            // Avatar selection has been moved to AvatarCelebrationScreen (step 2 of sign-up),
            // where it can be a rewarding moment rather than a buried form field.

            Spacer(modifier = Modifier.height(dimens.paddingMedium))

            // Privacy policy agreement
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Checkbox(
                    checked = acceptPolicy,
                    onCheckedChange = { acceptPolicy = it },
                    colors = CheckboxDefaults.colors(
                        uncheckedColor = Color.Gray,
                        checkedColor = MaterialTheme.colorScheme.inversePrimary,
                        checkmarkColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                PrivacyPolicyText()
            }

            Spacer(modifier = Modifier.height(dimens.paddingMedium / 2))

            // Sign Up Button
            Button(
                onClick = {
                    if (name.isBlank() || email.isBlank() || password.isBlank() || !acceptPolicy) {
                        showToast(context, "Please fill all the details!")
                        return@Button
                    }
                    // Phase 6: no callback — navigation/toast handled via AuthUiEvent channel.
                    // Phase 2: pass null for avatar — it will be saved in AvatarCelebrationScreen.
                    viewModel.registerUser(email, password, name, null)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.inversePrimary,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.White
                ),
                shape = RoundedCornerShape(dimens.cornerRadius),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.paddingMedium),
                enabled = acceptPolicy
            ) {
                Text(text = "Sign Up")
            }

            Spacer(modifier = Modifier.height(dimens.paddingSmall / 2))

            // Login redirect
            TextButton(onClick = onNavigateToLogin) {
                Text(
                    text = "Already have an account? Log in",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(bottom = dimens.paddingSmall)
                )
            }

            // Error feedback
            if (uiState.errorMessage != null) {
                Text(text = uiState.errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
