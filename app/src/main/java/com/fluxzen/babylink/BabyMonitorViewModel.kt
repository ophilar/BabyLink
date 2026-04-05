package com.fluxzen.babylink

import androidx.lifecycle.ViewModel
import com.fluxzen.ui_design.sync.NearbyTransportLayer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BabyMonitorViewModel @Inject constructor(
    private val nearbyTransport: NearbyTransportLayer
) : ViewModel() {

    fun startBabyStation(deviceName: String) {
        nearbyTransport.startAdvertising(deviceName)
    }

    fun startParentStation() {
        nearbyTransport.startDiscovery()
    }

    fun stop() {
        nearbyTransport.stopAll()
    }
}
