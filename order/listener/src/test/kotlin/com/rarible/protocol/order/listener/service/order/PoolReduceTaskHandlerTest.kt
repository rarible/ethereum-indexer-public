package com.rarible.protocol.order.listener.service.order

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.order.core.data.randomSellOnChainAmmOrder
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class PoolReduceTaskHandlerTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var handler: PoolReduceTaskHandler

    @Test
    fun `should reduce pool order`() = runBlocking<Unit> {
        val poolCreate = randomSellOnChainAmmOrder()
        val logEvent = ReversedEthereumLogRecord(
            id = ObjectId().toHexString(),
            data = poolCreate,
            address = randomAddress(),
            topic = Word.apply(RandomUtils.nextBytes(32)),
            transactionHash = randomWord(),
            index = RandomUtils.nextInt(),
            minorLogIndex = 0,
            status = EthereumBlockStatus.CONFIRMED
        )
        poolHistoryRepository.save(logEvent).awaitFirst()
        val hash = handler.runLongTask(from = null, param = "").toList().map { Word.apply(it) }.single()
        assertThat(hash).isEqualTo(poolCreate.hash)
        val order = orderRepository.findById(hash)
        assertThat(order).isNotNull
    }
}
