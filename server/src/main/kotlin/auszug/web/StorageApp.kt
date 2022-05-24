package auszug.web

import auszug.auth.UserRoleAuth
import auszug.auth.parseUsers
import auszug.storage.XodusStorage
import auzug.common.API_AUTH_REALM
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlin.io.path.Path


private const val PROP_STORAGE_INMEMORY = "storage.inMemory"
private const val STORAGE_PATH = "data"
private const val SALT_SUFFIX = "auszug"


fun Application.storageModule() {
    val userRoleAuth = UserRoleAuth(
        getDigestFunction("SHA-256") { "$SALT_SUFFIX${it.length}" },
        environment.config.parseUsers()
    )

    val xodusStorage = XodusStorage(
        environment.config.property(PROP_STORAGE_INMEMORY).getString().toBooleanStrict(),
        Path(System.getProperty("user.dir"), STORAGE_PATH).toString()
    )

    install(ContentNegotiation) {
        json()
    }
    install(Authentication) {
        basic("auth-basic") {
            realm = API_AUTH_REALM
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