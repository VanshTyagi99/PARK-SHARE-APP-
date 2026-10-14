package com.parkshare.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream

object ImageUtils {

    /**
     * Converts an image Uri to a compressed Base64 data URL string.
     * Scales down images larger than maxDimension to conserve payload size.
     */
    fun uriToBase64(
        context: Context,
        uri: Uri,
        maxDimension: Int = 800,
        quality: Int = 75
    ): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
                val width = originalBitmap.width
                val height = originalBitmap.height

                val scaledBitmap = if (width > maxDimension || height > maxDimension) {
                    val ratio = width.toFloat() / height.toFloat()
                    val (newWidth, newHeight) = if (ratio > 1f) {
                        Pair(maxDimension, (maxDimension / ratio).toInt().coerceAtLeast(1))
                    } else {
                        Pair((maxDimension * ratio).toInt().coerceAtLeast(1), maxDimension)
                    }
                    Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
                } else {
                    originalBitmap
                }

                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                val byteArray = outputStream.toByteArray()
                "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Decodes a Base64 string (with or without data URL prefix) to a Compose ImageBitmap.
     */
    fun base64ToImageBitmap(base64Str: String?): ImageBitmap? {
        if (base64Str.isNullOrBlank()) return null
        return try {
            val cleanBase64 = if (base64Str.contains(",")) {
                base64Str.substringAfter(",")
            } else {
                base64Str
            }
            val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Creates a sample document bitmap encoded as JPEG Base64 data URL for testing and demo.
     */
    fun createSampleDocumentBase64(
        title: String,
        subtitle: String,
        bgColor: Int = 0xFF1E3A8A.toInt()
    ): String {
        val width = 480
        val height = 320
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        val bgPaint = android.graphics.Paint().apply {
            color = bgColor
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val cardPaint = android.graphics.Paint().apply {
            color = 0xFFFFFFFF.toInt()
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawRoundRect(20f, 20f, (width - 20).toFloat(), (height - 20).toFloat(), 16f, 16f, cardPaint)

        val bannerPaint = android.graphics.Paint().apply {
            color = 0xFFDC2626.toInt() // Red
            textSize = 12f
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.drawText("*** DEMO TEST DATA - NOT A REAL ID ***", width / 2f, 42f, bannerPaint)

        val textPaint = android.graphics.Paint().apply {
            color = 0xFF111827.toInt()
            textSize = 22f
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.drawText(title, width / 2f, height / 2f - 15, textPaint)

        val subPaint = android.graphics.Paint().apply {
            color = 0xFF6B7280.toInt()
            textSize = 15f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.drawText(subtitle, width / 2f, height / 2f + 25, subPaint)

        val badgePaint = android.graphics.Paint().apply {
            color = 0xFF2563EB.toInt() // Blue badge
            textSize = 13f
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.drawText("PARKSHARE PROTOTYPE DEMO CONTENT", width / 2f, height / 2f + 70, badgePaint)

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        return "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}

