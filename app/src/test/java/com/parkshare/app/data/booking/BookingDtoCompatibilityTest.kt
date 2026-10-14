package com.parkshare.app.data.booking

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.parkshare.app.data.api.ApiResponse
import com.parkshare.app.data.api.BookingDto
import com.parkshare.app.data.api.BookingListResponse
import com.parkshare.app.data.api.CreateBookingRequest
import com.parkshare.app.data.api.QrVerificationRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookingDtoCompatibilityTest {

    private val gson = Gson()

    @Test
    fun testSuccessfulBookingDtoParsing() {
        val json = """
            {
              "success": true,
              "message": "Booking request submitted successfully. Awaiting owner approval.",
              "data": {
                "_id": "670c1234567890abcdef1234",
                "bookingCode": "PS-20261014-ABCDEF",
                "listingId": "507f1f77bcf86cd799439011",
                "listingTitle": "Sector 18 Atta Market",
                "listingAddress": "B-Block, Sector 18, Noida",
                "driverFirebaseUid": "driver_uid_101",
                "ownerFirebaseUid": "owner_uid_202",
                "startTime": "2026-10-15T10:00:00.000Z",
                "endTime": "2026-10-15T12:00:00.000Z",
                "totalHours": 2,
                "hourlyRateSnapshot": 50.0,
                "totalAmount": 100.0,
                "status": "PENDING",
                "vehicleNumber": "DL 01 AB 1234",
                "vehicleType": "Car",
                "driverNotes": "Visiting market",
                "qrToken": null,
                "confirmationDeadline": null,
                "statusHistory": [
                  {
                    "status": "PENDING",
                    "timestamp": "2026-10-14T10:00:00.000Z",
                    "note": "Created"
                  }
                ],
                "createdAt": "2026-10-14T10:00:00.000Z",
                "updatedAt": "2026-10-14T10:00:00.000Z"
              }
            }
        """.trimIndent()

        val parsedType = object : com.google.gson.reflect.TypeToken<ApiResponse<BookingDto>>() {}.type
        val response: ApiResponse<BookingDto> = gson.fromJson(json, parsedType)

        assertTrue(response.success)
        assertNotNull(response.data)

        val dto = response.data!!
        assertEquals("670c1234567890abcdef1234", dto.id)
        assertEquals("PS-20261014-ABCDEF", dto.bookingCode)
        assertEquals("507f1f77bcf86cd799439011", dto.listingId)
        assertEquals("Sector 18 Atta Market", dto.listingTitle)
        assertEquals("driver_uid_101", dto.driverFirebaseUid)
        assertEquals("owner_uid_202", dto.ownerFirebaseUid)
        assertEquals(2, dto.totalHours)
        assertEquals(50.0, dto.hourlyRateSnapshot, 0.001)
        assertEquals(100.0, dto.totalAmount, 0.001)
        assertEquals("PENDING", dto.status)
        assertEquals("DL 01 AB 1234", dto.vehicleNumber)
        assertNull(dto.qrToken)
        assertNull(dto.confirmationDeadline)
        assertEquals(1, dto.statusHistory?.size)
        assertEquals("PENDING", dto.statusHistory?.get(0)?.status)
    }


    @Test
    fun testConfirmedBookingWithQrTokenParsing() {
        val json = """
            {
              "success": true,
              "message": "Booking confirmed successfully.",
              "data": {
                "_id": "670c1234567890abcdef1234",
                "bookingCode": "PS-20261014-ABCDEF",
                "listingId": "507f1f77bcf86cd799439011",
                "driverFirebaseUid": "driver_uid_101",
                "ownerFirebaseUid": "owner_uid_202",
                "startTime": "2026-10-15T10:00:00.000Z",
                "endTime": "2026-10-15T12:00:00.000Z",
                "status": "CONFIRMED",
                "qrToken": "7f9a1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f",
                "driverConfirmedAt": "2026-10-14T10:30:00.000Z"
              }
            }
        """.trimIndent()

        val parsedType = object : com.google.gson.reflect.TypeToken<ApiResponse<BookingDto>>() {}.type
        val response: ApiResponse<BookingDto> = gson.fromJson(json, parsedType)

        assertTrue(response.success)
        val dto = response.data!!
        assertEquals("CONFIRMED", dto.status)
        assertNotNull(dto.qrToken)
        assertEquals(48, dto.qrToken!!.length)
        assertEquals("7f9a1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f", dto.qrToken)
        assertEquals("2026-10-14T10:30:00.000Z", dto.driverConfirmedAt)
    }

    @Test
    fun testBookingListResponseParsing() {
        val json = """
            {
              "success": true,
              "count": 2,
              "data": [
                {
                  "_id": "booking_id_1",
                  "listingId": "listing_1",
                  "driverFirebaseUid": "driver_1",
                  "ownerFirebaseUid": "owner_1",
                  "startTime": "2026-10-15T10:00:00.000Z",
                  "endTime": "2026-10-15T12:00:00.000Z",
                  "status": "PENDING"
                },
                {
                  "_id": "booking_id_2",
                  "listingId": "listing_2",
                  "driverFirebaseUid": "driver_1",
                  "ownerFirebaseUid": "owner_2",
                  "startTime": "2026-10-16T14:00:00.000Z",
                  "endTime": "2026-10-16T16:00:00.000Z",
                  "status": "CONFIRMED",
                  "qrToken": "abcdef1234567890abcdef1234567890abcdef1234567890"
                }
              ]
            }
        """.trimIndent()

        val response: BookingListResponse = gson.fromJson(json, BookingListResponse::class.java)

        assertTrue(response.success)
        assertEquals(2, response.count)
        assertEquals(2, response.data.size)
        assertEquals("booking_id_1", response.data[0].id)
        assertEquals("PENDING", response.data[0].status)
        assertNull(response.data[0].qrToken)
        assertEquals("booking_id_2", response.data[1].id)
        assertEquals("CONFIRMED", response.data[1].status)
        assertEquals(48, response.data[1].qrToken?.length)
    }

    @Test
    fun testNullableAndMissingFieldsDoNotCrash() {
        val minimalJson = """
            {
              "_id": "min_id_123",
              "listingId": "list_123",
              "driverFirebaseUid": "driver_123",
              "ownerFirebaseUid": "owner_123",
              "startTime": "2026-10-15T10:00:00.000Z",
              "endTime": "2026-10-15T12:00:00.000Z"
            }
        """.trimIndent()

        val dto: BookingDto = gson.fromJson(minimalJson, BookingDto::class.java)

        assertEquals("min_id_123", dto.id)
        assertEquals("list_123", dto.listingId)
        assertNull(dto.status) // Safely null when omitted from JSON
        assertEquals(0, dto.totalHours)
        assertEquals(0.0, dto.hourlyRateSnapshot, 0.001)

        assertEquals(0.0, dto.totalAmount, 0.001)
        assertNull(dto.qrToken)
        assertNull(dto.bookingCode)
        assertNull(dto.statusHistory)

    }

    @Test
    fun testUnknownFutureStatusValueSafeHandling() {
        val json = """
            {
              "_id": "status_test_id",
              "listingId": "list_1",
              "driverFirebaseUid": "d1",
              "ownerFirebaseUid": "o1",
              "startTime": "2026-10-15T10:00:00.000Z",
              "endTime": "2026-10-15T12:00:00.000Z",
              "status": "FUTURE_CUSTOM_STATUS"
            }
        """.trimIndent()

        val dto: BookingDto = gson.fromJson(json, BookingDto::class.java)
        assertEquals("FUTURE_CUSTOM_STATUS", dto.status)
    }

    @Test
    fun testConflictErrorResponseParsing() {
        val errorJson = """
            {
              "success": false,
              "message": "This parking slot has already been reserved for the selected time window.",
              "conflict": {
                "startTime": "2026-10-15T10:00:00.000Z",
                "endTime": "2026-10-15T12:00:00.000Z",
                "status": "CONFIRMED"
              }
            }
        """.trimIndent()

        val jsonObject = JsonParser.parseString(errorJson).asJsonObject
        val message = jsonObject.get("message")?.asString
        val success = jsonObject.get("success")?.asBoolean

        assertEquals(false, success)
        assertEquals("This parking slot has already been reserved for the selected time window.", message)
    }

    @Test
    fun testRequestSerialization() {
        val createReq = CreateBookingRequest(
            listingId = "507f1f77bcf86cd799439011",
            startTime = "2026-10-15T10:00:00.000Z",
            endTime = "2026-10-15T12:00:00.000Z",
            vehicleNumber = "DL 01 AB 1234",
            vehicleType = "Car",
            driverNotes = "Testing"
        )
        val serialized = gson.toJson(createReq)
        assertTrue(serialized.contains("507f1f77bcf86cd799439011"))
        assertTrue(serialized.contains("2026-10-15T10:00:00.000Z"))
        assertTrue(serialized.contains("DL 01 AB 1234"))

        val qrReq = QrVerificationRequest(qrToken = "test_qr_token_12345")
        val qrSerialized = gson.toJson(qrReq)
        assertTrue(qrSerialized.contains("test_qr_token_12345"))
    }
}
