package ai.opencode.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import java.util.concurrent.Executors

class OpenCodeApplication : Application() {

    companion object {
        const val NOTIFICATION_CHANNEL_TERMINAL = "terminal_sessions"
        const val NOTIFICATION_CHANNEL_SETUP = "setup_progress"

        lateinit var instance: OpenCodeApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val terminalChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_TERMINAL,
                getString(R.string.notification_channel_terminal),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when terminal sessions are active"
            }

            val setupChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_SETUP,
                "Setup Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows setup progress"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(terminalChannel)
            manager.createNotificationChannel(setupChannel)
        }
    }
}
