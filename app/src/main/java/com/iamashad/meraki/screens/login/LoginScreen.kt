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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.iamashad.meraki.navigation.Screens
import com.iamashad.meraki.screens.register.RegisterViewModel
import com.iamashad.meraki.utils.LocalDimens
import com.iamashad.meraki.utils.ProvideDimens
import com.iamashad.meraki.utils.rememberWindowSizeClass

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
    var userName by remember { mutableStateOf<String?>(null) }
    var userProfilePic by remember { mutableStateOf<Int?>(null) }
    val errorMessage by viewModel.errorMessage.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val windowSize = rememberWindowSizeClass()

    ProvideDimens(windowSize) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
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
                    unfocusedTextColor = MaterialTheme.colorScheme.background,
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
                            contentDescription = "Toggle Password Visibility",
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
                    viewModel.resetPassword(email) { success, message ->
                        showToast(context, if (success) "Check your email for reset instructions" else "Error: $message")
                    }
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
                    viewModel.loginUser(email, password) { success, name, email, profilePicRes ->
                        if (success) {
                            userName = name
                            userProfilePic = profilePicRes ?: R.drawable.avatar1
                            showToast(context, "Welcome back, $name!")
                            navController.navigate(Screens.HOME.name) {
                                popUpTo(Screens.LOGIN.name) { inclusive = true }
                            }
                        } else {
                            showToast(context, "Invalid email or password")
                        }
                    }
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

            if (errorMessage != null) {
                showToast(context, "Error")
            }
        }
    }
}
