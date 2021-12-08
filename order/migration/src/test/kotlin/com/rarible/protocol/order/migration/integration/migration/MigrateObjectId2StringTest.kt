package com.rarible.protocol.order.migration.integration.migration

import com.rarible.core.common.Identifiable
import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.EventData
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.migration.integration.AbstractMigrationTest
import com.rarible.protocol.order.migration.integration.IntegrationTest
import com.rarible.protocol.order.migration.mongock.mongo.ChangeLog00015MigrateObjectId2String
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions
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
import scalether.domain.AddressFactory
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom

@IntegrationTest
class MigrateObjectId2StringTest : AbstractMigrationTest() {

    val migration = ChangeLog00015MigrateObjectId2String()

    @Autowired
    lateinit var template: ReactiveMongoTemplate

    @Autowired
    private lateinit var operator: TransactionalOperator

    @Test
    fun `should migrate exchange history`() = runBlocking<Unit> {
        val legacyLogs = (0..10).map { createLogEvent() }
        legacyLogs.map {
            val saved = template.insert(it, ExchangeHistoryRepository.COLLECTION).awaitSingle()
            Assertions.assertThat(saved.id).isEqualTo(it.id)
        }

        migration.exchangeHistory(template, operator)

        // check objectId -> string
        legacyLogs.forEach {
            val logEvent = template.findById(it.id.toString(), LogEvent::class.java, ExchangeHistoryRepository.COLLECTION).awaitSingle()
            Assertions.assertThat(logEvent.id).isEqualTo(it.id.toString())
        }

        val count = template.count<LogEvent>(Query(), ExchangeHistoryRepository.COLLECTION).awaitSingle()
        Assertions.assertThat(count).isEqualTo(legacyLogs.count().toLong())
    }

    @Test
    fun `should migrate order versions`() = runBlocking<Unit> {
        val legacyOrderVersions = (0..10).map { createErc721BidOrderVersion() }
        legacyOrderVersions.map {
            val saved = template.insert(it, OrderVersionRepository.COLLECTION).awaitSingle()
            Assertions.assertThat(saved.id).isEqualTo(it.id)
        }

        migration.orderVersion(template, operator)

        // check objectId -> string
        legacyOrderVersions.forEach {
            val logEvent = template.findById(it.id.toString(), OrderVersion::class.java, OrderVersionRepository.COLLECTION).awaitSingle()
            Assertions.assertThat(logEvent.id).isEqualTo(it.id.toString())
        }

        val count = template.count<OrderVersion>(Query(), OrderVersionRepository.COLLECTION).awaitSingle()
        Assertions.assertThat(count).isEqualTo(legacyOrderVersions.count().toLong())
    }

    private fun createLogEvent() = LegacyLogEvent(
        data = OrderSideMatch(
            hash = Word.apply(RandomUtils.nextBytes(32)),
            counterHash = Word.apply(RandomUtils.nextBytes(32)),
            side = OrderSide.LEFT,
            fill = EthUInt256.ONE,
            make = createErc721Asset(),
            take = createEthAsset(),
            maker = createAddress(),
            taker = createAddress(),
            makeUsd = null,
            takeUsd = null,
            makePriceUsd = null,
            takePriceUsd = null,
            makeValue = null,
            takeValue = null,
            source = HistorySource.RARIBLE
        ),
        address = createAddress(),
        topic = Word.apply(RandomUtils.nextBytes(32)),
        transactionHash = Word.apply(RandomUtils.nextBytes(32)),
        index = RandomUtils.nextInt(),
        minorLogIndex = 0,
        status = LogEventStatus.CONFIRMED
    )

    private fun createEthAsset() = Asset(
        EthAssetType,
        EthUInt256.of((1L..1000L).random())
    )

    private fun createErc721Asset() = Asset(
        Erc721AssetType(
            token = AddressFactory.create(),
            tokenId = EthUInt256.of((1L..100L).random())
        ),
        EthUInt256.ONE
    )

    private fun createAddress(): Address {
        val bytes = ByteArray(20)
        ThreadLocalRandom.current().nextBytes(bytes)
        return Address.apply(bytes)
    }

    private fun createErc721BidOrderVersion(): LegacyOrderVersion {
        val make = createEthAsset()
        val take = createErc721Asset()
        return createOrderVersion(make, take)
    }

    private fun createOrderVersion(make: Asset, take: Asset) = LegacyOrderVersion(
        hash = Word.apply(RandomUtils.nextBytes(32)),
        maker = createAddress(),
        taker = createAddress(),
        makePriceUsd = (1..100).random().toBigDecimal(),
        takePriceUsd = (1..100).random().toBigDecimal(),
        makePrice = (1..100).random().toBigDecimal(),
        takePrice = (1..100).random().toBigDecimal(),
        makeUsd = (1..100).random().toBigDecimal(),
        takeUsd = (1..100).random().toBigDecimal(),
        make = make,
        take = take,
        platform = Platform.RARIBLE,
        type = OrderType.RARIBLE_V2,
        salt = EthUInt256.TEN,
        start = null,
        end = null,
        data = OrderRaribleV2DataV1(emptyList(), emptyList()),
        signature = null
    )

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

    data class LegacyOrderVersion(
        val maker: Address,
        val taker: Address?,
        val make: Asset,
        val take: Asset,
        val makePriceUsd: BigDecimal?,
        val takePriceUsd: BigDecimal?,
        val makePrice: BigDecimal?,
        val takePrice: BigDecimal?,
        val makeUsd: BigDecimal?,
        val takeUsd: BigDecimal?,
        @Id
        val id: ObjectId = ObjectId.get(),
        val onChainOrderKey: LogEventKey? = null,
        val createdAt: Instant = nowMillis(),
        val platform: Platform = Platform.RARIBLE,
        val type: OrderType,
        val salt: EthUInt256,
        val start: Long?,
        val end: Long?,
        val data: OrderData,
        val signature: Binary?,

        val hash: Word = Order.hashKey(maker, make.type, take.type, salt.value, data)
    )
}
