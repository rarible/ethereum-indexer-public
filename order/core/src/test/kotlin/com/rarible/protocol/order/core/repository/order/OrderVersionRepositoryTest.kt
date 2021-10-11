package com.rarible.protocol.order.core.repository.order

import com.ninjasquad.springmockk.MockkBean
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomWord
import com.rarible.core.test.ext.MongoTest
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.configuration.RepositoryConfiguration
import com.rarible.protocol.order.core.data.createOrderVersion
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.LogEventKey
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.dao.DuplicateKeyException
import org.springframework.test.context.ContextConfiguration
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigDecimal

@MongoTest
@DataMongoTest
@ContextConfiguration(classes = [RepositoryConfiguration::class])
internal class OrderVersionRepositoryTest {

    @MockkBean
    private lateinit var publisher: ProtocolOrderPublisher

    @Autowired
    private lateinit var orderVersionRepository: OrderVersionRepository

    @BeforeEach
    fun beforeEach() = runBlocking {
        orderVersionRepository.createIndexes()
        orderVersionRepository.dropIndexes()
    }

    @Test
    fun `should sort version by price correctly`() = runBlocking<Unit> {
        val contract = AddressFactory.create()
        val tokenId = EthUInt256.TEN
        val take = Asset(Erc721AssetType(contract, tokenId), EthUInt256.ONE)

        val version1 = createOrderVersion()
            .copy(takePriceUsd = BigDecimal.valueOf(1), take = take)
        val version2 = createOrderVersion()
            .copy(takePriceUsd = null, take = take)
        val version3 = createOrderVersion()
            .copy(takePriceUsd = BigDecimal.valueOf(2), take = take)
        val version4 = createOrderVersion()
            .copy(takePriceUsd = null, take = take)

        save(version1, version2, version3, version4)

        val filter = PriceOrderVersionFilter.BidByItem(
            Address.apply(contract),
            tokenId,
            null,
            null,
            null,
            null,
            null,
            null,
            200,
            null
        )
        val versions = orderVersionRepository.search(filter).collectList().awaitFirst()
        assertThat(versions.size).isEqualTo(4)
        assertThat(versions[0]).isEqualTo(version3)
        assertThat(versions[1]).isEqualTo(version1)
    }

    @Test
    fun `exists and delete by onChainEventKey`() = runBlocking<Unit> {
        val key = createRandomKey()
        val version = createOrderVersion().copy(onChainOrderKey = key)
        orderVersionRepository.deleteByOnChainOrderKey(key).awaitFirstOrNull()
        assertFalse(orderVersionRepository.existsByOnChainOrderKey(key).awaitFirst())
        orderVersionRepository.save(version).awaitFirst()
        assertTrue(orderVersionRepository.existsByOnChainOrderKey(key).awaitFirst())
        orderVersionRepository.deleteByOnChainOrderKey(key).awaitFirstOrNull()
        assertFalse(orderVersionRepository.existsByOnChainOrderKey(key).awaitFirst())
    }

    @Test
    fun `throw DuplicateKeyException on duplicated onChainEventKey`() = runBlocking<Unit> {
        val key = createRandomKey()
        val version = createOrderVersion().copy(onChainOrderKey = key)
        val otherVersion = version.copy(
            onChainOrderKey = key, // Duplicated
            id = ObjectId() // New ID
        )
        orderVersionRepository.save(version).awaitFirst()
        assertThrows<DuplicateKeyException> { runBlocking { orderVersionRepository.save(otherVersion).awaitFirst() } }
    }

    private fun createRandomKey(): LogEventKey = LogEventKey(
        transactionHash = Word.apply(randomWord()),
        blockNumber = randomLong(),
        topic = Word.apply(randomWord()),
        index = randomInt(),
        minorLogIndex = randomInt(),
        contractAddress = randomAddress()
    )

    private suspend fun save(vararg order: OrderVersion) {
        order.forEach { orderVersionRepository.save(it).awaitFirst() }
    }
}
