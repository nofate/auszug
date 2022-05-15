package auszug.storage

import jetbrains.exodus.ByteIterable
import jetbrains.exodus.bindings.ByteBinding
import jetbrains.exodus.bindings.StringBinding
import jetbrains.exodus.env.Environment
import jetbrains.exodus.env.Environments
import jetbrains.exodus.env.StoreConfig
import jetbrains.exodus.env.Transaction
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class XodusStorage(dataDir: String) {

    private val environment = Environments.newInstance(dataDir)
    //todo: ключ транзакции: user + id
    private val transactions = ConcurrentHashMap<Long, Transaction>()
    private val idCounter = AtomicLong(0);

    fun startTransaction(): Long {
        val transaction = environment.beginTransaction()
        val tranId = idCounter.incrementAndGet();
        transactions[tranId] = transaction
        return tranId
    }

    fun commit(tranId: Long) {
        transactions.remove(tranId)?.commit()
    }

    fun rollback(tranId: Long) {
        transactions.remove(tranId)?.abort()
    }

    fun put(tranId: Long, key: String, value: String) {
        val trx = transactions[tranId]
        trx?.let {
            val store = environment.openStore("store", StoreConfig.WITHOUT_DUPLICATES, it)
            store.put(it, StringBinding.stringToEntry(key), StringBinding.stringToEntry(value))
        }
    }

    fun get(tranId: Long, key: String): String? {
        val trx = transactions[tranId]
        return trx?.let {
            val store = environment.openStore("store", StoreConfig.WITHOUT_DUPLICATES, it)
            return store.get(it, StringBinding.stringToEntry(key))?.let(StringBinding::entryToString)
        }
    }

    fun delete(tranId: Long, key: String): Boolean? {
        val trx = transactions[tranId]
        return trx?.let {
            val store = environment.openStore("store", StoreConfig.WITHOUT_DUPLICATES, it)
            return store.delete(it, StringBinding.stringToEntry(key))
        }
    }

    fun shutdown() {
        environment.close()
    }
}


fun main() {
    val xodus = XodusStorage("/home/nofate/work/private/auszug/run/")

    xodus.startTransaction().let { tid ->
        xodus.put(tid, "foo", "some foo")
        xodus.rollback(tid)
    }

    xodus.startTransaction().let { tid ->
        val res = xodus.get(tid, "foo")
        println(res)
        xodus.commit(tid)
    }

    xodus.startTransaction().let { tid ->
        val res = xodus.delete(tid, "foo")
        println(res)
        xodus.commit(tid)
    }

    xodus.startTransaction().let { tid ->
        val res = xodus.get(tid, "foo")
        println(res)
        xodus.commit(tid)
    }
}