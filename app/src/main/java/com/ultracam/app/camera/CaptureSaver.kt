package com.ultracam.app.camera

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Where photos go:
 *  - Android 10+ : MediaStore, Pictures/ULTRACAM (scoped storage, no permission needed)
 *  - Android 8/9 : app external pictures dir + MediaScanner so gallery apps see it
 */
object CaptureSaver {

    class Request(
        val options: ImageCapture.OutputFileOptions,
        val mediaUri: Uri?,
        val file: File?,
        val name: String
    )

    fun newRequest(context: Context): Request {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val name = "ULTRACAM_$stamp.jpg"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ULTRACAM")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val collection =
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = context.contentResolver.insert(collection, values)
                ?: throw IllegalStateException("MediaStore rejected the image")
            Request(
                options = ImageCapture.OutputFileOptions.Builder(context.contentResolver, uri).build(),
                mediaUri = uri,
                file = null,
                name = name
            )
        } else {
            @Suppress("DEPRECATION")
            val dir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "ULTRACAM"
            ).apply { mkdirs() }
            val file = File(dir, name)
            Request(
                options = ImageCapture.OutputFileOptions.Builder(file).build(),
                mediaUri = null,
                file = file,
                name = name
            )
        }
    }

    fun finalize(context: Context, request: Request) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val uri = request.mediaUri ?: return
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            try {
                context.contentResolver.update(uri, values, null, null)
            } catch (_: Exception) {
            }
        } else {
            val file = request.file ?: return
            try {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    arrayOf("image/jpeg"),
                    null
                )
            } catch (_: Exception) {
            }
        }
    }

    fun cleanup(context: Context, request: Request) {
        try {
            val uri = request.mediaUri
            if (uri != null) {
                context.contentResolver.delete(uri, null, null)
            } else {
                request.file?.delete()
            }
        } catch (_: Exception) {
        }
    }

    /** A shareable content:// URI for the request, on every API level. */
    fun shareableUri(context: Context, request: Request): Uri =
        request.mediaUri ?: request.file?.let {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)
        } ?: Uri.EMPTY
}
