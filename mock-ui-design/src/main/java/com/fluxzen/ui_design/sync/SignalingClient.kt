package com.fluxzen.ui_design.sync

import javax.inject.Inject
import javax.inject.Singleton

interface SignalingClient {
    fun sendOffer(sdp: org.webrtc.SessionDescription)
    fun sendAnswer(sdp: org.webrtc.SessionDescription)
    fun sendIceCandidate(candidate: org.webrtc.IceCandidate)
}

data class SignalingMessage(
    val type: String = "",
    val sdp: String? = null,
    val sdpMid: String? = null,
    val sdpMLineIndex: Int? = null,
    val isOn: Boolean? = null
)
