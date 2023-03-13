package org.hildan.krossbow.websocket.test.autobahn

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.hildan.krossbow.websocket.*
import org.hildan.krossbow.websocket.test.runSuspendingTest
import kotlin.test.*

data class CaseExclusion(
    val caseIdPrefixes: List<String>,
    val reason: String,
) {
    fun excludes(caseId: String) = caseIdPrefixes.any { caseId.startsWith(it) }
}

/**
 * Runs tests from the [Autobahn test suite](https://github.com/crossbario/autobahn-testsuite).
 *
 * This test suite requires a running Autobahn fuzzing server. It can be started with the following docker command:
 * ```
 * docker run -it --rm -v "./autobahn/config:/config" -v "./build/autobahn/reports:/reports" -p 12345:12345 crossbario/autobahn-testsuite
 * ```
 * Alternatively, Gradle can be configured to start the relevant container when running tests.
 * See Krossbow's repository for an example setup using `com.avast.gradle.docker-compose` Gradle plugin.
 */
abstract class AutobahnClientTestSuite(
    private val agentUnderTest: String,
    private val exclusions: List<CaseExclusion> = emptyList(),
    private val config: AutobahnConfig = getDefaultAutobahnConfig(),
) {
    private val reportsClient = AutobahnReportsClient(config)

    abstract fun provideClient(): WebSocketClient

    @Test
    fun autobahn_1_1_1_echo_text_payload() = runAutobahnTestCase("1.1.1")

    @Test
    fun autobahn_1_1_2_echo_text_payload() = runAutobahnTestCase("1.1.2")

    @Test
    fun autobahn_1_1_3_echo_text_payload() = runAutobahnTestCase("1.1.3")

    @Test
    fun autobahn_1_1_4_echo_text_payload() = runAutobahnTestCase("1.1.4")

    @Test
    fun autobahn_1_1_5_echo_text_payload() = runAutobahnTestCase("1.1.5")

    @Test
    fun autobahn_1_1_6_echo_text_payload() = runAutobahnTestCase("1.1.6")

    @Test
    fun autobahn_1_1_7_echo_text_payload() = runAutobahnTestCase("1.1.7")

    @Test
    fun autobahn_1_1_8_echo_text_payload() = runAutobahnTestCase("1.1.8")

    @Test
    fun autobahn_1_2_1_echo_binary_payload() = runAutobahnTestCase("1.2.1")

    @Test
    fun autobahn_1_2_2_echo_binary_payload() = runAutobahnTestCase("1.2.2")

    @Test
    fun autobahn_1_2_3_echo_binary_payload() = runAutobahnTestCase("1.2.3")

    @Test
    fun autobahn_1_2_4_echo_binary_payload() = runAutobahnTestCase("1.2.4")

    @Test
    fun autobahn_1_2_5_echo_binary_payload() = runAutobahnTestCase("1.2.5")

    @Test
    fun autobahn_1_2_6_echo_binary_payload() = runAutobahnTestCase("1.2.6")

    @Test
    fun autobahn_1_2_7_echo_binary_payload() = runAutobahnTestCase("1.2.7")

    @Test
    fun autobahn_1_2_8_echo_binary_payload() = runAutobahnTestCase("1.2.8")

    @Test
    fun autobahn_2_1_ping_pong() = runAutobahnTestCase("2.1")

    @Test
    fun autobahn_2_2_ping_pong() = runAutobahnTestCase("2.2")

    @Test
    fun autobahn_2_3_ping_pong() = runAutobahnTestCase("2.3")

    @Test
    fun autobahn_2_4_ping_pong() = runAutobahnTestCase("2.4")

    @Test
    fun autobahn_2_5_ping_pong() = runAutobahnTestCase("2.5")

    @Test
    fun autobahn_2_6_ping_pong() = runAutobahnTestCase("2.6")

    @Test
    fun autobahn_2_7_ping_pong() = runAutobahnTestCase("2.7")

    @Test
    fun autobahn_2_8_ping_pong() = runAutobahnTestCase("2.8")

    @Test
    fun autobahn_2_9_ping_pong() = runAutobahnTestCase("2.9")

    // Not all pongs are required according to the spec, but this tests returns FAILED status if some are missing
    // https://github.com/crossbario/autobahn-testsuite/issues/101
    // It might be necessary to ignore this test case for some clients.
    @Test
    fun autobahn_2_10_ping_pong() = runAutobahnTestCase("2.10")

    // Not all pongs are required according to the spec, but this tests returns FAILED status if some are missing
    // https://github.com/crossbario/autobahn-testsuite/issues/101
    // It might be necessary to ignore this test case for some clients.
    @Test
    fun autobahn_2_11_ping_pong() = runAutobahnTestCase("2.11")

    @Test
    fun autobahn_3_1_reserved_bits() = runAutobahnTestCase("3.1")

    @Test
    fun autobahn_3_2_reserved_bits() = runAutobahnTestCase("3.2")

    @Test
    fun autobahn_3_3_reserved_bits() = runAutobahnTestCase("3.3")

    @Test
    fun autobahn_3_4_reserved_bits() = runAutobahnTestCase("3.4")

    @Test
    fun autobahn_3_5_reserved_bits() = runAutobahnTestCase("3.5")

    @Test
    fun autobahn_3_6_reserved_bits() = runAutobahnTestCase("3.6")

    @Test
    fun autobahn_3_7_reserved_bits() = runAutobahnTestCase("3.7")

    @Test
    fun autobahn_4_1_1_opcodes() = runAutobahnTestCase("4.1.1")

    @Test
    fun autobahn_4_1_2_opcodes() = runAutobahnTestCase("4.1.2")

    @Test
    fun autobahn_4_1_3_opcodes() = runAutobahnTestCase("4.1.3")

    @Test
    fun autobahn_4_1_4_opcodes() = runAutobahnTestCase("4.1.4")

    @Test
    fun autobahn_4_1_5_opcodes() = runAutobahnTestCase("4.1.5")

    @Test
    fun autobahn_4_2_1_opcodes() = runAutobahnTestCase("4.2.1")

    @Test
    fun autobahn_4_2_2_opcodes() = runAutobahnTestCase("4.2.2")

    @Test
    fun autobahn_4_2_3_opcodes() = runAutobahnTestCase("4.2.3")

    @Test
    fun autobahn_4_2_4_opcodes() = runAutobahnTestCase("4.2.4")

    @Test
    fun autobahn_4_2_5_opcodes() = runAutobahnTestCase("4.2.5")

    @Test
    fun autobahn_5_1_echo_payload() = runAutobahnTestCase("5.1")

    @Test
    fun autobahn_5_2_echo_payload() = runAutobahnTestCase("5.2")

    @Test
    fun autobahn_5_3_echo_payload() = runAutobahnTestCase("5.3")

    @Test
    fun autobahn_5_4_echo_payload() = runAutobahnTestCase("5.4")

    @Test
    fun autobahn_5_5_echo_payload() = runAutobahnTestCase("5.5")

    @Test
    fun autobahn_5_6_echo_payload() = runAutobahnTestCase("5.6")

    @Test
    fun autobahn_5_7_echo_payload() = runAutobahnTestCase("5.7")

    @Test
    fun autobahn_5_8_echo_payload() = runAutobahnTestCase("5.8")

    @Test
    fun autobahn_5_9_echo_payload() = runAutobahnTestCase("5.9")

    private fun runAutobahnTestCase(caseId: String) = runSuspendingTest {
        val matchedExclusion = exclusions.find { it.excludes(caseId) }
        if (matchedExclusion != null) {
            println("Test case $caseId disabled for agent $agentUnderTest: ${matchedExclusion.reason}")
            return@runSuspendingTest
        }
        val autobahnClientTester = AutobahnClientTester(provideClient(), config, agentUnderTest)
        val case = AutobahnCase.fromTuple(caseId)

        // It would be best to update reports only after all tests of the class, but it's not possible at the moment.
        // In the meantime, we only write reports in case of error because updateReports itself fails sometimes.
        autobahnClientTester.withReportsOnError {
            autobahnClientTester.runTestCase(case)
        }
        autobahnClientTester.assertTestCaseResult(case)
    }

    private suspend fun AutobahnClientTester.assertTestCaseResult(case: AutobahnCase) {
        val status = getCaseStatus(case.id)
        if (!status.isAcceptable) {
            updateReports() // required to fetch the report details
            failTestCaseWithReportDetails(case)
        }
    }

    private suspend fun failTestCaseWithReportDetails(case: AutobahnCase) {
        val testResult = reportsClient.getTestResult(agentUnderTest, case.id)
        val status = testResult.behavior
        assertFalse(status.isAcceptable, "Case status was not acceptable but the report says $status")

        val details = testResult.fetchReport()?.describeExpectedAndActual()
        fail("Test case ${case.id} finished with status $status, expected OK or NON-STRICT:\n$details")
    }

    private suspend fun AutobahnTestCaseResult.fetchReport(): AutobahnTestCaseReport? = try {
        reportsClient.getTestCaseReport(reportFile)
    } catch (e: Exception) {
        println("Error while fetching test report: ${e.message}")
        null
    }
}

