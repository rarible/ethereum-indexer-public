package com.rarible.protocol.nft.api.e2e.pending

import com.rarible.contracts.test.erc721.TestERC721
import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.contracts.erc721.v3.MintableOwnableToken
import com.rarible.protocol.contracts.erc721.v4.rarible.MintableToken
import com.rarible.protocol.dto.CreateTransactionRequestDto
import com.rarible.protocol.dto.LogEventDto
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.api.misc.SignUtils
import com.rarible.protocol.nft.core.model.ContractStatus
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.ReduceVersion
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.model.toEth
import com.rarible.protocol.nft.core.repository.PendingLogItemPropertiesRepository
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.BlockProcessor
import com.rarible.protocol.nft.core.service.IpfsService
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesService
import com.rarible.protocol.nft.core.service.item.meta.descriptors.PendingLogItemPropertiesResolver
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.query.Query
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric
import scalether.domain.Address
import scalether.domain.response.Transaction
import scalether.domain.response.TransactionReceipt
import java.math.BigInteger

@End2EndTest
class PendingTransactionFt : SpringContainerBaseTest() {
    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var tokenRepository: TokenRepository

    @Autowired
    private lateinit var nftHistoryRepository: NftHistoryRepository

    @Autowired
    private lateinit var nftItemHistoryRepository: NftItemHistoryRepository

    @Autowired
    private lateinit var pendingLogItemPropertiesRepository: PendingLogItemPropertiesRepository

    @Autowired
    private lateinit var pendingLogItemPropertiesResolver: PendingLogItemPropertiesResolver

    @Autowired
    private lateinit var itemPropertiesService: ItemPropertiesService

    @Autowired
    private lateinit var blockProcessor: BlockProcessor

