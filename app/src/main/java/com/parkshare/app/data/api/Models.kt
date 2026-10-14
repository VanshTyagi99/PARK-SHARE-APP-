package com.parkshare.app.data.api

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: T? = null,
    @SerializedName("message") val message: String? = null
)

data class HealthResponse(
    @SerializedName("status") val status: String,
    @SerializedName("database") val database: String,
    @SerializedName("timestamp") val timestamp: String
)

data class SyncUserRequest(
    @SerializedName("fullName") val fullName: String,
    @SerializedName("phone") val phone: String? = null
)

data class UserProfileResponse(
    @SerializedName("firebaseUid") val firebaseUid: String,
    @SerializedName("email") val email: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("role") val role: String,
    @SerializedName("verificationStatus") val verificationStatus: String? = "NOT_SUBMITTED",
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

// MARK: - Parking Listing DTOs

data class ParkingListingDto(
    @SerializedName("_id") val id: String,
    @SerializedName("ownerFirebaseUid") val ownerFirebaseUid: String,
    @SerializedName("title") val title: String,
    @SerializedName("address") val address: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("parkingType") val parkingType: String = "Car",
    @SerializedName("vehicleCompatibility") val vehicleCompatibility: String = "CAR",
    @SerializedName("pricePerHour") val pricePerHour: Double,
    @SerializedName("availableFrom") val availableFrom: String,
    @SerializedName("availableUntil") val availableUntil: String,
    @SerializedName("photos") val photos: List<String> = emptyList(),
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("distanceKm") val distanceKm: Double? = null,
    @SerializedName("isActive") val isActive: Boolean = true,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class CreateParkingRequest(
    @SerializedName("title") val title: String,
    @SerializedName("address") val address: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("parkingType") val parkingType: String = "Car",
    @SerializedName("vehicleCompatibility") val vehicleCompatibility: String? = null,
    @SerializedName("pricePerHour") val pricePerHour: Double,
    @SerializedName("availableFrom") val availableFrom: String,
    @SerializedName("availableUntil") val availableUntil: String,
    @SerializedName("photos") val photos: List<String> = emptyList(),
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null
)

data class UpdateParkingRequest(
    @SerializedName("title") val title: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("parkingType") val parkingType: String? = null,
    @SerializedName("vehicleCompatibility") val vehicleCompatibility: String? = null,
    @SerializedName("pricePerHour") val pricePerHour: Double? = null,
    @SerializedName("availableFrom") val availableFrom: String? = null,
    @SerializedName("availableUntil") val availableUntil: String? = null,
    @SerializedName("photos") val photos: List<String>? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null
)

data class ListingStatusRequest(
    @SerializedName("isActive") val isActive: Boolean
)

data class MyListingsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("count") val count: Int = 0,
    @SerializedName("maxLimit") val maxLimit: Int = 10,
    @SerializedName("data") val data: List<ParkingListingDto> = emptyList(),
    @SerializedName("message") val message: String? = null
)

data class SearchParkingResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("count") val count: Int = 0,
    @SerializedName("total") val total: Int = 0,
    @SerializedName("data") val data: List<ParkingListingDto> = emptyList(),
    @SerializedName("message") val message: String? = null
)

// MARK: - Booking DTOs

data class StatusHistoryEntryDto(
    @SerializedName("status") val status: String,
    @SerializedName("timestamp") val timestamp: String? = null,
    @SerializedName("note") val note: String? = null
)

