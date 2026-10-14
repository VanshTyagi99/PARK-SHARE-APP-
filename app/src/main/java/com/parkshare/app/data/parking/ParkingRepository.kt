package com.parkshare.app.data.parking

import com.google.gson.Gson
import com.parkshare.app.data.api.ApiClient
import com.parkshare.app.data.api.ApiResponse
import com.parkshare.app.data.api.CreateParkingRequest
import com.parkshare.app.data.api.ListingStatusRequest
import com.parkshare.app.data.api.ParkingListingDto
import com.parkshare.app.data.api.UpdateParkingRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class ParkingRepository {

    private val api = ApiClient.apiService
    private val gson = Gson()

    suspend fun searchParking(
        query: String? = null,
        vehicleType: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        radiusKm: Double? = null,
        page: Int? = null,
        limit: Int? = null
    ): Result<List<ParkingListingDto>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.searchParking(
                    query = query,
                    vehicleType = vehicleType,
                    minPrice = minPrice,
                    maxPrice = maxPrice,
                    latitude = latitude,
                    longitude = longitude,
                    radiusKm = radiusKm,
                    page = page,
                    limit = limit
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        Result.success(body.data)
                    } else {
                        Result.failure(Exception(body?.message ?: "Failed to search parking"))
                    }
                } else {
                    val errorMsg = parseErrorMessage(response)
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(mapException(e))
            }
        }

    suspend fun createParking(request: CreateParkingRequest): Result<ParkingListingDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.createParking(request)
                handleApiResponse(response)
            } catch (e: Exception) {
                Result.failure(mapException(e))
            }
        }

    suspend fun getMyListings(): Result<List<ParkingListingDto>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getMyListings()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        Result.success(body.data)
                    } else {
                        Result.failure(Exception(body?.message ?: "Failed to retrieve your listings"))
                    }
                } else {
                    val errorMsg = parseErrorMessage(response)
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(mapException(e))
            }
        }

    suspend fun getParkingDetails(id: String): Result<ParkingListingDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getParkingDetails(id)
                handleApiResponse(response)
            } catch (e: Exception) {
                Result.failure(mapException(e))
            }
        }

    suspend fun updateParking(id: String, request: UpdateParkingRequest): Result<ParkingListingDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.updateParking(id, request)
                handleApiResponse(response)
            } catch (e: Exception) {
                Result.failure(mapException(e))
            }
        }

    suspend fun updateParkingStatus(id: String, isActive: Boolean): Result<ParkingListingDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.updateParkingStatus(id, ListingStatusRequest(isActive = isActive))
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
            Result.failure(Exception(errorMsg))
        }
    }

    private fun <T> parseErrorMessage(response: Response<T>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                val parsed = gson.fromJson(errorBody, ApiResponse::class.java)
                parsed.message ?: "Server returned error ${response.code()}"
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
