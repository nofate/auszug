package auszug.auth

import io.ktor.server.auth.*
import io.ktor.server.config.*
import io.ktor.util.*


enum class Role(val value: String) {
    READ_WRITE("read-write"),
    READ_ONLY("read-only");

    companion object {
        private val roles = values().associateBy(Role::value)
        operator fun get(value: String): Role = roles[value] ?: throw IllegalArgumentException("Unknown user role: $value")
    }
}

data class UserRolePrincipal(val name: String, val role: Role) : Principal


class UserRoleAuth(val digester: (String) -> ByteArray, val users: Map<String, Pair<ByteArray, Role>>)  {

    fun authenticate(credential: UserPasswordCredential): UserRolePrincipal? {
        return users[credential.name]?.let { (secretHash, role) ->
            if (digester(credential.password).contentEquals(secretHash)) {
                return UserRolePrincipal(credential.name, role)
            } else {
                return null
            }
        }
    }
}

fun ApplicationConfig.parseUsers(): Map<String, Pair<ByteArray, Role>> =
    configList("ktor.security.users")
        .map {
            val username = it.property("name").getString()
            val secret = it.property("secret").getString().decodeBase64Bytes()
            val role = Role[it.property("role").getString()]
            return@map (username to Pair(secret, role))
        }
        .toMap()