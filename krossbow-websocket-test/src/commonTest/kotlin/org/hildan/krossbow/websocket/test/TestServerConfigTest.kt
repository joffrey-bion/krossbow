import org.hildan.krossbow.websocket.test.*
import kotlin.test.*

class TestServerConfigTest {

    @Test
    fun serverConfigIsAvailable() {
        val config = getTestServerConfig()
        assertTrue(config.host.isNotBlank())
        assertTrue(config.wsPort > 0)
        assertTrue(config.httpPort > 0)
    }
}
