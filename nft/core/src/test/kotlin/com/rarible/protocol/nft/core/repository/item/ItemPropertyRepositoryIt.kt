package com.rarible.protocol.nft.core.repository.item

import com.ninjasquad.springmockk.MockkBean
import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.repository.data.createAddress
import io.mockk.clearMocks
import io.mockk.every
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigInteger
import java.time.Clock
import java.time.Duration
import java.util.*
import java.util.concurrent.ThreadLocalRandom

@FlowPreview
@IntegrationTest
internal class ItemPropertyRepositoryIt : AbstractIntegrationTest() {

    @MockkBean
    private lateinit var clock: Clock

    @Autowired
    private lateinit var itemPropertyRepository: ItemPropertyRepository

    @BeforeEach
    fun setup() {
        clearMocks(clock)
    }

    @Test
    fun `should save and get item properties`() = runBlocking<Unit> {
        val itemId =
            ItemId(createAddress(), EthUInt256.of(BigInteger.valueOf(ThreadLocalRandom.current().nextLong(1, 10000))))
        val properties = UUID.randomUUID().toString()

        every { clock.instant() } returns nowMillis() - Duration.ofDays(1)
        val saved = itemPropertyRepository.save(itemId, properties).awaitFirst()

        assertThat(saved).isTrue()

        every { clock.instant() } returns nowMillis()
        val savedProperties = itemPropertyRepository.get(itemId, Duration.ofDays(3)).awaitFirst()

        assertThat(savedProperties).isEqualTo(properties)
    }

    @Test
    fun `should save and get fresh item properties`() = runBlocking<Unit> {
        val itemId = ItemId(createAddress(), EthUInt256.of(BigInteger.valueOf(ThreadLocalRandom.current().nextLong(1, 10000))))
        val properties = UUID.randomUUID().toString()

        every { clock.instant() } returns nowMillis()

        val saved = itemPropertyRepository.save(itemId, properties).awaitFirst()
        assertThat(saved).isTrue()

        val savedProperties = itemPropertyRepository.get(itemId, Duration.ofDays(3)).awaitFirst()
        assertThat(savedProperties).isEqualTo(properties)
    }

    @Test
    fun `should save item properties many times`() = runBlocking<Unit> {
        val itemId = ItemId(createAddress(), EthUInt256.of(BigInteger.valueOf(ThreadLocalRandom.current().nextLong(1, 10000))))
        val properties1 = UUID.randomUUID().toString()
        val properties2 = UUID.randomUUID().toString()

        every { clock.instant() } returns nowMillis()
        val saved = itemPropertyRepository.save(itemId, properties1).awaitFirst() && itemPropertyRepository.save(
            itemId,
            properties2
        ).awaitFirst()

        assertThat(saved).isTrue()

        val savedProperties = itemPropertyRepository.get(itemId, Duration.ofDays(3)).awaitFirst()
        assertThat(savedProperties).isEqualTo(properties2)
    }

    @Test
    fun `should get null if item properties was expired`() = runBlocking<Unit> {
        val itemId = ItemId(createAddress(), EthUInt256.of(BigInteger.valueOf(ThreadLocalRandom.current().nextLong(1, 10000))))
        val properties = UUID.randomUUID().toString()

        every { clock.instant() } returns nowMillis() - Duration.ofDays(4)
        val saved = itemPropertyRepository.save(itemId, properties).awaitFirst()

        assertThat(saved).isTrue()

        every { clock.instant() } returns nowMillis()
        val savedProperties = itemPropertyRepository.get(itemId, Duration.ofDays(3)).awaitFirstOrNull()

        assertThat(savedProperties).isNull()
    }
}