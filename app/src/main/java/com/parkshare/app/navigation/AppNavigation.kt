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
import com.parkshare.app.ui.screens.MyListings.MyListingsScreen
import com.parkshare.app.ui.screens.OwnerBookings.OwnerBookingsScreen

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoute.Splash.route
    ) {

        composable(AppRoute.Splash.route) {
            SplashScreen(
                onSplashFinished = { isLoggedIn ->
                    val destination = if (isLoggedIn) AppRoute.Home.route else AppRoute.Onboarding.route
                    navController.navigate(destination) {
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
                },
                onParkingClick = { listingId ->
                    navController.navigate(AppRoute.ParkingDetails.createRoute(listingId))
                }
            )
        }

        composable(AppRoute.Search.route) {

            Searchscreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onParkingClick = { listingId ->
                    navController.navigate(AppRoute.ParkingDetails.createRoute(listingId))
                }
            )
        }

        composable(
            route = AppRoute.ParkingDetails.route,
            arguments = listOf(navArgument("listingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId")
            ParkingDetailsScreen(
                listingId = listingId,
                onBackClick = {
                    navController.popBackStack()
                },
                onBookClick = { id ->
                    navController.navigate(AppRoute.Booking.createRoute(id))
                }
            )
        }

        composable(
            route = AppRoute.Booking.route,
            arguments = listOf(navArgument("listingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId")
            BookingScreen(
                listingId = listingId,
                onBackClick = {
                    navController.popBackStack()
                },
                onConfirmBooking = { bookingId ->
                    navController.navigate(AppRoute.BookingConfirmation.createRoute(bookingId))
                }
            )
        }

        composable(
            route = AppRoute.BookingConfirmation.route,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId")
            BookingConfirmationScreen(
                bookingId = bookingId,
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

        composable(AppRoute.OwnerBookings.route) {
            OwnerBookingsScreen(
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
                onOwnerBookingsClick = {
                    navController.navigate(AppRoute.OwnerBookings.route)
                },
                onOfferParkingClick = {
                    navController.navigate(AppRoute.OfferParking.route)
                },
                onMyListingsClick = {
                    navController.navigate(AppRoute.MyListings.route)
                },
                onLogoutClick = {
                    navController.navigate(AppRoute.Login.route) {
                        popUpTo(0) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(AppRoute.MyListings.route) {
            MyListingsScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onAddNewClick = {
                    navController.navigate(AppRoute.OfferParking.route)
                },
                onEditClick = { listingId ->
                    navController.navigate(AppRoute.EditParking.createRoute(listingId))
                }
            )
        }

        composable(
            route = AppRoute.EditParking.route,
            arguments = listOf(navArgument("listingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId")
            OfferParkingScreen(
                listingId = listingId,
                onBackClick = {
                    navController.popBackStack()
                },
                onSubmitClick = {
                    navController.popBackStack()
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