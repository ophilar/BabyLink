package com.fluxzen.babylink

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnectionFactory
import org.webrtc.audio.JavaAudioDeviceModule
import kotlin.math.log10
import kotlin.math.sqrt

class AudioProcessingPipeline(private val context: Context) {
    private val TAG = "AudioProcessingPipeline"
    private var job: Job? = null
    private var inferenceJob: Job? = null
    private var audioClassifier: AudioClassifier? = null
    private var tensorAudio: TensorAudio? = null

    private var factory: PeerConnectionFactory? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var audioDeviceModule: JavaAudioDeviceModule? = null

    private val sampleRate = 16000
    private val dbThreshold = 45.0 // Decibel threshold for gating
    @Volatile private var isCurrentlyNoisy = false
    private var lastCryTime = 0L
    private val cryDebounceTimeMs = 5000L

    fun start(coroutineScope: CoroutineScope, onCryDetected: () -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "RECORD_AUDIO permission not granted. Cannot start audio pipeline.")
            return
        }

        job = coroutineScope.launch(Dispatchers.IO) {
            initTFLite()
            startWebRTCPipeline()
            startInferenceLoop(coroutineScope, onCryDetected)
        }
    }

    private fun initTFLite() {
        try {
            val options = AudioClassifier.AudioClassifierOptions.builder()
                .setMaxResults(2)
                .build()
            // Placeholder model asset name
            val classifier = AudioClassifier.createFromFileAndOptions(context, "cry_detection_model.tflite", options)
            audioClassifier = classifier
            tensorAudio = classifier.createInputTensorAudio()
            Log.d(TAG, "TFLite AudioClassifier initialized.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load TFLite model: ${e.message}")
        }
    }

    private fun startWebRTCPipeline() {
        try {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                    .setEnableInternalTracer(true)
                    .createInitializationOptions()
            )

            // Stage 1: WebRTC APM for signal conditioning (HPF, AGC, NS)
            audioDeviceModule = JavaAudioDeviceModule.builder(context)
                .setUseHardwareAcousticEchoCanceler(false)
                .setUseHardwareNoiseSuppressor(false) // Force WebRTC's software APM
                .setSamplesReadyCallback { audioSamples ->
                    val buffer = audioSamples.data
                    val length = buffer.size

                    // Convert byte array to short array
                    val shortBuffer = ShortArray(length / 2)
                    for (i in shortBuffer.indices) {
                        val byte1 = buffer[i * 2].toInt() and 0xFF
                        val byte2 = buffer[i * 2 + 1].toInt()
                        shortBuffer[i] = ((byte2 shl 8) or byte1).toShort()
                    }

                    // Fast load into TensorAudio's ring buffer
                    try {
                        tensorAudio?.load(shortBuffer, 0, shortBuffer.size)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading audio into TensorAudio: ${e.message}")
                    }

                    // Stage 2: RMS/Decibel threshold gating via WebRTC's callback buffer
                    val rms = calculateRMS(shortBuffer, shortBuffer.size)
                    val db = if (rms > 0) 20 * log10(rms) else 0.0

                    isCurrentlyNoisy = db > dbThreshold
                }
                .createAudioDeviceModule()

            factory = PeerConnectionFactory.builder()
                .setAudioDeviceModule(audioDeviceModule)
                .createPeerConnectionFactory()

            val audioConstraints = MediaConstraints()
            audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))

            audioSource = factory?.createAudioSource(audioConstraints)
            localAudioTrack = factory?.createAudioTrack("ARDAMSa0", audioSource)
            localAudioTrack?.setEnabled(true)

            Log.d(TAG, "WebRTC Audio Pipeline started.")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start WebRTC Pipeline: ${e.message}", e)
        }
    }

    private fun calculateRMS(buffer: ShortArray, length: Int): Double {
        if (length == 0) return 0.0
        var sum = 0.0
        for (i in 0 until length) {
            val sample = buffer[i].toDouble()
            sum += sample * sample
        }
        val mean = sum / length
        return sqrt(mean)
    }

    private fun startInferenceLoop(coroutineScope: CoroutineScope, onCryDetected: () -> Unit) {
        inferenceJob = coroutineScope.launch(Dispatchers.Default) {
            while (isActive) {
                // Run inference at a fixed interval (e.g., 1000ms),
                // but only if the RMS gate indicates there's enough noise.
                delay(1000)

                if (isCurrentlyNoisy) {
                    runInference(onCryDetected)
                }
            }
        }
    }

    private fun runInference(onCryDetected: () -> Unit) {
        val classifier = audioClassifier ?: return
        val tensor = tensorAudio ?: return
        try {
            // Stage 3: TFLite AudioClassifier for inference (Cry vs Non-Cry)
            val results = classifier.classify(tensor)
            if (results.isNotEmpty()) {
                val classifications = results[0].categories
                val cryCategory = classifications.find { it.label.equals("cry", ignoreCase = true) || it.label.contains("baby crying", ignoreCase = true) }

                if (cryCategory != null && cryCategory.score > 0.6f) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastCryTime > cryDebounceTimeMs) {
                        lastCryTime = currentTime
                        Log.d(TAG, "Cry detected! Confidence: ${cryCategory.score}")
                        onCryDetected()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Inference exception: ${e.message}")
        }
    }

    fun stop() {
        job?.cancel()
        inferenceJob?.cancel()

        localAudioTrack?.dispose()
        localAudioTrack = null

        audioSource?.dispose()
        audioSource = null

        factory?.dispose()
        factory = null

        audioDeviceModule?.release()
        audioDeviceModule = null

        audioClassifier?.close()
        audioClassifier = null

        Log.d(TAG, "AudioProcessingPipeline stopped and resources released.")
    }
}
