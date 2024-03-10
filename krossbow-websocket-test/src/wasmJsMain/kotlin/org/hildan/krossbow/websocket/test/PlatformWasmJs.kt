package org.hildan.krossbow.websocket.test

actual fun currentPlatform(): Platform = currentWasmJsPlatform()

fun currentWasmJsPlatform(): Platform.WasmJs = if (isBrowser()) Platform.WasmJs.Browser else Platform.WasmJs.NodeJs

private fun isBrowser(): Boolean = js("typeof window !== 'undefined' && typeof window.document !== 'undefined'")
