package auszug.storage

import jetbrains.exodus.ArrayByteIterable
import jetbrains.exodus.bindings.StringBinding
import jetbrains.exodus.env.*
import jetbrains.exodus.io.inMemory.Memory
import jetbrains.exodus.io.inMemory.MemoryDataReader
import jetbrains.exodus.io.inMemory.MemoryDataWriter
import jetbrains.exodus.log.LogConfig
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong


data class TransactionKey(val username: String, val transactionId: Long)

class XodusStorage(inMemory: Boolean, dataDir: String?) {

    val log = LoggerFactory.getLogger(XodusStorage::class.java)
    private val environment = createEnvironment(inMemory, dataDir)
    private val transactions = ConcurrentHashMap<TransactionKey, Transaction>()
    private val idCounter = AtomicLong(0)

    fun startTransaction(username: String, readOnly: Boolean): Long {
        val tranId = idCounter.incrementAndGet()
        log.debug("Starting transaction {} for user '{}', readonly={}", tranId, username, readOnly)
        val transaction = if (readOnly) {
            environment.beginReadonlyTransaction()
        } else {
            environment.beginTransaction()
        }
        transactions[TransactionKey(username, tranId)] = transaction
        return tranId
    }

    fun commit(transactionKey: TransactionKey) {
        log.debug("Committing transaction {} for user '{}'", transactionKey.transactionId, transactionKey.username)
        transactions.remove(transactionKey)?.commit()
    }

    fun rollback(transactionKey: TransactionKey) {
        log.debug("Rolling back transaction {} for user '{}'", transactionKey.transactionId, transactionKey.username)
        transactions.remove(transactionKey)?.abort()
    }

    fun put(transactionKey: TransactionKey, storeName: String, key: String, value: ByteArray) {
        log.debug("Put {} bytes to {}:{}", value.size, storeName, key)
        val trx = transactions[transactionKey]
        trx?.let {
            val store = environment.openStore(storeName, StoreConfig.WITHOUT_DUPLICATES, it)
            store.put(it, StringBinding.stringToEntry(key), ArrayByteIterable(value))
        }
    }

    fun get(transactionKey: TransactionKey, storeName: String, key: String): ByteArray? {
        log.debug("Get from {}:{}", storeName, key)
        val trx = transactions[transactionKey]
        return trx?.let {
            val store = environment.openStore(storeName, StoreConfig.WITHOUT_DUPLICATES, it)
            return store.get(it, StringBinding.stringToEntry(key))?.let { byteIterable ->
                val result = byteIterable.bytesUnsafe
                log.debug("Found {}:{}, {}", storeName, key, result.size)
                return result
            }
        }
    }

    fun delete(transactionKey: TransactionKey, storeName: String, key: String): Boolean {
        log.debug("Delete {}:{}", storeName, key)
        val trx = transactions[transactionKey]
        return trx?.let {
            val store = environment.openStore(storeName, StoreConfig.WITHOUT_DUPLICATES, it)
            return store.delete(it, StringBinding.stringToEntry(key))
        } ?: false
    }

    fun shutdown() {
        environment.close()
    }

    private fun createEnvironment(inMemory: Boolean, dataDir: String?): Environment {
        return if (inMemory) {
            val memory = Memory()
            val environmentConfig = EnvironmentConfig()
            environmentConfig.gcUtilizationFromScratch = true
            Environments.newInstance(
                LogConfig.create(MemoryDataReader(memory), MemoryDataWriter(memory)),
                environmentConfig
            )
        } else {
            if (dataDir == null) throw IllegalArgumentException("dataDir cannot be null")
            Environments.newInstance(dataDir)
        }
    }
}