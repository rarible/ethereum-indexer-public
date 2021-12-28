package com.rarible.protocol.nft.listener.service.descriptors.creators

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.erc721.fakeCreator.FakeCreatorERC721
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.integration.IntegrationTest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.math.BigInteger

/**
 * Tests for RPN-106: make sure the Item "creators" is set only for true creators.
 *
 * We use the following fake contract that imitates a malicious ERC721 token
 * whose goal is to trick our indexer to consider the "creators" be a famous address.
 *
 * ```
 *   contract FakeCreatorERC721 is ERC721Upgradeable {
 *     function mintDirect_without_CreatorsEvent(address to, uint tokenId) external {
 *         _mint(to, tokenId);
 *     }
 *   }
 *```
 */
@FlowPreview
@IntegrationTest
class ItemCreatorMinterValidationIt : AbstractIntegrationTest() {

    private val tokenId = EthUInt256.of(BigInteger.TEN)

    @BeforeEach
    fun enableFlag() {
        nftIndexerProperties.featureFlags.validateCreatorByTransactionSender = true
    }

    @AfterEach
    fun disableFlag() {
        nftIndexerProperties.featureFlags.validateCreatorByTransactionSender = false
    }

    /**
     * Make sure we do not set creator to an arbitrary address if the mint transaction was sent by another user.
     */
    @Test
    fun `should leave creators empty if the item was minted by random user`() = runBlocking<Unit> {
        val (_, deployerSender) = newSender()
        val fakeCreatorERC721 = FakeCreatorERC721.deployAndWait(deployerSender, poller).awaitFirst()
        val token = fakeCreatorERC721.address()
        val (_, mintSender) = newSender()
        val famousAddress = randomAddress()
        val receipt = fakeCreatorERC721.mintDirect_without_CreatorsEvent(famousAddress, tokenId.value)
            .withSender(mintSender)
            .execute().verifySuccess()
        val mintInstant = receipt.getTimestamp()
        Wait.waitAssert {
            assertThat(nftItemHistoryRepository.findAllItemsHistory().asFlow().toList()).anySatisfy { (_, logEvent) ->
                val data = logEvent.data
                assertThat(data).isEqualTo(
                    ItemTransfer(
                        owner = famousAddress,
                        token = token,
                        tokenId = tokenId,
                        date = mintInstant,
                        from = Address.ZERO(),
                        value = EthUInt256.ONE
                    )
                )
            }
        }

        Wait.waitAssert {
            val item = itemRepository.findById(ItemId(token, tokenId)).awaitFirstOrNull()
            assertThat(item).isNotNull; item!!
            assertThat(item.creators).isEmpty()
        }
    }
}
