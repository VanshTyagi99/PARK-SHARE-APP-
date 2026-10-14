package com.parkshare.app.navigation

sealed class AppRoute(val route: String) {

    data object Splash : AppRoute("splash")

    data object Onboarding : AppRoute("onboarding")

    data object Login : AppRoute("login")

    data object Signup : AppRoute("signup")

    data object Home : AppRoute("home")

    data object Search : AppRoute(route = "search")

    data object ParkingDetails : AppRoute(route = "parking_details/{listingId}") {
        fun createRoute(listingId: String) = "parking_details/$listingId"
    }


    data object Booking : AppRoute(route = "booking/{listingId}") {
        fun createRoute(listingId: String) = "booking/$listingId"
    }

    data object BookingConfirmation : AppRoute(route = "booking_confirmation/{bookingId}") {
        fun createRoute(bookingId: String) = "booking_confirmation/$bookingId"
    }

    data object MyBookings : AppRoute(route = "my_bookings")

    data object OwnerBookings : AppRoute(route = "owner_bookings")


    data object OfferParking : AppRoute(route = "offer_parking")


    data object ParkingSubmitted : AppRoute("parking_submitted")

    data object MyListings : AppRoute("my_listings")

    data object EditParking : AppRoute("edit_parking/{listingId}") {
        fun createRoute(listingId: String) = "edit_parking/$listingId"
    }

    data object Profile : AppRoute("profile")
}