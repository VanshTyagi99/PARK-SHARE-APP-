package com.parkshare.app.data.booking

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrCodeHelper {

    /**
     * Formats the payload to encode in the QR code.
     * Contains the bookingId and authoritative qrToken.
     */
    fun createQrPassPayload(bookingId: String, qrToken: String, bookingCode: String? = null): String {
        val json = JsonObject().apply {
            addProperty("bookingId", bookingId)
            addProperty("qrToken", qrToken)
            if (!bookingCode.isNullOrBlank()) {
                addProperty("bookingCode", bookingCode)
            }
        }
        return json.toString()
    }

    /**
     * Parses scanned QR content. Supports:
     * 1. JSON payload: {"bookingId":"...","qrToken":"..."}
     * 2. Colon-delimited: PSPASS:<bookingId>:<qrToken>
     * 3. Raw 48-character token: <qrToken> (bookingId will be null)
     *
     * @return Pair of (bookingId?, qrToken)
     */
    fun parseQrPass(scannedContent: String): Pair<String?, String> {
        val trimmed = scannedContent.trim()
        if (trimmed.isEmpty()) {
            return Pair(null, "")
        }

        // Try JSON parsing first
        try {
            val json = JsonParser.parseString(trimmed).asJsonObject
            val bookingId = json.get("bookingId")?.asString?.takeIf { it.isNotBlank() }
            val qrToken = json.get("qrToken")?.asString?.trim() ?: trimmed
            return Pair(bookingId, qrToken)
        } catch (_: Exception) {
            // Not JSON
        }

        // Try colon-delimited PSPASS:<bookingId>:<token>
        if (trimmed.startsWith("PSPASS:") && trimmed.contains(":")) {
            val parts = trimmed.split(":")
            if (parts.size >= 3) {
                return Pair(parts[1].trim(), parts[2].trim())
            }
        }

        // Raw token fallback
        return Pair(null, trimmed)
    }

    /**
     * Generates a square QR code Bitmap of the specified size.
     */
    fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1
        )
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
        return bitmap
    }

    /**
     * Convenience method for Jetpack Compose UI rendering.
     */
    fun generateQrImageBitmap(content: String, size: Int = 512): ImageBitmap {
        return generateQrBitmap(content, size).asImageBitmap()
    }
}
