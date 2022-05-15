package auszug.web

import auszug.storage.XodusStorage
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap


val store = XodusStorage("/home/nofate/work/private/auszug/run/");

@Serializable
data class TransactionResponse(val tranId: Long)

fun Route.storageRouting() {
    route("/v1") {

        route("/storage") {
            get("/{id}") {
                val key = call.parameters["id"] ?: throw IllegalArgumentException()
                val tranId = call.request.queryParameters["tranId"]?.toLong() ?: throw IllegalArgumentException()

                val value = store.get(tranId, key)
                call.respondText(value ?: "{}", ContentType.Application.Json)
            }

            post("/{id}") {
                val key = call.parameters["id"] ?: throw IllegalArgumentException()
                val tranId = call.request.queryParameters["tranId"]?.toLong() ?: throw IllegalArgumentException()
                val body = call.receiveText()
                store.put(tranId, key, body)
                call.respond(HttpStatusCode.Created)
            }

            delete("/{id}") {
                val key = call.parameters["id"] ?: throw IllegalArgumentException()
                val tranId = call.request.queryParameters["tranId"]?.toLong() ?: throw IllegalArgumentException()

                store.delete(tranId, key)
                call.respond(HttpStatusCode.OK)
            }
        }

        route("/transaction") {
            post() {
                val tranId = store.startTransaction()
                call.respond(HttpStatusCode.Created, TransactionResponse(tranId))
            }

            post("/{id}/commit") {
                val tranId = call.parameters["id"]?.toLong() ?: throw IllegalArgumentException()
                store.commit(tranId)
                call.respond(HttpStatusCode.OK)
            }

            post("/{id}/rollback") {
                val tranId = call.parameters["id"]?.toLong() ?: throw IllegalArgumentException()
                store.rollback(tranId)
                call.respond(HttpStatusCode.OK)
            }
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