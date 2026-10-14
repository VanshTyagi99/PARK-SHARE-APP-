package com.parkshare.app.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ParkShareApiService {

    @GET("health")
    suspend fun getHealth(): Response<HealthResponse>

    @GET("users/me")
    suspend fun getCurrentUser(): Response<ApiResponse<UserProfileResponse>>

    @POST("users/sync")
    suspend fun syncUser(
        @Body request: SyncUserRequest
    ): Response<ApiResponse<UserProfileResponse>>

    // MARK: - Parking Listing Endpoints

    @GET("parking/search")
    suspend fun searchParking(
        @Query("query") query: String? = null,
        @Query("vehicleType") vehicleType: String? = null,
        @Query("minPrice") minPrice: Double? = null,
        @Query("maxPrice") maxPrice: Double? = null,
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
        @Query("radiusKm") radiusKm: Double? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<SearchParkingResponse>

    @POST("parking")
    suspend fun createParking(
        @Body request: CreateParkingRequest
    ): Response<ApiResponse<ParkingListingDto>>

    @GET("parking/my-listings")
    suspend fun getMyListings(): Response<MyListingsResponse>

    @GET("parking/{id}")
    suspend fun getParkingDetails(
        @Path("id") id: String
    ): Response<ApiResponse<ParkingListingDto>>

    @PUT("parking/{id}")
    suspend fun updateParking(
        @Path("id") id: String,
        @Body request: UpdateParkingRequest
    ): Response<ApiResponse<ParkingListingDto>>

    @PATCH("parking/{id}/status")
    suspend fun updateParkingStatus(
        @Path("id") id: String,
        @Body request: ListingStatusRequest
    ): Response<ApiResponse<ParkingListingDto>>

    // MARK: - Booking Endpoints

    @POST("bookings")
    suspend fun createBooking(
        @Body request: CreateBookingRequest
    ): Response<ApiResponse<BookingDto>>

    @GET("bookings/my-bookings")
    suspend fun getMyBookings(
        @Query("status") status: String? = null
    ): Response<BookingListResponse>

    @GET("bookings/owner-requests")
    suspend fun getOwnerRequests(
        @Query("status") status: String? = null
    ): Response<BookingListResponse>

    @GET("bookings/{id}")
    suspend fun getBookingDetails(
        @Path("id") id: String
    ): Response<ApiResponse<BookingDto>>

    @PATCH("bookings/{id}/accept")
    suspend fun acceptBooking(
        @Path("id") id: String
    ): Response<ApiResponse<BookingDto>>

    @PATCH("bookings/{id}/confirm")
    suspend fun confirmBooking(
        @Path("id") id: String
    ): Response<ApiResponse<BookingDto>>

    @PATCH("bookings/{id}/reject")
    suspend fun rejectBooking(
        @Path("id") id: String,
        @Body request: RejectBookingRequest = RejectBookingRequest()
    ): Response<ApiResponse<BookingDto>>

    @PATCH("bookings/{id}/cancel")
    suspend fun cancelBooking(
        @Path("id") id: String,
        @Body request: CancelBookingRequest = CancelBookingRequest()
    ): Response<ApiResponse<BookingDto>>

    @POST("bookings/{id}/check-in")
    suspend fun checkInBooking(
        @Path("id") id: String,
        @Body request: QrVerificationRequest
    ): Response<ApiResponse<BookingDto>>

    @POST("bookings/{id}/check-out")
    suspend fun checkOutBooking(
        @Path("id") id: String,
        @Body request: QrVerificationRequest
    ): Response<ApiResponse<BookingDto>>
}

