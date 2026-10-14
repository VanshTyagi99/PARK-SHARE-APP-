package com.parkshare.app.data.admin

import com.google.gson.annotations.SerializedName
import com.parkshare.app.data.chat.ListingChatSummaryDto

data class AdminDashboardStatsDto(
    val totalUsers: Int = 0,
    val activeListings: Int = 0,
    val totalBookings: Int = 0,
    val pendingVerifications: Int = 0,
    val pendingBookingRequests: Int = 0,
    val completedBookings: Int = 0
)

data class AdminUserItemDto(
    @SerializedName("_id")
    val id: String,
    val name: String? = null,
    val email: String? = null,
    val role: String? = "user",
    val verificationStatus: String? = "NOT_SUBMITTED",
    val listingCount: Int? = 0,
    val createdAt: String? = null
)

data class AdminListingItemDto(
    @SerializedName("_id")
    val id: String,
    val title: String,
    val address: String,
    val pricePerHour: Double,
    val parkingType: String? = "Car",
    val vehicleCompatibility: String? = "CAR",
    val isActive: Boolean = true,
    val ownerFirebaseUid: String,
    val createdAt: String? = null
)

data class AdminBookingItemDto(
    @SerializedName("_id")
    val id: String,
    val bookingCode: String? = null,
    val status: String,
    val totalAmount: Double,
    val startTime: String,
    val endTime: String,
    val driverFirebaseUid: String,
    val ownerFirebaseUid: String,
    val listingId: ListingChatSummaryDto? = null,
    val createdAt: String? = null
)
