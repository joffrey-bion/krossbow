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
import kotlin.test.*

internal class AutobahnClientTester(
    private val wsClient: WebSocketClient,
    private val testServerUrl: String,
    private val agentUnderTest: String,
) {
    suspend fun connectForAutobahnTestCase(case: String): WebSocketConnection =
        wsClient.connect("$testServerUrl/runCase?casetuple=$case&agent=$agentUnderTest")

    suspend fun getCaseCount(): Int = callAndGetJson("getCaseCount")

    suspend fun getCaseInfo(case: String): AutobahnCaseInfo = callAndGetJson("getCaseInfo?casetuple=$case")

    suspend fun getCaseStatus(case: String): TestCaseStatus =
        callAndGetJson<AutobahnCaseStatus>("getCaseStatus?casetuple=$case&agent=$agentUnderTest").behavior

    suspend fun updateReports(): Unit = call("updateReports?agent=$agentUnderTest")

    suspend fun stopServer(): Unit = call("stopServer")

    @OptIn(ExperimentalSerializationApi::class)
    private suspend inline fun <reified T> callAndGetJson(endpoint: String): T {
        val connection = wsClient.connect("$testServerUrl/$endpoint")
        val dataFrame = connection.expectFrame<WebSocketFrame.Text>()
        return Json.decodeFromString<T>(dataFrame.text).also {
            connection.expectFrame<WebSocketFrame.Close>()
            connection.expectNoMoreFrames()
        }
    }

    private suspend fun call(endpoint: String) {
        val connection = wsClient.connect("$testServerUrl/$endpoint")
        connection.expectFrame<WebSocketFrame.Close>()
        connection.expectNoMoreFrames()
    }

    private suspend inline fun <reified T : WebSocketFrame> WebSocketConnection.expectFrame(): T {
        val frameType = T::class.simpleName
        val result = withTimeoutOrNull(2000) { incomingFrames.receiveCatching() }
        assertNotNull(result, "Timed out while waiting for $frameType frame")
        assertFalse(result.isClosed, "Expected $frameType frame, but the channel was closed")
        assertFalse(result.isFailure,
            "Expected $frameType frame, but the channel was failed: ${result.exceptionOrNull()}")

        val frame = result.getOrThrow()
        assertIs<T>(frame, "Should have received $frameType frame, but got $frame")
        return frame
    }

    private suspend fun WebSocketConnection.expectNoMoreFrames() {
        val result = withTimeoutOrNull(1000) { incomingFrames.receiveCatching() }
        assertNotNull(result, "Timed out while waiting for incoming frames channel to be closed")
        assertTrue(result.isClosed, "Frames channel should be closed now, got $result")
    }
}

@Serializable
internal data class AutobahnCaseInfo(val id: String, val description: String)

// cannot be private (otherwise "serializer not found")
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
