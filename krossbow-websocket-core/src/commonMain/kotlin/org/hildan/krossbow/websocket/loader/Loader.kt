package org.hildan.krossbow.websocket.loader

import kotlinx.atomicfu.atomic
import org.hildan.krossbow.websocket.WebSocketClient

/**
 * Gets the first [WebSocketClient] implementation in the dependencies for the current platform.
 */
expect fun WebSocketClient.Companion.fromDependencies(): WebSocketClientFactory<*>

/**
 * Base interface for [WebSocketClient] configuration types.
 */
interface WebSocketClientConfig

/**
 * Creates [WebSocketClient]s that can be customized via configurations of type [C].
 */
interface WebSocketClientFactory<out C : WebSocketClientConfig> {

    fun create(configure: C.() -> Unit): WebSocketClient
}

/**
 * A way to register [WebSocketClientFactory] implementations from Kotlin/Native or Kotlin/JS auto-discovery.
 * In Kotlin/JVM, a `ServiceLoader` should be used instead.
 */
object WebSocketClientFactoriesRegistry {
    private val head = atomic<Node?>(null)

    /**
     * Registers the given web socket implementation.
     */
    fun register(factory: WebSocketClientFactory<*>) {
        while (true) {
            val current = head.value
            val new = Node(factory, current)

            if (head.compareAndSet(current, new)) {
                break
            }
        }
    }

    /**
     * Finds an auto-registered native web socket implementation.
     */
    fun find(): WebSocketClientFactory<*>? = head.value?.item

    /**
     * Lists all auto-registered native web socket implementations.
     */
    fun list(): Sequence<WebSocketClientFactory<*>> = sequence {
        var current = head.value
        while (current != null) {
            yield(current.item)
            current = current.next
        }
    }

    private class Node(
        val item: WebSocketClientFactory<*>,
        val next: Node?
    )
}
