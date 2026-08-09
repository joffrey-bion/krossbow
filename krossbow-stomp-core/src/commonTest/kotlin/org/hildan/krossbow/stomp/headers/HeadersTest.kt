package org.hildan.krossbow.stomp.headers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HeadersTest {

    @Test
    fun StompConnectHeaders_fails_on_invalid_LF_in_header_value() {
        val e = assertFailsWith<InvalidStompHeaderException> {
            StompConnectHeaders(host = "some.host", forStompCommand = false) {
                login = "foo\nbar"
            }
        }
        assertEquals(
            expected = "New line characters are not allowed in the headers of a CONNECT or CONNECTED frame, got 'foo\nbar'",
            actual = e.message,
        )
    }

    @Test
    fun StompConnectHeaders_fails_on_invalid_CR_in_header_value() {
        val e = assertFailsWith<InvalidStompHeaderException> {
            StompConnectHeaders(host = "some.host", forStompCommand = false) {
                login = "value\rwith\r\nCRLF"
            }
        }
        assertEquals(
            expected = "New line characters are not allowed in the headers of a CONNECT or CONNECTED frame, got 'value\rwith\r\nCRLF'",
            actual = e.message,
        )
    }

    @Test
    fun StompConnectHeaders_fails_on_invalid_LF_in_header_name() {
        val e = assertFailsWith<InvalidStompHeaderException> {
            StompConnectHeaders(host = "some.host", forStompCommand = false) {
                set("name\nwith\nnewline", "foo bar")
            }
        }
        assertEquals(
            expected = "New line characters are not allowed in the headers of a CONNECT or CONNECTED frame, got 'name\nwith\nnewline'",
            actual = e.message,
        )
    }

    @Test
    fun StompConnectHeaders_fails_on_invalid_CR_in_header_name() {
        val e = assertFailsWith<InvalidStompHeaderException> {
            StompConnectHeaders(host = "some.host", forStompCommand = false) {
                set("name\rwith\r\nCRLF", "foo bar")
            }
        }
        assertEquals(
            expected = "New line characters are not allowed in the headers of a CONNECT or CONNECTED frame, got 'name\rwith\r\nCRLF'",
            actual = e.message,
        )
    }

    @Test
    fun StompConnectHeaders_fails_on_invalid_colon_in_header_name() {
        val e = assertFailsWith<InvalidStompHeaderException> {
            StompConnectHeaders(host = "some.host", forStompCommand = false) {
                set("name:withcolon", "foo bar")
            }
        }
        assertEquals(
            expected = "Colon ':' characters are not allowed in the header names of a CONNECT or CONNECTED frame, got 'name:withcolon'",
            actual = e.message,
        )
    }

    @Test
    fun StompConnectHeaders_accepts_any_character_for_STOMP_command() {
        StompConnectHeaders(host = "some.host", forStompCommand = true) {
            login = "foo\nbar:baz"
            passcode = "foo\r\nbar"
            set("my:very\nweird\rheader", "oh:my\r\nthis is crazy")
        }
    }

    @Test
    fun StompConnectHeaders_fails_on_NUL_in_header_value() {
        val e = assertFailsWith<InvalidStompHeaderException> {
            StompConnectHeaders(host = "some.host", forStompCommand = false) {
                login = "foo\u0000bar"
            }
        }
        assertEquals(
            expected = "The NUL character is not allowed in STOMP headers because it terminates the " +
                "frame and has no escape sequence, got 'foo\u0000bar'",
            actual = e.message,
        )
    }

    @Test
    fun StompConnectHeaders_fails_on_NUL_in_header_name() {
        val e = assertFailsWith<InvalidStompHeaderException> {
            StompConnectHeaders(host = "some.host", forStompCommand = false) {
                set("name\u0000withNUL", "value")
            }
        }
        assertEquals(
            expected = "The NUL character is not allowed in STOMP headers because it terminates the " +
                "frame and has no escape sequence, got 'name\u0000withNUL'",
            actual = e.message,
        )
    }

    @Test
    fun StompConnectHeaders_fails_on_NUL_even_for_STOMP_command() {
        // The STOMP frame escapes special chars, but NUL has no escape sequence, so it must still be rejected.
        assertFailsWith<InvalidStompHeaderException> {
            StompConnectHeaders(host = "some.host", forStompCommand = true) {
                login = "foo\u0000bar"
            }
        }
        assertFailsWith<InvalidStompHeaderException> {
            StompConnectHeaders(host = "some.host", forStompCommand = true) {
                set("name\u0000withNUL", "value")
            }
        }
    }

    @Test
    fun StompSendHeaders_fails_on_NUL_in_header_value() {
        // SEND frames escape special chars in headers, but NUL has no escape sequence, so it must still be rejected.
        val e = assertFailsWith<InvalidStompHeaderException> {
            StompSendHeaders(destination = "/some/dest") {
                set("my-header", "foo\u0000bar")
            }
        }
        assertEquals(
            expected = "The NUL character is not allowed in STOMP headers because it terminates the " +
                "frame and has no escape sequence, got 'foo\u0000bar'",
            actual = e.message,
        )
    }

    @Test
    fun StompSendHeaders_fails_on_NUL_in_header_name() {
        val e = assertFailsWith<InvalidStompHeaderException> {
            StompSendHeaders(destination = "/some/dest") {
                set("name\u0000withNUL", "value")
            }
        }
        assertEquals(
            expected = "The NUL character is not allowed in STOMP headers because it terminates the " +
                "frame and has no escape sequence, got 'name\u0000withNUL'",
            actual = e.message,
        )
    }

}
