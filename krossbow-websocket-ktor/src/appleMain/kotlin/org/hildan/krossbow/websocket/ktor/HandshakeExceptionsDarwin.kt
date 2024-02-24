package org.hildan.krossbow.websocket.ktor

import io.ktor.client.engine.darwin.DarwinHttpRequestException
import kotlinx.cinterop.*
import platform.Foundation.*

/*
io.ktor.client.engine.darwin.DarwinHttpRequestException:
Exception in http request: Error Domain=NSURLErrorDomain Code=-1011 "There was a bad response from the server."
UserInfo={
  NSErrorFailingURLStringKey=ws://localhost:49347/failHandshakeWithStatusCode/200,
  NSErrorFailingURLKey=ws://localhost:49347/failHandshakeWithStatusCode/200,
  _NSURLErrorWebSocketHandshakeFailureReasonKey=0,
  _NSURLErrorRelatedURLSessionTaskErrorKey=("LocalWebSocketTask <26F4D5BA-7104-4506-A521-DBC19B1CC2B0>.<1>"),
  _NSURLErrorFailingURLSessionTaskErrorKey=LocalWebSocketTask <26F4D5BA-7104-4506-A521-DBC19B1CC2B0>.<1>,
  NSLocalizedDescription=There was a bad response from the server.
}
 */

internal actual fun extractHandshakeFailureDetails(handshakeException: Exception): HandshakeFailureDetails = when (handshakeException) {
    is DarwinHttpRequestException -> extractHandshakeFailureDetails(handshakeException.origin)
    else -> genericFailureDetails(handshakeException)
}

// FIXME clean this up after investigation
@OptIn(UnsafeNumber::class)
private fun extractHandshakeFailureDetails(originError: NSError): HandshakeFailureDetails = HandshakeFailureDetails(
    statusCode = null, // TODO find out if we can get it from somewhere
    additionalInfo = """
        domain=${originError.domain}
        code=${originError.code}
        description=${originError.description}
        userInfo=${originError.userInfo}
        underlyingErrors=${originError.underlyingErrors}
        localizedFailureReason=${originError.localizedFailureReason}
        localizedRecoveryOptions=${originError.localizedRecoveryOptions}
        helpAnchor=${originError.helpAnchor}
        recoveryAttempter=${originError.recoveryAttempter}
        
        originError to string: $originError
    """.trimIndent(),
)
