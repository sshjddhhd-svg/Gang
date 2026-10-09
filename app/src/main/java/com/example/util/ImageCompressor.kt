package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageCompressor {

    suspend fun uriToCompressedBase64(
        context: Context,
        uriString: String,
        maxDimension: Int = 800,
        quality: Int = 75
    ): String = withContext(Dispatchers.IO) {
        if (!uriString.startsWith("content://") && !uriString.startsWith("file://")) {
            return@withContext uriString
        }

        try {
            val uri = Uri.parse(uriString)
            val resolver = context.contentResolver

            // First decode bounds
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) {
                return@withContext uriString
            }

            var inSampleSize = 1
            val maxEdge = max(origWidth, origHeight)
            while (maxEdge / (inSampleSize * 2) >= maxDimension) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }

            val decodedBitmap = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext uriString

            // Scale down if still larger than maxDimension
            val scaledBitmap = if (decodedBitmap.width > maxDimension || decodedBitmap.height > maxDimension) {
                val ratio = minOf(
                    maxDimension.toFloat() / decodedBitmap.width,
                    maxDimension.toFloat() / decodedBitmap.height
                )
                val targetW = (decodedBitmap.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (decodedBitmap.height * ratio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(decodedBitmap, targetW, targetH, true).also {
                    if (it != decodedBitmap) decodedBitmap.recycle()
                }
            } else {
                decodedBitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val bytes = outputStream.toByteArray()
            scaledBitmap.recycle()

            val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64String"
        } catch (e: Exception) {
            e.printStackTrace()
            uriString
        }
    }
}
