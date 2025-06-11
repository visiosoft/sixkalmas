package com.mpo.sixkalmas

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.widget.Toast

class AlarmReceiver : BroadcastReceiver() {
    private var mediaPlayer: MediaPlayer? = null

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val prayerName = intent.getStringExtra("prayer_name") ?: return

            // Create notification channel for Android O and above
            createNotificationChannel(context)

            // Play azan with wake lock
            playAzan(context)

            // Show notification
            showNotification(context, prayerName)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error in prayer alarm", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playAzan(context: Context) {
        try {
            // Release any existing MediaPlayer
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.release()
            }

            // Create and configure new MediaPlayer
            MediaPlayer.create(context, R.raw.azan)?.let { newPlayer ->
                mediaPlayer = newPlayer
                mediaPlayer?.apply {
                    setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                    setOnCompletionListener { mp ->
                        try {
                            mp.release()
                            mediaPlayer = null
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    setOnErrorListener { mp, what, extra ->
                        try {
                            mp.release()
                            mediaPlayer = null
                            Toast.makeText(context, "Error playing azan", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        true
                    }
                    start()
                }
            } ?: run {
                Toast.makeText(context, "Could not create media player", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error playing azan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showNotification(context: Context, prayerName: String) {
        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Prayer Time")
                .setContentText("It's time for $prayerName prayer")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setSound(null) // Disable default sound since we're playing azan
                .setVibrate(longArrayOf(0, 500, 200, 500)) // Vibrate pattern

            with(NotificationManagerCompat.from(context)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notify(prayerName.hashCode(), builder.build())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Prayer Alarms"
                val descriptionText = "Channel for prayer time alarms"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val CHANNEL_ID = "prayer_alarm_channel"
    }
} 