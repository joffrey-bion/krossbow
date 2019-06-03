package org.hildan.krossbow.engines.webstompjs

import js.webstomp.client.Client
import js.webstomp.client.Heartbeat
import js.webstomp.client.client
import js.webstomp.client.Options
import org.hildan.krossbow.engines.HeartBeat
import org.hildan.krossbow.engines.KrossbowEngine

class WebstompHeartbeat(
    override var outgoing: Number,
    override var incoming: Number
) : Heartbeat

private fun HeartBeat.toWebstomp(): WebstompHeartbeat = WebstompHeartbeat(minSendPeriodMillis, expectedPeriodMillis)

class WebstompOptions(
    override var protocols: Array<String>? = null,
    override var binary: Boolean = false,
    override var heartbeat: WebstompHeartbeat,
    override var debug: Boolean = false
) : Options

class WebstompJSEngine(private val client: Client): KrossbowEngine {

    constructor(url: String, heartBeat: HeartBeat = HeartBeat()):
            this(client(url, WebstompOptions(heartbeat = heartBeat.toWebstomp())))

}
