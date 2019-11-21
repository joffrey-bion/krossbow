@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS", "EXTERNAL_DELEGATION", "NESTED_CLASS_IN_EXTERNAL_INTERFACE")

external fun client(url: String, options: Options? = definedExternally /* null */): Client

external fun over(socketType: Any, options: Options? = definedExternally /* null */): Client

open external class Client {
    open fun connect(headers: ConnectionHeaders, connectCallback: (frame: Frame? /* = null */) -> Any, errorCallback: ((error: String) -> Any)? = definedExternally /* null */)
    open fun connect(login: String, passcode: String, connectCallback: (frame: Frame? /* = null */) -> Any, errorCallback: ((error: String) -> Any)? = definedExternally /* null */, host: String? = definedExternally /* null */)
    open fun disconnect(disconnectCallback: () -> Any, headers: DisconnectHeaders? = definedExternally /* null */)
    open fun send(destination: String, body: String? = definedExternally /* null */, headers: ExtendedHeaders? = definedExternally /* null */)
    open fun subscribe(destination: String, callback: ((message: Message) -> Any)? = definedExternally /* null */, headers: SubscribeHeaders? = definedExternally /* null */): Subscription
    open fun unsubscribe(id: String, header: UnsubscribeHeaders? = definedExternally /* null */)
    open fun begin(transaction: String)
    open fun commit(transaction: String)
    open fun abort(transaction: String)
    open fun ack(messageID: String, subscription: Subscription, headers: AckHeaders? = definedExternally /* null */)
    open fun nack(messageID: String, subscription: Subscription, headers: NackHeaders? = definedExternally /* null */)
}

open external class Frame(command: String, headers: Any? = definedExternally /* null */, body: String? = definedExternally /* null */) {
    override fun toString(): String
    open fun sizeOfUTF8(s: String): Number
    open fun unmarshall(datas: Any): Any
    open fun marshall(command: String, headers: Any? = definedExternally /* null */, body: String? = definedExternally /* null */): Any
}

external object VERSIONS {
    var V1_0: String
    var V1_1: String
    var V1_2: String
    var supportedVersions: () -> String
    var supportedProtocols: () -> Array<String>
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
    var body: String
    var headers: ExtendedHeaders
    fun ack(headers: AckHeaders? = definedExternally /* null */): Any
    fun nack(headers: NackHeaders? = definedExternally /* null */): Any
}

external interface Options : ClientOptions {
    var protocols: Array<String>
}

external interface ClientOptions {
    var binary: Boolean
    var heartbeat: dynamic /* Heartbeat | Boolean */
    var debug: Boolean
}

external interface ConnectionHeaders {
    var login: String
    var passcode: String
    var host: String?
        get() = definedExternally
        set(value) = definedExternally
}

external interface DisconnectHeaders {
    var `receipt`: String?
        get() = definedExternally
        set(value) = definedExternally
}

external interface StandardHeaders : DisconnectHeaders {
    // content-length defined as extension
    // content-type defined as extension
}
var StandardHeaders.contentLength: String?
    get() = asDynamic()["content-length"]
    set(value) { asDynamic()["content-length"] = value }

var StandardHeaders.contentType: String?
    get() = asDynamic()["content-type"]
    set(value) { asDynamic()["content-type"] = value }

external interface ExtendedHeaders : StandardHeaders {
//    var `amqp-message-id`: String?
//        get() = definedExternally
//        set(value) = definedExternally
//    var `app-id`: String?
//        get() = definedExternally
//        set(value) = definedExternally
//    var `content-encoding`: String?
//        get() = definedExternally
//        set(value) = definedExternally
//    var `correlation-id`: String?
//        get() = definedExternally
//        set(value) = definedExternally
    var custom: String?
        get() = definedExternally
        set(value) = definedExternally
    var destination: String?
        get() = definedExternally
        set(value) = definedExternally
//    var `message-id`: String?
//        get() = definedExternally
//        set(value) = definedExternally
    var persistent: String?
        get() = definedExternally
        set(value) = definedExternally
    var redelivered: String?
        get() = definedExternally
        set(value) = definedExternally
//    var `reply-to`: String?
//        get() = definedExternally
//        set(value) = definedExternally
    var subscription: String?
        get() = definedExternally
        set(value) = definedExternally
    var timestamp: String?
        get() = definedExternally
        set(value) = definedExternally
    var type: String?
        get() = definedExternally
        set(value) = definedExternally
}

external interface UnsubscribeHeaders : StandardHeaders {
    var id: String?
        get() = definedExternally
        set(value) = definedExternally
}

external interface SubscribeHeaders : UnsubscribeHeaders {
    var ack: String?
        get() = definedExternally
        set(value) = definedExternally
}

external interface AckHeaders : UnsubscribeHeaders {
    var transaction: String?
        get() = definedExternally
        set(value) = definedExternally
}

external interface NackHeaders : AckHeaders
