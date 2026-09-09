package com.parkshare.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.parkshare.app.navigation.AppNavigation
import com.parkshare.app.ui.theme.ParkShareTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ParkShareTheme {
                AppNavigation()
            }
        }
    }
}