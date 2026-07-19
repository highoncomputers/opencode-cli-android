package ai.opencode.android.opencode

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import ai.opencode.android.OpenCodeApplication
import ai.opencode.android.R

/**
 * Foreground service that keeps the opencode process alive
 * when the terminal activity is in the background.
 */
class OpenCodeService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, OpenCodeApplication.NOTIFICATION_CHANNEL_TERMINAL)
            .setContentTitle("OpenCode CLI")
            .setContentText("OpenCode is running")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
