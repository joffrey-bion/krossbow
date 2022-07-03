package org.hildan.krossbow.stomp.utils

import com.benasher44.uuid.uuid4

/**
 * Generates a new UUID (v4) as a string.
 */
internal fun generateUuid(): String = uuid4().toString()
