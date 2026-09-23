package de.konstellarum.phronesis.update

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast

class UpdateLauncher(
    private val context: Context,
) {
    fun install(downloadUrl: String) {
        val downloadManager = context.getSystemService(DownloadManager::class.java)
            ?: run {
                Toast.makeText(context, "DownloadManager ist nicht verfügbar", Toast.LENGTH_LONG).show()
                return
            }

        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle("Phronesis Update")
            .setDescription("Update wird heruntergeladen")
            .setMimeType(APK_MIME_TYPE)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                APK_FILE_NAME,
            )

        val downloadId = downloadManager.enqueue(request)
        Toast.makeText(context, "Update-Download gestartet (#$downloadId)", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val APK_FILE_NAME = "phronesis-latest.apk"
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}
