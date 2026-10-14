package com.parkshare.app.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.parkshare.app.data.auth.AuthRepository
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: (isLoggedIn: Boolean) -> Unit
) {
    val authRepository = AuthRepository()

    LaunchedEffect(Unit) {
        delay(1500)
        onSplashFinished(authRepository.isUserLoggedIn())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ParkShare",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        Text(
            text = "Find. Park. Share.",
            fontSize = 16.sp,
            color = Color(0xFF64748B)
        )
    }
}