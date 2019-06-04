package org.hildan.krossbow.client

import org.hildan.krossbow.engines.KrossbowEngine
import org.hildan.krossbow.engines.webstompjs.WebstompKrossbowEngine

internal actual fun defaultEngine(): KrossbowEngine = WebstompKrossbowEngine
