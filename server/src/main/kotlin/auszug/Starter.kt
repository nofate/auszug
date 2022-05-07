import auszug.web.storageModule
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.*

class Starter {

}

fun main() {
    embeddedServer(Netty, port = 8080,  configure = {
        connectionGroupSize = 1
    }) {
        storageModule()
    }.start(true)
}