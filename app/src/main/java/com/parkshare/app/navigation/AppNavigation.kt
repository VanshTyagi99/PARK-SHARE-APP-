package com.parkshare.app.navigation

import com.parkshare.app.ui.screens.splash.SplashScreen
import com.parkshare.app.ui.screens.Onboarding.Onboardingscreen
import com.parkshare.app.ui.screens.Login.Loginscreen
import com.parkshare.app.ui.screens.Signup.Signupscreen
import com.parkshare.app.ui.screens.Home.Homescreen
import com.parkshare.app.ui.screens.Search.Searchscreen
import com.parkshare.app.ui.screens.ParkingDetails.ParkingDetailsScreen
import com.parkshare.app.ui.screens.Booking.BookingScreen
import com.parkshare.app.ui.screens.BookingConfirmation.BookingConfirmationScreen
import com.parkshare.app.ui.screens.MyBookings.MyBookingsScreen
import com.parkshare.app.ui.screens.Profile.ProfileScreen
import com.parkshare.app.ui.screens.OfferParking.OfferParkingScreen
import com.parkshare.app.ui.screens.ParkingSubmitted.ParkingSubmittedScreen


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
                },
                onProfileClick = {
                    navController.navigate(AppRoute.Profile.route)
                },

                        onOfferParkingClick = {
                    navController.navigate(AppRoute.OfferParking.route)
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
                },
                onBookClick = {
                    navController.navigate(AppRoute.Booking.route)
                }
            )
        }


        composable(AppRoute.Booking.route) {

            BookingScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onConfirmBooking = {
                    navController.navigate(AppRoute.BookingConfirmation.route)
                }
            )
        }


        composable(AppRoute.BookingConfirmation.route) {

            BookingConfirmationScreen(
                onViewBookingsClick = {
                    navController.navigate(AppRoute.MyBookings.route)
                },
                onHomeClick = {
                    navController.navigate(AppRoute.Home.route) {
                        popUpTo(AppRoute.Home.route) {
                            inclusive = false
                        }
                    }
                }
            )
        }

        composable(AppRoute.MyBookings.route) {

            MyBookingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }




        composable(AppRoute.Profile.route) {

            ProfileScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onBookingsClick = {
                    navController.navigate(AppRoute.MyBookings.route)
                },
                onOfferParkingClick = {
                    navController.navigate(AppRoute.OfferParking.route)
                },
                onLogoutClick = {
                    navController.navigate(AppRoute.Login.route) {
                        popUpTo(AppRoute.Home.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }


        composable(AppRoute.OfferParking.route) {

            OfferParkingScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onSubmitClick = {
                    navController.navigate(AppRoute.ParkingSubmitted.route)
                }
            )
        }


        composable(AppRoute.ParkingSubmitted.route) {

            ParkingSubmittedScreen(
                onGoHomeClick = {
                    navController.navigate(AppRoute.Home.route) {
                        popUpTo(AppRoute.ParkingSubmitted.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }




    }
}