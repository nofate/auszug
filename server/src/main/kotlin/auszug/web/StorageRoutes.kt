package auszug.web

import auszug.auth.Role
import auszug.auth.UserRolePrincipal
import auszug.storage.TransactionKey
import auszug.storage.XodusStorage
import auzug.common.API_PATH
import auzug.common.TransactionResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable


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
    route(API_PATH) {

        route("/transaction") {
            post() {
                val (username, role) = ensurePrincipal(call)
                val readOnly = role == Role.READ_ONLY
                val tranId = xodusStorage.startTransaction(username, readOnly)
                call.respond(HttpStatusCode.Created, TransactionResponse(tranId, readOnly))
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
                            call.respondBytes(value, ContentType.Application.Cbor)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }

                    put("/{storeName}/{id}") {
                        val key = call.parameters["id"] ?: throw IllegalArgumentException()
                        val storeName = call.parameters["storeName"] ?: throw IllegalArgumentException()
                        val body = call.receive<ByteArray>()
                        xodusStorage.put(createTransactionKey(call), storeName, key, body)
                        call.respond(HttpStatusCode.OK)
                    }

                    delete("/{storeName}/{id}") {
                        val key = call.parameters["id"] ?: throw IllegalArgumentException()
                        val storeName = call.parameters["storeName"] ?: throw IllegalArgumentException()
                        if (xodusStorage.delete(createTransactionKey(call), storeName, key)) {
                            call.respond(HttpStatusCode.OK)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }
                }
            }
        }
    }
}
