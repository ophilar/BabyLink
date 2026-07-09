package com.fluxzen.babybeam

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
import com.fluxzen.ui_design.sync.WebRtcManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import org.webrtc.audio.JavaAudioDeviceModule
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.sqrt
import java.io.File

@Singleton
class AudioProcessingPipeline @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val webRtcManager: WebRtcManager
) {
    private val TAG = "AudioProcessingPipeline"
    private var job: Job? = null
    private var audioClassifier: AudioClassifier? = null
    
    private val sampleRate = 16000
    private val dbThreshold = 45.0 // Decibel threshold for gating
    @Volatile private var isCurrentlyNoisy = false
    private var lastCryTime = 0L
    private val cryDebounceTimeMs = 5000L
    private var categoryCache = ByteArray(1024)
    private var preAllocatedShortBuffer: ShortArray? = null
    private var preAllocatedFloatBuffer: FloatArray? = null
    private var preAllocatedByteBuffer: ByteBuffer? = null
    private var preAllocatedShortBufferView: java.nio.ShortBuffer? = null

    fun start(coroutineScope: CoroutineScope, onCryDetected: () -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "RECORD_AUDIO permission not granted. Cannot start audio pipeline.")
            return
        }

        job = coroutineScope.launch(Dispatchers.IO) {
            initMediaPipe(onCryDetected)
            webRtcManager.onAudioSamplesReady = { audioSamples: org.webrtc.audio.JavaAudioDeviceModule.AudioSamples ->
                processAudioSamples(audioSamples)
            }
        }
    }

    private fun initMediaPipe(onCryDetected: () -> Unit) {
        try {
            // Check if model file exists before initializing
            val modelPath = "cry_detection_model.tflite"
            var hasModel = false
            try {
                context.assets.open(modelPath).use {
                    hasModel = true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Model file $modelPath not found in assets. MediaPipe will not be initialized.")
            }

            if (!hasModel) return

            val baseOptionsBuilder = BaseOptions.builder()
                .setModelAssetPath(modelPath)
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

    private fun processAudioSamples(audioSamples: JavaAudioDeviceModule.AudioSamples) {
        if (audioClassifier == null) return

        val buffer = audioSamples.data
        val length = buffer.size
        
        // 1. Convert Bytes to Shorts (16-bit PCM)
        val requiredShortLength = length / 2
        var currentShortBuffer = preAllocatedShortBuffer
        if (currentShortBuffer == null || currentShortBuffer.size != requiredShortLength) {
            currentShortBuffer = ShortArray(requiredShortLength)
            preAllocatedShortBuffer = currentShortBuffer
        }
        var currentByteBuffer = preAllocatedByteBuffer
        if (currentByteBuffer == null || currentByteBuffer.capacity() != length) {
            currentByteBuffer = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN)
            preAllocatedByteBuffer = currentByteBuffer
            preAllocatedShortBufferView = currentByteBuffer.asShortBuffer()
        }
        currentByteBuffer!!.clear()
        currentByteBuffer.put(buffer)

        val shortBufferView = preAllocatedShortBufferView!!
        shortBufferView.clear()
        shortBufferView.get(currentShortBuffer)

        // 2. RMS Gating
        val rms = calculateRMS(currentShortBuffer)
        val db = if (rms > 0) 20 * log10(rms) else 0.0
        isCurrentlyNoisy = db > dbThreshold

        // 3. Convert to Floats and Feed to MediaPipe if noisy
        if (isCurrentlyNoisy) {
            var currentFloatBuffer = preAllocatedFloatBuffer
            if (currentFloatBuffer == null || currentFloatBuffer.size != currentShortBuffer.size) {
                currentFloatBuffer = FloatArray(currentShortBuffer.size)
                preAllocatedFloatBuffer = currentFloatBuffer
            }

            for (i in currentShortBuffer.indices) {
                currentFloatBuffer[i] = currentShortBuffer[i] / 32768.0f // Normalize to [-1.0, 1.0]
            }

            val audioData = AudioData.create(
                AudioData.AudioDataFormat.builder()
                    .setNumOfChannels(1)
                    .setSampleRate(audioSamples.sampleRate.toFloat())
                    .build(),
                currentFloatBuffer.size
            )
            audioData.load(currentFloatBuffer)

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

        // Look for "cry" or "baby crying" using dynamically built cache
        val cryCategory = categories.find { category: Category -> 
            val idx = category.index()
            val status = if (idx < categoryCache.size) categoryCache[idx].toInt() else 0
            if (status != 0) {
                status == 2
            } else {
                val isCry = category.categoryName().contains("cry", ignoreCase = true) ||
                            category.displayName().contains("cry", ignoreCase = true)
                if (idx >= categoryCache.size) {
                    categoryCache = categoryCache.copyOf(maxOf(categoryCache.size * 2, idx + 1))
                }
                categoryCache[idx] = if (isCry) 2.toByte() else 1.toByte()
                isCry
            }
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
        var sumSq = 0L
        for (i in buffer.indices) {
            val s = buffer[i].toLong()
            sumSq += s * s
        }
        return sqrt(sumSq.toDouble() / buffer.size)
    }

    fun stop() {
        job?.cancel()
        audioClassifier?.close()
        audioClassifier = null
        webRtcManager.onAudioSamplesReady = null
        Log.d(TAG, "AudioProcessingPipeline stopped.")
    }
}
