package org.hildan.krossbow.client

import org.hildan.krossbow.engines.KrossbowEngine
import org.hildan.krossbow.engines.spring.SpringKrossbowEngine

internal actual fun defaultEngine(): KrossbowEngine = SpringKrossbowEngine
