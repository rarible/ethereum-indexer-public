package com.rarible.protocol.order.migration.integration.migration

import com.rarible.blockchain.scanner.ethereum.model.EthereumLogStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.data.createOrderRaribleV2DataV1
import com.rarible.protocol.order.core.data.createOrderSideMatch
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.Part
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.migration.integration.AbstractMigrationTest
import com.rarible.protocol.order.migration.integration.IntegrationTest
import com.rarible.protocol.order.migration.mongock.mongo.ChangeLog00019AddOriginFeesToSideMatch
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

@IntegrationTest
internal class ChangeLog00019AddOriginFeesToSideMatchTest : AbstractMigrationTest() {

    val migration = ChangeLog00019AddOriginFeesToSideMatch()

    @Autowired
    lateinit var exchangeHistoryRepository: ExchangeHistoryRepository

    @Autowired
    lateinit var template: ReactiveMongoTemplate

    @Test
    fun `should set prices for orders`() = runBlocking<Unit> {
        val data1 = createOrderRaribleV2DataV1()
        val historyWithData1 = save(
            createOrderSideMatch().copy(data = data1, originFees = null)
        )

        val data2 = createOrderRaribleV2DataV1()
        val historyWithData2 = save(
            createOrderSideMatch().copy(data = data2, originFees = null)
        )

        val historyWithNoData = save(
            createOrderSideMatch().copy(data = null, originFees = null)
        )

        val originFees = listOf(Part(randomAddress(), EthUInt256.TEN))
        val historyWithOriginFees = save(
            createOrderSideMatch().copy(data = null, originFees = originFees)
        )
        migration.addOriginFeesToSideMatch(template)

        val savedHistoryWithData1 = exchangeHistoryRepository.findById(ObjectId(historyWithData1.id)).awaitFirst()
        assertThat((savedHistoryWithData1.data as OrderSideMatch).originFees).isEqualTo(data1.originFees)

        val savedHistoryWithData2 = exchangeHistoryRepository.findById(ObjectId(historyWithData2.id)).awaitFirst()
        assertThat((savedHistoryWithData2.data as OrderSideMatch).originFees).isEqualTo(data2.originFees)

        val savedHistoryWithNoData = exchangeHistoryRepository.findById(ObjectId(historyWithNoData.id)).awaitFirst()
        assertThat((savedHistoryWithNoData.data as OrderSideMatch).originFees).isEmpty()

        val savedHistoryWithOriginFees = exchangeHistoryRepository.findById(ObjectId(historyWithOriginFees.id)).awaitFirst()
        assertThat((savedHistoryWithOriginFees.data as OrderSideMatch).originFees).isEqualTo(originFees)
    }

    suspend fun save(orderSideMatch: OrderSideMatch): ReversedEthereumLogRecord {
        return exchangeHistoryRepository.save(
            ReversedEthereumLogRecord(
                id = ObjectId().toHexString(),
                data = orderSideMatch,
                address = randomAddress(),
                topic = Word.apply(ByteArray(32)),
                transactionHash = randomWord(),
                status = EthereumLogStatus.CONFIRMED,
                index = 0,
                logIndex = 0,
                minorLogIndex = 0
            )
        ).awaitFirst()
    }
}
