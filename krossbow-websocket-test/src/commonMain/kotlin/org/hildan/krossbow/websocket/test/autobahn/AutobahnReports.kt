package org.hildan.krossbow.websocket.test.autobahn

import kotlinx.serialization.*

/*
Technically, these classes are unused at the moment but can be useful to deserialize the Autobahn reports.
 */

@Serializable
internal data class AutobahnReportIndex(
    val resultsByAgent: Map<String, AutobahnAgentResults>
)

@Serializable
internal data class AutobahnAgentResults(
    val resultsByTestCase: Map<String, AutobahnTestCaseResult>
)

@Serializable
internal data class AutobahnTestCaseResult(
    val behavior: TestCaseStatus,
    val behaviorClose: TestCaseStatus,
    val duration: Long,
    val remoteCloseCode: Int,
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
    val localCloseCode: Int,
    val localCloseReason: String?,
    val remoteCloseCode: Int,
    val remoteCloseReason: String?,
    val wasClean: Boolean,
    val wasNotCleanReason: String?,
    val wasOpenHandshakeTimeout: Boolean,
    val wasCloseHandshakeTimeout: Boolean,
    val wasServerConnectionDropTimeout: Boolean,
)
