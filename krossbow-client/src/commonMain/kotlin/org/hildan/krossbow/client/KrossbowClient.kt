package org.hildan.krossbow.client

import org.hildan.krossbow.engines.KrossbowClient
import org.hildan.krossbow.engines.KrossbowConfig
import org.hildan.krossbow.engines.KrossbowEngine

/**
 * Creates an instance of [KrossbowClient] based on the current platform's default engine. The provided configuration
 * function is applied to the newly created client.
 */
@Suppress("FunctionName")
fun KrossbowClient(configure: KrossbowConfig.() -> Unit = {}): KrossbowClient =
    KrossbowClient(defaultEngine(), configure)

internal expect fun defaultEngine(): KrossbowEngine
