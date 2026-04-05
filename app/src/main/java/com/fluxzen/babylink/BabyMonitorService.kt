package com.fluxzen.babylink

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.fluxzen.ui_design.sync.NearbyTransportLayer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import javax.inject.Inject

@AndroidEntryPoint
class BabyMonitorService : Service() {

    @Inject
    lateinit var nearbyTransport: NearbyTransportLayer

    private val notificationId = 1
    private val channelId = "baby_monitor_channel"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var audioPipeline: AudioProcessingPipeline

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        audioPipeline = AudioProcessingPipeline(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Baby Monitor Active")
            .setContentText("Monitoring for crying...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()

        startForeground(notificationId, notification)

        audioPipeline.start(serviceScope) {
            Log.i("BabyMonitorService", "Cry Detected! Initiating alert/stream...")
            // Integrate with nearbyTransport or alert system here
        }
        
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
        audioPipeline.stop()
        serviceScope.cancel()
        nearbyTransport.stopAll()
    }
}
