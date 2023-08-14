package com.rarible.protocol.nft.listener.service.descriptors.erc721

import com.rarible.core.test.wait.Wait
import com.rarible.protocol.contracts.erc721.rarible.ERC721Rarible
import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftCollectionUpdateEventDto
import com.rarible.protocol.nft.core.model.CollectionOwnershipTransferred
import com.rarible.protocol.nft.core.model.CreateCollection
import com.rarible.protocol.nft.core.test.TestKafkaHandler
import com.rarible.protocol.nft.listener.test.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.test.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address

@IntegrationTest
class CollectionOwnershipTransferDescriptorIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var testCollectionHandler: TestKafkaHandler<NftCollectionEventDto>

    @Test
    fun `on minting the ownership is transferred from the zero address`() = runBlocking<Unit> {
        val (creatorAddress, creatorSender) = newSender()

        val token = ERC721Rarible.deployAndWait(creatorSender, poller).awaitFirst()
        token.__ERC721Rarible_init("Test", "TEST", "BASE", "URI")
            .execute().verifySuccess()

        Wait.waitAssert {
            val histories = nftHistoryRepository.findAllByCollection(token.address()).collectList().awaitFirst()
            assertThat(histories).hasSize(2)
            // Ownership of the contract is set first, then "CreateERC721Rarible" event is emitted.
            val (ownershipLog, createLog) = histories
            assertEquals(
                CollectionOwnershipTransferred(token.address(), Address.ZERO(), creatorAddress),
                ownershipLog.data as CollectionOwnershipTransferred
            )
            assertThat(createLog.data).isInstanceOf(CreateCollection::class.java)
        }

        Wait.waitAssert {
            val collectionEvents = testCollectionHandler.events
            assertThat(collectionEvents).anyMatch {
                it is NftCollectionUpdateEventDto && it.collection == NftCollectionDto(
                    id = token.address(),
                    type = NftCollectionDto.Type.ERC721,
                    owner = creatorAddress,
                    status = NftCollectionDto.Status.CONFIRMED,
                    name = "Test",
                    symbol = "TEST",
                    features = it.collection.features,
                    supportsLazyMint = it.collection.supportsLazyMint,
                    minters = listOf(creatorAddress),
                    isRaribleContract = true,
                )
            }
        }
    }

    @Test
    fun `ownership transferred`() = runBlocking<Unit> {
        val (creatorAddress, creatorSender) = newSender()

        val token = ERC721Rarible.deployAndWait(creatorSender, poller).awaitFirst()
        token.__ERC721Rarible_init("Test", "TEST", "BASE", "URI").execute().verifySuccess()

        val (newOwnerAddress, _) = newSender()
        token.transferOwnership(newOwnerAddress).withSender(creatorSender).execute().verifySuccess()

        Wait.waitAssert {
            val histories = nftHistoryRepository.findAllByCollection(token.address()).collectList().awaitFirst()
            assertThat(histories).hasSize(3)
            val data = histories.last().data
            assertThat(data).isInstanceOf(CollectionOwnershipTransferred::class.java)
            assertEquals(
                CollectionOwnershipTransferred(token.address(), creatorAddress, newOwnerAddress),
                data as CollectionOwnershipTransferred
            )
        }
        Wait.waitAssert {
            val collectionEvents = testCollectionHandler.events
            assertThat(collectionEvents).anyMatch {
                it is NftCollectionUpdateEventDto && it.collection == NftCollectionDto(
                    id = token.address(),
                    type = NftCollectionDto.Type.ERC721,
                    owner = newOwnerAddress,
                    status = NftCollectionDto.Status.CONFIRMED,
                    name = "Test",
                    symbol = "TEST",
                    features = it.collection.features,
                    supportsLazyMint = it.collection.supportsLazyMint,
                    minters = listOf(newOwnerAddress),
                    isRaribleContract = true,
                )
            }
        }
    }
}
