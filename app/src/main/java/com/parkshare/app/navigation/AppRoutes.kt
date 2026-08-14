package com.parkshare.app.navigation

sealed class AppRoute(val route: String) {

    data object Splash : AppRoute("splash")

    data object Onboarding : AppRoute("onboarding")

    data object Login : AppRoute("login")

    data object Signup : AppRoute("signup")

    data object Home : AppRoute("home")

    data object Search : AppRoute(route = "search")

    data object ParkingDetails : AppRoute(route = "parking_details")

    data object Profile : AppRoute("profile")
}