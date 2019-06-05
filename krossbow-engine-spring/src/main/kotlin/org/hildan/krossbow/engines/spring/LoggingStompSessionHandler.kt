package org.hildan.krossbow.engines.spring

import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter

typealias SubscriptionExceptionHandler = (Throwable) -> Unit

internal class LoggingStompSessionHandler : StompSessionHandlerAdapter() {

    private val subscriptionHandlers = mutableMapOf<String, SubscriptionExceptionHandler>()

    override fun afterConnected(session: StompSession, connectedHeaders: StompHeaders) {
        logger.error("STOMP session connected: ${session.sessionId}")
    }

    override fun handleException(
        session: StompSession,
        command: StompCommand,
        headers: StompHeaders,
        payload: ByteArray,
        exception: Throwable
    ) {
        val subscriptionId = headers.subscription
        if (subscriptionId != null) {
            handleSubscriptionException(subscriptionId, exception)
        } else {
            logger.error("Exception thrown in session ${session.sessionId}", exception)
        }
    }

    private fun handleSubscriptionException(subscriptionId: String, exception: Throwable) {
        val handler = subscriptionHandlers[subscriptionId]
        if (handler != null) {
            handler(exception)
        } else {
            logger.error("Exception thrown in subscription $subscriptionId", exception)
        }
    }

    override fun handleTransportError(session: StompSession, exception: Throwable?) {
        logger.error("Transport exception thrown in session ${session.sessionId}", exception)
    }

    fun registerExceptionHandler(subscriptionId: String, handler: SubscriptionExceptionHandler) {
        subscriptionHandlers[subscriptionId] = handler
    }

    companion object {

        private val logger = LoggerFactory.getLogger(LoggingStompSessionHandler::class.java)
    }
}
