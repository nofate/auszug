package auszug.client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable


@Serializable
data class TransactionResponse(val tranId: Long)

class StorageClient(baseUri: String, username: String, password: String) {

    val API_PATH = "/v1"
    val URI = "${baseUri}${API_PATH}"

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
        install(Auth) {
            basic {
                credentials {
                    BasicAuthCredentials(username = username, password = password)
                }
                sendWithoutRequest { true }
                realm = "Storage API"
            }
        }
    }

    suspend fun startTransaction(): Long {
        val response = client.post("$URI/transaction")
        val tran: TransactionResponse = response.body()
        return tran.tranId
    }

    suspend fun commit(tranId: Long) {
        val response = client.post("$URI/transaction/$tranId/commit")
    }

    suspend fun rollback(tranId: Long) {
        val response = client.post("$URI/transaction/$tranId/rollback")
    }

    suspend inline fun <reified T> put(tranId: Long, store: String, key: String, value: T) {
        val response = client.post("$URI/storage/$store/$key?tranId=$tranId") {
            contentType(ContentType.Application.Json)
            setBody(value)
        }
    }

    suspend inline fun <reified T> get(tranId: Long, store: String, key: String): T? {
        val response = client.get("$URI/storage/$store/$key?tranId=$tranId") {
            accept(ContentType.Application.Json)
        }

        return if (response.status == HttpStatusCode.OK) {
            val obj: T = response.body()
            obj
        } else {
            null
        }
    }

    suspend fun <T> delete(tranId: Long, store: String, key: String): Boolean {
        val response = client.delete("$URI/storage/$store/$key?tranId=$tranId")
        return true
    }
}