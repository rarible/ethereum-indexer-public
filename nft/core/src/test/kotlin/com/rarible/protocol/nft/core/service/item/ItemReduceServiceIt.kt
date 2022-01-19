package com.rarible.protocol.nft.core.service.item

import com.rarible.core.common.nowMillis
import com.rarible.core.content.meta.loader.ContentMeta
import com.rarible.core.test.data.randomString
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.dto.NftItemDeleteEventDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.nft.core.converters.dto.NftItemMetaDtoConverter
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemContentMeta
import com.rarible.protocol.nft.core.model.ItemCreators
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemLazyMint
import com.rarible.protocol.nft.core.model.ItemMeta
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.ItemRoyalty
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.core.model.ReduceVersion
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository.Companion.COLLECTION
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import io.daonomic.rpc.domain.WordFactory
import io.mockk.coEvery
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.time.Instant
import java.util.stream.Stream

@IntegrationTest
class ItemReduceServiceIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var historyService: ItemReduceService

    @Autowired
    private lateinit var ownershipRepository: OwnershipRepository

    @BeforeEach
    fun setUpMeta() {
        coEvery { mockItemMetaResolver.resolveItemMeta(any()) } returns itemMeta
    }

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun mintItem(version: ReduceVersion) = withReducer(version) {
        val owner = AddressFactory.create()
        val token = AddressFactory.create()
        val tokenId = EthUInt256.ONE
        val blockTimestamp = Instant.ofEpochSecond(12)

        saveToken(
            Token(token, name = "TEST", standard = TokenStandard.ERC721)
        )
        val transfer = ItemTransfer(
            owner = owner,
            token = token,
            tokenId = tokenId,
            date = nowMillis(),
            from = Address.ZERO(),
            value = EthUInt256.ONE
        )
        saveItemHistory(transfer, logIndex = 0, from = owner, blockTimestamp = blockTimestamp)

        historyService.update(token, tokenId).awaitFirstOrNull()

        val item = itemRepository.findById(ItemId(token, tokenId)).awaitFirst()

        when (version) {
            ReduceVersion.V1 -> assertThat(item.mintedAt).isEqualTo(transfer.date)
            ReduceVersion.V2 -> assertThat(item.mintedAt).isEqualTo(blockTimestamp)
        }

        assertThat(item.creators).isEqualTo(listOf(Part.fullPart(owner)))
        assertThat(item.supply).isEqualTo(EthUInt256.ONE)

        checkItemEventWasPublished(
            token,
            tokenId,
            NftItemMetaDtoConverter.convert(itemMeta),
            pendingSize = 0,
            NftItemUpdateEventDto::class.java
        )
    }

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun `should get creator from tokenId for OpenSea tokenId`(version: ReduceVersion) = withReducer(version)  {
        val token = Address.apply("0x495f947276749ce646f68ac8c248420045cb7b5e")

        val owner = AddressFactory.create()
        // https://opensea.io/assets/0x495f947276749ce646f68ac8c248420045cb7b5e/43635738831738903259797022654371755363838740687517624872331458295230642520065
        // https://ethereum-api.rarible.org/v0.1/nft/items/0x495f947276749ce646f68ac8c248420045cb7b5e:43635738831738903259797022654371755363838740687517624872331458295230642520065
        val tokenId = EthUInt256.of("43635738831738903259797022654371755363838740687517624872331458295230642520065")
        val creator = Address.apply("0x6078f3f4a50eec358790bdfae15b351647e9cbb4")

        saveToken(
            Token(token, name = "TEST", standard = TokenStandard.ERC721)
        )

        val transfer = ItemTransfer(
            owner = owner,
            token = token,
            tokenId = tokenId,
            date = nowMillis(),
            from = Address.ZERO(),
            value = EthUInt256.ONE
        )
        saveItemHistory(transfer)

        historyService.update(token, tokenId).awaitFirstOrNull()

        val item = itemRepository.findById(ItemId(token, tokenId)).awaitFirst()

        assertThat(item.creators).isEqualTo(listOf(Part.fullPart(creator)))
        assertThat(item.supply).isEqualTo(EthUInt256.ONE)

        checkItemEventWasPublished(
            token,
            tokenId,
            NftItemMetaDtoConverter.convert(itemMeta),
            pendingSize = 0,
            NftItemUpdateEventDto::class.java
        )
    }

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun mintItemViaPending(version: ReduceVersion) = withReducer(version)  {
        val token = AddressFactory.create()
        val owner = AddressFactory.create()
        val tokenId = EthUInt256.ONE

        saveToken(
            Token(token, name = "TEST", standard = TokenStandard.ERC721)
        )
        val transfer = saveItemHistory(
            ItemTransfer(
                owner = owner,
                token = token,
                tokenId = tokenId,
                date = nowMillis(),
                from = Address.ZERO(),
                value = EthUInt256.ONE
            ),
            status = LogEventStatus.PENDING
        )

        historyService.update(token, tokenId).awaitFirstOrNull()
        checkItem(token = token, tokenId = tokenId, expSupply = EthUInt256.ZERO)
        checkOwnership(owner = owner, token = token, tokenId = tokenId, expValue = EthUInt256.ZERO, expLazyValue = EthUInt256.ZERO)

        checkItemEventWasPublished(
            token,
            tokenId,
            NftItemMetaDtoConverter.convert(itemMeta),
            pendingSize = 1,
            NftItemUpdateEventDto::class.java
        )
        checkOwnershipEventWasPublished(token, tokenId, owner, NftOwnershipUpdateEventDto::class.java)

        val pendingMint = nftItemHistoryRepository.findAllItemsHistory().collectList().awaitFirst().single()
        mongo.remove(Query(Criteria("_id").isEqualTo(pendingMint.log.id)), LogEvent::class.java, COLLECTION).awaitFirst()
        assertThat(nftItemHistoryRepository.findAllItemsHistory().collectList().awaitFirst()).isEmpty()

        saveItemHistory(
            ItemTransfer(
                owner = owner,
                token = token,
                tokenId = tokenId,
                date = nowMillis(),
                from = Address.ZERO(),
                value = EthUInt256.ONE
            ),
            status = LogEventStatus.CONFIRMED,
            logIndex = 1
        )

        historyService.update(token, tokenId).then().awaitFirstOrNull()
        checkItem(token = token, tokenId = tokenId, expSupply = EthUInt256.ONE)
        checkOwnership(owner = owner, token = token, tokenId = tokenId, expValue = EthUInt256.ONE, expLazyValue = EthUInt256.ZERO)

        checkItemEventWasPublished(
            token,
            tokenId,
            NftItemMetaDtoConverter.convert(itemMeta),
            pendingSize = 0,
            NftItemUpdateEventDto::class.java
        )
        checkOwnershipEventWasPublished(token, tokenId, owner, NftOwnershipUpdateEventDto::class.java)
    }

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun deleteErrorEntities(version: ReduceVersion) = withReducer(version) {
        val token = AddressFactory.create()
        val owner = AddressFactory.create()
        val tokenId = EthUInt256.ONE

        saveToken(
            Token(token, name = "TEST", standard = TokenStandard.ERC721)
        )
        val transfer = ItemTransfer(
            owner = owner,
            token = token,
            tokenId = tokenId,
            date = nowMillis(),
            from = Address.ZERO(),
            value = EthUInt256.ONE
        )
        val logEvent = LogEvent(
            data = transfer,
            address = AddressFactory.create(),
            topic = WordFactory.create(),
            transactionHash = WordFactory.create(),
            status = LogEventStatus.DROPPED,
            blockNumber = 1,
            logIndex = null,
            minorLogIndex = 0,
            index = 0
        )
        nftItemHistoryRepository.save(logEvent).awaitFirst()

        val id = OwnershipId(token, tokenId, owner)

        ownershipRepository.save(
            Ownership(
                token = token,
                tokenId = tokenId,
                owner = owner,
                value = EthUInt256.ZERO,
                lazyValue = EthUInt256.ZERO,
                date = nowMillis(),
                creators = listOf(Part(AddressFactory.create(), 1000)),
                pending = emptyList(),
                revertableEvents = emptyList()
            )
        ).awaitFirst()

        historyService.update(token, tokenId).awaitFirstOrNull()

        val ownership = ownershipRepository.findById(id).awaitFirstOrNull()
        if (ownership != null) {
            assertThat(ownership.deleted).isTrue()
        }
        checkItem(token = token, tokenId = tokenId, expSupply = EthUInt256.ZERO, deleted = true)

        checkItemEventWasPublished(
            token,
            tokenId,
            NftItemMetaDtoConverter.convert(itemMeta),
            pendingSize = 0,
            NftItemDeleteEventDto::class.java
        )
        checkOwnershipEventWasPublished(token, tokenId, owner, NftOwnershipDeleteEventDto::class.java)
    }

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun transferToSelf(version: ReduceVersion) = withReducer(version) {
        val token = AddressFactory.create()
        val tokenId = EthUInt256.ONE
        val owner = AddressFactory.create()

        saveToken(
            Token(token, name = "TEST", standard = TokenStandard.ERC1155)
        )
        val transfer = ItemTransfer(
            owner = owner,
            token = token,
            tokenId = tokenId,
            date = nowMillis(),
            from = Address.ZERO(),
            value = EthUInt256.TEN
        )
        val transfer2 = ItemTransfer(
            owner = owner,
            token = token,
            tokenId = tokenId,
            date = nowMillis(),
            from = owner,
            value = EthUInt256.ONE
        )
        saveItemHistory(transfer, token, logIndex = 0)
        saveItemHistory(transfer2, token, logIndex = 1)

        historyService.update(token, tokenId).awaitFirstOrNull()
        val ownership = ownershipRepository.findById(OwnershipId(token, tokenId, owner)).awaitFirst()
        assertThat(ownership.value).isEqualTo(EthUInt256.TEN)
        checkItem(token = token, tokenId = tokenId, expSupply = EthUInt256.TEN)
    }

    @ParameterizedTest
    @MethodSource("invalidLogEventStatus")
    fun deleteItemAfterLogEventChangeStatusFromPendingToInvalidStatus(
        invalidStatus: LogEventStatus,
        version: ReduceVersion
    ) = withReducer(version) {
        val token = AddressFactory.create()
        val owner = AddressFactory.create()
        val tokenId = EthUInt256.ONE

        saveToken(
            Token(token, name = "TEST", standard = TokenStandard.ERC721)
        )

        val transfer = ItemTransfer(
            owner = owner,
            token = token,
            tokenId = tokenId,
            date = nowMillis(),
            from = Address.ZERO(),
            value = EthUInt256.ONE
        )
        val logEvent = LogEvent(
            data = transfer,
            address = AddressFactory.create(),
            topic = WordFactory.create(),
            transactionHash = WordFactory.create(),
            status = LogEventStatus.PENDING,
            index = 0,
            minorLogIndex = 0
        )
        val savedLogEvent = nftItemHistoryRepository.save(logEvent).awaitFirst()
        historyService.update(token, tokenId).awaitFirstOrNull()

        val newItem = itemRepository.findById(ItemId(token, tokenId)).awaitFirst()
        assertThat(newItem.deleted).isFalse()

        nftItemHistoryRepository.save(savedLogEvent.copy(status = invalidStatus)).awaitFirst()
        historyService.update(token, tokenId).awaitFirstOrNull()

        val updatedItem = itemRepository.findById(ItemId(token, tokenId)).awaitFirst()
        assertThat(updatedItem.deleted).isTrue()
    }

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun burnItem(version: ReduceVersion) = withReducer(version) {
        val token = AddressFactory.create()
        val tokenId = EthUInt256.ONE
        val owner = AddressFactory.create()

        saveToken(
            Token(token, name = "TEST", standard = TokenStandard.ERC721)
        )
        val transfer = ItemTransfer(
            owner = owner,
            token = token,
            tokenId = tokenId,
            date = nowMillis(),
            from = Address.ZERO(),
            value = EthUInt256.of(3)
        )
        saveItemHistory(transfer, logIndex = 1)

        val transfer2 = ItemTransfer(
            owner = Address.ZERO(),
            token = token,
            tokenId = tokenId,
            date = nowMillis(),
            from = owner,
            value = EthUInt256.of(3)
        )
        saveItemHistory(transfer2, logIndex = 2)
        ownershipRepository.save(Ownership(
            token = token, tokenId = tokenId, owner = owner, value = EthUInt256.ONE, date = Instant.now(), pending = emptyList()
        )).awaitFirst()

        historyService.update(token, tokenId).awaitFirstOrNull()
        val item = itemRepository.findById(ItemId(token, tokenId)).awaitFirst()
        assertThat(item.supply).isEqualTo(EthUInt256.ZERO)
        assertThat(item.deleted).isEqualTo(true)

        checkItemEventWasPublished(
            token,
            tokenId,
            NftItemMetaDtoConverter.convert(itemMeta),
            pendingSize = 0,
            NftItemDeleteEventDto::class.java
        )
        checkOwnershipEventWasPublished(token, tokenId, owner, NftOwnershipDeleteEventDto::class.java)
    }

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun pendingItemTransfer(version: ReduceVersion) = withReducer(version) {
        val from = AddressFactory.create()
        val token = AddressFactory.create()
        val tokenId = EthUInt256.ONE
        val owner = AddressFactory.create()

        saveToken(
            Token(token, name = "TEST", standard = TokenStandard.ERC721)
        )
        val mint = ItemTransfer(
            owner = from,
            token = token,
            tokenId = tokenId,
            date = nowMillis(),
            from = Address.ZERO(),
            value = EthUInt256.ONE
        )
        val transfer = ItemTransfer(
            owner = owner,
            token = token,
            tokenId = tokenId,
            date = nowMillis(),
            from = AddressFactory.create(),
            value = EthUInt256.ZERO
        )
        saveItemHistory(mint, token = token, status = LogEventStatus.CONFIRMED, logIndex = 0, from = from)
        saveItemHistory(transfer, token = token, status = LogEventStatus.PENDING, logIndex = 1)

        historyService.update(token, tokenId).then().awaitFirstOrNull()

        val item = itemRepository.findById(ItemId(token, tokenId)).awaitFirst()
        assertThat(item.creators).isEqualTo(listOf(Part.fullPart(from)))
        assertThat(item.supply).isEqualTo(EthUInt256.ONE)
        checkOwnership(owner = owner, token = token, tokenId = tokenId, expValue = EthUInt256.ZERO, expLazyValue = EthUInt256.ZERO)
    }

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun confirmedItemTransfer(version: ReduceVersion) = withReducer(version) {
        val token = AddressFactory.create()
        val tokenId = EthUInt256.ONE

        saveToken(
            Token(token, name = "TEST", standard = TokenStandard.ERC721)
        )
        val transfer = ItemTransfer(
            owner = AddressFactory.create(),
            token = token,
            tokenId = tokenId,
            date = nowMillis(),
            from = Address.ZERO(),
            value = EthUInt256.of(2)
        )
        saveItemHistory(transfer, logIndex = 1)

        val owner = AddressFactory.create()
        val transfer2 = ItemTransfer(
            owner = owner,
            token = token,
            tokenId = tokenId,
            date = nowMillis(),
            from = Address.ZERO(),
            value = EthUInt256.of(3)
        )
        saveItemHistory(transfer2, logIndex = 2)

        historyService.update(token, tokenId).awaitFirstOrNull()
        checkItem(token = token, tokenId = tokenId, expSupply = EthUInt256.of(5))
    }

    @Test
    @Disabled
    fun confirmedItemRoyalty() = withReducer(ReduceVersion.V1) {
        val token = AddressFactory.create()
        val tokenId = EthUInt256.ONE
        val owner = AddressFactory.create()

        saveToken(
            Token(token, name = "TEST", standard = TokenStandard.ERC1155)
        )

        val transfer = ItemTransfer(owner, token, tokenId, nowMillis(), Address.ZERO(), EthUInt256.TEN)
        saveItemHistory(transfer, token)

        val royalty =
            ItemRoyalty(token = token, tokenId = tokenId, date = nowMillis(), royalties = listOf(Part(owner, 2)))
        saveItemHistory(royalty, token)

        historyService.update(token, tokenId).then().block()
        val item = itemRepository.findById(ItemId(token, tokenId)).awaitFirst()
        assertThat(item.royalties).isEqualTo(listOf(Part(owner, 2)))
    }

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun confirmedItemMint(version: ReduceVersion) = withReducer(version) {
        val token = AddressFactory.create()
        val minter = AddressFactory.create()
        val tokenId = EthUInt256.ONE

        saveToken(
            Token(token, name = "TEST", standard = TokenStandard.ERC1155)
        )

        val transfer = ItemTransfer(minter, token, tokenId, nowMillis(), Address.ZERO(), EthUInt256.TEN)
        saveItemHistory(transfer, token, logIndex = 1)

        val creatorsList = listOf(Part(AddressFactory.create(), 1), Part(AddressFactory.create(), 2))
        val creators = ItemCreators(token, tokenId, nowMillis(), creatorsList)
        saveItemHistory(creators, token, logIndex = 2)

        historyService.update(token, tokenId).then().block()

        val item = itemRepository.findById(ItemId(token, tokenId)).awaitFirst()
        assertThat(item.creators).isEqualTo(creatorsList)
    }

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun `update ownership for ERC1155`(version: ReduceVersion) = withReducer(version) {
        val token = AddressFactory.create()
        val tokenId = EthUInt256.ONE
        val owner = AddressFactory.create()
        saveToken(
            Token(token, name = "TEST", standard = TokenStandard.ERC1155)
        )

        val transfer = ItemTransfer(
            owner = owner,
            token = token,
            tokenId = tokenId,
            date = nowMillis(),
            from = Address.ZERO(),
            value = EthUInt256.TEN
        )
        saveItemHistory(transfer, token, logIndex = 1)

        historyService.update(token, tokenId).awaitFirstOrNull()
        checkItem(token, tokenId, EthUInt256.TEN)
        checkOwnership(owner, token, tokenId, expValue = EthUInt256.TEN, expLazyValue = EthUInt256.ZERO)

        val buyer = AddressFactory.create()

        val transferAsBuying = ItemTransfer(buyer, token, tokenId, nowMillis(), owner, EthUInt256.Companion.of(2))
        saveItemHistory(transferAsBuying, token, logIndex = 2)

        historyService.update(token, tokenId).awaitFirstOrNull()

        checkItem(token, tokenId, EthUInt256.TEN)
        checkOwnership(buyer, token, tokenId, expValue = EthUInt256.of(2), expLazyValue = EthUInt256.ZERO)
        checkOwnership(owner, token, tokenId, expValue = EthUInt256.of(8), expLazyValue = EthUInt256.ZERO)

        checkOwnershipEventWasPublished(token, tokenId, buyer, NftOwnershipUpdateEventDto::class.java)
    }

    /**
     * Check that ownership of ERC721 is removed for the previous owner and a new ownership for the new owner is created
     */
    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun `ownership transferred for ERC721`(version: ReduceVersion) = withReducer(version) {
        val token = AddressFactory.create()
        val tokenId = EthUInt256.ONE
        val owner = AddressFactory.create()
        saveToken(
            Token(token, name = "TEST", standard = TokenStandard.ERC721)
        )

        val transfer = ItemTransfer(
            owner = owner,
            token = token,
            tokenId = tokenId,
            date = nowMillis(),
            from = Address.ZERO(),
            value = EthUInt256.ONE
        )
        saveItemHistory(transfer, token, logIndex = 1)

        historyService.update(token, tokenId).awaitFirstOrNull()
        checkItem(token, tokenId, EthUInt256.ONE)
        checkOwnership(owner, token, tokenId, expValue = EthUInt256.ONE, expLazyValue = EthUInt256.ZERO)

        val buyer = AddressFactory.create()

        val transferAsBuying = ItemTransfer(buyer, token, tokenId, nowMillis(), owner, EthUInt256.ONE)
        saveItemHistory(transferAsBuying, token, logIndex = 2)

        historyService.update(token, tokenId).awaitFirstOrNull()

        checkItem(token, tokenId, EthUInt256.ONE)
        checkOwnership(buyer, token, tokenId, expValue = EthUInt256.ONE, expLazyValue = EthUInt256.ZERO)
        checkEmptyOwnership(owner, token, tokenId)

        checkOwnershipEventWasPublished(token, tokenId, buyer, NftOwnershipUpdateEventDto::class.java)
        checkOwnershipEventWasPublished(token, tokenId, owner, NftOwnershipDeleteEventDto::class.java)
    }

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun `should set pending log only for target ownerships`(version: ReduceVersion) = withReducer(version) {
        val token = AddressFactory.create()
        val tokenId = EthUInt256.ONE
        val owner1 = AddressFactory.create()
        val owner2 = AddressFactory.create()
        val owner3 = AddressFactory.create()
        saveToken(
            Token(token, name = "TEST", standard = TokenStandard.ERC1155)
        )

        val transfer1 = ItemTransfer(
            owner = owner1,
            token = token,
            tokenId = tokenId,
            date = nowMillis(),
            from = Address.ZERO(),
            value = EthUInt256.TEN
        )
        val transfer2 = ItemTransfer(
            owner = owner2,
            token = token,
            tokenId = tokenId,
            date = nowMillis(),
            from = Address.ZERO(),
            value = EthUInt256.TEN
        )
        val transfer3 = ItemTransfer(
            owner = owner3,
            token = token,
            tokenId = tokenId,
            date = nowMillis(),
            from = owner2,
            value = EthUInt256.of(2)
        )

        saveItemHistory(transfer1, token)
        saveItemHistory(transfer2, token)
        saveItemHistory(transfer3, token, status = LogEventStatus.PENDING)

        historyService.update(token, tokenId).awaitFirstOrNull()

        val ownership1 = ownershipRepository.findById(OwnershipId(token, tokenId, owner1)).awaitFirst()
        assertThat(ownership1.value).isEqualTo(EthUInt256.TEN)

        val ownership2 = ownershipRepository.findById(OwnershipId(token, tokenId, owner2)).awaitFirst()
        assertThat(ownership2.value).isEqualTo(EthUInt256.TEN)

        val ownership3 = ownershipRepository.findById(OwnershipId(token, tokenId, owner3)).awaitFirst()
        assertThat(ownership3.value).isEqualTo(EthUInt256.ZERO)


        when (version) {
            ReduceVersion.V1 -> {
                assertThat(ownership1.pending).isEmpty()
                assertThat(ownership2.pending).hasSize(1)
                assertThat(ownership3.pending).hasSize(1)
            }
            ReduceVersion.V2 -> {
                assertThat(ownership1.getPendingEvents()).isEmpty()
                assertThat(ownership2.getPendingEvents()).hasSize(1)
                assertThat(ownership3.getPendingEvents()).hasSize(1)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun `should lazy mint`(version: ReduceVersion) = withReducer(version) {
        val token = Address.ONE()
        val tokenId = EthUInt256.ONE
        val creator = AddressFactory.create()

        val value = EthUInt256.of(20)

        saveToken(
            Token(token, name = "TEST", standard = TokenStandard.ERC1155)
        )
        lazyNftItemHistoryRepository.save(
            ItemLazyMint(
                token = token,
                tokenId = tokenId,
                date = nowMillis(),
                standard = TokenStandard.ERC1155,
                value = value,
                uri = "test",
                creators = creators(creator),
                royalties = emptyList(),
                signatures = emptyList()
            )
        ).awaitFirst()

        historyService.update(token, tokenId).awaitFirstOrNull()

        checkOwnership(creator, token, tokenId, expValue = EthUInt256.of(20), expLazyValue = EthUInt256.of(20))
    }

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun `should calculate lazy after real mint`(version: ReduceVersion) = withReducer(version) {
        val token = Address.ONE()
        val tokenId = EthUInt256.ONE
        val creator = AddressFactory.create()
        val owner1 = AddressFactory.create()

        val value = EthUInt256.of(10)
        saveToken(
            Token(token, name = "TEST", standard = TokenStandard.ERC1155)
        )

        lazyNftItemHistoryRepository.save(
            ItemLazyMint(
                token = token,
                tokenId = tokenId,
                date = nowMillis(),
                value = value,
                standard = TokenStandard.ERC1155,
                uri = "test",
                creators = creators(creator),
                royalties = emptyList(),
                signatures = emptyList()
            )
        ).awaitFirst()

        historyService.update(token, tokenId).awaitFirstOrNull()

        saveItemHistory(
            ItemTransfer(
                owner = owner1,
                token = token,
                tokenId = tokenId,
                date = nowMillis(),
                from = Address.ZERO(),
                value = EthUInt256.Companion.of(2)
            ),
            logIndex = 1,
        )

        historyService.update(token, tokenId).awaitFirstOrNull()

        checkOwnership(
            creator,
            token,
            tokenId,
            expValue = EthUInt256.Companion.of(8),
            expLazyValue = EthUInt256.Companion.of(8)
        )
        checkOwnership(
            owner1,
            token,
            tokenId,
            expValue = EthUInt256.Companion.of(2),
            expLazyValue = EthUInt256.Companion.of(0)
        )
        checkItem(token, tokenId, expSupply = value, expLazySupply = EthUInt256.Companion.of(8), expCreator = creator)

        //transfer to owner1, to creator
        val owner2 = AddressFactory.create()
        saveItemHistory(
            ItemTransfer(
                owner = owner2,
                token = token,
                tokenId = tokenId,
                date = nowMillis(),
                from = Address.ZERO(),
                value = EthUInt256.Companion.of(5)
            ),
            logIndex = 2
        )
        saveItemHistory(
            ItemTransfer(
                owner = creator,
                token = token,
                tokenId = tokenId,
                date = nowMillis(),
                from = Address.ZERO(),
                value = EthUInt256.Companion.of(1)
            ),
            logIndex = 3
        )

        historyService.update(token, tokenId).awaitFirstOrNull()

        checkOwnership(
            creator,
            token,
            tokenId,
            expValue = EthUInt256.Companion.of(3),
            expLazyValue = EthUInt256.Companion.of(2)
        )
        checkOwnership(
            owner1,
            token,
            tokenId,
            expValue = EthUInt256.Companion.of(2),
            expLazyValue = EthUInt256.Companion.of(0)
        )
        checkOwnership(
            owner2,
            token,
            tokenId,
            expValue = EthUInt256.Companion.of(5),
            expLazyValue = EthUInt256.Companion.of(0)
        )
        checkItem(token, tokenId, expSupply = value, expLazySupply = EthUInt256.Companion.of(2))

        saveItemHistory(
            ItemTransfer(
                owner = owner1,
                token = token,
                tokenId = tokenId,
                date = nowMillis(),
                from = Address.ZERO(),
                value = EthUInt256.Companion.of(2)
            )
        )

        historyService.update(token, tokenId).awaitFirstOrNull()

        checkOwnership(creator, token, tokenId, expValue = EthUInt256.Companion.of(1), expLazyValue = EthUInt256.Companion.of(0))
        checkOwnership(owner1, token, tokenId, expValue = EthUInt256.Companion.of(4), expLazyValue = EthUInt256.Companion.of(0))
        checkOwnership(owner2, token, tokenId, expValue = EthUInt256.Companion.of(5), expLazyValue = EthUInt256.Companion.of(0))
        checkItem(token, tokenId, expSupply = value, expLazySupply = EthUInt256.Companion.of(0))
    }

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
     *
     * Make sure we do not set creator to an arbitrary address if the mint transaction was sent by another user.
     */
    @Test
    @Disabled
    fun `should calculate royalties after real mint of lazy nft`() = withReducer(ReduceVersion.V1) {
        val token = Address.ONE()
        val tokenId = EthUInt256.ONE
        val royalties = listOf(Part(AddressFactory.create(), 1), Part(AddressFactory.create(), 2))
        val creator = AddressFactory.create()
        val value = EthUInt256.of(10)

        saveToken(
            Token(token, name = "TEST", standard = TokenStandard.ERC1155)
        )

        lazyNftItemHistoryRepository.save(
            ItemLazyMint(
                token = token,
                tokenId = tokenId,
                date = nowMillis(),
                value = value,
                standard = TokenStandard.ERC1155,
                uri = "test",
                creators = creators(creator),
                royalties = royalties,
                signatures = emptyList()
            )
        ).awaitFirst()

        historyService.update(token, tokenId).awaitFirstOrNull()

        val lazyItem = itemRepository.findById(ItemId(token, tokenId)).awaitFirst()
        assertThat(lazyItem.royalties).isEqualTo(royalties)

        val realRoyalties = listOf(Part(AddressFactory.create(), 10), Part(AddressFactory.create(), 20))
        saveItemHistory(
            ItemRoyalty(token = token, tokenId = tokenId, date = nowMillis(), royalties = realRoyalties),
            logIndex = 1
        )

        historyService.update(token, tokenId).awaitFirstOrNull()

        val realItem = itemRepository.findById(ItemId(token, tokenId)).awaitFirst()
        assertThat(realItem.royalties).isEqualTo(realRoyalties)
    }

    private val itemMeta = ItemMeta(
        properties = ItemProperties(
            name = "Test Item",
            description = "Test Description",
            image = "imageUrl",
            imagePreview = null,
            imageBig = null,
            animationUrl = null,
            attributes = listOf(ItemAttribute(randomString(), randomString())),
            rawJsonContent = null
        ),
        itemContentMeta = ItemContentMeta(
            imageMeta = ContentMeta("imageUrl", 123, 456),
            animationMeta = null
        )
    )

    private suspend fun saveToken(token: Token) {
        tokenRepository.save(token).awaitFirst()
    }

    private suspend fun checkItem(
        token: Address,
        tokenId: EthUInt256,
        expSupply: EthUInt256,
        expLazySupply: EthUInt256 = EthUInt256.ZERO,
        expCreator: Address? = null,
        deleted: Boolean = false
    ) {
        val item = itemRepository.findById(ItemId(token, tokenId)).awaitFirst()

        assertThat(item)
            .hasFieldOrPropertyWithValue(Item::supply.name, expSupply)
            .hasFieldOrPropertyWithValue(Item::lazySupply.name, expLazySupply)
            .hasFieldOrPropertyWithValue(Item::deleted.name, deleted)

        expCreator?.run {
            assertThat(item.creators)
                .isEqualTo(listOf(Part(expCreator, 10000)))
        }
    }

    private suspend fun checkEmptyOwnership(
        owner: Address,
        token: Address,
        tokenId: EthUInt256
    ) {
        val ownershipId = OwnershipId(token, tokenId, owner)
        val found = ownershipRepository.findById(ownershipId).awaitFirstOrNull()
        if (found != null) {
            assertThat(found.deleted).isTrue()
        }
    }

    private suspend fun checkOwnership(
        owner: Address,
        token: Address,
        tokenId: EthUInt256,
        expValue: EthUInt256,
        expLazyValue: EthUInt256
    ) {
        val ownership = ownershipRepository.findById(OwnershipId(token, tokenId, owner)).awaitFirst()
        assertThat(ownership.value).isEqualTo(expValue)
        assertThat(ownership.lazyValue).isEqualTo(expLazyValue)
        assertThat(ownership.deleted).isEqualTo(false)
    }

    companion object {
        @JvmStatic
        fun invalidLogEventStatus(): Stream<Arguments> = Stream.of(
            Arguments.of(LogEventStatus.DROPPED, ReduceVersion.V1),
            Arguments.of(LogEventStatus.INACTIVE, ReduceVersion.V1),
            Arguments.of(LogEventStatus.DROPPED, ReduceVersion.V2),
            Arguments.of(LogEventStatus.INACTIVE, ReduceVersion.V2),
        )

        private fun creators(vararg creator: Address): List<Part> = creators(creator.toList())

        private fun creators(creators: List<Address>): List<Part> {
            val every = 10000 / creators.size
            return creators.map { Part(it, every) }
        }
    }
}
