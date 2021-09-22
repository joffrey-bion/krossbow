package org.hildan.krossbow.websocket.test.autobahn

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.WebSocketConnection
import org.hildan.krossbow.websocket.test.connectWithTimeout
import org.hildan.krossbow.websocket.test.expectCloseFrame
import org.hildan.krossbow.websocket.test.expectNoMoreFrames
import org.hildan.krossbow.websocket.test.expectTextFrame

internal class AutobahnClientTester(
    private val wsClient: WebSocketClient,
    private val testServerUrl: String,
    private val agentUnderTest: String,
) {
    suspend fun connectForAutobahnTestCase(case: String): WebSocketConnection =
        wsClient.connectWithTimeout("$testServerUrl/runCase?casetuple=$case&agent=$agentUnderTest")

    suspend fun getCaseCount(): Int = callAndGetJson("getCaseCount")

    suspend fun getCaseInfo(case: String): AutobahnCaseInfo = callAndGetJson("getCaseInfo?casetuple=$case")

    suspend fun getCaseStatus(case: String): TestCaseStatus =
        callAndGetJson<AutobahnCaseStatus>("getCaseStatus?casetuple=$case&agent=$agentUnderTest").behavior

    suspend fun updateReports(): Unit = call("updateReports?agent=$agentUnderTest")

    suspend fun stopServer(): Unit = call("stopServer")

    @OptIn(ExperimentalSerializationApi::class)
    private suspend inline fun <reified T> callAndGetJson(endpoint: String): T {
        val connection = wsClient.connectWithTimeout("$testServerUrl/$endpoint")
        val dataFrame = connection.expectTextFrame("JSON data for endpoint $endpoint")
        return Json.decodeFromString<T>(dataFrame.text).also {
            connection.expectCloseFrame("after JSON data for endpoint $endpoint")
            connection.expectNoMoreFrames("after close frame for endpoint $endpoint")
        }
    }

    private suspend fun call(endpoint: String) {
        val connection = wsClient.connectWithTimeout("$testServerUrl/$endpoint")
        connection.expectCloseFrame("no data for endpoint $endpoint")
        connection.expectNoMoreFrames("after close frame for endpoint $endpoint")
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
