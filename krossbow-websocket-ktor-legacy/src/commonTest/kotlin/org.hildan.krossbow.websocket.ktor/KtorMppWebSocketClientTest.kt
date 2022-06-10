package org.hildan.krossbow.websocket.ktor

// FIXME these tests don't work on JS platform because Ktor 2 is incorrectly present at runtime (due to how JS modules
//  are organized)
//  See https://youtrack.jetbrains.com/issue/KT-31504
//  Interestingly, even ignoring them is not sufficient and we need to completely comment them.

//@IgnoreOnJS
//class KtorWebSocketClientTest : WebSocketClientTestSuite() {
//
//    override fun provideClient(): WebSocketClient = KtorWebSocketClient()
//}
