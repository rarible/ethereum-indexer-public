package com.rarible.protocol.nft.core.service.token

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.contracts.collection.CreateEvent
import com.rarible.protocol.contracts.erc20.test.SimpleERC20
import com.rarible.protocol.contracts.erc721.OwnershipTransferredEvent
import com.rarible.protocol.contracts.erc721.rarible.ERC721Rarible
import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.model.CollectionEvent
import com.rarible.protocol.nft.core.model.CollectionOwnershipTransferred
import com.rarible.protocol.nft.core.model.ContractStatus
import com.rarible.protocol.nft.core.model.CreateCollection
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenEvent
import com.rarible.protocol.nft.core.model.TokenFeature
import com.rarible.protocol.nft.core.model.TokenStandard
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import scalether.domain.AddressFactory

@IntegrationTest
class TokenServiceIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var tokenService: TokenService

    @Autowired
    lateinit var tokenReduceService: TokenReduceService

    @Test
    fun `should send msg to external topic`() = runBlocking<Unit> {

        val id = randomAddress()
        val owner = randomAddress()

        tokenHistoryRepository.save(
            LogEvent(
                CreateCollection(
                    id = id,
                    owner = owner,
                    name = "Test",
                    symbol = "TEST"
                ),
                address = id,
                topic = CreateEvent.id(),
                transactionHash = Word.apply(randomWord()),
                status = LogEventStatus.CONFIRMED,
                blockNumber = 1,
                logIndex = 0,
                minorLogIndex = 0,
                index = 0
            )
        ).awaitFirst()

        tokenReduceService.reduce(id)

        checkCollectionWasPublished(
            NftCollectionDto(
                id = id,
                name = "Test",
                symbol = "TEST",
                supportsLazyMint = false,
                type = NftCollectionDto.Type.ERC721,
                status = NftCollectionDto.Status.CONFIRMED,
                owner = owner,
                features = listOf(
                    NftCollectionDto.Features.APPROVE_FOR_ALL,
                    NftCollectionDto.Features.SET_URI_PREFIX,
                    NftCollectionDto.Features.BURN
                ),
                isRaribleContract = true,
                minters = listOf(owner),
            )
        )
    }

    @Test
    fun `change owner for a token registered via service`() = runBlocking<Unit> {
        val id = randomAddress()
        val previousOwner = randomAddress()
        val newOwner = randomAddress()
        tokenService.register(id) {
            Token(
                id = id,
                owner = previousOwner,
                name = "Name",
                symbol = "Symbol",
                standard = TokenStandard.ERC721
            )
        }

        tokenHistoryRepository.save(
            LogEvent(
                CollectionOwnershipTransferred(
                    id = id,
                    previousOwner = previousOwner,
                    newOwner = newOwner
                ),
                address = id,
                topic = OwnershipTransferredEvent.id(),
                transactionHash = Word.apply(randomWord()),
                status = LogEventStatus.CONFIRMED,
                blockNumber = 1,
                logIndex = 0,
                minorLogIndex = 0,
                index = 0
            )
        ).awaitFirst()

        val updated = tokenReduceService.reduce(id)
        assertThat(updated).isEqualToIgnoringGivenFields(
            Token(
                id = id,
                owner = newOwner,
                name = "Name",
                symbol = "Symbol",
                standard = TokenStandard.ERC721
            ),
            Token::lastEventId.name,
            Token::version.name,
            Token::revertableEvents.name,
            Token::dbUpdatedAt.name
        )
    }

    @Test
    fun `return token registered with via service having no log events`() = runBlocking<Unit> {
        val id = randomAddress()
        val token = Token(
            id = id,
            owner = randomAddress(),
            name = "Name",
            symbol = "Symbol",
            standard = TokenStandard.ERC721
        )
        tokenService.register(id) { token }
        val updated = tokenReduceService.reduce(id)
        assertThat(updated).isEqualTo(updated)
    }

    @Test
    fun `should not change lastEventId`() = runBlocking<Unit> {
        val collectionId = randomAddress()
        prepareStorage(
            CreateCollection(
                id = collectionId,
                owner = randomAddress(),
                name = "Test",
                symbol = "TEST"
            )
        )
        val token = tokenReduceService.reduce(collectionId)
        assertThat(token).isNotNull

        assertThat(token!!.revertableEvents).hasSize(1)
        assertThat(token.revertableEvents.single()).isInstanceOf(TokenEvent.TokenCreateEvent::class.java)

        val sameToken = tokenReduceService.reduce(collectionId)
        assertThat(sameToken?.copy(version = 0, dbUpdatedAt = token.dbUpdatedAt)).isEqualTo(token.copy(version = 0))
    }

    @Test
    fun `should change lastEventId if a new event is added`() = runBlocking<Unit> {
        val collectionId = randomAddress()
        val previousOwner = randomAddress()
        prepareStorage(
            CreateCollection(
                collectionId,
                previousOwner,
                "Test",
                "TEST"
            ),
            blockNumber = 1
        )
        val token = tokenReduceService.reduce(collectionId)
        assertThat(token).isNotNull
        assertThat(token!!.revertableEvents).hasSize(1)
        assertThat(token.revertableEvents.single()).isInstanceOf(TokenEvent.TokenCreateEvent::class.java)

        val newOwner = randomAddress()
        prepareStorage(
            CollectionOwnershipTransferred(
                collectionId,
                previousOwner,
                newOwner
            ),
            blockNumber = 2
        )

        val withUpdatedOwner = tokenReduceService.reduce(collectionId)
        println("NEW $withUpdatedOwner")
        println("OWN $newOwner")

        assertThat(withUpdatedOwner).isNotNull
        assertThat(withUpdatedOwner!!.owner).isEqualTo(newOwner)
        assertThat(withUpdatedOwner.revertableEvents).hasSize(2)
        assertThat(withUpdatedOwner.revertableEvents[0]).isInstanceOf(TokenEvent.TokenCreateEvent::class.java)
        assertThat(withUpdatedOwner.revertableEvents[1]).isInstanceOf(TokenEvent.TokenChangeOwnershipEvent::class.java)
    }

    @Test
    fun `should fix collection with NONE standard`() = runBlocking<Unit> {
        val (contract, owner) = createContract("Test", "TEST")

        // Let's assume we failed to set standard and features for the first time
        val token = createToken().copy(
            id = contract,
            standard = TokenStandard.NONE,
            features = emptySet(),
            status = ContractStatus.CONFIRMED
        )
        tokenRepository.save(token).awaitFirstOrNull()

        val updated = tokenService.update(contract)!!
        assertThat(updated.standard).isEqualTo(TokenStandard.ERC721)

        checkCollectionWasPublished(
            NftCollectionDto(
                id = contract,
                name = "Test",
                symbol = "TEST",
                supportsLazyMint = true,
                type = NftCollectionDto.Type.ERC721,
                status = NftCollectionDto.Status.CONFIRMED,
                owner = owner,
                features = listOf(
                    NftCollectionDto.Features.APPROVE_FOR_ALL,
                    NftCollectionDto.Features.BURN,
                    NftCollectionDto.Features.MINT_AND_TRANSFER
                ),
                isRaribleContract = false,
                minters = emptyList(),
            )
        )
    }

    @Test
    fun `register ERC721 token`() = runBlocking<Unit> {
        val (contract, owner) = createContract("Name", "Symbol")
        val token = tokenService.register(contract)
        val expectedToken = Token(
            id = contract,
            name = "Name",
            symbol = "Symbol",
            owner = owner,
            status = ContractStatus.CONFIRMED,
            features = setOf(
                TokenFeature.APPROVE_FOR_ALL,
                TokenFeature.BURN,
                TokenFeature.MINT_AND_TRANSFER
            ),
            standard = TokenStandard.ERC721,
            // Byte code MUST be always the same for test contract
            byteCodeHash = Word.apply("0x66a01ac3911dbf81d3f7a37e56590a632b3d61f59feb0f10cacc9a2b9d565598")
        )
        assertThat(token).isEqualToIgnoringGivenFields(
            expectedToken,
            Token::lastEventId.name,
            Token::version.name,
            Token::dbUpdatedAt.name
        )
    }

    @Test
    fun `update - keep isRaribleContract flag`() = runBlocking<Unit> {
        val (contract, owner) = createContract()

        // Let's assume we failed to set standard and features for the first time
        val token = createToken().copy(
            id = contract,
            standard = TokenStandard.NONE,
            isRaribleContract = true
        )
        tokenRepository.save(token).awaitFirstOrNull()

        val result = tokenService.update(contract)!!

        assertThat(result.isRaribleContract).isEqualTo(true)
        assertThat(result.standard).isEqualTo(TokenStandard.ERC721)
    }

    private suspend fun createContract(
        name: String = "Name",
        symbol: String = "Symbol",
        baseUri: String = "baseURI",
        contractUri: String = "contractURI",
    ): Pair<Address, Address> {
        val adminSender = newSender().second
        val erc721 = ERC721Rarible.deployAndWait(adminSender, poller).awaitFirst()

        erc721.__ERC721Rarible_init(name, symbol, baseUri, contractUri)
            .execute()
            .verifySuccess()

        return erc721.address() to adminSender.from()
    }

    @Test
    fun `register ERC20 token by checking interface`() = runBlocking<Unit> {
        val adminSender = newSender().second
        val erc20 = SimpleERC20.deployAndWait(adminSender, poller).awaitFirst()

        val token = tokenService.register(erc20.address())
        assertThat(token.standard).isEqualTo(TokenStandard.ERC20)
    }

    private suspend fun prepareStorage(
        vararg events: CollectionEvent,
        blockNumber: Long = System.currentTimeMillis()
    ) {
        events.forEach { event ->
            tokenHistoryRepository.save(
                LogEvent(
                    data = event,
                    address = AddressFactory.create(),
                    topic = if (event is CreateCollection) CreateEvent.id() else Word.apply(randomWord()),
                    transactionHash = Word.apply(randomWord()),
                    status = LogEventStatus.CONFIRMED,
                    blockNumber = blockNumber,
                    logIndex = 0,
                    minorLogIndex = 0,
                    index = 0,
                    createdAt = nowMillis(),
                    updatedAt = nowMillis()
                )
            ).awaitFirst()
        }
    }
}
