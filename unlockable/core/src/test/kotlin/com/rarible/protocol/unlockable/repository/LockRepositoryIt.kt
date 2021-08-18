package com.rarible.protocol.unlockable.repository

import com.rarible.core.common.nowMillis
import com.rarible.core.test.ext.MongoTest
import com.rarible.protocol.unlockable.configuration.CoreConfiguration
import com.rarible.protocol.unlockable.configuration.ItConfiguration
import com.rarible.protocol.unlockable.domain.Lock
import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.runBlocking
import org.bson.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import scalether.domain.Address
import java.util.*

@MongoTest
@EnableAutoConfiguration
@SpringBootTest(classes = [CoreConfiguration::class, ItConfiguration::class])
class LockRepositoryIt {

    @Autowired
    lateinit var lockRepository: LockRepository

    @Autowired
    lateinit var mongo: ReactiveMongoTemplate

    @Test
    fun `test lock raw format`() {
        val lock = Lock(
            "itemId",
            "content",
            Address.THREE(),
            Binary.apply(ByteArray(4) { it.toByte() }),
            nowMillis().minusSeconds(120),
            UUID.randomUUID().toString(),
            null
        )

        val saved = runBlocking { lockRepository.save(lock) }
        val document = mongo.findById(
            saved.id,
            Document::class.java,
            mongo.getCollectionName(Lock::class.java)
        ).block()

        assertEquals(lock.itemId, document.getString(Lock::itemId.name))
        assertEquals(lock.content, document.getString(Lock::content.name))
        assertEquals(lock.author, Address.apply(document.getString(Lock::author.name)))
        assertEquals(lock.signature, Binary.apply(document.getString(Lock::signature.name)))
        assertEquals(lock.unlockDate?.toEpochMilli(), document.getDate(Lock::unlockDate.name).time)
        assertEquals(lock.id, document.getString("_id"))
    }

}
