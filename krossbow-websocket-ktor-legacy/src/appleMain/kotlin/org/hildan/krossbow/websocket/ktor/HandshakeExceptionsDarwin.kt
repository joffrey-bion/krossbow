package org.hildan.krossbow.websocket.ktor

internal actual fun extractHandshakeFailureDetails(handshakeException: Exception): HandshakeFailureDetails =
    genericFailureDetails(handshakeException)

/*
We cannot extract any response code from the exception.

The original error is almost the same for all response codes:
io.ktor.client.engine.darwin.DarwinHttpRequestException: Exception in http request: Error Domain=NSURLErrorDomain Code=-1011 "There was a bad response from the server."

NSError object attached to the DarwinHttpRequestException:
{
    code = -1011
    description = Error Domain=NSURLErrorDomain Code=-1011 "There was a bad response from the server." UserInfo={NSErrorFailingURLStringKey=ws://localhost:49504/failHandshakeWithStatusCode/200, NSErrorFailingURLKey=ws://localhost:49504/failHandshakeWithStatusCode/200, _NSURLErrorWebSocketHandshakeFailureReasonKey=0, NSLocalizedDescription=There was a bad response from the server.}
    userInfo = {
      NSErrorFailingURLStringKey = ws://localhost:49347/failHandshakeWithStatusCode/200,
      NSErrorFailingURLKey = ws://localhost:49347/failHandshakeWithStatusCode/200,
      _NSURLErrorWebSocketHandshakeFailureReasonKey = 0, // sometimes different for tvOS
      _NSURLErrorRelatedURLSessionTaskErrorKey = ("LocalWebSocketTask <26F4D5BA-7104-4506-A521-DBC19B1CC2B0>.<1>"),
      _NSURLErrorFailingURLSessionTaskErrorKey = LocalWebSocketTask <26F4D5BA-7104-4506-A521-DBC19B1CC2B0>.<1>,
      NSLocalizedDescription=There was a bad response from the server.
    }
    underlyingErrors = []
    localizedFailureReason = null
    localizedRecoveryOptions = null
    helpAnchor = null
    recoveryAttempter = null
}
 */
