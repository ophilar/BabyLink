package com.fluxzen.babybeam

import android.content.Context
import com.fluxzen.ui_design.sync.*
import com.google.android.gms.nearby.connection.Payload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class BabyMonitorViewModelTest {

    private lateinit var viewModel: BabyMonitorViewModel
    private val events = MutableSharedFlow<NearbyTransportLayer.TransportEvent>()
    private val transportLayer = mock<NearbyTransportLayer>()
    private val webRtcManager = mock<WebRtcManager>()
    private val sharedPrefs = mock<android.content.SharedPreferences>()
    private val editor = mock<android.content.SharedPreferences.Editor>()
    private val context = mock<android.content.Context>()
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        whenever(transportLayer.events).thenReturn(events)
        whenever(context.getSharedPreferences(any(), any())).thenReturn(sharedPrefs)
        whenever(sharedPrefs.getString(any(), any())).thenReturn(null)
        whenever(sharedPrefs.edit()).thenReturn(editor)
        whenever(editor.putString(any(), any())).thenReturn(editor)
        
        viewModel = BabyMonitorViewModel(context, transportLayer, webRtcManager)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = runTest {
        assertFalse(viewModel.isCryDetected.first())
        assertTrue(viewModel.vibrationEnabled.first())
        assertTrue(viewModel.visualAlertEnabled.first())
    }

    @Test
    fun `setVibration updates state`() = runTest {
        viewModel.setVibration(false)
        assertFalse(viewModel.vibrationEnabled.first())
    }

    @Test
    fun `dismissAlert clears detection state`() = runTest {
        viewModel.dismissAlert()
        assertFalse(viewModel.isCryDetected.first())
    }

    @Test
    fun `triggerAlert resets alert after delay`() = runTest {
        // Disable vibration to avoid Android framework VibrationEffect.createOneShot exception in local unit test
        viewModel.setVibration(false)

        // Emit data received event
        val signedMessage = SecurityUtil.generateSignedMessage(null, "cry_detected")
        val payload = mock<Payload> {
            on { asBytes() } doReturn signedMessage.toByteArray()
        }
        val event = NearbyTransportLayer.TransportEvent.DataReceived("endpointId", payload)
        
        events.emit(event)
        runCurrent()

        // Wait for coroutine and check alert
        assertTrue(viewModel.isCryDetected.value)

        advanceTimeBy(10001)
        runCurrent()

        // Check if alert was reset
        assertFalse(viewModel.isCryDetected.value)
    }
}
