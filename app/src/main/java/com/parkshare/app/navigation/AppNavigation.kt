package com.parkshare.app.navigation

import com.parkshare.app.ui.screens.splash.SplashScreen
import com.parkshare.app.ui.screens.Onboarding.Onboardingscreen
import com.parkshare.app.ui.screens.Login.Loginscreen
import com.parkshare.app.ui.screens.Signup.Signupscreen
import com.parkshare.app.ui.screens.Home.Homescreen
import com.parkshare.app.ui.screens.Search.Searchscreen
import com.parkshare.app.ui.screens.ParkingDetails.ParkingDetailsScreen



import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoute.Splash.route
    ) {

        composable(AppRoute.Splash.route) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(AppRoute.Onboarding.route) {
                        popUpTo(AppRoute.Splash.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(AppRoute.Onboarding.route) {

            Onboardingscreen(
                onGetStarted = {
                    navController.navigate(AppRoute.Login.route)
                },
                onLogin = {
                    navController.navigate(AppRoute.Login.route)
                }
            )
        }

        composable(AppRoute.Login.route) {

            Loginscreen(
                onLogin = {
                    navController.navigate(AppRoute.Home.route)
                },
                onSignup = {
                    navController.navigate(AppRoute.Signup.route)
                }
            )
        }

        composable(AppRoute.Signup.route) {

            Signupscreen(
                onSignup = {
                    navController.navigate(AppRoute.Home.route)
                },
                onLogin = {
                    navController.navigate(AppRoute.Login.route)
                }
            )
        }

        composable(AppRoute.Home.route) {
            Homescreen(
                onSearchClick = {
                    navController.navigate(AppRoute.Search.route)
                }
            )
        }

        composable(AppRoute.Search.route) {

            Searchscreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onParkingClick = {
                    navController.navigate(AppRoute.ParkingDetails.route)
                }
            )
        }



        composable(AppRoute.ParkingDetails.route) {

            ParkingDetailsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }





        composable(AppRoute.Profile.route) {
            // Profile Screen - baad me banayenge
        }
    }
}