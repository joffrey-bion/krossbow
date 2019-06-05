package org.hildan.krossbow.engines

// TODO handle headers typing for params and received frames
interface MessageHeaders

interface UnsubscribeHeaders

data class KrossbowReceipt(val id: String)

data class KrossbowMessage<out T>(
    val payload: T,
    val headers: MessageHeaders
)
