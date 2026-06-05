package app.clearspace.network.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import kotlin.math.max

object MediaUtils {

    fun compressAndSaveImage(context: Context, uri: Uri, maxSize: Int = 2048): Pair<String, String>? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close() ?: return null
            
            // Calculate scale
            val ratio = max(originalBitmap.width.toFloat() / maxSize, originalBitmap.height.toFloat() / maxSize)
            val scaledBitmap = if (ratio > 1) {
                Bitmap.createScaledBitmap(originalBitmap, (originalBitmap.width / ratio).toInt(), (originalBitmap.height / ratio).toInt(), true)
            } else {
                originalBitmap
            }

            // Save full scaled image
            val fileName = "img_${UUID.randomUUID()}.jpg"
            val file = File(context.filesDir, fileName)
            val outputStream = FileOutputStream(file)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            outputStream.flush()
            outputStream.close()

            // Generate tiny thumbnail
            val thumbScale = max(scaledBitmap.width.toFloat() / 150, scaledBitmap.height.toFloat() / 150)
            val tinyBitmap = if (thumbScale > 1) {
                Bitmap.createScaledBitmap(scaledBitmap, (scaledBitmap.width / thumbScale).toInt(), (scaledBitmap.height / thumbScale).toInt(), true)
            } else {
                scaledBitmap
            }
            val thumbOutputStream = ByteArrayOutputStream()
            tinyBitmap.compress(Bitmap.CompressFormat.JPEG, 60, thumbOutputStream)
            val base64Thumb = Base64.encodeToString(thumbOutputStream.toByteArray(), Base64.NO_WRAP)

            if (originalBitmap != scaledBitmap) originalBitmap.recycle()
            if (scaledBitmap != tinyBitmap) scaledBitmap.recycle()
            tinyBitmap.recycle()

            Pair(file.absolutePath, base64Thumb)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun copyAndSaveVideo(context: Context, uri: Uri): Pair<String, String>? {
        return try {
            // First, extract a thumbnail
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val frame = retriever.getFrameAtTime(0) ?: return null
            retriever.release()

            val thumbScale = max(frame.width.toFloat() / 150, frame.height.toFloat() / 150)
            val tinyBitmap = if (thumbScale > 1) {
                Bitmap.createScaledBitmap(frame, (frame.width / thumbScale).toInt(), (frame.height / thumbScale).toInt(), true)
            } else {
                frame
            }
            val thumbOutputStream = ByteArrayOutputStream()
            tinyBitmap.compress(Bitmap.CompressFormat.JPEG, 60, thumbOutputStream)
            val base64Thumb = Base64.encodeToString(thumbOutputStream.toByteArray(), Base64.NO_WRAP)
            frame.recycle()
            tinyBitmap.recycle()

            // Copy video file (Since trimming video without transcoding is error prone across devices, we just copy)
            val fileName = "vid_${UUID.randomUUID()}.mp4"
            val file = File(context.filesDir, fileName)
            
            val inputStream = context.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.flush()
            outputStream.close()

            Pair(file.absolutePath, base64Thumb)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun compressAndSaveProfilePhoto(context: Context, uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close() ?: return null
            
            val ratio = max(originalBitmap.width.toFloat() / 512, originalBitmap.height.toFloat() / 512)
            val scaledBitmap = if (ratio > 1) {
                Bitmap.createScaledBitmap(originalBitmap, (originalBitmap.width / ratio).toInt(), (originalBitmap.height / ratio).toInt(), true)
            } else {
                originalBitmap
            }

            val fileName = "avatar_${UUID.randomUUID()}.jpg"
            val file = File(context.filesDir, fileName)
            val outputStream = FileOutputStream(file)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()

            if (originalBitmap != scaledBitmap) originalBitmap.recycle()
            scaledBitmap.recycle()

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
