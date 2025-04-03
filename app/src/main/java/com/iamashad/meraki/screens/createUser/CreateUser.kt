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
import com.iamashad.meraki.navigation.Screens
import com.iamashad.meraki.screens.register.RegisterViewModel
import com.iamashad.meraki.utils.LocalDimens
import com.iamashad.meraki.utils.ProvideDimens
import com.iamashad.meraki.utils.rememberWindowSizeClass

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
    var selectedAvatar by remember { mutableStateOf<Int?>(null) }
    var acceptPolicy by remember { mutableStateOf(false) }

    // Observe error state from ViewModel
    val errorMessage by viewModel.errorMessage.collectAsState()

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // List of available avatar drawable resources
    val avatars = listOf(
        R.drawable.avatar1, R.drawable.avatar2, R.drawable.avatar3,
        R.drawable.avatar4, R.drawable.avatar5, R.drawable.avatar6,
        R.drawable.avatar7, R.drawable.avatar8, R.drawable.avatar9, R.drawable.avatar10
    )

    val windowSize = rememberWindowSizeClass()

    ProvideDimens(windowSize) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
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

            Spacer(modifier = Modifier.height(dimens.paddingMedium))

            // Avatar Picker
            Text(
                text = "Choose Your Avatar",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.inversePrimary
                )
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(dimens.paddingSmall / 4),
                modifier = Modifier.padding(horizontal = dimens.paddingSmall / 2)
            ) {
                items(avatars) { avatarRes ->
                    Image(
                        painter = painterResource(id = avatarRes),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(dimens.avatarSize / 6)
                            .clip(CircleShape)
                            .background(
                                if (selectedAvatar == avatarRes) MaterialTheme.colorScheme.inversePrimary else Color.Transparent
                            )
                            .clickable { selectedAvatar = avatarRes }
                            .padding(dimens.paddingSmall / 2)
                    )
                }
            }

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
                    viewModel.registerUser(email, password, name, selectedAvatar) { success, message ->
                        if (success) {
                            showToast(context, "Account created successfully!")
                            navController.navigate(Screens.HOME.name) {
                                popUpTo(Screens.CREATEUSER.name) { inclusive = true }
                            }
                        } else {
                            println("Error: $message")
                        }
                    }
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
            if (errorMessage != null) {
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
