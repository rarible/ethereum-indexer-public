package com.rarible.protocol.order.api.controller

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.contracts.exchange.crypto.punks.CryptoPunksMarket
import com.rarible.protocol.dto.CreateTransactionRequestDto
import com.rarible.protocol.dto.LogEventDto
import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.CRYPTO_PUNKS_SALT
import com.rarible.protocol.order.core.model.CryptoPunksAssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.MakeBalanceState
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderCryptoPunksData
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.Platform
import io.daonomic.rpc.domain.Binary
import io.mockk.coEvery
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.web3j.utils.Numeric
import scalether.domain.Address
import scalether.domain.response.TransactionReceipt
import java.math.BigDecimal
import java.math.BigInteger

@IntegrationTest
class CryptoPunksPendingTransactionsFt : AbstractIntegrationTest() {

    protected lateinit var cryptoPunksMarket: CryptoPunksMarket

    @BeforeEach
    fun initializeCryptoPunksMarket() = runBlocking<Unit> {
        val (_, creatorSender) = newSender()
        cryptoPunksMarket = CryptoPunksMarket.deployAndWait(creatorSender, poller).awaitFirst()
        cryptoPunksMarket.allInitialOwnersAssigned().execute().awaitFirst()
        exchangeContractAddresses.cryptoPunks = cryptoPunksMarket.address()

        // Override asset make balance service to correctly reflect ownership of CryptoPunks.
        // By default, this service returns 1 for all ownerships, even if a punk does not belong to this address.
        coEvery { assetMakeBalanceProvider.getMakeBalance(any()) } coAnswers r@ {
            val order = arg<Order>(0)
            if (order.make.type is EthAssetType) {
                return@r MakeBalanceState(order.make.value)
            }
            val assetType = order.make.type as? CryptoPunksAssetType ?: return@r MakeBalanceState(EthUInt256.ONE)
            if (assetType.token != cryptoPunksMarket.address()) {
                return@r MakeBalanceState(EthUInt256.ONE)
            }
            val realOwner = cryptoPunksMarket.punkIndexToAddress(assetType.tokenId.value).awaitSingle()
            if (order.maker == realOwner) MakeBalanceState(EthUInt256.ONE) else MakeBalanceState(EthUInt256.ZERO)
        }
    }

    @Test
    fun `buy crypto punk which is on sale`() = runBlocking<Unit> {
        val (sellerAddress, sellerSender) = newSender()
        val punkIndex = 42.toBigInteger()
        cryptoPunksMarket.getPunk(punkIndex).withSender(sellerSender).execute().verifySuccess()

        val punkPrice = BigInteger.valueOf(100000)
        val listOrderTimestamp = cryptoPunksMarket.offerPunkForSale(punkIndex, punkPrice)
            .withSender(sellerSender).execute().verifySuccess().getTimestamp()

        val make = Asset(CryptoPunksAssetType(cryptoPunksMarket.address(), EthUInt256(punkIndex)), EthUInt256.ONE)
        val take = Asset(EthAssetType, EthUInt256(punkPrice))

        val punkPriceUsd = punkPrice.toBigDecimal(18) * 3000.toBigDecimal()

        val orderVersion = OrderVersion(
            maker = sellerAddress,
            taker = null,
            make = make,
            take = take,
            type = OrderType.CRYPTO_PUNKS,
            salt = CRYPTO_PUNKS_SALT,
            data = OrderCryptoPunksData,
            start = null,
            end = null,
            signature = null,
            createdAt = listOrderTimestamp,
            makePriceUsd = punkPriceUsd,
            takePriceUsd = null,
            makePrice = BigDecimal("1.00000E-13"),
            takePrice = punkPriceUsd,
            makeUsd = null,
            takeUsd = null,
            platform = Platform.CRYPTO_PUNKS
        )

        orderUpdateService.save(orderVersion)

        // Sell the punk.
        val (buyerAddress, buyerSender) = newSender()
        depositInitialBalance(buyerAddress, punkPrice)
        val receipt = cryptoPunksMarket.buyPunk(punkIndex).withSender(buyerSender).withValue(punkPrice).execute().verifySuccess()

        exchangeContractAddresses.cryptoPunks = cryptoPunksMarket.address()
        processTransaction(receipt, 2)

        val makeHash = Order.hashKey(sellerAddress, make.type, take.type, CRYPTO_PUNKS_SALT.value)
        val takeHash = Order.hashKey(buyerAddress, take.type, make.type, CRYPTO_PUNKS_SALT.value)

        for (hash in listOf(makeHash, takeHash)) {
            val history = exchangeHistoryRepository.findLogEvents(hash, null).collectList().awaitFirst()
            assertThat(history).hasSize(1)
            assertThat(history.single().status).isEqualTo(LogEventStatus.PENDING)
            assertThat(history.single().data).isInstanceOf(OrderSideMatch::class.java)
        }

        val savedOrder = orderRepository.findById(orderVersion.hash)
        assertThat(savedOrder?.pending).hasSize(1)
        assertThat(savedOrder?.pending?.single()).isInstanceOf(OrderSideMatch::class.java)
    }

