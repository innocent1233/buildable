package com.libraryx.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Replaces src/lib/mobile.ts. The original `isNativeApp()`/`savePdf()`/`printOrPdf()`
 * existed to branch between a plain web download and the Capacitor-wrapped APK shell
 * (which had to open generated PDFs in a viewer instead of doing a browser-style download).
 * Now that this *is* the native app, there's only one path: write to the Downloads
 * collection via MediaStore and hand back a `content://` Uri the caller can view or share —
 * this is the direct equivalent of the original's native-mode fallback branch.
 */
object FileSaver {

    /** Saves raw bytes (PDF, CSV, JSON, ...) to the Downloads folder and returns a shareable Uri. */
    fun saveToDownloads(context: Context, filename: String, mimeType: String, bytes: ByteArray): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.also {
                context.contentResolver.openOutputStream(it)?.use { out -> out.write(bytes) }
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, filename)
            FileOutputStream(file).use { it.write(bytes) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
    }

    /** Builds a share/open Intent for a previously-saved file — mirrors `window.open(url, "_blank")`. */
    fun viewOrShareIntent(uri: Uri, mimeType: String): Intent {
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return Intent.createChooser(view, "Open with")
    }
}
