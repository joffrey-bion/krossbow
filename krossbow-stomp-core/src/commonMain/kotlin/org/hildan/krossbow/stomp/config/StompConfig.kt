package org.hildan.krossbow.stomp.config

import org.hildan.krossbow.stomp.LostReceiptException
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.instrumentation.KrossbowInstrumentation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for the STOMP client.
 */
class StompConfig {
    /**
     * Whether to automatically attach a `receipt` header to the sent frames in order to track receipts.
     */
    var autoReceipt: Boolean = false

    /**
     * Whether to automatically compute and add the `content-length` header in sent frames.
     */
    var autoContentLength: Boolean = true

    /**
     * Whether to use the `STOMP` command instead of `CONNECT` to establish the connection.
     *
     * Clients that use the `STOMP` frame instead of the `CONNECT` frame will only be able to connect to STOMP 1.2
     * servers (as well as some STOMP 1.1 servers) but the advantage is that a protocol sniffer/discriminator will be
     * able to differentiate the STOMP connection from an HTTP connection.
     */
    var connectWithStompCommand: Boolean = false

    /**
     * The [HeartBeat] to request for the STOMP sessions.
     *
     * This is part of a negotiation and does not imply that this exact heart beat configuration will be used.
     * The actual heart beats are defined by the CONNECTED frame received from the server as a result of the
     * negotiation. This behaviour is
     * [defined by the specification](https://stomp.github.io/stomp-specification-1.2.html#Heart-beating).
     */
    var heartBeat: HeartBeat = HeartBeat()

    /**
     * Defines tolerance for heart beats.
     *
     * If both the client and server really stick to the heart beats periods negotiated and given by the CONNECTED frame,
     * network latencies will make them miss their marks. That's why we need some sort of tolerance.
     *
     * In case the server is too strict about its expectations, we can send heart beats a little bit earlier than we're
     * supposed to (see [HeartBeatTolerance.outgoingMargin]).
     *
     * In case the server really sticks to its own period without such margin, we need to allow a little bit of delay to
     * make up for network latencies before we fail and close the connection (see
     * [HeartBeatTolerance.incomingMargin]).
     */
    var heartBeatTolerance: HeartBeatTolerance = HeartBeatTolerance()

    /**
     * Defines how long to wait for the websocket+STOMP connection to be established before throwing an exception.
     */
    var connectionTimeout: Duration = 15.seconds

    /**
     * Defines how long to wait for a RECEIPT frame from the server before throwing a [LostReceiptException].
     * Only crashes when a `receipt` header was actually present in the sent frame (and thus a RECEIPT was expected).
     * Such header is always present if [autoReceipt] is enabled.
     *
     * Note that this doesn't apply to the DISCONNECT frames, use [disconnectTimeout] instead for that.
     */
    var receiptTimeout: Duration = 1.seconds

    /**
     * Like [receiptTimeout] but only for the receipt of the DISCONNECT frame.
     * This is ignored if [gracefulDisconnect] is disabled.
     *
     * Note that if this timeout expires, the [StompSession.disconnect] call doesn't throw an exception.
     * This is to allow servers to close the connection quickly (sometimes too quick for sending a RECEIPT/ERROR) as
     * [mentioned in the specification](http://stomp.github.io/stomp-specification-1.2.html#DISCONNECT).
     */
    var disconnectTimeout: Duration = 200.milliseconds

    /**
     * When the session is disconnected or when an error occurs, subscription flows complete or throw an exception.
     * This timeout defines how long the STOMP session will wait for subscribers to reach this completion/error before
     * cancelling them along with internal coroutines.
     *
     * Slow subscribers that take longer than this timeout may miss messages that were buffered before the completion
     * or error, because they will instead get a [CancellationException][kotlinx.coroutines.CancellationException].
     * The original error will be provided as cause of the cancellation in this case.
     * They may miss previously buffered messages.
     */
    @Deprecated(
        "The internal implementation of the session no longer requires an explicit timeout, and this property has no " +
            "effect anymore. Subscribers can take as much time as they need and will eventually reach the completion/error."
    )
    var subscriptionCompletionTimeout: Duration = 1.seconds

    /**
     * Enables [graceful disconnect](https://stomp.github.io/stomp-specification-1.2.html#DISCONNECT).
     *
     * If enabled, when disconnecting from the server, the client first sends a DISCONNECT frame with a `receipt`
     * header, and then waits for a RECEIPT frame before closing the connection.
     *
     * If this graceful disconnect is disabled, then calling [StompSession.disconnect] immediately closes the web
     * socket connection.
     * In this case, there is no guarantee that the server received all previous messages.
     */
    var gracefulDisconnect: Boolean = true

    /**
     * An additional coroutine context used for the collection and decoding of the STOMP frames within [StompSession]s
     * created by this client.
     *
     * It can be overridden independently for each session when calling
     * [StompClient.connect][org.hildan.krossbow.stomp.StompClient.connect].
     *
     * This is mostly useful to inject a test dispatcher.
     */
    var defaultSessionCoroutineContext: CoroutineContext = EmptyCoroutineContext

    /**
     * A set of hooks that are called in different places of the internal execution of Krossbow.
     * The instrumentation can be used for monitoring, logging or debugging purposes.
     */
    var instrumentation: KrossbowInstrumentation? = null
}

/**
 * Defines heart beats for STOMP sessions, as
 * [defined in the STOMP specification](https://stomp.github.io/stomp-specification-1.2.html#Heart-beating).
 */
data class HeartBeat(
    /**
     * Represents what the sender of the frame can do (outgoing heart-beats).
     * The value 0 means it cannot send heart-beats, otherwise it is the smallest time between heart-beats that it
     * can guarantee.
     */
    val minSendPeriod: Duration = Duration.ZERO,
    /**
     * Represents what the sender of the frame would like to get (incoming heart-beats).
     * The value 0 means it does not want to receive heart-beats, otherwise it is the desired time between heart-beats.
     */
    val expectedPeriod: Duration = Duration.ZERO,
)

/**
 * Defines tolerance for heart beats.
 *
 * If both the client and server really stick to the heart beats periods negotiated and given by the CONNECTED frame,
 * network latencies will make them miss their marks. That's why we need some sort of tolerance.
 *
 * In case the server is too strict about its expectations, we can send heart beats a little earlier than we're
 * supposed to (see [outgoingMargin]).
 *
 * In case the server really sticks to its own period without such margin, we need to allow a little delay to
 * make up for network latencies before we fail and close the connection (see [incomingMargin]).
 */
data class HeartBeatTolerance(
    /**
     * How much time in advance heart beats should be sent.
     * This is to avoid issues when servers are not very tolerant on heart beats reception.
     *
     * By default, we expect the server to be tolerant in its expectations, so we send heart beats on time.
     */
    val outgoingMargin: Duration = Duration.ZERO,
    /**
     * How much more to wait before failing when we don't receive a heart beat from the server in the expected time.
     *
     * By default, we expect the server to send heart beats on time, so we add some margin in our own expectation.
     */
    val incomingMargin: Duration = 500.milliseconds,
)
