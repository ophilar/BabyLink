package org.webrtc.audio

class JavaAudioDeviceModule {
    class AudioSamples(val audioFormat: Int, val channelCount: Int, val sampleRate: Int, val data: ByteArray)
}
