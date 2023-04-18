import org.w3c.fetch.Response
import kotlin.js.Promise

// This function could be taken from node-fetch but Kotlin/JS doesn't support this type of module.
// Using isomorphic-fetch solves the problem, and also gives the same API from browser and node.
@JsNonModule
@JsModule("isomorphic-fetch")
external fun fetch(url: String): Promise<Response>
