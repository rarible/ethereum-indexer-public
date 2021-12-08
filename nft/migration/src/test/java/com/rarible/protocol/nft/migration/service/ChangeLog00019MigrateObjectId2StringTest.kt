package com.rarible.protocol.nft.migration.service

import com.rarible.core.common.Identifiable
import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.EventData
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import com.rarible.protocol.nft.migration.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.migration.integration.IntegrationTest
import com.rarible.protocol.nft.migration.mongock.mongo.ChangeLog00019MigrateObjectId2String
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.count
import org.springframework.data.mongodb.core.query.Query
import org.springframework.transaction.reactive.TransactionalOperator
import scalether.domain.Address
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom

@IntegrationTest
class ChangeLog00019MigrateObjectId2StringTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var template: ReactiveMongoTemplate

    @Autowired
    private lateinit var operator: TransactionalOperator

    private val changeLog = ChangeLog00019MigrateObjectId2String()

    @Test
    fun migrateIds() = runBlocking<Unit> {

        val legacyLogs = (0..10).map { createLogEvent() }
        legacyLogs.map {
            val saved = template.insert(it, NftItemHistoryRepository.COLLECTION).awaitSingle()
            assertThat(saved.id).isEqualTo(it.id)
        }

        changeLog.setStringId(template, operator)

        // check objectId -> string
        legacyLogs.forEach {
            val logEvent = template.findById(it.id.toString(), LogEvent::class.java, NftItemHistoryRepository.COLLECTION).awaitSingle()
            assertThat(logEvent.id).isEqualTo(it.id.toString())
        }

        val count = template.count<LogEvent>(Query(), NftItemHistoryRepository.COLLECTION).awaitSingle()
        assertThat(count).isEqualTo(legacyLogs.count().toLong())
    }

    private fun createLogEvent() = LegacyLogEvent(
        data = ItemTransfer(
            owner = createAddress(),
            from = createAddress(),
            token = createAddress(),
            tokenId = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 2)),
            date = nowMillis(),
            value = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 10000))
        ),
        address = createAddress(),
        topic = Word.apply(RandomUtils.nextBytes(32)),
        transactionHash = Word.apply(RandomUtils.nextBytes(32)),
        index = RandomUtils.nextInt(),
        minorLogIndex = 0,
        status = LogEventStatus.CONFIRMED
    )

    private fun createAddress(): Address {
        val bytes = ByteArray(20)
        ThreadLocalRandom.current().nextBytes(bytes)
        return Address.apply(bytes)
    }

    data class LegacyLogEvent(
        val data: EventData,
        val address: Address,
        val topic: Word,
        val transactionHash: Word,
        val status: LogEventStatus,
        val blockHash: Word? = null,
        val blockNumber: Long? = null,
        val logIndex: Int? = null,
        val minorLogIndex: Int,
        val index: Int,
        val visible: Boolean = true,

        @Id
        override val id: ObjectId = ObjectId.get(),
        @Version
        val version: Long? = null,

        val createdAt: Instant = Instant.EPOCH,
        val updatedAt: Instant = Instant.EPOCH
    ) : Identifiable<ObjectId>
}
