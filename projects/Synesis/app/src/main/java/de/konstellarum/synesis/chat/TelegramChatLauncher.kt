package de.konstellarum.synesis.chat

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import de.konstellarum.synesis.core.chat.TelegramChatLink

class TelegramChatLauncher(private val context: Context) {

    fun open(link: TelegramChatLink): Boolean {
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link.appUri))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(appIntent)
            return true
        } catch (_: ActivityNotFoundException) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link.fallbackUrl))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return try {
                context.startActivity(fallbackIntent)
                true
            } catch (_: ActivityNotFoundException) {
                false
            }
        }
    }
}
