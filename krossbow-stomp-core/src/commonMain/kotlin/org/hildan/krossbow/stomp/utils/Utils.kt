package org.hildan.krossbow.stomp.utils

import kotlin.uuid.*

/**
 * Generates a new UUID (v4) as a string.
 */
@OptIn(ExperimentalUuidApi::class)
internal fun generateUuid(): String = Uuid.random().toString()