private suspend fun AutobahnClientTester.runTestCase(case: AutobahnCase) {
    val receivedFrames = mutableListOf<WebSocketFrame>()
    try {
        val session = connectForAutobahnTestCase(case.id)
        withTimeout(15000) {
            session.incomingFrames
                .onEach { receivedFrames.add(it) }
                .takeWhile { it !is WebSocketFrame.Close }
                .collect {
                    session.echoFrame(it)
                }
        }
    } catch (e: TimeoutCancellationException) { // caught separately because it's never an expected failure
        val formattedReceivedFrames = receivedFrames.formatForPrinting()
        fail("Test case ${case.id} timed out while echoing frames. $formattedReceivedFrames", e)
    } catch (e: Exception) {
        if (!case.expectFailure) {
            val formattedReceivedFrames = receivedFrames.formatForPrinting()
            throw IllegalStateException("Unexpected exception during test case ${case.id}: ${e.message}\n$formattedReceivedFrames", e)
        }
    }
}

private fun List<WebSocketFrame>.formatForPrinting() = if (isEmpty()) {
    "No frames received."
} else {
    "Received frames:\n" + joinToString("\n") { it.toString() }
}

private suspend fun WebSocketConnection.echoFrame(frame: WebSocketFrame) {
    when (frame) {
        is WebSocketFrame.Text -> sendText(frame.text)
        is WebSocketFrame.Binary -> sendBinary(frame.bytes)
        is WebSocketFrame.Ping -> Unit // nothing special, we expect the underlying impl to PONG properly
        is WebSocketFrame.Pong -> Unit // nothing to do for unsolicited PONG
        is WebSocketFrame.Close -> error("should not receive CLOSE frame at that point")
    }
}

