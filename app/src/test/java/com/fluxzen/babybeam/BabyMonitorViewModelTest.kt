package com.fluxzen.babybeam

import android.content.Context
import com.fluxzen.ui_design.security.SecurityUtil
import com.fluxzen.ui_design.sync.NearbyTransportLayer
import com.fluxzen.ui_design.sync.SignalingMessage
import com.fluxzen.ui_design.sync.WebRtcManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.lang.reflect.Method

@OptIn(ExperimentalCoroutinesApi::class)
class BabyMonitorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `handleSignalingMessage does not throw exception on unknown type`() {
        val context = mock<Context>()
        val nearbyTransport = mock<NearbyTransportLayer>()
        whenever(nearbyTransport.events).thenReturn(MutableSharedFlow())

        val webRtcManager = mock<WebRtcManager>()
        val securityUtil = mock<SecurityUtil>()

        val viewModel = BabyMonitorViewModel(context, nearbyTransport, webRtcManager, securityUtil)

        val method: Method = BabyMonitorViewModel::class.java.getDeclaredMethod("handleSignalingMessage", SignalingMessage::class.java)
        method.isAccessible = true

        val garbageMessage = SignalingMessage(type = "garbage_type_123")

        assertDoesNotThrow {
            try {
                method.invoke(viewModel, garbageMessage)
            } catch (e: java.lang.reflect.InvocationTargetException) {
                throw e.targetException
            }
        }
    }
}
