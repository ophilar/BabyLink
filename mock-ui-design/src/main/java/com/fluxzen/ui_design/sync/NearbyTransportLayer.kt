package com.fluxzen.ui_design.sync
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NearbyTransportLayer @Inject constructor() {
    sealed class TransportEvent {
        class DataReceived(val endpointId: String, val payload: com.google.android.gms.nearby.connection.Payload) : TransportEvent()
        object AdvertisingStarted : TransportEvent()
        object DiscoveryStarted : TransportEvent()
        class ConnectionResult(val statusCode: Int) : TransportEvent()
    }
    val events: SharedFlow<TransportEvent> = MutableSharedFlow()
    fun broadcastMessage(message: String) {}
    fun startAdvertising(name: String) {}
    fun startDiscovery() {}
    fun stopAll() {}
}
