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
 
    data object VerificationStatus : AppRoute("verification_status")

    data object VerificationForm : AppRoute("verification_form")

    data object AdminVerificationQueue : AppRoute("admin_verification_queue")

    data object AdminVerificationDetail : AppRoute("admin_verification_detail/{verificationId}") {
        fun createRoute(verificationId: String) = "admin_verification_detail/$verificationId"
    }

    data object Chat : AppRoute("chat?bookingId={bookingId}&conversationId={conversationId}") {
        fun createRoute(bookingId: String? = null, conversationId: String? = null): String {
            return when {
                !bookingId.isNullOrBlank() -> "chat?bookingId=$bookingId&conversationId="
                !conversationId.isNullOrBlank() -> "chat?bookingId=&conversationId=$conversationId"
                else -> "chat?bookingId=&conversationId="
            }
        }
    }

    data object Conversations : AppRoute("conversations")

    data object AdminDashboard : AppRoute("admin_dashboard")

    data object AdminUsers : AppRoute("admin_users")

    data object AdminListings : AppRoute("admin_listings")

    data object AdminBookings : AppRoute("admin_bookings")
}