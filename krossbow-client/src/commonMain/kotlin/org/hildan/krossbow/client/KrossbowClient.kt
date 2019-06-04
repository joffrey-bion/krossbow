package org.hildan.krossbow.client

import org.hildan.krossbow.engines.KrossbowClient
import org.hildan.krossbow.engines.KrossbowConfig
import org.hildan.krossbow.engines.KrossbowEngine

fun KrossbowClient(configure: KrossbowConfig.() -> Unit = {}): KrossbowClient =
    KrossbowClient(defaultEngine(), configure)

internal expect fun defaultEngine(): KrossbowEngine
