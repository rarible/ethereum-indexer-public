package com.rarible.protocol.nft.listener.service.descriptors.crypto.punks

import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.test.crypto.punks.AssignEvent
import com.rarible.protocol.contracts.test.crypto.punks.CryptoPunksMarket
import com.rarible.protocol.contracts.test.crypto.punks.PunkBoughtEvent
import com.rarible.protocol.contracts.test.crypto.punks.PunkTransferEvent
import com.rarible.protocol.dto.MintDto
import com.rarible.protocol.dto.TransferDto
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.ItemType
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.model.ReduceVersion
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.listener.test.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.test.IntegrationTest
import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.web3j.utils.Numeric
import scalether.domain.Address
import scalether.domain.request.Transaction
import java.math.BigInteger
import java.time.Duration

@IntegrationTest
class CryptoPunksLogDescriptorIt : AbstractIntegrationTest() {

    private lateinit var market: CryptoPunksMarket

    @BeforeEach
    fun beforeAll() = runBlocking {
        featureFlags.reduceVersion = ReduceVersion.V2
        market = createMarket()
    }

    @Test
    fun getPunkTest() = runBlocking {
        val (owner, sender) = newSender()
        val tokenId = 33.toBigInteger()

        market.getPunk(tokenId).withSender(sender).execute().verifySuccess()
        assertOwnership(tokenId, owner)

        waitAssert {
            val assignments = nftItemHistoryRepository
                .findItemsHistory(market.address(), tokenId = null)
                .filter { it.log.topic == AssignEvent.id() }
                .collectList().awaitFirst()

            assertThat(assignments.size).isEqualTo(1)

            val itemHistory = assignments.single().item
            assertThat(itemHistory.owner).isEqualTo(owner)
            assertThat(itemHistory.tokenId.value).isEqualTo(tokenId)
        }

        waitAssert {
            val itemId = ItemId(market.address(), EthUInt256(tokenId))
            val item = itemRepository.findById(itemId).awaitFirstOrNull()

            assertThat(item).isNotNull

            assertThat(item!!.token).isEqualTo(market.address())
            assertThat(item.tokenId.value).isEqualTo(tokenId)
            assertThat(item.supply).isEqualTo(EthUInt256.of(1))

            checkActivityWasPublished(item.token, item.tokenId, AssignEvent.id(), MintDto::class.java)
        }
    }

    @Test
    fun acceptBidForPunk() = runBlocking {
        val (_, ownerSender) = newSender()
        val tokenId = randomBigInt(3)

        market.getPunk(tokenId)
            .withSender(ownerSender)
            .execute()
            .verifySuccess()

        val (bidderAddress, bidderSender) = newSender()
        val bidPrice = randomBigInt()
        depositInitialBalance(bidderAddress, bidPrice)

        market.enterBidForPunk(tokenId)
            .withSender(bidderSender)
            .withValue(bidPrice)
            .execute()
            .verifySuccess()

        market.acceptBidForPunk(tokenId, BigInteger.ZERO)
            .withSender(ownerSender)
            .execute()
            .verifySuccess()

        assertOwnership(tokenId, bidderAddress)
    }

    @Test
    fun transferPunkTest() = runBlocking {
        transferOrBuyPunkTest(true)
    }

    @Test
    fun buyPunkTest() = runBlocking {
        transferOrBuyPunkTest(false)
    }

