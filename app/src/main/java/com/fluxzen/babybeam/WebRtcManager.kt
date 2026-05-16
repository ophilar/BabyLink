package com.fluxzen.babybeam

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.Collections

class WebRtcManager(private val context: Context, private val signalingClient: SignalingClient) : PeerConnection.Observer {
    private val TAG = "WebRtcManager"
    private var factory: PeerConnectionFactory? = null
    var peerConnection: PeerConnection? = null
    var localVideoTrack: VideoTrack? = null
    var localAudioTrack: AudioTrack? = null
    var remoteVideoTrack: VideoTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private val eglBase: EglBase = EglBase.create()
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private val _remoteVideoTrackFlow = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrackFlow = _remoteVideoTrackFlow.asStateFlow()

    var onAudioSamplesReady: ((JavaAudioDeviceModule.AudioSamples) -> Unit)? = null

    init {
        initWebRTC()
    }

    private fun initWebRTC() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )
        val options = PeerConnectionFactory.Options()
        val defaultVideoEncoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val defaultVideoDecoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        val audioDeviceModule = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .setSamplesReadyCallback { audioSamples ->
                onAudioSamplesReady?.invoke(audioSamples)
            }
            .createAudioDeviceModule()

        factory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoEncoderFactory(defaultVideoEncoderFactory)
            .setVideoDecoderFactory(defaultVideoDecoderFactory)
            .setAudioDeviceModule(audioDeviceModule)
            .createPeerConnectionFactory()
    }

    fun getEglBaseContext(): EglBase.Context = eglBase.eglBaseContext

    fun startLocalVideoAndAudio() {
        if (factory == null) return

        videoCapturer = createCameraCapturer(context)
        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        val videoSource = factory?.createVideoSource(videoCapturer?.isScreencast == true)
        videoCapturer?.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
        videoCapturer?.startCapture(1024, 720, 30)

        localVideoTrack = factory?.createVideoTrack("ARDAMSv0", videoSource)
        localVideoTrack?.setEnabled(true)

        val audioConstraints = MediaConstraints()
        audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
        audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
        audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
        val audioSource = factory?.createAudioSource(audioConstraints)
        localAudioTrack = factory?.createAudioTrack("ARDAMSa0", audioSource)
        localAudioTrack?.setEnabled(true)
    }

    fun startPeerConnection() {
        if (peerConnection != null) return

        val rtcConfig = PeerConnection.RTCConfiguration(
            listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
        )
        peerConnection = factory?.createPeerConnection(rtcConfig, this)

        localVideoTrack?.let { peerConnection?.addTrack(it, Collections.singletonList("ARDAMS")) }
        localAudioTrack?.let { peerConnection?.addTrack(it, Collections.singletonList("ARDAMS")) }
    }

    fun createOffer() {
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                peerConnection?.setLocalDescription(this, sdp)
                sdp?.let { signalingClient.sendOffer(it) }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) { Log.e(TAG, "Create offer failed: $error") }
            override fun onSetFailure(error: String?) { Log.e(TAG, "Set local description failed: $error") }
        }, MediaConstraints())
    }

    fun createAnswer() {
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                peerConnection?.setLocalDescription(this, sdp)
                sdp?.let { signalingClient.sendAnswer(it) }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) { Log.e(TAG, "Create answer failed: $error") }
            override fun onSetFailure(error: String?) { Log.e(TAG, "Set local description failed: $error") }
        }, MediaConstraints())
    }

    fun handleOffer(sdp: SessionDescription) {
        startPeerConnection()
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                createAnswer()
            }
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(error: String?) { Log.e(TAG, "Set remote description failed: $error") }
        }, sdp)
    }

    fun handleAnswer(sdp: SessionDescription) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(error: String?) { Log.e(TAG, "Set remote description failed: $error") }
        }, sdp)
    }

    fun handleIceCandidate(iceCandidate: IceCandidate) {
        peerConnection?.addIceCandidate(iceCandidate)
    }

    private fun createCameraCapturer(context: Context): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        for (deviceName in enumerator.deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        for (deviceName in enumerator.deviceNames) {
            if (enumerator.isBackFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        return null
    }

    fun stop() {
        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        surfaceTextureHelper?.dispose()
        peerConnection?.close()
        factory?.dispose()
        eglBase.release()
        _remoteVideoTrackFlow.value = null
    }

    // PeerConnection.Observer methods
    override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
    override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
    override fun onIceConnectionReceivingChange(receiving: Boolean) {}
    override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
    override fun onIceCandidate(candidate: IceCandidate?) {
        candidate?.let { signalingClient.sendIceCandidate(it) }
    }
    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
    override fun onAddStream(stream: MediaStream?) {
        if (stream?.videoTracks?.isNotEmpty() == true) {
            remoteVideoTrack = stream.videoTracks[0]
            _remoteVideoTrackFlow.value = remoteVideoTrack
        }
    }
    override fun onRemoveStream(stream: MediaStream?) {}
    override fun onDataChannel(dataChannel: DataChannel?) {}
    override fun onRenegotiationNeeded() {}
    override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {}
}

interface SignalingClient {
    fun sendOffer(sdp: SessionDescription)
    fun sendAnswer(sdp: SessionDescription)
    fun sendIceCandidate(candidate: IceCandidate)
}
