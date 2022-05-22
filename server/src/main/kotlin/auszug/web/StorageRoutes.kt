package auszug.web

import auszug.auth.Role
import auszug.auth.UserRolePrincipal
import auszug.storage.TransactionKey
import auszug.storage.XodusStorage
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable


@Serializable
data class TransactionResponse(val tranId: Long)


fun createTransactionKey(call: ApplicationCall): TransactionKey {
    val tranId = call.parameters["tranId"]?.toLong() ?: throw IllegalArgumentException()
    val username = ensurePrincipal(call).name
    return TransactionKey(username, tranId)
}

fun ensurePrincipal(call: ApplicationCall): UserRolePrincipal {
    val principal = call.principal<UserRolePrincipal>()
    if (principal != null) {
        return principal
    } else {
        throw IllegalStateException()
    }
}

fun Route.storageRouting(xodusStorage: XodusStorage) {
    route("/v1") {

        route("/transaction") {
            post() {
                val (username, role) = ensurePrincipal(call)
                val tranId = xodusStorage.startTransaction(username, role == Role.READ_ONLY)
                call.respond(HttpStatusCode.Created, TransactionResponse(tranId))
            }

            route("/{tranId}") {

                post("/commit") {
                    xodusStorage.commit(createTransactionKey(call))
                    call.respond(HttpStatusCode.OK)
                }

                post("/rollback") {
                    xodusStorage.rollback(createTransactionKey(call))
                    call.respond(HttpStatusCode.OK)
                }

                route("/storage") {
                    get("/{storeName}/{id}") {
                        val key = call.parameters["id"] ?: throw IllegalArgumentException()
                        val storeName = call.parameters["storeName"] ?: throw IllegalArgumentException()
                        val value = xodusStorage.get(createTransactionKey(call), storeName, key)
                        if (value != null) {
                            call.respondText(value, ContentType.Application.Json)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }

                    post("/{storeName}/{id}") {
                        val key = call.parameters["id"] ?: throw IllegalArgumentException()
                        val storeName = call.parameters["storeName"] ?: throw IllegalArgumentException()
                        val body = call.receiveText()
                        xodusStorage.put(createTransactionKey(call), key, storeName, body)
                        call.respond(HttpStatusCode.Created)
                    }

                    delete("/{storeName}/{id}") {
                        val key = call.parameters["id"] ?: throw IllegalArgumentException()
                        val storeName = call.parameters["storeName"] ?: throw IllegalArgumentException()
                        xodusStorage.delete(createTransactionKey(call), storeName, key)
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }
    }
}
