package com.fluxzen.babybeam

data class SignalingMessage(
    val type: String, // "offer", "answer", "candidate", "toggle_light", "toggle_mic", "toggle_lullaby"
    val sdp: String? = null,
    val candidate: String? = null,
    val sdpMid: String? = null,
    val sdpMLineIndex: Int? = null,
    val isOn: Boolean? = null
)
