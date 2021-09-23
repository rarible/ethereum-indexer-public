package com.rarible.protocol.nft.listener.service.descriptors.crypto.punks

import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.test.crypto.punks.AssignEvent
import com.rarible.protocol.contracts.test.crypto.punks.CryptoPunksMarket
import com.rarible.protocol.contracts.test.crypto.punks.PunkBoughtEvent
import com.rarible.protocol.contracts.test.crypto.punks.PunkTransferEvent
import com.rarible.protocol.dto.MintDto
import com.rarible.protocol.dto.TransferDto
import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.integration.IntegrationTest
import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.web3j.utils.Numeric
import scalether.domain.Address
import scalether.domain.request.Transaction
import java.math.BigInteger

@IntegrationTest
@FlowPreview
class CryptoPunksLogDescriptorTest : AbstractIntegrationTest() {

    @Test
    fun getPunkTest() = runBlocking {
        val market = deployCryptoPunkMarket()

        val (punk33OwnerAddress, punk33Sender) = newSender()
        val punkIndex = 33.toBigInteger()
        market.getPunk(punkIndex).withSender(punk33Sender).execute().verifySuccess()
        assertOwnership(market.address(), EthUInt256(punkIndex), punk33OwnerAddress, punk33OwnerAddress)

        Wait.waitAssert {
            val assignments = nftItemHistoryRepository
                .findItemsHistory(market.address(), tokenId = null)
                .filter { it.log.topic == AssignEvent.id() }
                .collectList().awaitFirst()

            assertEquals(1, assignments.size)
            val itemHistory = assignments.single().item
            assertEquals(punk33OwnerAddress, itemHistory.owner)
            assertEquals(punkIndex, itemHistory.tokenId.value)
        }

        Wait.waitAssert {
            val punkItems = mongo.findAll<Item>().collectList().awaitFirst()
            assertEquals(1, punkItems.size)

            val punkItem = punkItems.single()
            assertEquals(market.address(), punkItem.token)
            assertEquals(punkIndex, punkItem.tokenId.value)
            assertEquals(EthUInt256.of(1), punkItem.supply)

            val savedNftTokens = tokenRepository.findAll().collectList().awaitFirst()
            assertEquals(1, savedNftTokens.size)

            val savedNft = savedNftTokens.single()
            assertEquals(market.address(), savedNft.id)
            assertEquals(TokenStandard.CRYPTO_PUNKS, savedNft.standard)

            checkActivityWasPublished(punkItem.token, punkItem.tokenId, AssignEvent.id(), MintDto::class.java)
        }
    }

    @Test
    fun transferPunkTest() {
        transferOrBuyPunkTest(true)
    }

    @Test
    fun buyPunkTest() {
        transferOrBuyPunkTest(false)
    }

    @Test
    fun acceptBidForPunk() = runBlocking {
        val market = deployCryptoPunkMarket()

        val (ownerAddress, ownerSender) = newSender()
        val punkIndex = 42.toBigInteger()
        val punkTokenId = EthUInt256(punkIndex)
        market.getPunk(punkIndex).withSender(ownerSender).execute().verifySuccess()

        val (bidderAddress, bidderSender) = newSender()
        val bidPrice = 100500.toBigInteger()
        depositInitialBalance(bidderAddress, bidPrice)
        market.enterBidForPunk(punkIndex).withSender(bidderSender).withValue(bidPrice)
            .execute().verifySuccess()

        market.acceptBidForPunk(punkIndex, BigInteger.ZERO).withSender(ownerSender).execute().verifySuccess()

        assertOwnership(market.address(), punkTokenId, bidderAddress, ownerAddress)
    }

