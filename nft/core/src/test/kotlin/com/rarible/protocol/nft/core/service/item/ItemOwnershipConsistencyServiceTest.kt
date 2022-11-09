package com.rarible.protocol.nft.core.service.item

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.data.createRandomItemId
import com.rarible.protocol.nft.core.data.createRandomOwnership
import com.rarible.protocol.nft.core.model.ItemProblemType
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import com.rarible.protocol.nft.core.service.item.ItemOwnershipConsistencyService.Companion.CURRENT_FIX_VERSION
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.reactor.flux
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@ExtendWith(MockKExtension::class)
class ItemOwnershipConsistencyServiceTest {

    @RelaxedMockK
    private lateinit var itemReduceService: ItemReduceService

    @RelaxedMockK
    private lateinit var ownershipRepository: OwnershipRepository

    @RelaxedMockK
    private lateinit var itemRepository: ItemRepository

    @InjectMockKs
    private lateinit var service: ItemOwnershipConsistencyService

    @Test
    fun `should check item - success`() = runBlocking<Unit> {
        // given
        val item = createRandomItem().copy(supply = EthUInt256.Companion.of(5))
        val ownership1 = createRandomOwnership().copy(value = EthUInt256.Companion.of(3))
        val ownership2 = createRandomOwnership().copy(value = EthUInt256.Companion.of(2))
        coEvery { ownershipRepository.search(any()) } returns listOf(ownership1, ownership2)

        // when
        val actual = service.checkItem(item)

        // then
        assertThat(actual).isExactlyInstanceOf(ItemOwnershipConsistencyService.CheckResult.Success::class.java)
        coVerify {
            ownershipRepository.search(any())
        }
        confirmVerified(ownershipRepository)
    }

    @Test
    fun `should check item - failure - supply mismatch`() = runBlocking<Unit> {
        // given
        val item = createRandomItem().copy(supply = EthUInt256.Companion.of(10))
        val ownership1 = createRandomOwnership().copy(value = EthUInt256.Companion.of(3))
        val ownership2 = createRandomOwnership().copy(value = EthUInt256.Companion.of(2))
        coEvery { ownershipRepository.search(any()) } returns listOf(ownership1, ownership2)

        // when
        val actual = service.checkItem(item)

        // then
        assertThat(actual).isExactlyInstanceOf(ItemOwnershipConsistencyService.CheckResult.Failure::class.java)
        actual as ItemOwnershipConsistencyService.CheckResult.Failure
        assertThat(actual.type).isEqualTo(ItemProblemType.SUPPLY_MISMATCH)
        assertThat(actual.supply).isEqualTo(item.supply)
        assertThat(actual.ownerships).isEqualTo(EthUInt256.of(5))
        coVerify {
            ownershipRepository.search(any())
        }
        confirmVerified(ownershipRepository)
    }

    @Test
    fun `should check item by id - success`() = runBlocking<Unit> {
        // given
        val item = createRandomItem().copy(supply = EthUInt256.Companion.of(5))
        val ownership1 = createRandomOwnership().copy(value = EthUInt256.Companion.of(3))
        val ownership2 = createRandomOwnership().copy(value = EthUInt256.Companion.of(2))
        coEvery { ownershipRepository.search(any()) } returns listOf(ownership1, ownership2)
        coEvery { itemRepository.findById(item.id) } returns item.toMono()

        // when
        val actual = service.checkItem(item.id)

        // then
        assertThat(actual).isExactlyInstanceOf(ItemOwnershipConsistencyService.CheckResult.Success::class.java)
        coVerify {
            itemRepository.findById(item.id)
            ownershipRepository.search(any())
        }
        confirmVerified(ownershipRepository, itemRepository)
    }

    @Test
    fun `should check item by id - failure - not found`() = runBlocking<Unit> {
        // given
        val item = createRandomItem()
        coEvery { itemRepository.findById(item.id) } returns Mono.empty()

        // when
        val actual = service.checkItem(item.id)

        // then
        assertThat(actual).isExactlyInstanceOf(ItemOwnershipConsistencyService.CheckResult.Failure::class.java)
        actual as ItemOwnershipConsistencyService.CheckResult.Failure
        assertThat(actual.type).isEqualTo(ItemProblemType.NOT_FOUND)
        coVerify {
            itemRepository.findById(item.id)
        }
        confirmVerified(ownershipRepository, itemRepository)
    }

    @Test
    fun `should fix item`() = runBlocking<Unit> {
        // given
        val item = createRandomItem()
        coEvery { itemReduceService.update(item.token, item.tokenId) } returns flux {  }
        coEvery { itemRepository.findById(item.id) } returns item.toMono()

        // when
        val actual = service.tryFix(item)

        // then
        assertThat(actual.itemId).isEqualTo(item.id)
        assertThat(actual.item).isEqualTo(item)
        assertThat(actual.fixVersion).isEqualTo(CURRENT_FIX_VERSION)
        coVerify {
            itemReduceService.update(item.token, item.tokenId)
            itemRepository.findById(item.id)
        }
        confirmVerified(itemReduceService, itemRepository, ownershipRepository)
    }

    @Test
    fun `should fix item by id`() = runBlocking<Unit> {
        // given
        val itemId = createRandomItemId()
        val item = createRandomItem()
        coEvery { itemReduceService.update(itemId.token, itemId.tokenId) } returns flux {  }
        coEvery { itemRepository.findById(itemId) } returns item.toMono()

        // when
        val actual = service.tryFix(itemId)

        // then
        assertThat(actual.itemId).isEqualTo(itemId)
        assertThat(actual.item).isEqualTo(item)
        assertThat(actual.fixVersion).isEqualTo(CURRENT_FIX_VERSION)
        coVerify {
            itemReduceService.update(itemId.token, itemId.tokenId)
            itemRepository.findById(itemId)
        }
        confirmVerified(itemReduceService, itemRepository, ownershipRepository)
    }

    @Test
    fun `should fix item with ownership deletion`() = runBlocking<Unit> {
        // given
        val item = createRandomItem()
        coEvery { itemReduceService.update(item.token, item.tokenId) } returns flux {  }
        coEvery { itemRepository.findById(item.id) } returns item.toMono()
        coEvery { ownershipRepository.deleteAllByItemId(item.id) } returns flux {  }

        // when
        val actual = service.tryFix(item, true)

        // then
        assertThat(actual.itemId).isEqualTo(item.id)
        assertThat(actual.item).isEqualTo(item)
        assertThat(actual.fixVersion).isEqualTo(CURRENT_FIX_VERSION)
        coVerify {
            ownershipRepository.deleteAllByItemId(item.id)
            itemReduceService.update(item.token, item.tokenId)
            itemRepository.findById(item.id)
        }
        confirmVerified(itemReduceService, itemRepository, ownershipRepository)
    }
}
