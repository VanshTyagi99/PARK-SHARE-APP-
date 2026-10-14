package com.parkshare.app.data.chat

import com.google.gson.annotations.SerializedName

data class ChatApiResponse<T>(
    val success: Boolean,
    val count: Int? = null,
    val data: T? = null,
    val message: String? = null
)

data class ConversationDto(
    @SerializedName("_id")
    val id: String,
    val bookingId: BookingChatSummaryDto? = null,
    val driverFirebaseUid: String,
    val ownerFirebaseUid: String,
    val listingId: ListingChatSummaryDto? = null,
    val lastMessageText: String? = "",
    val lastMessageSenderUid: String? = "",
    val lastMessageAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class BookingChatSummaryDto(
    @SerializedName("_id")
    val id: String? = null,
    val bookingCode: String? = null,
    val status: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val totalAmount: Double? = null
)

data class ListingChatSummaryDto(
    @SerializedName("_id")
    val id: String? = null,
    val title: String? = null,
    val address: String? = null,
    val vehicleCompatibility: String? = null,
    val pricePerHour: Double? = null
)

data class MessageDto(
    @SerializedName("_id")
    val id: String,
    val conversationId: String,
    val bookingId: String,
    val senderFirebaseUid: String,
    val senderRole: String, // "DRIVER" or "OWNER"
    val text: String,
    val createdAt: String
)

data class SendMessageRequest(
    val text: String
)
