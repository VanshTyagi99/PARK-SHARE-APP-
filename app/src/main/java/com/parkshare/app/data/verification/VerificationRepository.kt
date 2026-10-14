package com.parkshare.app.data.verification

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.parkshare.app.data.api.ApiClient
import com.parkshare.app.data.api.ApiResponse
import com.parkshare.app.data.api.ReviewVerificationRequest
import com.parkshare.app.data.api.ReviewVerificationResponseData
import com.parkshare.app.data.api.VerificationDocumentsDto
import com.parkshare.app.data.api.VerificationQueueItemDto
import com.parkshare.app.data.api.VerificationQueueResponse
import com.parkshare.app.data.api.VerificationStatusDto
import com.parkshare.app.data.api.VerificationSubmissionData
import com.parkshare.app.data.api.VerificationSubmissionRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class VerificationAuthException(message: String) : Exception(message)
class VerificationValidationException(message: String) : Exception(message)
class VerificationServerException(message: String) : Exception(message)

class VerificationRepository {

    private val api = ApiClient.apiService
    private val gson = Gson()

    suspend fun getVerificationStatus(): Result<VerificationStatusDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getVerificationStatus()
                handleApiResponse(response)
            } catch (e: Exception) {
                Result.failure(mapException(e))
            }
        }

    suspend fun submitVerification(
        identityDocumentType: String,
        identityDocumentData: String,
        addressDocumentType: String,
        addressDocumentData: String,
        selfieData: String
    ): Result<VerificationSubmissionData> =
        withContext(Dispatchers.IO) {
            try {
                val request = VerificationSubmissionRequest(
                    identityDocumentType = identityDocumentType,
                    identityDocumentData = identityDocumentData,
                    addressDocumentType = addressDocumentType,
                    addressDocumentData = addressDocumentData,
                    selfieData = selfieData
                )
                val response = api.submitVerification(request)
                handleApiResponse(response)
            } catch (e: Exception) {
                Result.failure(mapException(e))
            }
        }

    suspend fun getAdminVerificationQueue(status: String? = null): Result<List<VerificationQueueItemDto>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getAdminVerificationQueue(status)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Result.success(body.data)
                    } else {
                        Result.failure(VerificationServerException(body?.message ?: "Failed to fetch verification queue"))
                    }
                } else {
                    Result.failure(extractHttpError(response))
                }
            } catch (e: Exception) {
                Result.failure(mapException(e))
            }
        }

    suspend fun getAdminVerificationDocuments(id: String): Result<VerificationDocumentsDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getAdminVerificationDocuments(id)
                handleApiResponse(response)
            } catch (e: Exception) {
                Result.failure(mapException(e))
            }
        }

    suspend fun reviewVerification(
        id: String,
        status: String,
        rejectionReason: String? = null
    ): Result<ReviewVerificationResponseData> =
        withContext(Dispatchers.IO) {
            try {
                val request = ReviewVerificationRequest(
                    status = status,
                    rejectionReason = rejectionReason
                )
                val response = api.reviewVerification(id, request)
                handleApiResponse(response)
            } catch (e: Exception) {
                Result.failure(mapException(e))
            }
        }

    private fun <T> handleApiResponse(response: Response<ApiResponse<T>>): Result<T> {
        return if (response.isSuccessful) {
            val body = response.body()
            if (body != null && body.success && body.data != null) {
                Result.success(body.data)
            } else {
                Result.failure(
                    VerificationServerException(
                        body?.message ?: "Operation completed without data"
                    )
                )
            }
        } else {
            Result.failure(extractHttpError(response))
        }
    }

    private fun extractHttpError(response: Response<*>): Exception {
        val errorBody = response.errorBody()?.string()
        val message = parseErrorMessage(errorBody)
            ?: "Request failed with HTTP ${response.code()}"

        return when (response.code()) {
            400 -> VerificationValidationException(message)
            401, 403 -> VerificationAuthException(message)
            else -> VerificationServerException(message)
        }
    }

    private fun parseErrorMessage(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val jsonObject = JsonParser.parseString(errorBody).asJsonObject
            when {
                jsonObject.has("message") && !jsonObject.get("message").isJsonNull ->
                    jsonObject.get("message").asString
                jsonObject.has("error") && !jsonObject.get("error").isJsonNull ->
                    jsonObject.get("error").asString
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun mapException(e: Exception): Exception {
        return when (e) {
            is VerificationAuthException,
            is VerificationValidationException,
            is VerificationServerException -> e
            else -> VerificationServerException(e.message ?: "Network error occurred")
        }
    }
}
