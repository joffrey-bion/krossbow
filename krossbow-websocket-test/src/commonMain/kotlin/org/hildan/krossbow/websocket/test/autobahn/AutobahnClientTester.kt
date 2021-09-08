package org.hildan.krossbow.websocket.test.autobahn

import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.WebSocketConnection
import org.hildan.krossbow.websocket.WebSocketFrame
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class AutobahnClientTester(
    private val wsClient: WebSocketClient,
    private val testServerUrl: String,
    private val agentUnderTest: String,
) {
    suspend fun connectForAutobahnTestCase(case: String): WebSocketConnection =
        wsClient.connect("$testServerUrl/runCase?casetuple=$case&agent=$agentUnderTest")

    suspend fun getCaseCount(): Int = callAndGetJson("getCaseCount")

    suspend fun getCaseInfo(case: String): Int = callAndGetJson("getCaseInfo?casetuple=$case")

    suspend fun getCaseStatus(case: String): AutobahnCaseStatus = callAndGetJson("getCaseStatus?casetuple=$case&agent=$agentUnderTest")

    suspend fun updateReports() {
        call("updateReports?agent=$agentUnderTest")
    }

    suspend fun stopServer() {
        call("stopServer")
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend inline fun <reified T> callAndGetJson(endpoint: String): T {
        val frame = callAndGet(endpoint)
        assertIs<WebSocketFrame.Text>(frame)
        return Json.decodeFromString(frame.text)
    }

    private suspend fun callAndGet(endpoint: String): WebSocketFrame {
        val connection = wsClient.connect("$testServerUrl/$endpoint")
        val frame = connection.incomingFrames.receive()
        connection.expectClosed()
        return frame
    }

    private suspend fun call(endpoint: String) {
        val connection = wsClient.connect("$testServerUrl/$endpoint")
        connection.expectClosed()
    }

    private suspend fun WebSocketConnection.expectClosed() {
        val closeFrame = withTimeoutOrNull(500) { incomingFrames.receive() }
        assertNotNull(closeFrame, "Timed out while waiting for CLOSE frame")
        assertIs<WebSocketFrame.Close>(closeFrame, "Should have received CLOSE frame, but got $closeFrame")
        val result = incomingFrames.receiveCatching()
        assertTrue(result.isClosed, "Connection should be closed now, got $result")
    }
}

@Serializable
internal data class AutobahnCaseStatus(val behavior: TestCaseStatus)

@Serializable
internal enum class TestCaseStatus {
    OK,
    @SerialName("NON-STRICT")
    NON_STRICT,
    FAILED,
    @SerialName("WRONG CODE")
    WRONG_CODE,
    UNCLEAN,
    @SerialName("FAILED BY CLIENT")
    FAILED_BY_CLIENT,
    INFORMATIONAL,
    UNIMPLEMENTED,
    NO_CLOSE,
}
