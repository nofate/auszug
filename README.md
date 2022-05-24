# Auszug: Xodus-backed storage service

## Running a server

### Build docker image:
```shell
docker build -t auszug-server .
```
### Edit users 
Users and their roles are configured with `run/application.conf`. Look for `ktor.security.users` property.  
Each user record is an object defined by 3 attributes:  
* `name`
* `role` which can be either `"read-write"` or `"read-only"`  
* `secret` containing base64-encoded sha256 hash of user's password. It can be produced by the following snippet:
  ```kotlin
   fun hash(password:String) = 
    MessageDigest.getInstance("SHA-256").digest("auszug${password.length}${password}".encodeToByteArray()).encodeBase64() 
  ```
For your convenience `application.conf` already contains two commented out user records:
* `gooduser / foobar / read-write`
* `baduser / barfoo / read-only`

Uncomment them or use as an example.   

Run container:
```shell
docker run -it --volume=$(pwd)/run:/run -w /run -p 8080:8080 auszug-server
```
Container expects you to mount `/run` volume, containing `application.conf` configuration file. This volume will be used as a working directory for running application.

## Using client library

Auszug client side consists of a single `StorageClient`. It has a very straightforward API.

1. First, initialize a client instance. Provide it API entry point, username and password:
    ```kotlin
    val c = StorageClient("http://localhost:8080", "gooduser", "foobar")
    ```
2. Start transaction. Depending on you user's role it can be read-write or read-only transaction.
    ```kotlin
    val tx = c.startTransaction()
    ```
3. Define a data class representing your data record. Annotate it with `@Serializable` annotation from [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization).
    ```kotlin
    @Serializable
    data class MyData(val name: String, val flag: Boolean, val value: Int)
    ```
4. Use transaction instance to `put()`, `get()` or `delete()` records in storage. All records are addressed by `store` (representing some namespace or collection) and unique `key`:
    ```kotlin
    val data = MyData("somestring", true, 100)
    tx.put("store1", "mykey", data)
    val result: MyData? = tx.get("store1", "mykey")
    tx.delete("store1", "mykey")
    ```
5. Use `commit()` or `rollback()` to finalize transaction.
   ```kotlin
   tx.commit()
    ```
   
Complete example:
```kotlin
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable

@Serializable
data class MyData(val name: String, val flag: Boolean, val value: Int)

fun main() = runBlocking {
    val c = StorageClient("http://localhost:8080", "gooduser", "foobar")
    c.startTransaction().let { tx ->
        val data = MyData("somestring", true, 100)
        tx.put("store1", "mykey", data)
        tx.commit()
    }

    c.startTransaction().let { tx ->
        val result: MyData? = tx.get("store1", "mykey")
        println(result)
    }
}
```