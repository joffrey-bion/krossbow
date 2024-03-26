package org.hildan.krossbow.websocket.test

sealed interface Platform {
    data object Jvm : Platform
    sealed interface Js : Platform {
        data object Browser : Js
        data object NodeJs : Js
    }
    sealed interface Native : Platform
    data class Apple(val os: String) : Native {
        override fun toString(): String = "Apple-${os.replace(Regex("""[\s_]"""), "-")}"
    }
    data object Linux : Native
    data object Windows : Native
}

expect fun currentPlatform(): Platform
