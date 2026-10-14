package com.parkshare.app.ui.screens.Login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parkshare.app.data.api.ApiClient
import com.parkshare.app.data.api.SyncUserRequest
import com.parkshare.app.data.auth.AuthRepository
import com.parkshare.app.ui.components.buttons.PrimaryButton
import kotlinx.coroutines.launch

@Composable
fun Loginscreen(
    onLogin: () -> Unit,
    onSignup: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()


    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    val authRepository = remember {
        AuthRepository()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // App Name
        Text(
            text = "ParkShare",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Heading
        Text(
            text = "Welcome Back!",
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Email
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = ""
            },
            label = {
                Text("Email")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = ""
            },
            label = {
                Text("Password")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Error Message
        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Login Button
        PrimaryButton(
            text = if (isLoading) {
                "Logging in..."
            } else {
                "Login"
            },
            onClick = {

                when {
                    email.isBlank() -> {
                        errorMessage = "Please enter your email"
                    }

                    !android.util.Patterns.EMAIL_ADDRESS
                        .matcher(email.trim())
                        .matches() -> {
                        errorMessage = "Please enter a valid email"
                    }

                    password.isBlank() -> {
                        errorMessage = "Please enter your password"
                    }

                    else -> {

                        isLoading = true
                        errorMessage = ""

                        authRepository.login(
                            email = email.trim(),
                            password = password,

                            onSuccess = {
                                coroutineScope.launch {
                                    try {
                                        ApiClient.apiService.syncUser(
                                            SyncUserRequest(fullName = "")
                                        )
                                    } catch (_: Exception) {}
                                    isLoading = false
                                    onLogin()
                                }
                            },

                            onError = { error ->
                                isLoading = false
                                errorMessage = error
                            }
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Signup
        TextButton(
            onClick = onSignup
        ) {
            Text(
                text = "Don't have an account? Sign Up"
            )
        }
    }
}