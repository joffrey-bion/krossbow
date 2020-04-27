package org.hildan.krossbow.utils

import com.benasher44.uuid.uuid4
import kotlinx.io.charsets.Charset

/**
 * Generates a new UUID (v4) as a string.
 */
internal fun generateUuid(): String = uuid4().toString()

internal fun extractCharset(mimeTypeText: String): Charset? = mimeTypeText.splitToSequence(';')
    .drop(1)
    .map { it.substringAfter("charset=", "") }
    .firstOrNull { it.isNotEmpty() }
    ?.let { Charset.forName(it) }
