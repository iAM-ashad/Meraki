package com.iamashad.meraki.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.iamashad.meraki.R
import com.iamashad.meraki.components.showToast
import com.iamashad.meraki.navigation.Home
import com.iamashad.meraki.navigation.Login
import com.iamashad.meraki.screens.register.AuthUiEvent
import com.iamashad.meraki.screens.register.RegisterViewModel
import com.iamashad.meraki.utils.LocalDimens
import com.iamashad.meraki.utils.ProvideDimens
import com.iamashad.meraki.utils.rememberWindowAdaptiveInfo

@Composable
fun LoginScreen(
    viewModel: RegisterViewModel = viewModel(),
    navController: NavController,
    onNavigateToRegister: () -> Unit
) {
    val dimens = LocalDimens.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    // Phase 2: collect consolidated AuthUiState instead of separate errorMessage StateFlow
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // Consume the one-shot toast emitted by resetPassword()
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            showToast(context, it)
            viewModel.clearToastMessage()
        }
    }

    // Phase 6: collect one-time navigation/toast events from AuthUiEvent channel.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AuthUiEvent.NavigateToHome -> navController.navigate(Home) {
                    popUpTo<Login> { inclusive = true }
                }
                is AuthUiEvent.ShowToast -> showToast(context, event.message)
                else -> Unit
            }
        }
    }

    val adaptiveInfo = rememberWindowAdaptiveInfo()

    ProvideDimens(adaptiveInfo) {
        Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Phase 1 fix: was hardcoded Color.White — broken in dark mode
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_signup_register),
                    contentDescription = "Login Illustration",
                    modifier = Modifier
                )
            }

            Spacer(modifier = Modifier.height(dimens.paddingMedium))

            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.inversePrimary
            )
            Text(
                text = "Login to continue",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
            )

            Spacer(modifier = Modifier.height(dimens.paddingMedium))

            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.inversePrimary,
                    unfocusedBorderColor = Color.Gray,
                    unfocusedTextColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.inversePrimary,
                    focusedLabelColor = MaterialTheme.colorScheme.inversePrimary,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(dimens.cornerRadius),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.paddingMedium)
                    .focusRequester(focusRequester)
            )
            Spacer(modifier = Modifier.height(dimens.paddingSmall))

            // **🔹 Password Input**
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.inversePrimary,
                    unfocusedBorderColor = Color.Gray,
                    unfocusedTextColor = MaterialTheme.colorScheme.background,
                    focusedTextColor = MaterialTheme.colorScheme.inversePrimary,
                    focusedLabelColor = MaterialTheme.colorScheme.inversePrimary,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            painter = painterResource(id = if (passwordVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off),
                            contentDescription = if (passwordVisible) "Hide Password" else "Show Password",
                            tint = MaterialTheme.colorScheme.inversePrimary,
                            modifier = Modifier.scale(.5f)
                        )
                    }
                },
                shape = RoundedCornerShape(dimens.cornerRadius),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.paddingMedium)
                    .focusRequester(focusRequester)
            )

            Spacer(modifier = Modifier.height(dimens.paddingSmall))

            // **🔹 Forgot Password?**
            TextButton(onClick = {
                if (email.isBlank()) {
                    showToast(context, "Enter your email to reset password")
                } else {
                    // Phase 2: no callback — result emitted as toastMessage in AuthUiState
                    viewModel.resetPassword(email)
                }
            }) {
                Text(
                    "Forgot Password?",
                    color = MaterialTheme.colorScheme.inversePrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(dimens.paddingSmall))
            
            // **🔹 Login Button**
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        showToast(context, "Please enter both email and password!")
                        return@Button  // Prevents execution if fields are empty
                    }
                    // Phase 6: no callback — navigation/toast handled via AuthUiEvent channel.
                    viewModel.loginUser(email, password)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.inversePrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(dimens.cornerRadius),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.paddingMedium)
            ) {
                Text(text = "Login")
            }


            Spacer(modifier = Modifier.height(dimens.paddingSmall))

            // **🔹 Don't have an account?**
            TextButton(onClick = onNavigateToRegister) {
                Text(
                    "Don't have an account? Sign Up",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }

        }

        // Full-screen video overlay while authentication request is in flight.
        // Pointer input is blocked by the Box, preventing double-taps or back navigation.
        if (uiState.isLoading) {
            com.iamashad.meraki.components.MerakiVideoLoader(
                modifier = Modifier.fillMaxSize()
            )
        }
        } // end Box
    }
}
