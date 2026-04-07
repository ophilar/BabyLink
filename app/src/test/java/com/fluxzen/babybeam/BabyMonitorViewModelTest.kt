package com.fluxzen.babybeam

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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class BabyMonitorViewModelTest {

    private lateinit var viewModel: BabyMonitorViewModel
    private val events = MutableSharedFlow<NearbyTransportLayer.TransportEvent>()
    private val transportLayer = mock<NearbyTransportLayer> {
        on { events } doReturn events
    }
    private val context = mock<android.content.Context>()
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
}

