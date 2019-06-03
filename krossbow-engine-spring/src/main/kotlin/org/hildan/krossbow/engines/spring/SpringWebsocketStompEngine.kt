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

class SpringWebsocketStompEngine: KrossbowEngine
