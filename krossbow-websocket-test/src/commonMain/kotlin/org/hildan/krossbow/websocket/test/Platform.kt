package org.hildan.krossbow.websocket.test

sealed interface Platform {
    data object Jvm : Platform
    sealed interface Js : Platform {
        data object Browser : Js
        data object NodeJs : Js
    }
    sealed interface Native : Platform
    data object Apple : Native
    data object Linux : Native
    data object Windows : Native
}

expect fun currentPlatform(): Platform