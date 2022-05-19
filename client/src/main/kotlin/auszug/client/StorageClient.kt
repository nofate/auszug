package auszug.client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
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
        val obj: T = response.body()
        return obj
    }

    suspend fun <T> delete(tranId: Long, store: String, key: String): Boolean {
        val response = client.delete("$URI/storage/$store/$key?tranId=$tranId")
        return true
    }
}


@Serializable
data class Foo(val someString: String, val someFlag: Boolean, val someNum: Int)


@Serializable
data class Bar(val bar: String)

fun main() = runBlocking {
    val client = StorageClient("http://localhost:8080", "", "")
    client.startTransaction().let { tranId ->
        client.put(tranId, "bar", "BAR", Bar("brrrrr"))
        client.commit(tranId)
    }

    client.startTransaction().let { tranId ->
        val bar: Bar? = client.get(tranId, "Bar", "bar")
        println(bar)
        client.commit(tranId)
    }
}