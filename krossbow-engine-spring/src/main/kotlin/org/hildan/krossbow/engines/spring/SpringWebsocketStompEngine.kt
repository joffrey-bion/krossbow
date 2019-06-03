package org.hildan.krossbow.engines.spring

import org.hildan.krossbow.engines.KrossbowEngine
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.web.socket.client.WebSocketClient
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.Transport
import org.springframework.web.socket.sockjs.client.WebSocketTransport

class SpringWebsocketStompEngine(private val client: WebSocketStompClient): KrossbowEngine {

    /**
     * Creates a `SpringWebsocketStompEngine` with convenient defaults.
     *
     * It creates a pre-configured [WebSocketStompClient] using a Spring [SockJsClient] and a Jackson message converter.
     *
     * Only the [WebSocketTransport] is used for the [SockJsClient]. For more transports options, please use another
     * constructor.
     */
    constructor(): this(createWsTransports())

    /**
     * Creates a `SpringWebsocketStompEngine` with convenient defaults.
     *
     * It creates a pre-configured [WebSocketStompClient] using a Spring [SockJsClient] on the given [transports] and a
     * Jackson message converter.
     */
    constructor(transports: List<Transport>): this(createWebSocketClient(transports))

    /**
     * Creates a `SpringWebsocketStompEngine` with convenient defaults.
     *
     * It creates a pre-configured [WebSocketStompClient] using the given [webSocketClient] and a Jackson message
     * converter.
     */
    constructor(webSocketClient: WebSocketClient): this(createStompClient(webSocketClient))
}

private fun createWsTransports(): List<Transport> {
    return listOf<Transport>(WebSocketTransport(StandardWebSocketClient()))
}

private fun createWebSocketClient(transports: List<Transport>): WebSocketClient {
    return SockJsClient(transports)
}

private fun createStompClient(webSocketClient: WebSocketClient): WebSocketStompClient {
    val stompClient = WebSocketStompClient(webSocketClient)
    stompClient.messageConverter = MappingJackson2MessageConverter() // for custom object exchanges
    stompClient.taskScheduler = createTaskScheduler() // for heartbeats
    return stompClient
}

private fun createTaskScheduler(): ThreadPoolTaskScheduler {
    val taskScheduler = ThreadPoolTaskScheduler()
    taskScheduler.afterPropertiesSet()
    return taskScheduler
}