private suspend fun AutobahnClientTester.withReportsOnError(function: suspend () -> Unit) {
    try {
        function.invoke()
    } catch (t: Throwable) { // we need to also catch AssertionError
        // We want to keep the original exception so we rethrow it,
        // but we still want to know about exceptions from updateReports itself
        val reportResult = runCatching { updateReports() }
        reportResult.exceptionOrNull()?.let { t.addSuppressed(it) }
        throw t
    }
}

private fun AutobahnTestCaseReport.describeExpectedAndActual(): String {
    val expectedFrames = expected.entries.joinToString("\n") { (status, frames) ->
        frames.formatAsList(header = "Expected (for $status)")
    }
    val actualFrames = received.formatAsList(header = "Actual")
    return "$expectedFrames\n$actualFrames"
}

private fun FramesHistory.formatAsList(header: String): String = "$header:\n    ${this.joinToString("\n    ")}"

/*
The following methods could be used for a more precise behaviour check.
 */

private suspend fun WebSocketConnection.echoExactFrameCountAndExpectClosure(case: AutobahnCase) {
    // FIXME properly expect number of data frames, pings and pongs
    echoNFrames(case.nExpectedFramesBeforeEnd)
    when (case.end) {
        CaseEnd.CLIENT_FORCE_CLOSE -> expectClientForceClosed()
        CaseEnd.SERVER_CLOSE -> expectServerClosed()
    }
}

private suspend fun WebSocketConnection.echoNFrames(n: Int) {
    incomingFrames.take(n).collect { frame ->
        echoFrame(frame)
    }
}

private suspend fun WebSocketConnection.expectClientForceClosed() {
    // Note: the channel might be closed, or a frame could be received before the client had time to force close, or
    // a frame could be received as part of the test, etc.
    // We can't really rely on channel closure generally, hence this "weak" check that works on all client-close tests
    val result = withTimeoutOrNull(2000) {
        incomingFrames.toList()
    }
    assertFalse(result == null, "Incoming frames flow should not hang (the client should have closed the connection)")
}

private suspend fun WebSocketConnection.expectServerClosed() {
    val frames = incomingFrames.toList()
    // ping-pongs are irrelevant because they are not echoed (they are more low-level frames)
    val relevantFrames = frames.filter { !(it is WebSocketFrame.Pong || it is WebSocketFrame.Ping) }
    assertEquals(1, relevantFrames.size, "The server should only have sent a single frame by now (CLOSE), got ${relevantFrames.map { it.truncated(20) }}")
    assertIs<WebSocketFrame.Close>(relevantFrames.single(), "The frame received from the server should be a CLOSE frame")
    assertTrue(incomingFrames.count() == 0, "The incoming frames flow should be completed by now")
}

private fun WebSocketFrame.truncated(length: Int) = if (this is WebSocketFrame.Text) {
    copy(text = text.take(length))
} else {
    this
}
