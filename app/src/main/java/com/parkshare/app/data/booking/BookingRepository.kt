package com.parkshare.app.data.booking

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.parkshare.app.data.api.ApiClient
import com.parkshare.app.data.api.ApiResponse
import com.parkshare.app.data.api.BookingDto
import com.parkshare.app.data.api.BookingListResponse
import com.parkshare.app.data.api.CancelBookingRequest
import com.parkshare.app.data.api.CreateBookingRequest
import com.parkshare.app.data.api.QrVerificationRequest
import com.parkshare.app.data.api.RejectBookingRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class BookingConflictException(message: String) : Exception(message)
class BookingAuthException(message: String) : Exception(message)
class BookingNotFoundException(message: String) : Exception(message)

class BookingRepository {

    private val api = ApiClient.apiService
    private val gson = Gson()

    suspend fun createBooking(request: CreateBookingRequest): Result<BookingDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.createBooking(request)
                handleApiResponse(response)
            } catch (e: Exception) {
                Result.failure(mapException(e))
            }
        }

    suspend fun getMyBookings(status: String? = null): Result<List<BookingDto>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getMyBookings(status)
                handleListResponse(response)
            } catch (e: Exception) {
                Result.failure(mapException(e))
            }
        }

    suspend fun getOwnerRequests(status: String? = null): Result<List<BookingDto>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getOwnerRequests(status)
                handleListResponse(response)
            } catch (e: Exception) {
                Result.failure(mapException(e))
            }
        }

    suspend fun getBookingDetails(id: String): Result<BookingDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getBookingDetails(id)
                handleApiResponse(response)
            } catch (e: Exception) {
                Result.failure(mapException(e))
            }
        }

    suspend fun acceptBooking(id: String): Result<BookingDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.acceptBooking(id)
                handleApiResponse(response)
            } catch (e: Exception) {
                Result.failure(mapException(e))
            }
        }

    suspend fun confirmBooking(id: String): Result<BookingDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.confirmBooking(id)
                handleApiResponse(response)
            } catch (e: Exception) {
                Result.failure(mapException(e))
            }
        }

    suspend fun rejectBooking(id: String, reason: String? = null): Result<BookingDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.rejectBooking(id, RejectBookingRequest(rejectionReason = reason))
                handleApiResponse(response)
            } catch (e: Exception) {
                Result.failure(mapException(e))
            }
        }

    suspend fun cancelBooking(id: String, reason: String? = null): Result<BookingDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.cancelBooking(id, CancelBookingRequest(cancellationReason = reason))
                handleApiResponse(response)
            } catch (e: Exception) {
                Result.failure(mapException(e))
            }
        }

    suspend fun checkInBooking(id: String, qrToken: String): Result<BookingDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.checkInBooking(id, QrVerificationRequest(qrToken = qrToken.trim()))
                handleApiResponse(response)
            } catch (e: Exception) {
                Result.failure(mapException(e))
            }
        }

    suspend fun checkOutBooking(id: String, qrToken: String): Result<BookingDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.checkOutBooking(id, QrVerificationRequest(qrToken = qrToken.trim()))
                handleApiResponse(response)
            } catch (e: Exception) {
                Result.failure(mapException(e))
            }
        }

    private fun <T> handleApiResponse(response: Response<ApiResponse<T>>): Result<T> {
        return if (response.isSuccessful) {
            val body = response.body()
            if (body?.success == true && body.data != null) {
                Result.success(body.data)
            } else {
                Result.failure(Exception(body?.message ?: "Unknown error from server"))
            }
        } else {
            val errorMsg = parseErrorMessage(response)
            when (response.code()) {
                409 -> Result.failure(BookingConflictException(errorMsg))
                401, 403 -> Result.failure(BookingAuthException(errorMsg))
                404 -> Result.failure(BookingNotFoundException(errorMsg))
                else -> Result.failure(Exception(errorMsg))
            }
        }
    }

    private fun handleListResponse(response: Response<BookingListResponse>): Result<List<BookingDto>> {
        return if (response.isSuccessful) {
            val body = response.body()
            if (body?.success == true) {
                Result.success(body.data)
            } else {
                Result.failure(Exception(body?.message ?: "Failed to retrieve bookings"))
            }
        } else {
            val errorMsg = parseErrorMessage(response)
            when (response.code()) {
                401, 403 -> Result.failure(BookingAuthException(errorMsg))
                else -> Result.failure(Exception(errorMsg))
            }
        }
    }

    private fun <T> parseErrorMessage(response: Response<T>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                val parsed = JsonParser.parseString(errorBody).asJsonObject
                parsed.get("message")?.asString ?: "Server returned error code ${response.code()}"
            } else {
                "Server returned error code ${response.code()}"
            }
        } catch (_: Exception) {
            "Server returned error code ${response.code()}"
        }
    }

    private fun mapException(e: Exception): Exception {
        return when (e) {
            is java.net.ConnectException -> Exception("Cannot connect to ParkShare server. Check if backend is running.")
            is java.net.SocketTimeoutException -> Exception("Connection timed out. Please try again.")
            is java.net.UnknownHostException -> Exception("Server address unreachable. Check network connection.")
            else -> e
        }
    }
}
