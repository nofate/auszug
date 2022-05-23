package auszug.client

import auzug.common.API_AUTH_REALM
import auzug.common.API_PATH
import auzug.common.FailureResponse
import auzug.common.TransactionResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.cbor.*
import io.ktor.serialization.kotlinx.json.*


class StorageClient(baseUri: String, val client: HttpClient) {

    val API_URI = "${baseUri}${API_PATH}"

    constructor(baseUri: String, username: String, password: String)
            : this(baseUri, HttpClient(CIO) {
        followRedirects = true

        install(ContentNegotiation) {
            cbor()
            json()
        }
        install(Auth) {
            basic {
                credentials {
                    BasicAuthCredentials(username = username, password = password)
                }
                sendWithoutRequest { true }
                realm = API_AUTH_REALM
            }
        }
    })


    suspend fun startTransaction(): ClientTransaction {
        val response = client.post("$API_URI/transaction")
        val tran: TransactionResponse = response.body()
        return ClientTransaction(tran.tranId, tran.readOnly)
    }

    inner class ClientTransaction(val tranId: Long, val readOnly: Boolean) {

        suspend fun commit() {
            val response = client.post("$API_URI/transaction/$tranId/commit")
        }

        suspend fun rollback() {
            val response = client.post("$API_URI/transaction/$tranId/rollback")
        }

        suspend inline fun <reified T> put(store: String, key: String, value: T) {
            if (readOnly) throw IllegalStateException("Transaction is read-only")

            val response = client.put("$API_URI/transaction/$tranId/storage/$store/$key") {
                contentType(ContentType.Application.Cbor)
                setBody(value)
            }
            if (response.status == HttpStatusCode.BadRequest) {
                val failureResponse = response.body<FailureResponse>()
                throw IllegalStateException(failureResponse.message)
            }
        }

        suspend inline fun <reified T> get(store: String, key: String): T? {
            val response = client.get("$API_URI/transaction/$tranId/storage/$store/$key") {
                accept(ContentType.Application.Cbor)
            }

            if (response.status == HttpStatusCode.OK) {
                val obj: T = response.body()
                return obj
            } else if (response.status == HttpStatusCode.NotFound) {
                return null
            } else throw IllegalStateException()
        }

        suspend fun delete(store: String, key: String): Boolean {
            if (readOnly) throw IllegalStateException("Transaction is read-only")

            val response = client.delete("$API_URI/transaction/$tranId/storage/$store/$key")
            if (response.status == HttpStatusCode.OK) {
                return true
            } else if (response.status == HttpStatusCode.NotFound) {
                return false
            } else throw IllegalStateException()
        }
    }
}