    @Autowired
    private lateinit var ipfsService: IpfsService

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun `pending minting`(version: ReduceVersion) = withReducer(version) {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        val address = Address.apply(Keys.getAddressFromPrivateKey(privateKey))

        val userSender = createSigningSender(privateKey)

        val token = MintableToken.deployAndWait(
            userSender,
            poller,
            "TEST",
            "TST",
            userSender.from(),
            "",
            ""
        ).block()!!
        val nonce = SignUtils.sign(privateKey, 0, token.address())
        tokenRepository.save(Token(token.address(), name = "TEST", standard = TokenStandard.ERC721)).awaitFirst()

        val itemProperties = ItemProperties(
            name = randomString(),
            description = randomString(),
            image = randomString(),
            imagePreview = randomString(),
            imageBig = randomString(),
            animationUrl = randomString(),
            attributes = listOf(ItemAttribute(randomString(), randomString(), randomString(), randomString())),
            rawJsonContent = null
        )
        val tokenId = EthUInt256(BigInteger.valueOf(nonce.value))
        val itemId = ItemId(token.address(), tokenId)

        coEvery { mockRariblePropertiesResolver.resolve(itemId) } returns itemProperties
        coEvery { mockRariblePropertiesResolver.resolveByTokenUri(itemId, any()) } returns itemProperties
        coEvery { mockItemPropertiesResolver.resolve(itemId) } coAnswers {
            // Delegate to the pendingLogItemPropertiesResolver
            pendingLogItemPropertiesResolver.resolve(itemId) ?:
            mockRariblePropertiesResolver.resolve(itemId)
        }
        val resolvedItemProperties = itemProperties.copy(
            image = ipfsService.resolveHttpUrl(itemProperties.image!!),
            imagePreview = ipfsService.resolveHttpUrl(itemProperties.imagePreview!!),
            imageBig = ipfsService.resolveHttpUrl(itemProperties.imageBig!!),
            animationUrl = ipfsService.resolveHttpUrl(itemProperties.animationUrl!!)
        )

        val receipt = token.mint(
            tokenId.value,
            nonce.v.toEth(),
            nonce.r.bytes(),
            nonce.s.bytes(),
            emptyArray(),
            "tokenUri"
        ).execute().verifySuccess()

        processTransaction(receipt)

        Wait.waitAssert {
            val item = itemRepository.search(Query()).first()

            assertThat(item.creators.single().account).isEqualTo(address)
            assertThat(item).hasFieldOrPropertyWithValue(Item::supply.name, EthUInt256.ZERO)

            when (version) {
                ReduceVersion.V1 -> {
                    assertThat(item.owners.single()).isEqualTo(address)
                    assertThat(item.pending).hasSize(1)
                }
                ReduceVersion.V2 -> {
                    assertThat(item.getPendingEvents()).hasSize(1)
                }
            }
            assertThat(itemPropertiesService.resolve(itemId)).isEqualToIgnoringGivenFields(
                resolvedItemProperties, ItemProperties::rawJsonContent.name
            )

            coVerify(exactly = 1) { mockItemPropertiesResolver.resolve(itemId) }
            val savedProperties = pendingLogItemPropertiesRepository.findById(itemId.decimalStringValue).awaitFirstOrNull()
            assertThat(savedProperties?.value).isEqualToIgnoringGivenFields(itemProperties, ItemProperties::rawJsonContent.name)
        }

        // Confirm the logs, run the item reducer.
        val pendingItem = itemRepository.findById(itemId).awaitFirstOrNull()
        when (version) {
            ReduceVersion.V1 -> {
                assertThat(pendingItem?.pending).hasSize(1)
            }
            ReduceVersion.V2 -> {
                assertThat(pendingItem?.getPendingEvents()).hasSize(1)
            }
        }
        val pendingItemDtp = nftItemApiClient.getNftItemById(itemId.decimalStringValue).awaitFirstOrNull()
        assertThat(pendingItemDtp?.pending).hasSize(1)

        val history = nftItemHistoryRepository
            .findItemsHistory(token = token.address(), tokenId = tokenId)
            .collectList().awaitFirst()

        assertThat(history).hasSize(1)
        val pendingLogEvent = history.single().log
        assertThat(pendingLogEvent.status).isEqualTo(LogEventStatus.PENDING)
        val confirmedLogEvent = pendingLogEvent.copy(status = LogEventStatus.CONFIRMED, blockNumber = 10, logIndex = 10)
        nftItemHistoryRepository.save(confirmedLogEvent).awaitFirst()
        blockProcessor.postProcessLogs(listOf(confirmedLogEvent)).awaitFirstOrNull()

        val confirmedItem = itemRepository.findById(ItemId(token.address(), tokenId)).awaitFirstOrNull()
        assertThat(confirmedItem?.pending).isEmpty()

        // Since the item is not pending anymore, the pending log item repository must not contain properties for it.
        Wait.waitAssert {
            // Wait while the 'itemPropertiesService' requests the pending log resolver again and this resolver invalidates the cache.
            assertThat(itemPropertiesService.resolve(itemId))
                .isEqualToIgnoringGivenFields(resolvedItemProperties, ItemProperties::rawJsonContent.name)
        }
    }

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun simpleMintAndTransfer(version: ReduceVersion) = withReducer(version) {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        val address = Address.apply(Keys.getAddressFromPrivateKey(privateKey))

        val sender = createSigningSender(privateKey)
        val contract = TestERC721.deployAndWait(sender, poller, "TEST", "TEST").awaitFirst()
        tokenRepository.save(Token(contract.address(), name = "TEST", standard = TokenStandard.ERC721)).awaitFirst()

        val receipt = contract.mint(address, BigInteger.ONE, "").execute().verifySuccess()
        processTransaction(receipt)

        Wait.waitAssert {
            val item = itemRepository.search(Query()).first()
            assertThat(item).hasFieldOrPropertyWithValue(Item::supply.name, EthUInt256.ZERO)

            when (version) {
                ReduceVersion.V1 -> {
                    assertThat(item.pending).hasSize(1)
                }
                ReduceVersion.V2 -> {
                    assertThat(item.getPendingEvents()).hasSize(1)
                }
            }
        }
    }

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun `should mintAndTransfer when minter == creator`(version: ReduceVersion) = withReducer(version) {
        val tx = CreateTransactionRequestDto(
            hash = Word.apply("0xf6bdeff6eb8aaddece60810dd6b71ad4c80ed0a735d49b305ee85a5351bf7fca"),
            from = Address.apply("0x19d2a55f2bd362a9e09f674b722782329f63f3fb"),
            nonce = 81,
            to = Address.apply("0x6ede7f3c26975aad32a475e1021d8f6f39c89d82"),
            input = Binary.apply(
                "0x22a775b6000000000000000000000000000000000000000000000000000000000000004000000000000000000000000019d2a55f2bd362a9e09f674b722782329f63f3fb19d2a55f2bd362a9e09f674b722782329f63f3fb00000000000000000000002e00000000000000000000000000000000000000000000000000000000000000a00000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000016000000000000000000000000000000000000000000000000000000000000001c0000000000000000000000000000000000000000000000000000000000000003a697066733a2f2f697066732f516d515774514567726a66506275646e61775a64777877465859644134616f534b34723156374731327662736636000000000000000000000000000000000000000000000000000000000000000000000000000100000000000000000000000019d2a55f2bd362a9e09f674b722782329f63f3fb0000000000000000000000000000000000000000000000000000000000002710000000000000000000000000000000000000000000000000000000000000000100000000000000000000000019d2a55f2bd362a9e09f674b722782329f63f3fb00000000000000000000000000000000000000000000000000000000000003e8000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000000"
            )
        )
        tokenRepository.save(
            Token(
                Address.apply("0x6ede7f3c26975aad32a475e1021d8f6f39c89d82"),
                name = "TEST",
                standard = TokenStandard.ERC721
            )
        ).awaitFirst()
        val eventLogs = nftTransactionApiClient.createNftPendingTransaction(tx).collectList().awaitFirst()
        assertEquals(1, eventLogs.size)
    }

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun `should mintAndTransfer when minter != creator`(version: ReduceVersion) = withReducer(version) {
        val tx = CreateTransactionRequestDto(
            hash = Word.apply("0xf6bdeff6eb8aaddece60810dd6b71ad4c80ed0a735d49b305ee85a5351bf7fca"),
            from = Address.apply("0xeb19d2a55f2bd362a9e09f674b722782329f63f3"),
            nonce = 81,
            to = Address.apply("0x6ede7f3c26975aad32a475e1021d8f6f39c89d82"),
            input = Binary.apply(
                "0x22a775b60000000000000000000000000000000000000000000000000000000000000040000000000000000000000000eb19d2a55f2bd362a9e09f674b722782329f63f3eb19d2a55f2bd362a9e09f674b722782329f63f300000000000000000000002e00000000000000000000000000000000000000000000000000000000000000a00000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000016000000000000000000000000000000000000000000000000000000000000001c0000000000000000000000000000000000000000000000000000000000000003a697066733a2f2f697066732f516d515774514567726a66506275646e61775a64777877465859644134616f534b34723156374731327662736636000000000000000000000000000000000000000000000000000000000000000000000000000100000000000000000000000019d2a55f2bd362a9e09f674b722782329f63f3fb0000000000000000000000000000000000000000000000000000000000002710000000000000000000000000000000000000000000000000000000000000000100000000000000000000000019d2a55f2bd362a9e09f674b722782329f63f3fb00000000000000000000000000000000000000000000000000000000000003e8000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000000"
            )
        )
        tokenRepository.save(
            Token(
                Address.apply("0x6ede7f3c26975aad32a475e1021d8f6f39c89d82"),
                name = "TEST",
                standard = TokenStandard.ERC721
            )
        ).awaitFirst()
        val eventLogs = nftTransactionApiClient.createNftPendingTransaction(tx).collectList().awaitFirst()
        assertEquals(2, eventLogs.size)
    }

