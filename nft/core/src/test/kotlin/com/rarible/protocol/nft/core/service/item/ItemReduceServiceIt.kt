package com.rarible.protocol.nft.core.service.item

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.dto.NftItemDeleteEventDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.nft.core.data.createRandomBurnAction
import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemCreators
import com.rarible.protocol.nft.core.model.ItemExState
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemLazyMint
import com.rarible.protocol.nft.core.model.ItemRoyalty
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipFilter
import com.rarible.protocol.nft.core.model.OwnershipFilterByItem
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.core.model.ReduceVersion
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.action.NftItemActionEventRepository
import com.rarible.protocol.nft.core.repository.item.ItemExStateRepository
import com.rarible.protocol.nft.core.repository.ownership.OwnershipFilterCriteria.toCriteria
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.provider.Arguments
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.stream.Stream

@IntegrationTest
class ItemReduceServiceIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var itemReduceService: ItemReduceService

    @Autowired
    private lateinit var ownershipRepository: OwnershipRepository

    @Autowired
    private lateinit var nftItemActionEventRepository: NftItemActionEventRepository

    @Autowired
    private lateinit var itemExStateRepository: ItemExStateRepository

    @Test
    fun mintItem() = runBlocking {
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

        itemReduceService.update(token, tokenId).awaitFirstOrNull()

        val item = itemRepository.findById(ItemId(token, tokenId)).awaitFirst()
        assertThat(item.mintedAt).isEqualTo(blockTimestamp)

        assertThat(item.creators).isEqualTo(listOf(Part.fullPart(owner)))
        assertThat(item.supply).isEqualTo(EthUInt256.ONE)

        checkItemEventWasPublished(
            token,
            tokenId,
            pendingSize = 0,
            NftItemUpdateEventDto::class.java
        )
    }

    @Test
    fun `set suspicious flag via state`() = runBlocking<Unit> {
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

        itemReduceService.update(token, tokenId).awaitFirstOrNull()

        val item = itemRepository.findById(ItemId(token, tokenId)).awaitFirst()
        assertThat(item.isSuspiciousOnOS).isFalse()

        val exState = ItemExState.initial(item.id).copy(isSuspiciousOnOS = true)
        itemExStateRepository.save(exState)

        itemReduceService.update(token, tokenId).awaitFirstOrNull()

        val suspiciousItem = itemRepository.findById(ItemId(token, tokenId)).awaitFirst()
        assertThat(suspiciousItem.isSuspiciousOnOS).isTrue
    }

    @Test
    fun `should full reduce existed item`() = runBlocking<Unit> {
        val owner = AddressFactory.create()
        val token = AddressFactory.create()
        val tokenId = EthUInt256.ONE
        val blockTimestamp = Instant.ofEpochSecond(12)

        val existedItem = itemRepository.save(
            createRandomItem().copy(token = token, tokenId = tokenId)
        ).awaitFirst()

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

        // First time all changes should be applied and item should be updated (since it has been changed)
        itemReduceService.update(token, tokenId, updateNotChanged = false).awaitFirstOrNull()

        val item = itemRepository.findById(ItemId(token, tokenId)).awaitFirst()
        assertThat(item.version).isEqualTo(existedItem.version!! + 1)

        // But if we call reduce second time in silent mode, item should stay the same since nothing changed
        itemReduceService.update(token, tokenId, updateNotChanged = false).awaitFirstOrNull()

        val notUpdatedItem = itemRepository.findById(ItemId(token, tokenId)).awaitFirst()
        assertThat(notUpdatedItem.version).isEqualTo(item.version)
    }

    @Test
    fun `full reduce only confirmed logs`() = runBlocking<Unit> {
        val owner = AddressFactory.create()
        val token = AddressFactory.create()
        val tokenId1 = EthUInt256.ONE
        val tokenId2 = EthUInt256.TEN
        val blockTimestamp = Instant.ofEpochSecond(12)
        saveToken(
            Token(token, name = "TEST", standard = TokenStandard.ERC721)
        )
        val mintTransfer = ItemTransfer(
            owner = owner,
            token = token,
            tokenId = tokenId1,
            date = nowMillis(),
            from = Address.ZERO(),
            value = EthUInt256.ONE
        )
        val revertedMintTransfer = ItemTransfer(
            owner = owner,
            token = token,
            tokenId = tokenId2,
            date = nowMillis(),
            from = Address.ZERO(),
            value = EthUInt256.ONE
        )
        saveItemHistory(
            mintTransfer,
            logIndex = 0,
            from = owner,
            blockTimestamp = blockTimestamp,
            status = LogEventStatus.CONFIRMED
        )
        saveItemHistory(
            revertedMintTransfer,
            logIndex = 0,
            from = owner,
            blockTimestamp = blockTimestamp,
            status = LogEventStatus.REVERTED
        )
        itemReduceService.update(token = token, tokenId = tokenId1, updateNotChanged = false).awaitFirstOrNull()
        itemReduceService.update(token = token, tokenId = tokenId2, updateNotChanged = false).awaitFirstOrNull()
        val item1 = itemRepository.findById(ItemId(token, tokenId1)).awaitFirstOrNull()
        val item2 = itemRepository.findById(ItemId(token, tokenId2)).awaitFirstOrNull()
        assertThat(item1).isNotNull
        assertThat(item2).isNull()
    }

    @Test
    fun `should get creator from tokenId for OpenSea tokenId`() = runBlocking {
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

        itemReduceService.update(token, tokenId).awaitFirstOrNull()

        val item = itemRepository.findById(ItemId(token, tokenId)).awaitFirst()

        assertThat(item.creators).isEqualTo(listOf(Part.fullPart(creator)))
        assertThat(item.supply).isEqualTo(EthUInt256.ONE)

        checkItemEventWasPublished(
            token,
            tokenId,
            pendingSize = 0,
            NftItemUpdateEventDto::class.java
        )
    }

    @Test
    fun transferToSelf() = runBlocking {
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

        itemReduceService.update(token, tokenId).awaitFirstOrNull()
        val ownership = ownershipRepository.findById(OwnershipId(token, tokenId, owner)).awaitFirst()
        assertThat(ownership.value).isEqualTo(EthUInt256.TEN)
        checkItem(token = token, tokenId = tokenId, expSupply = EthUInt256.TEN)
    }

    @Test
    fun burnItem() = runBlocking<Unit> {
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
        ownershipRepository.save(
            Ownership(
                token = token,
                tokenId = tokenId,
                owner = owner,
                value = EthUInt256.ONE,
                date = Instant.now(),
                pending = emptyList(),
                lastUpdatedAt = nowMillis()
            )
        ).awaitFirst()

        itemReduceService.update(token, tokenId).awaitFirstOrNull()
        val item = itemRepository.findById(ItemId(token, tokenId)).awaitFirst()
        assertThat(item.supply).isEqualTo(EthUInt256.ZERO)
        assertThat(item.deleted).isEqualTo(true)

        checkItemEventWasPublished(
            token,
            tokenId,
            pendingSize = 0,
            NftItemDeleteEventDto::class.java
        )
        checkOwnershipEventWasPublished(token, tokenId, owner, NftOwnershipDeleteEventDto::class.java)
    }

    @Test
    fun confirmedItemTransfer() = runBlocking<Unit> {
        val token = AddressFactory.create()
        val tokenId = EthUInt256.ONE
        val instantDate1 = LocalDate.parse("2022-04-12").atStartOfDay().toInstant(ZoneOffset.UTC)
        val instantDate2 = LocalDate.parse("2022-04-13").atStartOfDay().toInstant(ZoneOffset.UTC)

        saveToken(
            Token(token, name = "TEST", standard = TokenStandard.ERC721)
        )
        val transfer = ItemTransfer(
            owner = AddressFactory.create(),
            token = token,
            tokenId = tokenId,
            date = instantDate1,
            from = Address.ZERO(),
            value = EthUInt256.of(2)
        )
        saveItemHistory(transfer, logIndex = 1, blockTimestamp = instantDate1)

        val owner = AddressFactory.create()
        val transfer2 = ItemTransfer(
            owner = owner,
            token = token,
            tokenId = tokenId,
            date = instantDate2,
            from = Address.ZERO(),
            value = EthUInt256.of(3)
        )
        saveItemHistory(transfer2, logIndex = 2, blockTimestamp = instantDate2)

        itemReduceService.update(token, tokenId).awaitFirstOrNull()
        checkItem(token = token, tokenId = tokenId, expSupply = EthUInt256.of(5))
        val item = itemRepository.findById(ItemId(token, tokenId)).awaitFirst()
        assertThat(item.mintedAt).isEqualTo(instantDate1)
    }

    @Test
    @Disabled
    fun confirmedItemRoyalty() = runBlocking<Unit> {
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

        itemReduceService.update(token, tokenId).then().block()
        val item = itemRepository.findById(ItemId(token, tokenId)).awaitFirst()
        assertThat(item.royalties).isEqualTo(listOf(Part(owner, 2)))
    }

    @Test
    fun confirmedItemMint() = runBlocking<Unit> {
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

        itemReduceService.update(token, tokenId).then().block()

        val item = itemRepository.findById(ItemId(token, tokenId)).awaitFirst()
        assertThat(item.creators).isEqualTo(creatorsList)
    }

    @Test
    fun `update ownership for ERC1155`() = runBlocking<Unit> {
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

        itemReduceService.update(token, tokenId).awaitFirstOrNull()
        checkItem(token, tokenId, EthUInt256.TEN)
        checkOwnership(owner, token, tokenId, expValue = EthUInt256.TEN, expLazyValue = EthUInt256.ZERO)
        val lastUpdatedAt1 = getLastUpdatedAtForBeforeTest(owner = owner, token = token, tokenId = tokenId)

        val buyer = AddressFactory.create()

        val transferAsBuying = ItemTransfer(buyer, token, tokenId, nowMillis(), owner, EthUInt256.Companion.of(2))
        saveItemHistory(transferAsBuying, token, logIndex = 2)

        itemReduceService.update(token, tokenId).awaitFirstOrNull()

        checkItem(token, tokenId, EthUInt256.TEN)
        checkOwnership(buyer, token, tokenId, expValue = EthUInt256.of(2), expLazyValue = EthUInt256.ZERO)
        checkOwnership(owner, token, tokenId, expValue = EthUInt256.of(8), expLazyValue = EthUInt256.ZERO)
        val lastUpdatedAt2 = getOwnershipLastUpdatedAt(owner = owner, token = token, tokenId = tokenId)
        assertThat(lastUpdatedAt2!!.isAfter(lastUpdatedAt1)).isTrue

        checkOwnershipEventWasPublished(token, tokenId, buyer, NftOwnershipUpdateEventDto::class.java)
    }

    /**
     * Check that ownership of ERC721 is removed for the previous owner and a new ownership for the new owner is created
     */
    @Test
    fun `ownership transferred for ERC721`() = runBlocking<Unit> {
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

        itemReduceService.update(token, tokenId).awaitFirstOrNull()
        checkItem(token, tokenId, EthUInt256.ONE)
        checkOwnership(owner, token, tokenId, expValue = EthUInt256.ONE, expLazyValue = EthUInt256.ZERO)
        val lastUpdatedAt1 = getLastUpdatedAtForBeforeTest(owner = owner, token = token, tokenId = tokenId)

        val buyer = AddressFactory.create()

        val transferAsBuying = ItemTransfer(buyer, token, tokenId, nowMillis(), owner, EthUInt256.ONE)
        saveItemHistory(transferAsBuying, token, logIndex = 2)

        itemReduceService.update(token, tokenId).awaitFirstOrNull()

        checkItem(token, tokenId, EthUInt256.ONE)
        checkOwnership(buyer, token, tokenId, expValue = EthUInt256.ONE, expLazyValue = EthUInt256.ZERO)
        checkEmptyOwnership(owner, token, tokenId)
        val lastUpdatedAt2 = getOwnershipLastUpdatedAt(owner = owner, token = token, tokenId = tokenId)
        assertThat(lastUpdatedAt2!!.isAfter(lastUpdatedAt1)).isTrue

        checkOwnershipEventWasPublished(token, tokenId, buyer, NftOwnershipUpdateEventDto::class.java)
        checkOwnershipEventWasPublished(token, tokenId, owner, NftOwnershipDeleteEventDto::class.java)
    }

    @Test
    fun `should lazy mint`() = runBlocking<Unit> {
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

        itemReduceService.update(token, tokenId).awaitFirstOrNull()

        checkOwnership(creator, token, tokenId, expValue = EthUInt256.of(20), expLazyValue = EthUInt256.of(20))
    }

    @Test
    fun `should calculate lazy after real mint`() = runBlocking<Unit> {
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

        itemReduceService.update(token, tokenId).awaitFirstOrNull()

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

        itemReduceService.update(token, tokenId).awaitFirstOrNull()

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

        // transfer to owner1, to creator
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

        itemReduceService.update(token, tokenId).awaitFirstOrNull()

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

        itemReduceService.update(token, tokenId).awaitFirstOrNull()

        checkOwnership(
            creator,
            token,
            tokenId,
            expValue = EthUInt256.Companion.of(1),
            expLazyValue = EthUInt256.Companion.of(0)
        )
        checkOwnership(
            owner1,
            token,
            tokenId,
            expValue = EthUInt256.Companion.of(4),
            expLazyValue = EthUInt256.Companion.of(0)
        )
        checkOwnership(
            owner2,
            token,
            tokenId,
            expValue = EthUInt256.Companion.of(5),
            expLazyValue = EthUInt256.Companion.of(0)
        )
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
    fun `should calculate royalties after real mint of lazy nft`() = runBlocking<Unit> {
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

        itemReduceService.update(token, tokenId).awaitFirstOrNull()

        val lazyItem = itemRepository.findById(ItemId(token, tokenId)).awaitFirst()
        assertThat(lazyItem.royalties).isEqualTo(royalties)

        val realRoyalties = listOf(Part(AddressFactory.create(), 10), Part(AddressFactory.create(), 20))
        saveItemHistory(
            ItemRoyalty(token = token, tokenId = tokenId, date = nowMillis(), royalties = realRoyalties),
            logIndex = 1
        )

        itemReduceService.update(token, tokenId).awaitFirstOrNull()

        val realItem = itemRepository.findById(ItemId(token, tokenId)).awaitFirst()
        assertThat(realItem.royalties).isEqualTo(realRoyalties)
    }

    @Test
    @Disabled
    fun `should burn item by action`() = runBlocking<Unit> {
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
        val burnAction = createRandomBurnAction().copy(
            token = token,
            tokenId = tokenId,
            actionAt = nowMillis() - Duration.ofDays(1)
        )
        nftItemActionEventRepository.save(burnAction).awaitFirst()

        itemReduceService.update(token, tokenId).awaitFirstOrNull()

        val item = itemRepository.findById(ItemId(token, tokenId)).awaitFirst()
        assertThat(item.supply).isEqualTo(EthUInt256.ZERO)
        assertThat(item.deleted).isTrue()
        val ownershipFilter = OwnershipFilterByItem(
            sort = OwnershipFilter.Sort.LAST_UPDATE,
            contract = token,
            tokenId = tokenId.value
        )
        val ownerships = ownershipRepository.search(ownershipFilter.toCriteria(null, null))
        assertThat(ownerships).hasSize(0)
    }

    @Test
    fun `should not burn item by action if action not time to apply`() = runBlocking<Unit> {
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
        val burnAction = createRandomBurnAction().copy(
            token = token,
            tokenId = tokenId,
            actionAt = nowMillis() + Duration.ofDays(1)
        )
        nftItemActionEventRepository.save(burnAction).awaitFirst()

        itemReduceService.update(token, tokenId).awaitFirstOrNull()

        val item = itemRepository.findById(ItemId(token, tokenId)).awaitFirst()
        assertThat(item.deleted).isFalse
    }

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
        expLazyValue: EthUInt256,
        deleted: Boolean = false
    ) {
        val ownership = ownershipRepository.findById(OwnershipId(token, tokenId, owner)).awaitFirst()
        assertThat(ownership.value).isEqualTo(expValue)
        assertThat(ownership.lazyValue).isEqualTo(expLazyValue)
        assertThat(ownership.deleted).isEqualTo(deleted)
    }

    private suspend fun getLastUpdatedAtForBeforeTest(
        owner: Address,
        token: Address,
        tokenId: EthUInt256
    ): Instant {
        val ownership = ownershipRepository.findById(OwnershipId(token, tokenId, owner)).awaitFirst()
        val reducedLastUpdatedAt = ownership.lastUpdatedAt!!.minusMillis(1)
        ownershipRepository.save(ownership.copy(lastUpdatedAt = reducedLastUpdatedAt)).awaitFirst()
        return reducedLastUpdatedAt
    }

    private suspend fun getOwnershipLastUpdatedAt(
        owner: Address,
        token: Address,
        tokenId: EthUInt256
    ): Instant? {
        return ownershipRepository.findById(OwnershipId(token, tokenId, owner)).awaitFirst().lastUpdatedAt
    }

    companion object {
        @JvmStatic
        fun invalidLogEventStatus(): Stream<Arguments> = Stream.of(
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
