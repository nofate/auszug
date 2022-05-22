package auszug.web

import auszug.auth.UserRoleAuth
import auszug.auth.parseUsers
import auszug.storage.XodusStorage
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.serialization.Serializable


val store = XodusStorage("/home/nofate/work/private/auszug/run/");

@Serializable
data class TransactionResponse(val tranId: Long)

fun Route.storageRouting() {
    route("/v1") {

        route("/storage") {
            get("/{storeName}/{id}") {
                val key = call.parameters["id"] ?: throw IllegalArgumentException()
                val storeName = call.parameters["storeName"] ?: throw IllegalArgumentException()
                val tranId = call.request.queryParameters["tranId"]?.toLong() ?: throw IllegalArgumentException()

                val value = store.get(tranId, storeName, key)
                if (value != null) {
                    call.respondText(value, ContentType.Application.Json)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            post("/{storeName}/{id}") {
                val key = call.parameters["id"] ?: throw IllegalArgumentException()
                val storeName = call.parameters["storeName"] ?: throw IllegalArgumentException()
                val tranId = call.request.queryParameters["tranId"]?.toLong() ?: throw IllegalArgumentException()
                val body = call.receiveText()
                store.put(tranId, key, storeName, body)
                call.respond(HttpStatusCode.Created)
            }

            delete("/{storeName}/{id}") {
                val key = call.parameters["id"] ?: throw IllegalArgumentException()
                val storeName = call.parameters["storeName"] ?: throw IllegalArgumentException()
                val tranId = call.request.queryParameters["tranId"]?.toLong() ?: throw IllegalArgumentException()

                store.delete(tranId, storeName, key)
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
        authenticate("auth-basic") {
            storageRouting()
        }
    }
}


fun Application.storageModule() {
    val userRoleAuth = UserRoleAuth(
        getDigestFunction("SHA-256") { "auszug${it.length}" },
        environment.config.parseUsers()
    )

    configureRouting()

    install(ContentNegotiation) {
        json()
    }

    install(Authentication) {
        basic("auth-basic") {
            realm = "Storage API"
            validate { creds ->
                userRoleAuth.authenticate(creds)
            }
        }
    }
}