package com.fluxzen.babybeam

import android.content.Context
import com.fluxzen.ui_design.sync.*
import com.fluxzen.ui_design.security.SecurityUtil
import com.google.android.gms.nearby.connection.Payload
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class BabyMonitorViewModelTest {

    private lateinit var viewModel: BabyMonitorViewModel
    private val events = MutableSharedFlow<NearbyTransportLayer.TransportEvent>()
    private val transportLayer = mock<NearbyTransportLayer>()
    private val webRtcManager = mock<WebRtcManager>()
    private val securityUtil = mock<SecurityUtil>()
    private val context = mock<android.content.Context>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val gson = Gson()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        whenever(transportLayer.events).thenReturn(events)
        viewModel = BabyMonitorViewModel(context, transportLayer, webRtcManager, securityUtil)
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
        viewModel.setVibration(false)

        val cryMsg = SignalingMessage(type = "cry_detected")
        val json = gson.toJson(cryMsg)
        val signedMessage = "dummy_signed_json"
        
        whenever(securityUtil.verifySignedMessage(eq(context), eq(signedMessage))).thenReturn(json)

        val payload = mock<Payload> {
            on { asBytes() } doReturn signedMessage.toByteArray()
        }
        val event = NearbyTransportLayer.TransportEvent.DataReceived("endpointId", payload)
        
        events.emit(event)
        runCurrent()

        assertTrue(viewModel.isCryDetected.value)

        advanceTimeBy(10001)
        runCurrent()

        assertFalse(viewModel.isCryDetected.value)
    }
}
