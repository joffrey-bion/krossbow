package org.hildan.krossbow.websocket.test.autobahn

import kotlinx.coroutines.delay
import kotlinx.serialization.*
import kotlinx.serialization.json.*

/**
 * Abstract network client, just for HTTP GET.
 *
 * We don't want to use Ktor in general because it would add unwanted dependencies to the tests of other engines.
 */
expect class HttpGetter() {
    suspend fun get(url: String): String
}

internal class AutobahnReportsClient(private val config: AutobahnConfig) {
    private val baseUrl = "http://${config.host}:${config.webPort}"
    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient = HttpGetter()

    suspend fun getReportsIndex(): AutobahnReportIndex =
        getEndpoint("cwd/reports/clients/index.json")

    suspend fun getTestCaseReport(fileName: String): AutobahnTestCaseReport =
        getEndpoint("cwd/reports/clients/$fileName")

    private suspend inline fun <reified T> getEndpoint(endpoint: String): T =
        json.decodeFromString(httpClient.get("$baseUrl/$endpoint"))
}

internal suspend fun AutobahnReportsClient.getAgentResults(agentUnderTest: String): AutobahnAgentResults {
    return getReportsIndex()[agentUnderTest] ?: error("No results found for agent $agentUnderTest")
}

internal suspend fun AutobahnReportsClient.getTestResult(agentUnderTest: String, case: String): AutobahnTestCaseResult {
    val agentResults = getAgentResults(agentUnderTest)
    repeat(5) {
        val autobahnTestCaseResult = agentResults[case]
        if (autobahnTestCaseResult != null) {
            return autobahnTestCaseResult
        }
        delay(200)
    }
    error("No results found for test case $case for agent $agentUnderTest")
}

internal typealias AutobahnReportIndex = Map<String, AutobahnAgentResults>
internal typealias AutobahnAgentResults = Map<String, AutobahnTestCaseResult>

@Serializable
internal data class AutobahnTestCaseResult(
    val behavior: TestCaseStatus,
    val behaviorClose: TestCaseStatus,
    val duration: Long,
    val remoteCloseCode: Int?,
    /**
     * Relative path to the full report file of this test case.
     */
    @SerialName("reportfile")
    val reportFile: String,
)

@Serializable
internal data class AutobahnTestCaseReport(
    val agent: String,
    val id: String,
    val case: Int,
    val description: String,
    val expectation: String,
    val expected: Map<TestCaseStatus, FramesHistory>,
    val received: FramesHistory,
    val behavior: TestCaseStatus,
    val behaviorClose: TestCaseStatus,
    val result: String,
    val resultClose: String,
    @SerialName("started")
    val startTime: String,
    val duration: Long,
    val closedByMe: Boolean,
    val failedByMe: Boolean,
    val droppedByMe: Boolean,
    val localCloseCode: Int?,
    val localCloseReason: String?,
    val remoteCloseCode: Int?,
    val remoteCloseReason: String?,
    val wasClean: Boolean,
    val wasNotCleanReason: String?,
    val wasOpenHandshakeTimeout: Boolean,
    val wasCloseHandshakeTimeout: Boolean,
    val wasServerConnectionDropTimeout: Boolean,
)

typealias FramesHistory = List<FrameDescription>

// pretty bad, but that's how it is in Autobahn's JSON
typealias FrameDescription = List<JsonPrimitive>
