package auszug.storage

import jetbrains.exodus.bindings.StringBinding
import jetbrains.exodus.env.Environments
import jetbrains.exodus.env.StoreConfig
import jetbrains.exodus.env.Transaction
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong


data class TransactionKey(val username: String, val transactionId: Long)

class XodusStorage(dataDir: String) {

    private val environment = Environments.newInstance(dataDir)
    private val transactions = ConcurrentHashMap<TransactionKey, Transaction>()
    private val idCounter = AtomicLong(0);

    fun startTransaction(username: String, readOnly: Boolean): Long {
        val transaction = if (readOnly) {
            environment.beginReadonlyTransaction()
        } else {
            environment.beginTransaction()
        }
        val tranId = idCounter.incrementAndGet();
        transactions[TransactionKey(username, tranId)] = transaction
        return tranId
    }

    fun commit(transactionKey: TransactionKey) {
        transactions.remove(transactionKey)?.commit()
    }

    fun rollback(transactionKey: TransactionKey) {
        transactions.remove(transactionKey)?.abort()
    }

    fun put(transactionKey: TransactionKey, storeName: String, key: String, value: String) {
        val trx = transactions[transactionKey]
        trx?.let {
            val store = environment.openStore(storeName, StoreConfig.WITHOUT_DUPLICATES, it)
            store.put(it, StringBinding.stringToEntry(key), StringBinding.stringToEntry(value))
        }
    }

    fun get(transactionKey: TransactionKey, storeName: String, key: String): String? {
        val trx = transactions[transactionKey]
        return trx?.let {
            val store = environment.openStore(storeName, StoreConfig.WITHOUT_DUPLICATES, it)
            return store.get(it, StringBinding.stringToEntry(key))?.let(StringBinding::entryToString)
        }
    }

    fun delete(transactionKey: TransactionKey, storeName: String, key: String): Boolean? {
        val trx = transactions[transactionKey]
        return trx?.let {
            val store = environment.openStore(storeName, StoreConfig.WITHOUT_DUPLICATES, it)
            return store.delete(it, StringBinding.stringToEntry(key))
        }
    }

    fun shutdown() {
        environment.close()
    }
}