data class BookingDto(
    @SerializedName("_id") val id: String,
    @SerializedName("bookingCode") val bookingCode: String? = null,
    @SerializedName("listingId") val listingId: String,
    @SerializedName("listingTitle") val listingTitle: String? = null,
    @SerializedName("listingAddress") val listingAddress: String? = null,
    @SerializedName("driverFirebaseUid") val driverFirebaseUid: String,
    @SerializedName("ownerFirebaseUid") val ownerFirebaseUid: String,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String,
    @SerializedName("totalHours") val totalHours: Int = 1,
    @SerializedName("hourlyRateSnapshot") val hourlyRateSnapshot: Double = 0.0,
    @SerializedName("totalAmount") val totalAmount: Double = 0.0,
    @SerializedName("status") val status: String? = "PENDING",
    @SerializedName("vehicleNumber") val vehicleNumber: String? = null,
    @SerializedName("vehicleType") val vehicleType: String? = null,
    @SerializedName("driverNotes") val driverNotes: String? = null,
    @SerializedName("qrToken") val qrToken: String? = null,
    @SerializedName("confirmationDeadline") val confirmationDeadline: String? = null,
    @SerializedName("ownerRespondedAt") val ownerRespondedAt: String? = null,
    @SerializedName("driverConfirmedAt") val driverConfirmedAt: String? = null,
    @SerializedName("checkedInAt") val checkedInAt: String? = null,
    @SerializedName("checkedOutAt") val checkedOutAt: String? = null,
    @SerializedName("completedAt") val completedAt: String? = null,
    @SerializedName("rejectionReason") val rejectionReason: String? = null,
    @SerializedName("cancelledAt") val cancelledAt: String? = null,
    @SerializedName("cancelledBy") val cancelledBy: String? = null,
    @SerializedName("cancellationReason") val cancellationReason: String? = null,
    @SerializedName("expiredAt") val expiredAt: String? = null,
    @SerializedName("expiryReason") val expiryReason: String? = null,
    @SerializedName("statusHistory") val statusHistory: List<StatusHistoryEntryDto>? = emptyList(),
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class CreateBookingRequest(
    @SerializedName("listingId") val listingId: String,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String,
    @SerializedName("vehicleNumber") val vehicleNumber: String? = null,
    @SerializedName("vehicleType") val vehicleType: String? = null,
    @SerializedName("driverNotes") val driverNotes: String? = null
)

data class RejectBookingRequest(
    @SerializedName("rejectionReason") val rejectionReason: String? = null
)

data class CancelBookingRequest(
    @SerializedName("cancellationReason") val cancellationReason: String? = null
)

data class QrVerificationRequest(
    @SerializedName("qrToken") val qrToken: String
)

data class BookingListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("count") val count: Int = 0,
    @SerializedName("data") val data: List<BookingDto> = emptyList(),
    @SerializedName("message") val message: String? = null
)

// MARK: - Verification DTOs

data class VerificationStatusDto(
    @SerializedName("status") val status: String = "NOT_SUBMITTED",
    @SerializedName("rejectionReason") val rejectionReason: String? = null,
    @SerializedName("submittedAt") val submittedAt: String? = null,
    @SerializedName("reviewedAt") val reviewedAt: String? = null
)

data class VerificationSubmissionRequest(
    @SerializedName("identityDocumentType") val identityDocumentType: String,
    @SerializedName("identityDocumentData") val identityDocumentData: String,
    @SerializedName("addressDocumentType") val addressDocumentType: String,
    @SerializedName("addressDocumentData") val addressDocumentData: String,
    @SerializedName("selfieData") val selfieData: String
)

data class VerificationSubmissionData(
    @SerializedName("status") val status: String,
    @SerializedName("submittedAt") val submittedAt: String? = null
)

data class VerificationQueueItemDto(
    @SerializedName("_id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("userEmail") val userEmail: String,
    @SerializedName("userName") val userName: String? = null,
    @SerializedName("identityDocumentType") val identityDocumentType: String,
    @SerializedName("addressDocumentType") val addressDocumentType: String,
    @SerializedName("status") val status: String,
    @SerializedName("rejectionReason") val rejectionReason: String? = null,
    @SerializedName("submittedAt") val submittedAt: String? = null,
    @SerializedName("reviewedAt") val reviewedAt: String? = null,
    @SerializedName("reviewedBy") val reviewedBy: String? = null
)

data class VerificationQueueResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("count") val count: Int = 0,
    @SerializedName("data") val data: List<VerificationQueueItemDto> = emptyList(),
    @SerializedName("message") val message: String? = null
)

data class VerificationDocumentsDto(
    @SerializedName("_id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("userEmail") val userEmail: String,
    @SerializedName("userName") val userName: String? = null,
    @SerializedName("identityDocumentType") val identityDocumentType: String,
    @SerializedName("identityDocumentData") val identityDocumentData: String,
    @SerializedName("addressDocumentType") val addressDocumentType: String,
    @SerializedName("addressDocumentData") val addressDocumentData: String,
    @SerializedName("selfieData") val selfieData: String,
    @SerializedName("status") val status: String,
    @SerializedName("rejectionReason") val rejectionReason: String? = null,
    @SerializedName("submittedAt") val submittedAt: String? = null,
    @SerializedName("reviewedAt") val reviewedAt: String? = null,
    @SerializedName("reviewedBy") val reviewedBy: String? = null
)

data class ReviewVerificationRequest(
    @SerializedName("status") val status: String,
    @SerializedName("rejectionReason") val rejectionReason: String? = null
)

data class ReviewVerificationResponseData(
    @SerializedName("_id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("status") val status: String,
    @SerializedName("rejectionReason") val rejectionReason: String? = null,
    @SerializedName("reviewedAt") val reviewedAt: String? = null,
    @SerializedName("reviewedBy") val reviewedBy: String? = null
)


