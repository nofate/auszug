package auszug.client

import auzug.common.API_AUTH_REALM
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.cbor.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

internal class StorageAppKtTest {

    @Serializable
    data class Foo(val name: String, val flag: Boolean, val value: Int)

    @Test
    fun testUnauthenticatedAccess() = testApplication {
        val response = client.post("/v1/transaction")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testPutGetDeleteGet() = testApplication {
        val client = getClient()
        val store = "Foo"
        val key = "foo1"
        val testedRecord = Foo("foo", true, 1)
        val storageClient = StorageClient("", client)

        // Uncommitted put
        storageClient.startTransaction().let { tx ->
            tx.put(store, key, testedRecord)
        }

        storageClient.startTransaction().let { tx ->
            val result: Foo? = tx.get(store, key)
            tx.commit()
            assertNull(result)
        }

        // Committed put
        storageClient.startTransaction().let { tx ->
            tx.put(store, key, testedRecord)
            tx.commit()
        }

        storageClient.startTransaction().let { tx ->
            val result: Foo? = tx.get(store, key)
            tx.commit()
            assertEquals(testedRecord, result)
        }

        // Uncommitted delete
        storageClient.startTransaction().let { tx ->
            val result = tx.delete(store, key)
            assertTrue(result)
        }

        storageClient.startTransaction().let { tx ->
            val result: Foo? = tx.get(store, key)
            tx.commit()
            assertEquals(testedRecord, result)
        }

        // Committed delete
        storageClient.startTransaction().let { tx ->
            val result = tx.delete(store, key)
            tx.commit()
            assertTrue(result)
        }

        storageClient.startTransaction().let { tx ->
            val result: Foo? = tx.get(store, key)
            tx.commit()
            assertNull(result)
        }
    }

    @Test
    fun testRollback() = testApplication {
        val client = getClient()
        val store = "Foo"
        val key = "foo2"
        val testedRecord = Foo("foo2", false, 2)
        val storageClient = StorageClient("", client)
        storageClient.startTransaction().let { tx ->
            tx.put(store, key, testedRecord)
            val result: Foo? = tx.get(store, key)
            assertEquals(testedRecord, result)
            tx.rollback()
        }

        storageClient.startTransaction().let { tx ->
            val result: Foo? = tx.get(store, key)
            assertNull(result)
            tx.commit()
        }
    }


    @Test
    fun testGetNonExisting() = testApplication {
        val client = getClient()
        val store = "Bar"
        val key = "foo2"
        val storageClient = StorageClient("", client)

        storageClient.startTransaction().let { tx ->
            val result: Foo? = tx.get(store, key)
            tx.commit()
            assertNull(result)
        }
    }

    @Test
    fun testDeleteNonExisting() = testApplication {
        val client = getClient()
        val store = "Bar"
        val key = "foo2"
        val storageClient = StorageClient("", client)

        storageClient.startTransaction().let { tx ->
            val result = tx.delete(store, key)
            tx.commit()
            assertFalse(result)
        }
    }

    @Test
    fun testPutByReadonlyUser() = testApplication {
        val client = getClient(readOnly = true)
        val store = "Foo"
        val key = "foo1"
        val testedRecord = Foo("foo", true, 1)
        val storageClient = StorageClient("", client)

        assertFailsWith<IllegalStateException> {
            storageClient.startTransaction().let { tx ->
                tx.put(store, key, testedRecord)
                tx.commit()
            }
        }
    }

    @Test
    fun testDeleteByReadonlyUser() = testApplication {
        val client = getClient(readOnly = true)
        val store = "Foo"
        val key = "foo1"
        val storageClient = StorageClient("", client)

        assertFailsWith<IllegalStateException> {
            storageClient.startTransaction().let { tx ->
                tx.delete(store, key)
                tx.commit()
            }
        }
    }


    private fun ApplicationTestBuilder.getClient(readOnly: Boolean = false) = createClient {
        install(ContentNegotiation) {
            json()
            cbor()
        }
        install(Auth) {
            basic {
                credentials {
                    if (readOnly) {
                        BasicAuthCredentials(username = "testuserRO", password = "barfoo")
                    } else {
                        BasicAuthCredentials(username = "testuserRW", password = "foobar")
                    }
                }
                sendWithoutRequest { true }
                realm = API_AUTH_REALM
            }
        }
    }
}