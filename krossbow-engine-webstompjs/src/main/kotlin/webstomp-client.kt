@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS", "EXTERNAL_DELEGATION")

external fun client(url: String, options: Options? = definedExternally /* null */): Client

external fun over(socketType: Any, options: Options? = definedExternally /* null */): Client

open external class Client {
    open var connected: Boolean
    open var isBinary: Boolean
    open var partialData: String
    open var subscriptions: SubscriptionsMap
    open var ws: Any
    open fun connect(headers: ConnectionHeaders, connectCallback: (frame: Frame? /* = null */) -> Any, errorCallback: ((error: dynamic /* CloseEvent | Frame */) -> Any)? = definedExternally /* null */)
    open fun connect(login: String, passcode: String, connectCallback: (frame: Frame? /* = null */) -> Any, errorCallback: ((error: dynamic /* CloseEvent | Frame */) -> Any)? = definedExternally /* null */, host: String? = definedExternally /* null */)
    open fun disconnect(disconnectCallback: (() -> Any)? = definedExternally /* null */, headers: DisconnectHeaders? = definedExternally /* null */)
    open fun send(destination: String, body: String? = definedExternally /* null */, headers: ExtendedHeaders? = definedExternally /* null */)
    open fun subscribe(destination: String, callback: ((message: Message) -> Any)? = definedExternally /* null */, headers: SubscribeHeaders? = definedExternally /* null */): Subscription
    open fun unsubscribe(id: String, header: UnsubscribeHeaders? = definedExternally /* null */)
    open fun begin(transaction: String)
    open fun commit(transaction: String)
    open fun abort(transaction: String)
    open fun ack(messageID: String, subscription: Subscription, headers: AckHeaders? = definedExternally /* null */)
    open fun nack(messageID: String, subscription: Subscription, headers: NackHeaders? = definedExternally /* null */)
    open fun debug(vararg args: Any)
    open fun onreceipt(frame: Frame)
}

external interface `T$0` {
    var frames: Array<Frame>
    var partial: String?
        get() = definedExternally
        set(value) = definedExternally
}

open external class Frame(command: String, headers: Headers? = definedExternally /* null */, body: String? = definedExternally /* null */) {
    open var command: String
    open var body: String
    open val headers: Headers
    override fun toString(): String

    companion object {
        fun unmarshallSingle(data: String): Frame
        fun unmarshall(datas: String): `T$0`
        fun marshall(command: String, headers: Headers? = definedExternally /* null */, body: String? = definedExternally /* null */): String
    }
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

external interface SubscriptionsMap

//@Suppress("NOTHING_TO_INLINE")
//inline operator fun SubscriptionsMap.get(id: String): ((message: Message) -> Any)? = asDynamic()[id]
//
//@Suppress("NOTHING_TO_INLINE")
//inline operator fun SubscriptionsMap.set(id: String, noinline value: (message: Message) -> Any) {
//    asDynamic()[id] = value
//}

external interface Message : Frame {
    override var headers: ExtendedHeaders
    fun ack(headers: AckHeaders? = definedExternally /* null */): Any
    fun nack(headers: NackHeaders? = definedExternally /* null */): Any
}

external interface Options : ClientOptions {
    var protocols: Array<String>?
        get() = definedExternally
        set(value) = definedExternally
}

external interface ClientOptions {
    var binary: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var heartbeat: dynamic /* Heartbeat | Boolean */
        get() = definedExternally
        set(value) = definedExternally
    var debug: Boolean?
        get() = definedExternally
        set(value) = definedExternally
}

external interface Headers

//@Suppress("NOTHING_TO_INLINE")
//inline operator fun Headers.get(key: String): String? = asDynamic()[key]
//
//@Suppress("NOTHING_TO_INLINE")
//inline operator fun Headers.set(key: String, value: String?) {
//    asDynamic()[key] = value
//}

external interface ConnectionHeaders : Headers {
    var login: String?
        get() = definedExternally
        set(value) = definedExternally
    var passcode: String?
        get() = definedExternally
        set(value) = definedExternally
    var host: String?
        get() = definedExternally
        set(value) = definedExternally
}

external interface DisconnectHeaders : Headers {
    var `receipt`: String?
        get() = definedExternally
        set(value) = definedExternally
}

external interface StandardHeaders : DisconnectHeaders {
//    var `content-length`: String?
//        get() = definedExternally
//        set(value) = definedExternally
//    var `content-type`: String?
//        get() = definedExternally
//        set(value) = definedExternally
}

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

@JsModule("webstomp-client")
external object webstomp {
    var Frame: Frame
    var VERSIONS: Any
    var client: (url: String, options: Options?) -> Client
    var over: (socketType: Any, options: Options?) -> Client
}
