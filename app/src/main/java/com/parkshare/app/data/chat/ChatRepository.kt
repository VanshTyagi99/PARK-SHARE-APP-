package com.parkshare.app.data.chat

import com.parkshare.app.data.api.ApiClient
import com.parkshare.app.data.api.ParkShareApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatRepository(
    private val api: ParkShareApiService = ApiClient.apiService
) {
    suspend fun getConversations(): Result<List<ConversationDto>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getConversations()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    Result.success(body.data ?: emptyList())
                } else {
                    Result.failure(Exception(body?.message ?: "Failed to retrieve conversations"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBookingConversation(bookingId: String): Result<ConversationDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.getBookingConversation(bookingId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.message ?: "Failed to retrieve booking conversation"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConversationMessages(conversationId: String): Result<List<MessageDto>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getConversationMessages(conversationId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    Result.success(body.data ?: emptyList())
                } else {
                    Result.failure(Exception(body?.message ?: "Failed to retrieve messages"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(conversationId: String, text: String): Result<MessageDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.sendMessage(conversationId, SendMessageRequest(text = text.trim()))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.message ?: "Failed to send message"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
