package com.fluxzen.babylink

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifier
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifierResult
import com.google.mediapipe.tasks.components.containers.AudioData
import com.google.mediapipe.tasks.components.containers.Category
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import kotlinx.coroutines.*
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.log10
import kotlin.math.sqrt

class AudioProcessingPipeline(private val context: Context) {
    private val TAG = "AudioProcessingPipeline"
    private var job: Job? = null
    private var audioClassifier: AudioClassifier? = null
    
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
            initMediaPipe(onCryDetected)
            startWebRTCPipeline()
        }
    }

    private fun initMediaPipe(onCryDetected: () -> Unit) {
        try {
            val baseOptionsBuilder = BaseOptions.builder()
                .setModelAssetPath("cry_detection_model.tflite")
                .setDelegate(Delegate.CPU)

            val optionsBuilder = AudioClassifier.AudioClassifierOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
                .setRunningMode(com.google.mediapipe.tasks.audio.core.RunningMode.AUDIO_STREAM)
                .setResultListener { result: AudioClassifierResult ->
                    processResults(result, onCryDetected)
                }
                .setErrorListener { error ->
                    Log.e(TAG, "MediaPipe Error: ${error.message}")
                }

            audioClassifier = AudioClassifier.createFromOptions(context, optionsBuilder.build())
            Log.d(TAG, "MediaPipe AudioClassifier initialized (16KB compliant).")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaPipe: ${e.message}")
        }
    }

    private fun startWebRTCPipeline() {
        try {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                    .setEnableInternalTracer(true)
                    .createInitializationOptions()
            )

            audioDeviceModule = JavaAudioDeviceModule.builder(context)
                .setUseHardwareAcousticEchoCanceler(true)
                .setUseHardwareNoiseSuppressor(true)
                .setSamplesReadyCallback { audioSamples ->
                    processAudioSamples(audioSamples)
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

    private fun processAudioSamples(audioSamples: JavaAudioDeviceModule.AudioSamples) {
        val buffer = audioSamples.data
        val length = buffer.size
        
        // 1. Convert Bytes to Shorts (16-bit PCM)
        val shortBuffer = ShortArray(length / 2)
        ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortBuffer)

        // 2. RMS Gating
        val rms = calculateRMS(shortBuffer)
        val db = if (rms > 0) 20 * log10(rms) else 0.0
        isCurrentlyNoisy = db > dbThreshold

        // 3. Convert to Floats and Feed to MediaPipe if noisy
        if (isCurrentlyNoisy) {
            val floatBuffer = FloatArray(shortBuffer.size)
            for (i in shortBuffer.indices) {
                floatBuffer[i] = shortBuffer[i] / 32768.0f // Normalize to [-1.0, 1.0]
            }

            val audioData = AudioData.create(
                AudioData.AudioDataFormat.builder()
                    .setNumOfChannels(1)
                    .setSampleRate(audioSamples.sampleRate.toFloat())
                    .build(),
                floatBuffer.size
            )
            audioData.load(floatBuffer)

            try {
                audioClassifier?.classifyAsync(audioData, System.currentTimeMillis())
            } catch (e: Exception) {
                Log.e(TAG, "Inference error: ${e.message}")
            }
        }
    }

    private fun processResults(result: AudioClassifierResult, onCryDetected: () -> Unit) {
        val classifications = result.classificationResults().firstOrNull()?.classifications()?.firstOrNull()
        val categories = classifications?.categories() ?: return

        // Look for "cry" or "baby crying"
        val cryCategory = categories.find { category: Category -> 
            category.categoryName().contains("cry", ignoreCase = true) || 
            category.displayName().contains("cry", ignoreCase = true) 
        }

        if (cryCategory != null && cryCategory.score() > 0.6f) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastCryTime > cryDebounceTimeMs) {
                lastCryTime = currentTime
                Log.d(TAG, "Cry detected! Confidence: ${cryCategory.score()}")
                onCryDetected()
            }
        }
    }

    private fun calculateRMS(buffer: ShortArray): Double {
        if (buffer.isEmpty()) return 0.0
        var sum = 0.0
        for (sample in buffer) {
            val s = sample.toDouble()
            sum += s * s
        }
        return sqrt(sum / buffer.size)
    }

    fun stop() {
        job?.cancel()
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
        Log.d(TAG, "AudioProcessingPipeline stopped.")
    }
}