    @Test
    fun `acceptBid crypto punk which is on sale`() = runBlocking<Unit> {
        val (sellerAddress, sellerSender) = newSender()
        val (buyerAddress, buyerSender) = newSender()
        val punkIndex = 42.toBigInteger()
        cryptoPunksMarket.getPunk(punkIndex).withSender(sellerSender).execute().verifySuccess()

        val punkPrice = BigInteger.valueOf(100000)
        val listOrderTimestamp = cryptoPunksMarket.offerPunkForSale(punkIndex, punkPrice)
            .withSender(sellerSender).execute().verifySuccess().getTimestamp()

        val make = Asset(CryptoPunksAssetType(cryptoPunksMarket.address(), EthUInt256(punkIndex)), EthUInt256.ONE)
        val take = Asset(EthAssetType, EthUInt256(punkPrice))

        val punkPriceUsd = punkPrice.toBigDecimal(18) * 3000.toBigDecimal()

        val orderVersion = OrderVersion(
            maker = buyerAddress,
            taker = null,
            make = take,
            take = make,
            type = OrderType.CRYPTO_PUNKS,
            salt = CRYPTO_PUNKS_SALT,
            data = OrderCryptoPunksData,
            start = null,
            end = null,
            signature = null,
            createdAt = listOrderTimestamp,
            makePriceUsd = punkPriceUsd,
            takePriceUsd = null,
            makePrice = BigDecimal("1.00000E-13"),
            takePrice = punkPriceUsd,
            makeUsd = null,
            takeUsd = null,
            platform = Platform.CRYPTO_PUNKS
        )

        orderUpdateService.save(orderVersion)

        exchangeContractAddresses.cryptoPunks = cryptoPunksMarket.address()

        // Bid the punk
        depositInitialBalance(buyerAddress, punkPrice)
        cryptoPunksMarket.enterBidForPunk(punkIndex).withSender(buyerSender).withValue(punkPrice).execute()
            .verifySuccess()

        // The new owner accepts the bid.
        val receipt = cryptoPunksMarket.acceptBidForPunk(punkIndex, punkPrice).withSender(sellerSender).execute().verifySuccess()

        processTransaction(receipt, 2)

        val makeHash = Order.hashKey(sellerAddress, make.type, take.type, CRYPTO_PUNKS_SALT.value)
        val takeHash = Order.hashKey(buyerAddress, take.type, make.type, CRYPTO_PUNKS_SALT.value)

        for (hash in listOf(makeHash, takeHash)) {
            val history = exchangeHistoryRepository.findLogEvents(hash, null).collectList().awaitFirst()
            assertThat(history).hasSize(1)
            assertThat(history.single().status).isEqualTo(LogEventStatus.PENDING)
            assertThat(history.single().data).isInstanceOf(OrderSideMatch::class.java)
        }

        val savedOrder = orderRepository.findById(orderVersion.hash)
        assertThat(savedOrder?.pending).hasSize(1)
        assertThat(savedOrder?.pending?.single()).isInstanceOf(OrderSideMatch::class.java)
    }

    private suspend fun processTransaction(receipt: TransactionReceipt, expectedSize: Int = 1) {
        val tx = ethereum.ethGetTransactionByHash(receipt.transactionHash()).awaitFirst().get()

        val transactions = transactionApi.createOrderPendingTransaction(tx.toRequest()).collectList().awaitFirst()

        assertThat(transactions).hasSize(expectedSize)
        assertThat(transactions).allSatisfy {
            assertThat(it.transactionHash).isEqualTo(tx.hash())
            assertThat(it.address).isEqualTo(tx.to())
            assertThat(it.status).isEqualTo(LogEventDto.Status.PENDING)
        }
    }

    private fun scalether.domain.response.Transaction.toRequest() = CreateTransactionRequestDto(
        hash = hash(),
        from = from(),
        input = input(),
        nonce = nonce().toLong(),
        to = to()
    )

    protected suspend fun depositInitialBalance(to: Address, amount: BigInteger) {
        val coinBaseWalletPrivateKey =
            BigInteger(Numeric.hexStringToByteArray("00120de4b1518cf1f16dc1b02f6b4a8ac29e870174cb1d8575f578480930250a"))
        val (coinBaseAddress, coinBaseSender) = newSender(coinBaseWalletPrivateKey)
        coinBaseSender.sendTransaction(
            scalether.domain.request.Transaction(
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
}
