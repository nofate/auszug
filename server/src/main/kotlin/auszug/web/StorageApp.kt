package auszug.web

import auszug.auth.Role
import auszug.auth.UserRoleAuth
import auszug.auth.UserRolePrincipal
import auszug.auth.parseUsers
import auszug.storage.TransactionKey
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


fun createTransactionKey(call: ApplicationCall): TransactionKey {
    val tranId = call.request.queryParameters["tranId"]?.toLong() ?: throw IllegalArgumentException()
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

fun Route.storageRouting() {
    route("/v1") {

        route("/storage") {
            get("/{storeName}/{id}") {
                val key = call.parameters["id"] ?: throw IllegalArgumentException()
                val storeName = call.parameters["storeName"] ?: throw IllegalArgumentException()
                val value = store.get(createTransactionKey(call), storeName, key)
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
                store.put(createTransactionKey(call), key, storeName, body)
                call.respond(HttpStatusCode.Created)
            }

            delete("/{storeName}/{id}") {
                val key = call.parameters["id"] ?: throw IllegalArgumentException()
                val storeName = call.parameters["storeName"] ?: throw IllegalArgumentException()
                val tranId = call.request.queryParameters["tranId"]?.toLong() ?: throw IllegalArgumentException()

                store.delete(createTransactionKey(call), storeName, key)
                call.respond(HttpStatusCode.OK)
            }
        }

        route("/transaction") {
            post() {
                val (username, role) = ensurePrincipal(call)
                val tranId = store.startTransaction(username, role == Role.READ_ONLY)
                call.respond(HttpStatusCode.Created, TransactionResponse(tranId))
            }

            post("/{id}/commit") {
                store.commit(createTransactionKey(call))
                call.respond(HttpStatusCode.OK)
            }

            post("/{id}/rollback") {
                store.rollback(createTransactionKey(call))
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