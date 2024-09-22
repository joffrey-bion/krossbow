import org.w3c.dom.WebSocket

// both annotations, so it's accessible from UMD
@JsModule("sockjs-client")
@JsNonModule
external object SockJS {
    @nativeInvoke
    operator fun invoke(url: String, protocols: List<String> = definedExternally, options: Options = definedExternally): WebSocket

    interface Options {
        var server: String?
            get() = definedExternally
            set(value) = definedExternally
        var sessionId: dynamic /* Number | SessionGenerator */
            get() = definedExternally
            set(value) = definedExternally
        var transports: dynamic /* String | Array<String> */
            get() = definedExternally
            set(value) = definedExternally
        var timeout: Number?
            get() = definedExternally
            set(value) = definedExternally
    }
}
