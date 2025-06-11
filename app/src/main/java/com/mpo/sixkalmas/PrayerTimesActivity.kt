package com.mpo.sixkalmas

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.mpo.sixkalmas.databinding.ActivityPrayerTimesBinding
import java.text.SimpleDateFormat
import java.util.*

class PrayerTimesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPrayerTimesBinding
    private var mInterstitialAd: InterstitialAd? = null
    private var nativeAd: NativeAd? = null
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private lateinit var alarmManager: AlarmManager
    private lateinit var mediaPlayer: MediaPlayer

    companion object {
        const val CHANNEL_ID = "prayer_alarm_channel"
        const val ALARM_ACTION = "com.mpo.sixkalmas.ALARM_TRIGGERED"
    }

    private val alarmReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ALARM_ACTION) {
                playAzan()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrayerTimesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize MediaPlayer
        mediaPlayer = MediaPlayer()

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this)

        // Initialize alarm manager
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        createNotificationChannel()

        // Register alarm receiver
        registerReceiver(alarmReceiver, IntentFilter(ALARM_ACTION))

        // Set up time pickers and alarm switches for each prayer time
        setupPrayerTime("Fajr", binding.fajrTime, binding.fajrAlarmSwitch)
        setupPrayerTime("Dhuhr", binding.dhuhrTime, binding.dhuhrAlarmSwitch)
        setupPrayerTime("Asr", binding.asrTime, binding.asrAlarmSwitch)
        setupPrayerTime("Maghrib", binding.maghribTime, binding.maghribAlarmSwitch)
        setupPrayerTime("Isha", binding.ishaTime, binding.ishaAlarmSwitch)

        // Load native ad
        loadNativeAd()
    }

    private fun setupPrayerTime(prayerName: String, timeTextView: TextView, alarmSwitch: SwitchMaterial) {
        // Set up time picker
        timeTextView.setOnClickListener {
            val currentTime = timeFormat.parse(timeTextView.text.toString())
            val calendar = Calendar.getInstance()
            currentTime?.let { calendar.time = it }

            TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)
                    timeTextView.text = timeFormat.format(calendar.time)
                    
                    // Update alarm if switch is on
                    if (alarmSwitch.isChecked) {
                        scheduleAlarm(prayerName, calendar.timeInMillis)
                    }
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            ).show()
        }

        // Set up alarm switch
        alarmSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val time = timeFormat.parse(timeTextView.text.toString())
                time?.let {
                    val calendar = Calendar.getInstance()
                    calendar.time = it
                    scheduleAlarm(prayerName, calendar.timeInMillis)
                    Toast.makeText(this, "$prayerName alarm set", Toast.LENGTH_SHORT).show()
                }
            } else {
                cancelAlarm(prayerName)
                Toast.makeText(this, "$prayerName alarm cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun scheduleAlarm(prayerName: String, timeInMillis: Long) {
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            action = ALARM_ACTION
            putExtra("prayer_name", prayerName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            prayerName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // If the time has already passed today, schedule for tomorrow
        val calendar = Calendar.getInstance()
        if (timeInMillis <= calendar.timeInMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val timeCalendar = Calendar.getInstance()
            timeCalendar.timeInMillis = timeInMillis
            calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
            calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
        }

        try {
            // Cancel any existing alarm for this prayer
            alarmManager.cancel(pendingIntent)

            // Schedule the new alarm
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }

            // Also set a repeating alarm for daily prayers
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )

            Toast.makeText(this, "$prayerName alarm set for ${timeFormat.format(calendar.time)}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to set alarm for $prayerName", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelAlarm(prayerName: String) {
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            action = ALARM_ACTION
            putExtra("prayer_name", prayerName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            prayerName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.cancel(pendingIntent)
            Toast.makeText(this, "$prayerName alarm cancelled", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to cancel alarm for $prayerName", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Prayer Alarms"
            val descriptionText = "Channel for prayer time alarms"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun playAzan() {
        try {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.reset()
            mediaPlayer = MediaPlayer.create(this, R.raw.azan)
            mediaPlayer.setOnCompletionListener { 
                it.release()
                mediaPlayer = MediaPlayer()
            }
            mediaPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error playing azan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadNativeAd() {
        val adLoader = AdLoader.Builder(this, getString(R.string.native_ad_unit_id))
            .forNativeAd { nativeAd ->
                // Clean up the previous native ad
                this.nativeAd?.destroy()
                this.nativeAd = nativeAd

                // Populate the native ad view
                populateNativeAdView(nativeAd, binding.nativeAdView.root)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    // Hide the native ad view if ad fails to load
                    binding.nativeAdView.root.visibility = android.view.View.GONE
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        // Set the media view
        adView.findViewById<ImageView>(R.id.adIcon)?.let { iconView ->
            nativeAd.icon?.drawable?.let { drawable ->
                iconView.setImageDrawable(drawable)
                iconView.visibility = android.view.View.VISIBLE
            } ?: run {
                iconView.visibility = android.view.View.GONE
            }
        }

        // Set the headline
        adView.findViewById<TextView>(R.id.adHeadline)?.let { headlineView ->
            headlineView.text = nativeAd.headline
            headlineView.visibility = android.view.View.VISIBLE
        }

        // Set the advertiser
        adView.findViewById<TextView>(R.id.adAdvertiser)?.let { advertiserView ->
            nativeAd.advertiser?.let {
                advertiserView.text = it
                advertiserView.visibility = android.view.View.VISIBLE
            } ?: run {
                advertiserView.visibility = android.view.View.GONE
            }
        }

        // Set the call to action
        adView.findViewById<Button>(R.id.adCallToAction)?.let { callToActionView ->
            nativeAd.callToAction?.let {
                callToActionView.text = it
                callToActionView.visibility = android.view.View.VISIBLE
            } ?: run {
                callToActionView.visibility = android.view.View.GONE
            }
        }

        // Set the native ad view
        adView.setNativeAd(nativeAd)
        adView.visibility = android.view.View.VISIBLE
    }

    private fun showInterstitialAd(onAdClosed: () -> Unit) {
        if (mInterstitialAd != null) {
            mInterstitialAd?.show(this)
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null
                    onAdClosed()
                }
            }
        } else {
            onAdClosed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(alarmReceiver)
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.release()
            nativeAd?.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
} 