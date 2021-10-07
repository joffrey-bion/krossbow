package org.hildan.krossbow.websocket.test.autobahn

// Comments mark test cases that have at least failed once with the exception:
/*
java.util.concurrent.RejectedExecutionException: Task java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask@1936ac2[Not completed,
task = java.util.concurrent.Executors$RunnableAdapter@3251bd45[Wrapped task = org.glassfish.tyrus.core.TyrusSession$IdleTimeoutCommand@49d2a1b0]]
rejected from java.util.concurrent.ScheduledThreadPoolExecutor@2fd7d9eb[Shutting down, pool size = 1, active threads = 1, queued tasks = 0, completed tasks = 2]
 */
internal val AUTOBAHN_CASES = listOf(
    AutobahnCase("1.1.1", nEchoFrames = 1), //
    AutobahnCase("1.1.2", nEchoFrames = 1), //
    AutobahnCase("1.1.3", nEchoFrames = 1), //
    AutobahnCase("1.1.4", nEchoFrames = 1),
    AutobahnCase("1.1.5", nEchoFrames = 1), //
    AutobahnCase("1.1.6", nEchoFrames = 1),
    AutobahnCase("1.1.7", nEchoFrames = 1), //
    AutobahnCase("1.1.8", nEchoFrames = 1), // 2
    AutobahnCase("1.2.1", nEchoFrames = 1), //
    AutobahnCase("1.2.2", nEchoFrames = 1),
    AutobahnCase("1.2.3", nEchoFrames = 1),
    AutobahnCase("1.2.4", nEchoFrames = 1), //
    AutobahnCase("1.2.5", nEchoFrames = 1), //
    AutobahnCase("1.2.6", nEchoFrames = 1),
    AutobahnCase("1.2.7", nEchoFrames = 1), // 2
    AutobahnCase("1.2.8", nEchoFrames = 1),
    AutobahnCase("2.1", nEchoFrames = 0, nPingFrames = 1),
    AutobahnCase("2.2", nEchoFrames = 0, nPingFrames = 1),
    AutobahnCase("2.3", nEchoFrames = 0, nPingFrames = 1),
    AutobahnCase("2.4", nEchoFrames = 0, nPingFrames = 1),
    AutobahnCase("2.5", nEchoFrames = 0, nPingFrames = 1, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("2.6", nEchoFrames = 0, nPingFrames = 1),
    AutobahnCase("2.7", nEchoFrames = 0, nUnsolicitedPongFrames = 1),
    AutobahnCase("2.8", nEchoFrames = 0, nUnsolicitedPongFrames = 1),
    AutobahnCase("2.9", nEchoFrames = 0, nPingFrames = 1, nUnsolicitedPongFrames = 1),
    AutobahnCase("2.10", nEchoFrames = 0, nPingFrames = 10), // FIXME not all pongs are necessary according to the spec, so FAILED status is acceptable here
    AutobahnCase("2.11", nEchoFrames = 0, nPingFrames = 10), // FIXME not all pongs are necessary according to the spec, so FAILED status is acceptable here
    AutobahnCase("3.1", nEchoFrames = 0, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("3.2", nEchoFrames = 1, nPingFrames = 0 /* fail before ping */, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("3.3", nEchoFrames = 1, nPingFrames = 0 /* fail before ping */, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("3.4", nEchoFrames = 1, nPingFrames = 0 /* fail before ping */, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("3.5", nEchoFrames = 0, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("3.6", nEchoFrames = 0, nPingFrames = 0 /* fail before ping */, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("3.7", nEchoFrames = 0, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("4.1.1", nEchoFrames = 0, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("4.1.2", nEchoFrames = 0, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("4.1.3", nEchoFrames = 1, nPingFrames = 0 /* fail before ping */, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("4.1.4", nEchoFrames = 1, nPingFrames = 0 /* fail before ping */, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("4.1.5", nEchoFrames = 1, nPingFrames = 0 /* fail before ping */, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("4.2.1", nEchoFrames = 0, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("4.2.2", nEchoFrames = 0, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("4.2.3", nEchoFrames = 1, nPingFrames = 0 /* fail before ping */, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("4.2.4", nEchoFrames = 1, nPingFrames = 0 /* fail before ping */, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("4.2.5", nEchoFrames = 1, nPingFrames = 0 /* fail before ping */, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("5.1", nEchoFrames = 0, nPingFrames = 0 /* fail before ping */, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("5.2", nEchoFrames = 0, nUnsolicitedPongFrames = 0 /* fail before pong */, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("5.3", nEchoFrames = 1), //
    AutobahnCase("5.4", nEchoFrames = 1), //
    AutobahnCase("5.5", nEchoFrames = 1), //
    AutobahnCase("5.6", nEchoFrames = 1, nPingFrames = 1),
    AutobahnCase("5.7", nEchoFrames = 1, nPingFrames = 1), // 2
    AutobahnCase("5.8", nEchoFrames = 1, nPingFrames = 1),
    AutobahnCase("5.9", nEchoFrames = 0, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("5.10", nEchoFrames = 0, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("5.11", nEchoFrames = 0, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("5.12", nEchoFrames = 0, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("5.13", nEchoFrames = 0, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("5.14", nEchoFrames = 0, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("5.15", nEchoFrames = 0, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("5.16", nEchoFrames = 0, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("5.17", nEchoFrames = 0, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("5.18", nEchoFrames = 0, expectFailure = true, end = CaseEnd.CLIENT_FORCE_CLOSE),
    AutobahnCase("5.19", nEchoFrames = 1, nPingFrames = 2), //
    AutobahnCase("5.20", nEchoFrames = 1, nPingFrames = 2),
)

internal data class AutobahnCase(
    val id: String,
    /** Number of frames actually echoed during the test (not control frames). */
    val nEchoFrames: Int,
    /** Number of received PING frames that are technically not echoed. */
    val nPingFrames: Int = 0,
    /** Number of received unsolicited PONG frames that are technically not echoed. */
    val nUnsolicitedPongFrames: Int = 0,
    val expectFailure: Boolean = false,
    val end: CaseEnd = CaseEnd.SERVER_CLOSE,
) {
    val nExpectedFramesBeforeEnd = nEchoFrames + nPingFrames + nUnsolicitedPongFrames
}

internal enum class CaseEnd {
    SERVER_CLOSE,
    CLIENT_FORCE_CLOSE,
}