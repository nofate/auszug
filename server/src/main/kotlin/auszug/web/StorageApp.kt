package auszug.web

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.concurrent.ConcurrentHashMap


val store = ConcurrentHashMap<String, Any>();

fun Route.storageRouting() {
    route("/v1/storage") {
        get("/{id}") {
            call.respond(store[call.parameters["id"]] ?: emptyList<Any>())
        }

        post("/{id}") {
            val id = call.parameters["id"].orEmpty()
            val body = call.receiveText()
            store.put(id, body)
            call.respond(HttpStatusCode.Created)
        }

        put("/{id}") {

        }

        delete("/{id}") {

        }
    }
}

fun Application.configureRouting() {
    routing {
        storageRouting()
    }
}


fun Application.storageModule() {
    configureRouting()
    install(ContentNegotiation) {
        json()
    }
}