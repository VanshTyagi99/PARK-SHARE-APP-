package com.parkshare.app.ui.screens.Onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parkshare.app.ui.components.buttons.PrimaryButton
import com.parkshare.app.ui.components.buttons.SecondaryButton

@Composable
fun Onboardingscreen(
    onGetStarted: () -> Unit,
    onLogin: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // App Name
        Text(
            text = "ParkShare",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Tagline
        Text(
            text = "Find. Park. Share.",
            fontSize = 18.sp,
            color = Color(0xFF64748B)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Main Heading
        Text(
            text = "Your Smart Parking Companion",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Description
        Text(
            text = "Discover nearby parking spaces, book them easily, " +
                    "and share your unused parking space with others.",
            fontSize = 16.sp,
            color = Color(0xFF64748B),
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Get Started Button
        PrimaryButton(
            text = "Get Started",
            onClick = onGetStarted
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Login Button
        SecondaryButton(
            text = "I already have an account",
            onClick = onLogin
        )
    }
}