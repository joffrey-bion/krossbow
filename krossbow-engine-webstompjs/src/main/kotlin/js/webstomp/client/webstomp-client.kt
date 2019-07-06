@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS", "EXTERNAL_DELEGATION", "NESTED_CLASS_IN_EXTERNAL_INTERFACE")
package js.webstomp.client

external fun client(url: String, options: Options? = definedExternally /* null */): Client = definedExternally
external fun over(socketType: Any, options: Options? = definedExternally /* null */): Client = definedExternally
open external class Client {
    open fun connect(headers: ConnectionHeaders, connectCallback: (frame: Frame? /*= null*/) -> Any, errorCallback: ((error: String) -> Any)? = definedExternally /* null */): Unit = definedExternally
    open fun connect(login: String, passcode: String, connectCallback: (frame: Frame? /*= null*/) -> Any, errorCallback: ((error: String) -> Any)? = definedExternally /* null */, host: String? = definedExternally /* null */): Unit = definedExternally
    open fun disconnect(disconnectCallback: () -> Any, headers: DisconnectHeaders? = definedExternally /* null */): Unit = definedExternally
    open fun send(destination: String, body: String? = definedExternally /* null */, headers: ExtendedHeaders? = definedExternally /* null */): Unit = definedExternally
    open fun subscribe(destination: String, callback: ((message: Message) -> Any)? = definedExternally /* null */, headers: SubscribeHeaders? = definedExternally /* null */): Subscription = definedExternally
    open fun unsubscribe(id: String, header: UnsubscribeHeaders? = definedExternally /* null */): Unit = definedExternally
    open fun begin(transaction: String): Unit = definedExternally
    open fun commit(transaction: String): Unit = definedExternally
    open fun abort(transaction: String): Unit = definedExternally
    open fun ack(messageID: String, subscription: Subscription, headers: AckHeaders? = definedExternally /* null */): Unit = definedExternally
    open fun nack(messageID: String, subscription: Subscription, headers: NackHeaders? = definedExternally /* null */): Unit = definedExternally
}
open external class Frame(command: String, headers: Any? = definedExternally /* null */, body: String? = definedExternally /* null */) {
    override fun toString(): String = definedExternally
    open fun sizeOfUTF8(s: String): Number = definedExternally
    open fun unmarshall(datas: Any): Any = definedExternally
    open fun marshall(command: String, headers: Any? = definedExternally /* null */, body: String? = definedExternally /* null */): Any = definedExternally
}
external object VERSIONS {
    var V1_0: String = definedExternally
    var V1_1: String = definedExternally
    var V1_2: String = definedExternally
    var supportedVersions: () -> String = definedExternally
    var supportedProtocols: () -> Array<String> = definedExternally
}
external interface Heartbeat {
    var outgoing: Number
    var incoming: Number
}
external interface Subscription {
    var id: String
    var unsubscribe: () -> Unit
}
external interface Message {
    var command: String
    var body: String?
    var headers: ExtendedHeaders
    fun ack(headers: AckHeaders? = definedExternally /* null */): Any
    fun nack(headers: NackHeaders? = definedExternally /* null */): Any
}
external interface Options : ClientOptions {
    var protocols: Array<String>?
}
external interface ClientOptions {
    var binary: Boolean
    var heartbeat: dynamic /* Heartbeat | Boolean */
    var debug: Boolean
}
external interface ConnectionHeaders {
    var login: String
    var passcode: String
    var host: String? get() = definedExternally; set(value) = definedExternally
}
external interface DisconnectHeaders {
    var `receipt`: String? get() = definedExternally; set(value) = definedExternally
}
external interface StandardHeaders : DisconnectHeaders {
//    var `content-length`: String? get() = definedExternally; set(value) = definedExternally
//    var `content-type`: String? get() = definedExternally; set(value) = definedExternally
}
external interface ExtendedHeaders : StandardHeaders {
//    var `amqp-message-id`: String? get() = definedExternally; set(value) = definedExternally
//    var `app-id`: String? get() = definedExternally; set(value) = definedExternally
//    var `content-encoding`: String? get() = definedExternally; set(value) = definedExternally
//    var `correlation-id`: String? get() = definedExternally; set(value) = definedExternally
    var custom: String? get() = definedExternally; set(value) = definedExternally
    var destination: String? get() = definedExternally; set(value) = definedExternally
//    var `message-id`: String? get() = definedExternally; set(value) = definedExternally
    var persistent: String? get() = definedExternally; set(value) = definedExternally
    var redelivered: String? get() = definedExternally; set(value) = definedExternally
//    var `reply-to`: String? get() = definedExternally; set(value) = definedExternally
    var subscription: String? get() = definedExternally; set(value) = definedExternally
    var timestamp: String? get() = definedExternally; set(value) = definedExternally
    var type: String? get() = definedExternally; set(value) = definedExternally
}
external interface UnsubscribeHeaders : StandardHeaders {
    var id: String? get() = definedExternally; set(value) = definedExternally
}
external interface SubscribeHeaders : UnsubscribeHeaders {
    var ack: String? get() = definedExternally; set(value) = definedExternally
}
external interface AckHeaders : UnsubscribeHeaders {
    var transaction: String? get() = definedExternally; set(value) = definedExternally
}
external interface NackHeaders : AckHeaders
