package com.parkshare.app.data.booking

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrCodeHelperTest {

    @Test
    fun testCreateQrPassPayload_containsRequiredFields() {
        val payload = QrCodeHelper.createQrPassPayload(
            bookingId = "6acf518c0aad4bf31e48bd9e",
            qrToken = "ea1821393fcb1218a4a2fa5e18e8828fcc78b32704123365",
            bookingCode = "PS-20261014-72B54D"
        )

        assertTrue(payload.contains("\"bookingId\":\"6acf518c0aad4bf31e48bd9e\""))
        assertTrue(payload.contains("\"qrToken\":\"ea1821393fcb1218a4a2fa5e18e8828fcc78b32704123365\""))
        assertTrue(payload.contains("\"bookingCode\":\"PS-20261014-72B54D\""))
    }

    @Test
    fun testParseQrPass_fromJsonPayload() {
        val jsonPayload = """{"bookingId":"6acf518c0aad4bf31e48bd9e","qrToken":"ea1821393fcb1218a4a2fa5e18e8828fcc78b32704123365"}"""
        val (bookingId, token) = QrCodeHelper.parseQrPass(jsonPayload)

        assertEquals("6acf518c0aad4bf31e48bd9e", bookingId)
        assertEquals("ea1821393fcb1218a4a2fa5e18e8828fcc78b32704123365", token)
    }

    @Test
    fun testParseQrPass_fromColonDelimitedPayload() {
        val colonPayload = "PSPASS:6acf518c0aad4bf31e48bd9e:ea1821393fcb1218a4a2fa5e18e8828fcc78b32704123365"
        val (bookingId, token) = QrCodeHelper.parseQrPass(colonPayload)

        assertEquals("6acf518c0aad4bf31e48bd9e", bookingId)
        assertEquals("ea1821393fcb1218a4a2fa5e18e8828fcc78b32704123365", token)
    }

    @Test
    fun testParseQrPass_fromRaw48HexToken() {
        val rawToken = "ea1821393fcb1218a4a2fa5e18e8828fcc78b32704123365"
        val (bookingId, token) = QrCodeHelper.parseQrPass(rawToken)

        assertNull(bookingId)
        assertEquals("ea1821393fcb1218a4a2fa5e18e8828fcc78b32704123365", token)
    }

    @Test
    fun testParseQrPass_withWhitespace() {
        val rawTokenWithWhitespace = "  ea1821393fcb1218a4a2fa5e18e8828fcc78b32704123365  \n"
        val (bookingId, token) = QrCodeHelper.parseQrPass(rawTokenWithWhitespace)

        assertNull(bookingId)
        assertEquals("ea1821393fcb1218a4a2fa5e18e8828fcc78b32704123365", token)
    }

    @Test
    fun testZXingMatrixGeneration() {
        val token = "ea1821393fcb1218a4a2fa5e18e8828fcc78b32704123365"
        val matrix = QRCodeWriter().encode(token, BarcodeFormat.QR_CODE, 256, 256)

        assertNotNull(matrix)
        assertEquals(256, matrix.width)
        assertEquals(256, matrix.height)
        // Ensure there are both black and white modules
        var hasBlack = false
        var hasWhite = false
        for (x in 0 until 256) {
            for (y in 0 until 256) {
                if (matrix.get(x, y)) hasBlack = true else hasWhite = true
            }
        }
        assertTrue(hasBlack)
        assertTrue(hasWhite)
    }
}
