package com.fluxzen.babybeam

import android.content.Context
import com.fluxzen.ui_design.sync.NearbyTransportLayer
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

@OptIn(ExperimentalCoroutinesApi::class)
class BabyMonitorViewModelTest {

    private lateinit var viewModel: BabyMonitorViewModel
    private val events = MutableSharedFlow<NearbyTransportLayer.TransportEvent>()
    private val transportLayer = mock<NearbyTransportLayer> {
        on { events } doReturn events
    }
    private val context = mock<android.content.Context> {
        on { getSystemService(any()) } doReturn null
    }
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = BabyMonitorViewModel(context, transportLayer)
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
        val payloadMock = mock<NearbyTransportLayer.Payload> {
            on { asBytes() } doReturn "cry_detected".toByteArray()
        }
        events.emit(NearbyTransportLayer.TransportEvent.DataReceived("endpointId", payloadMock))

        // Wait for coroutine and check alert
        assertTrue(viewModel.isCryDetected.value)

        advanceTimeBy(10001)
        runCurrent()

        // Check if alert was reset
        assertFalse(viewModel.isCryDetected.value)
    }
}
