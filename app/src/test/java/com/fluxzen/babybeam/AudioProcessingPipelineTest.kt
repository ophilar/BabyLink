package com.fluxzen.babybeam

import android.content.Context
import android.util.Log
import com.fluxzen.ui_design.sync.WebRtcManager
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifier
import kotlinx.coroutines.Job
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AudioProcessingPipelineTest {

    private lateinit var mockContext: Context
    private lateinit var mockWebRtcManager: WebRtcManager
    private lateinit var mockJob: Job
    private lateinit var mockClassifier: AudioClassifier
    private lateinit var pipeline: AudioProcessingPipeline
    private lateinit var logMock: MockedStatic<Log>

    @BeforeEach
    fun setup() {
        mockContext = mock()
        mockWebRtcManager = mock()
        mockJob = mock()
        mockClassifier = mock()
        pipeline = AudioProcessingPipeline(mockContext, mockWebRtcManager)

        // Mock android.util.Log to prevent RuntimeException during JVM tests
        logMock = Mockito.mockStatic(Log::class.java)
        logMock.`when`<Int> { Log.d(Mockito.anyString(), Mockito.anyString()) }.thenReturn(0)
    }

    @org.junit.jupiter.api.AfterEach
    fun teardown() {
        logMock.close()
    }

    @Test
    fun `stop should cancel job, close classifier and clear WebRtcManager callback`() {
        // Arrange: Inject mocks into private fields using reflection to simulate started state
        val jobField = AudioProcessingPipeline::class.java.getDeclaredField("job")
        jobField.isAccessible = true
        jobField.set(pipeline, mockJob)

        val classifierField = AudioProcessingPipeline::class.java.getDeclaredField("audioClassifier")
        classifierField.isAccessible = true
        classifierField.set(pipeline, mockClassifier)

        // Act
        pipeline.stop()

        // Assert
        verify(mockJob).cancel(null)
        verify(mockClassifier).close()
        verify(mockWebRtcManager).onAudioSamplesReady = null

        // Verify audioClassifier is nullified
        assertNull(classifierField.get(pipeline), "AudioClassifier should be nullified")

        // Note: The stop method DOES NOT nullify the job field, so we don't assert it here.
    }
}
