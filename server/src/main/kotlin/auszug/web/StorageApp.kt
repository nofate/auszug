package auszug.web

import auszug.auth.UserRoleAuth
import auszug.auth.parseUsers
import auszug.storage.XodusStorage
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.util.*


fun Application.storageModule() {
    val userRoleAuth = UserRoleAuth(
        getDigestFunction("SHA-256") { "auszug${it.length}" },
        environment.config.parseUsers()
    )

    val xodusStorage = XodusStorage(environment.config.property("storage.path").getString())

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

    routing {
        authenticate("auth-basic") {
            storageRouting(xodusStorage)
        }
    }

    environment.monitor.subscribe(ApplicationStopped) {
       xodusStorage.shutdown()
    }
}