package de.konstellarum.synesis.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PackageReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            ?: return

        context.startActivity(launchIntent)
    }
}