    private suspend fun transferOrBuyPunkTest(transferOrBuy: Boolean) {
        val (ownerAddress, ownerSender) = newSender()
        val tokenId = randomBigInt(3)

        market.getPunk(tokenId)
            .withSender(ownerSender)
            .execute()
            .verifySuccess()

        assertOwnership(tokenId, ownerAddress)

        val (receiverAddress, receiverSender) = newSender()

        if (transferOrBuy) {
            market.transferPunk(receiverAddress, tokenId)
                .withSender(ownerSender)
                .execute()
                .verifySuccess()
        } else {
            // 0. Deposit some ether to the buyer.
            val initialBalance = BigInteger.TEN
            depositInitialBalance(receiverAddress, initialBalance)

            // 1. Place the punk for sale.
            market.offerPunkForSale(tokenId, BigInteger.ONE)
                .withSender(ownerSender)
                .execute()
                .verifySuccess()

            // 2. Buy the punk.
            market.buyPunk(tokenId)
                .withSender(receiverSender)
                .withValue(BigInteger.ONE)
                .execute()
                .verifySuccess()

            // 3. Seller withdraws money from the contract.
            market.withdraw()
                .withSender(ownerSender)
                .execute()
                .verifySuccess()
        }

        waitAssert {
            val eventId = if (transferOrBuy) PunkTransferEvent.id() else PunkBoughtEvent.id()
            checkActivityWasPublished(market.address(), EthUInt256(tokenId), eventId, TransferDto::class.java)
        }

        waitAssert {
            val transfers = nftItemHistoryRepository
                .findItemsHistory(market.address(), EthUInt256(tokenId))
                .filter { it.log.topic in ItemType.TRANSFER.topic }
                .collectList().awaitFirst()
                .sortedBy { it.item.date }

            assertEquals(2, transfers.size)
            val mintTransfer = transfers.first().item as ItemTransfer
            assertThat(mintTransfer.from).isEqualTo(Address.ZERO())
            assertThat(mintTransfer.owner).isEqualTo(ownerAddress)
            assertThat(mintTransfer.value).isEqualTo(EthUInt256.ONE)

            val transfer = transfers.last().item as ItemTransfer
            assertThat(transfer.from).isEqualTo(ownerAddress)
            assertThat(transfer.owner).isEqualTo(receiverAddress)
            assertThat(transfer.value).isEqualTo(EthUInt256.ONE)
        }

        assertOwnership(tokenId, receiverAddress)
    }

    private suspend fun assertOwnership(
        tokenId: BigInteger,
        expectedOwner: Address
    ) = waitAssert {
        val ownershipId = OwnershipId(market.address(), EthUInt256(tokenId), expectedOwner)
        val ownership = ownershipRepository.findById(ownershipId).awaitFirstOrNull()
        assertThat(ownership).isNotNull
        assertThat(ownership!!.value).isEqualTo(EthUInt256.ONE)
        assertThat(ownership.owner).isEqualTo(expectedOwner)
    }

    private suspend fun createMarket(): CryptoPunksMarket {
        val (_, creatorSender) = newSender()
        val market = CryptoPunksMarket.deployAndWait(creatorSender, poller).awaitFirst()
        market.allInitialOwnersAssigned().execute().awaitFirst()
        nftIndexerProperties.cryptoPunksContractAddress = market.address().prefixed()
        tokenRepository.save(
            Token(
                id = market.address(),
                name = "CRYPTOPUNKS",
                symbol = "(Ͼ)",
                standard = TokenStandard.CRYPTO_PUNKS
            )
        ).awaitFirst()

        waitAssert {
            val collection = tokenRepository.findById(market.address()).awaitFirstOrNull()
            assertThat(collection).isNotNull
            assertThat(collection!!.id).isEqualTo(market.address())
            assertThat(TokenStandard.CRYPTO_PUNKS).isEqualTo(collection.standard)
        }

        return market
    }

    private suspend fun depositInitialBalance(to: Address, amount: BigInteger) {
        val coinBaseWalletPrivateKey = BigInteger(
            Numeric.hexStringToByteArray("00120de4b1518cf1f16dc1b02f6b4a8ac29e870174cb1d8575f578480930250a")
        )
        val (coinBaseAddress, coinBaseSender) = newSender(coinBaseWalletPrivateKey)
        coinBaseSender.sendTransaction(
            Transaction(
                to,
                coinBaseAddress,
                BigInteger.valueOf(8000000),
                BigInteger.ZERO,
                amount,
                Binary(ByteArray(1)),
                null
            )
        ).verifySuccess()
    }

    private suspend fun waitAssert(call: suspend () -> Unit) = Wait.waitAssertWithCheckInterval(
        timeout = Duration.ofSeconds(5),
        runnable = call
    )
}
