package com.fluxzen.babylink

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.fluxzen.ui_design.sync.NearbyTransportLayer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BabyMonitorService : Service() {

    @Inject
    lateinit var nearbyTransport: NearbyTransportLayer

    private val notificationId = 1
    private val channelId = "baby_monitor_channel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Baby Monitor Active")
            .setContentText("Monitoring for crying...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()

        startForeground(notificationId, notification)

        // TODO: Initialize TFLite Cry Detection
        // TODO: Initialize WebRTC Engine
        
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "Baby Monitor Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        nearbyTransport.stopAll()
    }
}
