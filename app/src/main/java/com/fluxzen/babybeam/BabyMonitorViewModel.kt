package com.fluxzen.babybeam

import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluxzen.ui_design.sync.NearbyTransportLayer
import com.google.android.gms.nearby.connection.Payload
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BabyMonitorViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val nearbyTransport: NearbyTransportLayer
) : ViewModel() {

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus = _connectionStatus.asStateFlow()

    private val _isCryDetected = MutableStateFlow(false)
    val isCryDetected = _isCryDetected.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled = _vibrationEnabled.asStateFlow()

    private val _visualAlertEnabled = MutableStateFlow(true)
    val visualAlertEnabled = _visualAlertEnabled.asStateFlow()

    init {
        observeEvents()
    }

    private fun observeEvents() {
        viewModelScope.launch {
            nearbyTransport.events.collectLatest { event ->
                when (event) {
                    is NearbyTransportLayer.TransportEvent.DataReceived -> {
                        val message = String(event.payload.asBytes() ?: byteArrayOf())
                        if (message == "cry_detected") {
                            triggerAlert()
                        }
                    }
                    is NearbyTransportLayer.TransportEvent.AdvertisingStarted -> _connectionStatus.value = "Advertising..."
                    is NearbyTransportLayer.TransportEvent.DiscoveryStarted -> _connectionStatus.value = "Discovering..."
                    is NearbyTransportLayer.TransportEvent.ConnectionResult -> {
                        _connectionStatus.value = if (event.statusCode == 0) "Connected" else "Connection Failed"
                    }
                    else -> {}
                }
            }
        }
    }

    private fun triggerAlert() {
        if (_visualAlertEnabled.value) {
            _isCryDetected.value = true
            viewModelScope.launch {
                delay(10000) // Reset alert after 10s
                _isCryDetected.value = false
            }
        }
        
        if (_vibrationEnabled.value) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    fun startMonitoring(activityContext: Context) {
        val intent = Intent(activityContext, BabyMonitorService::class.java)
        activityContext.startForegroundService(intent)
        nearbyTransport.startAdvertising("BabyDevice_${System.currentTimeMillis()}")
    }

    fun startDiscovery() {
        nearbyTransport.startDiscovery()
    }

    fun stop() {
        nearbyTransport.stopAll()
        _connectionStatus.value = "Disconnected"
    }

    fun setVibration(enabled: Boolean) {
        _vibrationEnabled.value = enabled
    }

    fun setVisualAlert(enabled: Boolean) {
        _visualAlertEnabled.value = enabled
    }

    fun dismissAlert() {
        _isCryDetected.value = false
    }
}





