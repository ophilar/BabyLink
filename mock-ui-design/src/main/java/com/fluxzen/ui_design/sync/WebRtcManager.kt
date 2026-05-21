package com.fluxzen.ui_design.sync

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
open class WebRtcManager @Inject constructor() {
    var onAudioSamplesReady: ((org.webrtc.audio.JavaAudioDeviceModule.AudioSamples) -> Unit)? = null
    val remoteVideoTrackFlow: StateFlow<org.webrtc.VideoTrack?> = MutableStateFlow(null)
    var signalingClient: SignalingClient? = null

    fun initWebRTC(context: android.content.Context) {}
    fun initWebRTC() {}
    fun startPeerConnection() {}
    fun startLocalVideoAndAudio() {}
    fun createOffer() {}
    fun handleOffer(sdp: org.webrtc.SessionDescription) {}
    fun handleAnswer(sdp: org.webrtc.SessionDescription) {}
    fun handleIceCandidate(candidate: org.webrtc.IceCandidate) {}
    fun stop() {}
    fun getEglBaseContext(): org.webrtc.EglBase.Context = org.webrtc.EglBase.create().eglBaseContext
}
