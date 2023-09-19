package org.hildan.krossbow.websocket.sockjs

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.spring.asKrossbowWebSocketClient
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.WebSocketTransport

@Suppress("FunctionName")
actual fun SockJSClient(): WebSocketClient = SockJsClient(createDefaultSockJSTransports()).asKrossbowWebSocketClient()

private fun createDefaultSockJSTransports() = listOf(
    WebSocketTransport(StandardWebSocketClient()),
    RestTemplateXhrTransport()
)
