package com.parkshare.app.data.admin

import com.parkshare.app.data.api.ApiClient
import com.parkshare.app.data.api.ParkShareApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdminRepository(
    private val api: ParkShareApiService = ApiClient.apiService
) {
    suspend fun getDashboardStats(): Result<AdminDashboardStatsDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAdminDashboardStats()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.message ?: "Failed to retrieve dashboard stats"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUsers(): Result<List<AdminUserItemDto>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAdminUsers()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    Result.success(body.data ?: emptyList())
                } else {
                    Result.failure(Exception(body?.message ?: "Failed to retrieve users"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getListings(): Result<List<AdminListingItemDto>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAdminListings()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    Result.success(body.data ?: emptyList())
                } else {
                    Result.failure(Exception(body?.message ?: "Failed to retrieve listings"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBookings(): Result<List<AdminBookingItemDto>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAdminBookings()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    Result.success(body.data ?: emptyList())
                } else {
                    Result.failure(Exception(body?.message ?: "Failed to retrieve bookings"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
