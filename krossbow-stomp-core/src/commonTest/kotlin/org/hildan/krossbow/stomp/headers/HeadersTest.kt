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

}