    private fun transferOrBuyPunkTest(transferOrBuy: Boolean) = runBlocking {
        val market = deployCryptoPunkMarket()

        val (sellerAddress, sellerSender) = newSender()
        val punkIndex = 42.toBigInteger()
        val punkTokenId = EthUInt256(punkIndex)
        market.getPunk(punkIndex).withSender(sellerSender).execute().verifySuccess()
        assertOwnership(market.address(), punkTokenId, sellerAddress, sellerAddress)

        val (buyerAddress, buyerSender) = newSender()
        if (transferOrBuy) {
            market.transferPunk(buyerAddress, punkIndex).withSender(sellerSender).execute().verifySuccess()
        } else {
            // 0. Deposit some ether to the buyer.
            val initialBalance = BigInteger.valueOf(100).ethToWei()
            depositInitialBalance(buyerAddress, initialBalance)

            assertEquals(initialBalance, getEthBalance(buyerAddress))
            assertEquals(BigInteger.ZERO, getEthBalance(sellerAddress))

            // 1. Place the punk for sale.
            val minPrice = BigInteger.TEN.ethToWei()
            market.offerPunkForSale(punkIndex, minPrice).withSender(sellerSender).execute().verifySuccess()

            // 2. Buy the punk.
            val buyPrice = minPrice.multiply(BigInteger.valueOf(2))
            market.buyPunk(punkIndex).withSender(buyerSender).withValue(buyPrice).execute().verifySuccess()

            // 3. Seller withdraws money from the contract.
            market.withdraw().withSender(sellerSender).execute().verifySuccess()

            assertEquals(buyPrice, getEthBalance(sellerAddress))
            assertEquals(initialBalance.minus(buyPrice), getEthBalance(buyerAddress))
        }

        Wait.waitAssert {
            val eventId = if (transferOrBuy) PunkTransferEvent.id() else PunkBoughtEvent.id()
            checkActivityWasPublished(market.address(), punkTokenId, eventId, TransferDto::class.java)
        }

        Wait.waitAssert {
            val transfers = nftItemHistoryRepository
                .findItemsHistory(market.address(), punkTokenId)
                .filter { it.log.topic in ItemType.TRANSFER.topic }
                .collectList().awaitFirst()
                .sortedBy { it.item.date }

            assertEquals(2, transfers.size)
            val mintTransfer = transfers.first().item as ItemTransfer
            assertEquals(Address.ZERO(), mintTransfer.from)
            assertEquals(sellerAddress, mintTransfer.owner)
            assertEquals(EthUInt256.ONE, mintTransfer.value)

            val transfer = transfers.last().item as ItemTransfer
            assertEquals(sellerAddress, transfer.from)
            assertEquals(buyerAddress, transfer.owner)
            assertEquals(EthUInt256.ONE, transfer.value)
        }

        assertOwnership(market.address(), punkTokenId, buyerAddress, sellerAddress)
    }

    private suspend fun assertOwnership(
        token: Address,
        tokenId: EthUInt256,
        expectedOwner: Address,
        expectedCreator: Address
    ) {
        Wait.waitAssert {
            val ownerships = ownershipRepository.search(Query(Criteria.where(Ownership::token.name).isEqualTo(token)))

            assertEquals(1, ownerships.size)
            val ownership = ownerships.single()
            assertEquals(tokenId, ownership.tokenId)
            assertEquals(expectedOwner, ownership.owner)
            assertEquals(listOf(Part(expectedCreator, 10000)), ownership.creators)
        }
    }

    private suspend fun deployCryptoPunkMarket(): CryptoPunksMarket {
        val (_, creatorSender) = newSender()
        val market = CryptoPunksMarket.deployAndWait(creatorSender, poller).awaitFirst()
        market.allInitialOwnersAssigned().execute().awaitFirst()
        nftIndexerProperties.cryptoPunksContractAddress = market.address().prefixed()
        tokenRepository.save(
            Token(
                market.address(),
                name = "CRYPTOPUNKS",
                symbol = "(Ͼ)",
                standard = TokenStandard.CRYPTO_PUNKS
            )
        ).awaitFirst()
        return market
    }

    private suspend fun depositInitialBalance(to: Address, amount: BigInteger) {
        val coinBaseWalletPrivateKey =
            BigInteger(Numeric.hexStringToByteArray("00120de4b1518cf1f16dc1b02f6b4a8ac29e870174cb1d8575f578480930250a"))
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

    private suspend fun getEthBalance(account: Address): BigInteger =
        ethereum.ethGetBalance(account, "latest").awaitFirst()

    private fun BigInteger.ethToWei() = this.multiply(BigInteger.TEN.pow(18))
}

