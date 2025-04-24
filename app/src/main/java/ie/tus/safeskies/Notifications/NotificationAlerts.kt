package ie.tus.finalyearproject.Notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.mutableStateListOf
import androidx.core.app.NotificationCompat
import ie.tus.safeskies.R

data class Notifications(
    val title: String,
    val message: String
)

class NotificationAlerts(private val context: Context) {
    val notifications = mutableStateListOf<Notifications>()

    fun triggerNotification(title: String, message: String) {
        val newNotification = Notifications(title, message)
        notifications.add(newNotification)

        showNotification(title, message)
    }

    private fun showNotification(title: String, message: String) {
        val channelID = "weather_alert_warning"
        val channelName = "Weather Alert | Dangerous Weather‼️"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelID,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for dangerous weather alert"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 500, 500)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 500, 500))
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}