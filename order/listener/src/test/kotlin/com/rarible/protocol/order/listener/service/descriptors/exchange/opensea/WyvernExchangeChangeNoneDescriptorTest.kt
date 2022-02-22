package com.rarible.protocol.order.listener.service.descriptors.exchange.opensea

import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.repository.nonce.NonceHistoryRepository
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import com.rarible.protocol.order.listener.integration.IntegrationTest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
internal class WyvernExchangeChangeNoneDescriptorTest : AbstractOpenSeaV1Test() {
    @Autowired
    private lateinit var nonceHistoryRepository: NonceHistoryRepository

    @Autowired
    private lateinit var properties: OrderListenerProperties

    @Test
    fun `should convert change nonce event`() = runBlocking {
        properties.openSeaNonceIncrement = 0L

        exchangeV2.incrementNonce()
            .withSender(userSender1)
            .execute()
            .verifySuccess()

        Wait.waitAssert {
            val events = nonceHistoryRepository.findAll().toList()
            assertThat(events).hasSize(1)

            val data = events.single().data as ChangeNonceHistory
            assertThat(data.maker).isEqualTo(userSender1.from())
            assertThat(data.newNonce).isEqualTo(EthUInt256.ONE)
        }
    }

    @Test
    fun `should convert change nonce event with custom increment`() = runBlocking {
        properties.openSeaNonceIncrement = 10L

        exchangeV2.incrementNonce()
            .withSender(userSender1)
            .execute()
            .verifySuccess()

        Wait.waitAssert {
            val events = nonceHistoryRepository.findAll().toList()
            assertThat(events).hasSize(1)

            val data = events.single().data as ChangeNonceHistory
            assertThat(data.maker).isEqualTo(userSender1.from())
            assertThat(data.newNonce).isEqualTo(EthUInt256.of(11))
        }
    }
}