    @Test
    fun createCollectionTransaction() = runBlocking<Unit> {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        val signer = Address.apply(RandomUtils.nextBytes(20))

        val userSender = createSigningSender(privateKey)
        val contractDeploy = MintableOwnableToken.deploy(
            userSender,
            "Test",
            "TST",
            "https://ipfs/",
            "https://ipfs/",
            signer
        )
        val receipt = poller.waitForTransaction(contractDeploy).awaitFirst()
        processTransaction(receipt, isCollection = true)

        Wait.waitAssert {
            val tokens = tokenRepository.findAll().collectList().awaitFirst()
            assertThat(tokens).hasSize(1)

            with(tokens.single()) {
                assertThat(id).isEqualTo(receipt.contractAddress())
                assertThat(name).isEqualTo("Test")
                assertThat(symbol).isEqualTo("TST")
                assertThat(status).isEqualTo(ContractStatus.PENDING)
            }
        }

        val history = nftHistoryRepository.findAllByCollection(receipt.contractAddress()).collectList().awaitFirst()
        assertThat(history).hasSize(1)
        assertThat(history.single().status).isEqualTo(LogEventStatus.PENDING)
    }

    private suspend fun processTransaction(receipt: TransactionReceipt, isCollection: Boolean = false) {
        val tx = ethereum.ethGetTransactionByHash(receipt.transactionHash()).awaitFirst().get()

        val eventLogs = nftTransactionApiClient.createNftPendingTransaction(tx.toRequest()).collectList().awaitFirst()

        assertThat(eventLogs).hasSize(1)

        with(eventLogs.single()) {
            assertThat(transactionHash).isEqualTo(tx.hash())
            assertThat(address).isEqualTo(if (isCollection) tx.creates() else tx.to())
            assertThat(status).isEqualTo(LogEventDto.Status.PENDING)
        }
    }

    private fun Transaction.toRequest() = CreateTransactionRequestDto(
        hash = hash(),
        from = from(),
        input = input(),
        nonce = nonce().toLong(),
        to = to()
    )
}
