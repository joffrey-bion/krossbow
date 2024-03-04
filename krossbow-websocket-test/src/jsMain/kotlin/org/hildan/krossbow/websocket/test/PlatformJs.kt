package org.hildan.krossbow.websocket.test

actual fun currentPlatform(): Platform = currentJsPlatform()

fun currentJsPlatform(): Platform.Js = if (isBrowser()) Platform.Js.Browser else Platform.Js.NodeJs

private fun isBrowser() = js("typeof window !== 'undefined' && typeof window.document !== 'undefined'") as Boolean
