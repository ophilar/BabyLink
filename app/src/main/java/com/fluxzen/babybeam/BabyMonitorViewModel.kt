package com.fluxzen.babybeam

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluxzen.ui_design.sync.NearbyTransportLayer
import com.fluxzen.ui_design.sync.WebRtcManager
import com.fluxzen.ui_design.sync.SignalingMessage
import com.fluxzen.ui_design.sync.SignalingClient
import com.fluxzen.ui_design.security.SecurityUtil
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import javax.inject.Inject

@HiltViewModel
class BabyMonitorViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val nearbyTransport: NearbyTransportLayer,
    val webRtcManager: WebRtcManager,
    private val securityUtil: SecurityUtil
) : ViewModel(), SignalingClient {

    private val TAG = "BabyMonitorViewModel"
    private val gson = Gson()

    private var _isSender = false

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus = _connectionStatus.asStateFlow()

    private val _connectedDevices = MutableStateFlow<List<String>>(emptyList())
    val connectedDevices = _connectedDevices.asStateFlow()

    private val _pendingConnections = MutableStateFlow<List<String>>(emptyList())
    val pendingConnections = _pendingConnections.asStateFlow()

    private val _isCryDetected = MutableStateFlow(false)
    val isCryDetected = _isCryDetected.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled = _vibrationEnabled.asStateFlow()

    private val _visualAlertEnabled = MutableStateFlow(true)
    val visualAlertEnabled = _visualAlertEnabled.asStateFlow()

    private val _isNightLightOn = MutableStateFlow(false)
    val isNightLightOn = _isNightLightOn.asStateFlow()

    private val _isMicActive = MutableStateFlow(false)
    val isMicActive = _isMicActive.asStateFlow()

    private val _isLullabyPlaying = MutableStateFlow(false)
    val isLullabyPlaying = _isLullabyPlaying.asStateFlow()

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            null
        }
    }


    init {
        webRtcManager.signalingClient = this
        observeEvents()
    }

    private fun observeEvents() {
        viewModelScope.launch {
            nearbyTransport.events.collectLatest { event ->
                when (event) {
                    is NearbyTransportLayer.TransportEvent.DataReceived -> {
                        processReceivedData(event)
                    }
                    is NearbyTransportLayer.TransportEvent.AdvertisingStarted -> _connectionStatus.value = "Advertising..."
                    is NearbyTransportLayer.TransportEvent.DiscoveryStarted -> _connectionStatus.value = "Discovering..."
                    is NearbyTransportLayer.TransportEvent.ConnectionResult -> {
                        if (event.statusCode == 0) {
                            _connectionStatus.value = "Connected"
                            if (!_isSender) {
                                webRtcManager.initWebRTC()
                                webRtcManager.startPeerConnection()
                            }
                        } else {
                            _connectionStatus.value = "Connection Failed"
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private suspend fun processReceivedData(event: NearbyTransportLayer.TransportEvent.DataReceived) {
        val messageStr = String(event.payload.asBytes() ?: byteArrayOf())

        val verifiedPayload = withContext(Dispatchers.Default) {
            securityUtil.verifySignedMessage(context, messageStr)
        }

        if (verifiedPayload != null) {
            try {
                val msg = withContext(Dispatchers.Default) { gson.fromJson(verifiedPayload, SignalingMessage::class.java) }
                if (msg?.type != null) {
                    handleSignalingMessage(msg)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse verified payload as JSON: $e")
            }
        } else {
            Log.w(TAG, "Received message failed security verification or was from untrusted peer.")
        }
    }

    private fun handleSignalingMessage(msg: SignalingMessage) {
        when (msg.type) {
            "offer" -> {
                msg.sdp?.let { sdp ->
                    webRtcManager.handleOffer(SessionDescription(SessionDescription.Type.OFFER, sdp))
                }
            }
            "answer" -> {
                msg.sdp?.let { sdp ->
                    webRtcManager.handleAnswer(SessionDescription(SessionDescription.Type.ANSWER, sdp))
                }
            }
            "candidate" -> {
                val sdp = msg.sdp
                val mid = msg.sdpMid
                val index = msg.sdpMLineIndex
                if (sdp != null && mid != null && index != null) {
                    webRtcManager.handleIceCandidate(IceCandidate(mid, index, sdp))
                }
            }
            "toggle_light" -> msg.isOn?.let { _isNightLightOn.value = it }
            "toggle_mic" -> msg.isOn?.let { _isMicActive.value = it }
            "toggle_lullaby" -> msg.isOn?.let { _isLullabyPlaying.value = it }
            "cry_detected" -> triggerAlert()
        }
    }

    override fun sendOffer(sdp: SessionDescription) {
        val msg = SignalingMessage(type = "offer", sdp = sdp.description)
        sendViaNearby(msg)
    }

    override fun sendAnswer(sdp: SessionDescription) {
        val msg = SignalingMessage(type = "answer", sdp = sdp.description)
        sendViaNearby(msg)
    }

    override fun sendIceCandidate(candidate: IceCandidate) {
        val msg = SignalingMessage(
            type = "candidate",
            sdp = candidate.sdp,
            sdpMid = candidate.sdpMid,
            sdpMLineIndex = candidate.sdpMLineIndex
        )
        sendViaNearby(msg)
    }

    private fun sendViaNearby(msg: SignalingMessage) {
        val json = gson.toJson(msg)
        val signedPayload = securityUtil.generateSignedMessage(context, json)
        nearbyTransport.broadcastMessage(signedPayload)
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
             vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    fun startMonitoring(activityContext: Context) {
        webRtcManager.initWebRTC()
        _isSender = true
        webRtcManager.startLocalVideoAndAudio()

        val intent = Intent(activityContext, BabyMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activityContext.startForegroundService(intent)
        } else {
            activityContext.startService(intent)
        }
        nearbyTransport.startAdvertising("BabyDevice_${System.currentTimeMillis()}")
    }

    fun startDiscovery() {
        webRtcManager.initWebRTC()
        _isSender = false
        nearbyTransport.startDiscovery()
    }

    fun stop() {
        webRtcManager.stop()
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

    fun acceptConnection(deviceName: String) {
        _pendingConnections.value = _pendingConnections.value - deviceName
        _connectedDevices.value = _connectedDevices.value + deviceName

        if (_isSender) {
            webRtcManager.startPeerConnection()
            webRtcManager.createOffer()
        }
    }

    fun denyConnection(deviceName: String) {
        _pendingConnections.value = _pendingConnections.value - deviceName
    }

    fun toggleNightLight() {
        val newState = !_isNightLightOn.value
        _isNightLightOn.value = newState
        sendViaNearby(SignalingMessage(type = "toggle_light", isOn = newState))
    }

    fun toggleMic() {
        val newState = !_isMicActive.value
        _isMicActive.value = newState
        sendViaNearby(SignalingMessage(type = "toggle_mic", isOn = newState))
    }

    fun toggleLullaby() {
        val newState = !_isLullabyPlaying.value
        _isLullabyPlaying.value = newState
        sendViaNearby(SignalingMessage(type = "toggle_lullaby", isOn = newState))
    }
}
