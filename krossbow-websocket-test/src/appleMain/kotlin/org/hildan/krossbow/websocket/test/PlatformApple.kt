package org.hildan.krossbow.websocket.test

import platform.Foundation.*

actual fun currentPlatform(): Platform = Platform.Apple(NSProcessInfo.processInfo.operatingSystemName())
