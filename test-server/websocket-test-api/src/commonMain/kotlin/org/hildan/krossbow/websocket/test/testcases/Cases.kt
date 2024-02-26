package org.hildan.krossbow.websocket.test.testcases

//object TestCases {
//    val ALL = listOf(
//        TestCase("1.1", "", events = emptyList())
//    )
//}
//
//data class TestCase(
//    val id: String,
//    val description: String,
//    val events: List<TestEvent>,
//)
//
//sealed interface TestEvent
//
//sealed interface ClientEvent : TestEvent {
//    data class TextFrame(val payload: String) : ClientEvent
//    data class BinaryFrame(val payload: ByteArray) : ClientEvent
//    data class CloseFrame(val code: Int, val reason: String? = null) : ClientEvent
//    data object ConnectionClosed : ClientEvent
//}
//
//sealed interface ServerEvent : TestEvent {
//    data class TextFrame(val payload: String) : ServerEvent
//    data class BinaryFrame(val payload: ByteArray) : ServerEvent
//    data class CloseFrame(val code: Int, val reason: String? = null) : ServerEvent
//    data object ConnectionClosed : ServerEvent
//}
