package de.konstellarum.synesis.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

class UpdateDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (downloadId == -1L) return

        val downloadManager = context.getSystemService(DownloadManager::class.java) ?: return
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query) ?: return

        cursor.use {
            if (!it.moveToFirst()) return
            val statusIndex = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
            if (statusIndex == -1 || it.getInt(statusIndex) != DownloadManager.STATUS_SUCCESSFUL) return
        }

        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: run {
                Toast.makeText(context, "Download-Verzeichnis nicht verfügbar", Toast.LENGTH_LONG).show()
                return
            }

        val apkFile = File(downloadsDir, UpdateLauncher.APK_FILE_NAME)

        if (!apkFile.exists()) {
            Toast.makeText(context, "Update-Datei nicht gefunden", Toast.LENGTH_LONG).show()
            return
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )

        val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = apkUri
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        }

        context.startActivity(installIntent)
    }
}
