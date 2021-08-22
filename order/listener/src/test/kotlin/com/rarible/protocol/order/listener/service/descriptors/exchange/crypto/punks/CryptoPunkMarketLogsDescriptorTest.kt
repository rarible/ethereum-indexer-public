package com.rarible.protocol.order.listener.service.descriptors.exchange.crypto.punks

import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.common.NewKeys
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.crypto.punks.CryptoPunksMarket
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.request.Transaction
import scalether.transaction.MonoGasPriceProvider
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger
import java.util.*

@IntegrationTest
@FlowPreview
class CryptoPunkMarketLogsDescriptorTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses

    @Test
    fun `PunkOffered event creates new sell Order, then PunkNoLongerForSale event cancels it`() = runBlocking {
        val market = deployCryptoPunkMarket()

        val (sellerAddress, sellerSender) = newSender()
        val punkIndex = 42.toBigInteger()
        market.getPunk(punkIndex).withSender(sellerSender).execute().verifySuccess()

        val minPrice = BigInteger.valueOf(100500)
        val sellTimestamp = market.offerPunkForSale(punkIndex, minPrice)
            .withSender(sellerSender).execute().verifySuccess().getTimestamp()

        val makeAsset = Asset(CryptoPunksAssetType(market.address(), punkIndex.toInt()), EthUInt256.ONE)
        val takeAsset = Asset(EthAssetType, EthUInt256(minPrice))

        val orderSalt = BigInteger.ZERO

        val expectedOrder = Order(
            maker = sellerAddress,
            taker = null,
            make = makeAsset,
            take = takeAsset,
            type = OrderType.CRYPTO_PUNKS,
            fill = EthUInt256.ZERO,
            cancelled = false,
            data = OrderCryptoPunksData(market.address()),

            // TODO[punk]: not sure about these values:
            makeStock = EthUInt256.ZERO,
            salt = EthUInt256(orderSalt),
            start = null,
            end = null,
            signature = null,
            createdAt = sellTimestamp,
            lastUpdateAt = sellTimestamp,
            pending = emptyList(),
            makePriceUsd = null,
            takePriceUsd = null,
            makeUsd = null,
            takeUsd = null,
            priceHistory = emptyList(),
            platform = Platform.CRYPTO_PUNKS,
            externalOrderExecutedOnRarible = null,

            version = 0
        )

        Wait.waitAssert {
            val order = orderRepository.findById(expectedOrder.hash)
            assertEquals(expectedOrder, order)
        }

        checkActivityWasPublished {
            this is OrderActivityListDto &&
                    date == sellTimestamp &&
                    source == OrderActivityDto.Source.CRYPTO_PUNKS &&
                    hash == expectedOrder.hash &&
                    maker == expectedOrder.maker &&
                    make.assetType is CryptoPunksAssetTypeDto &&
                    make.value == expectedOrder.make.value.value &&
                    take.assetType is EthAssetTypeDto &&
                    take.value == expectedOrder.take.value.value &&
                    price == minPrice.toBigDecimal(18) &&
                    priceUsd == null // TODO[punk]: not sure.
        }

        val cancelTimestamp = market.punkNoLongerForSale(punkIndex).withSender(sellerSender)
            .execute().verifySuccess().getTimestamp()
        Wait.waitAssert {
            val order = orderRepository.findById(expectedOrder.hash)
            assertEquals(expectedOrder.copy(cancelled = true, version = 1, lastUpdateAt = cancelTimestamp), order)
        }

        checkActivityWasPublished {
            this is OrderActivityCancelListDto &&
                    make is CryptoPunksAssetTypeDto &&
                    take is EthAssetTypeDto &&
                    maker == expectedOrder.maker &&
                    hash == expectedOrder.hash &&
                    date == cancelTimestamp &&
                    source == OrderActivityDto.Source.CRYPTO_PUNKS
        }
    }

    @Test
    fun `PunkBidEntered event creates a new bid Order then PunkBidWithdrawn event cancels it`() = runBlocking {
        val market = deployCryptoPunkMarket()

        val (_, ownerSender) = newSender()
        val punkIndex = 42.toBigInteger()
        market.getPunk(punkIndex).withSender(ownerSender).execute().verifySuccess()

        val (bidderAddress, bidderSender) = newSender()
        val bidPrice = 100500.toBigInteger()
        depositInitialBalance(bidderAddress, bidPrice)
        val bidTimestamp = market.enterBidForPunk(punkIndex).withSender(bidderSender).withValue(bidPrice)
            .execute().verifySuccess().getTimestamp()

        val makeAsset = Asset(EthAssetType, EthUInt256(bidPrice))
        val takeAsset = Asset(CryptoPunksAssetType(market.address(), punkIndex.toInt()), EthUInt256.ONE)

        val orderSalt = BigInteger.ZERO

        val expectedOrder = Order(
            maker = bidderAddress,
            taker = null,
            make = makeAsset,
            take = takeAsset,
            type = OrderType.CRYPTO_PUNKS,
            fill = EthUInt256.ZERO,
            cancelled = false,
            data = OrderCryptoPunksData(market.address()),

            // TODO[punk]: not sure about these values:
            makeStock = EthUInt256.ZERO,
            salt = EthUInt256(orderSalt),
            start = null,
            end = null,
            signature = null,
            createdAt = bidTimestamp,
            lastUpdateAt = bidTimestamp,
            pending = emptyList(),
            makePriceUsd = null,
            takePriceUsd = null,
            makeUsd = null,
            takeUsd = null,
            priceHistory = emptyList(),
            platform = Platform.CRYPTO_PUNKS,
            externalOrderExecutedOnRarible = null,

            version = 0
        )

        Wait.waitAssert {
            val order = orderRepository.findById(expectedOrder.hash)
            assertEquals(expectedOrder, order)
        }

        checkActivityWasPublished {
            this is OrderActivityBidDto &&
                    date == bidTimestamp &&
                    source == OrderActivityDto.Source.CRYPTO_PUNKS &&
                    hash == expectedOrder.hash &&
                    maker == expectedOrder.maker &&
                    make.assetType is EthAssetTypeDto &&
                    make.value == expectedOrder.make.value.value &&
                    take.assetType is CryptoPunksAssetTypeDto &&
                    take.value == expectedOrder.take.value.value &&
                    price == bidPrice.toBigDecimal(18) &&
                    priceUsd == null // TODO[punk]: not sure.
        }

        val cancelTimestamp = market.withdrawBidForPunk(punkIndex).withSender(bidderSender)
            .execute().verifySuccess().getTimestamp()

        Wait.waitAssert {
            val order = orderRepository.findById(expectedOrder.hash)
            assertEquals(expectedOrder.copy(cancelled = true, version = 1, lastUpdateAt = cancelTimestamp), order)
        }

        checkActivityWasPublished {
            this is OrderActivityCancelBidDto &&
                    make is EthAssetTypeDto &&
                    take is CryptoPunksAssetTypeDto &&
                    maker == expectedOrder.maker &&
                    hash == expectedOrder.hash &&
                    date == cancelTimestamp &&
                    source == OrderActivityDto.Source.CRYPTO_PUNKS
        }
    }

    @Test
    fun `PunkTransfer event creates order match with zero price`() = runBlocking {
        val market = deployCryptoPunkMarket()

        val (sellerAddress, sellerSender) = newSender()
        val punkIndex = 42.toBigInteger()
        market.getPunk(punkIndex).withSender(sellerSender).execute().verifySuccess()

        val (newOwnerAddress, _) = newSender()

        val transferTimestamp = market.transferPunk(newOwnerAddress, punkIndex).withSender(sellerSender)
            .execute().verifySuccess().getTimestamp()

        val marketAddress = market.address()
        val cryptoPunksAssetType = CryptoPunksAssetType(marketAddress, punkIndex.toInt())
        val make = Asset(cryptoPunksAssetType, EthUInt256.ONE)
        val take = Asset(EthAssetType, EthUInt256.ZERO /* Price = 0 */)
        val orderSalt = EthUInt256.ZERO

        val expectedMakeOrder = Order(
            maker = sellerAddress,
            taker = newOwnerAddress,
            make = make,
            take = take,
            type = OrderType.CRYPTO_PUNKS,
            cancelled = false,
            data = OrderCryptoPunksData(marketAddress),
            createdAt = transferTimestamp,
            lastUpdateAt = transferTimestamp,
            platform = Platform.CRYPTO_PUNKS,
            fill = EthUInt256.ZERO, // Filled! He hasn't demanded anything.

            // TODO[punk]: not sure about these values:
            makeStock = EthUInt256.ZERO,
            salt = orderSalt,
            start = null,
            end = null,
            signature = null,
            pending = emptyList(),
            makePriceUsd = null,
            takePriceUsd = null,
            makeUsd = null,
            takeUsd = null,
            priceHistory = emptyList(),
            externalOrderExecutedOnRarible = null,

            version = 0
        )
        val expectedTakeOrder = Order(
            maker = newOwnerAddress,
            taker = sellerAddress,
            make = take,
            take = make,
            type = OrderType.CRYPTO_PUNKS,
            cancelled = false,
            data = OrderCryptoPunksData(marketAddress),
            createdAt = transferTimestamp,
            lastUpdateAt = transferTimestamp,
            platform = Platform.CRYPTO_PUNKS,
            fill = EthUInt256.ONE, // Filled! He has received the punk.

            // TODO[punk]: not sure about these values:
            makeStock = EthUInt256.ZERO,
            salt = orderSalt,
            start = null,
            end = null,
            signature = null,
            pending = emptyList(),
            makePriceUsd = null,
            takePriceUsd = null,
            makeUsd = null,
            takeUsd = null,
            priceHistory = emptyList(),
            externalOrderExecutedOnRarible = null,

            version = 0
        )
        Wait.waitAssert {
            assertEquals(2, orderRepository.findAll().count())
            val makeOrder = orderRepository.findById(expectedMakeOrder.hash)
            val takeOrder = orderRepository.findById(expectedTakeOrder.hash)
            // Note that the 'version' of the 'expectedMakeOrder' hasn't changed because its 'fill' was 0 and became 0.
            assertEquals(expectedMakeOrder, makeOrder)
            assertEquals(expectedTakeOrder.copy(version = 1), takeOrder)
        }

        checkActivityWasPublished {
            this is OrderActivityMatchDto &&
                    date == transferTimestamp &&
                    source == OrderActivityDto.Source.CRYPTO_PUNKS &&

                    left.hash == expectedMakeOrder.hash &&
                    left.maker == expectedMakeOrder.maker &&
                    left.asset.assetType is CryptoPunksAssetTypeDto &&
                    left.asset.value == EthUInt256.ONE.value &&

                    right.hash == expectedTakeOrder.hash &&
                    right.maker == expectedTakeOrder.maker &&
                    right.asset.assetType is EthAssetTypeDto &&
                    right.asset.value == EthUInt256.ZERO.value &&

                    /*
                    TODO[punk]: discuss. By the time the OrderActivityMatchSideDto is constructed, the corresponding order
                    produced by PunkTransfer event is not yet inserted to the repository.
                    See `OrderActivityConverter.convertHistory`
                     */
                    left.type == null &&
                    right.type == null
        }
    }

    @Test
    fun `PunkBought event creates order match`() = runBlocking {
        val market = deployCryptoPunkMarket()
        val marketAddress = market.address()

        val (sellerAddress, sellerSender) = newSender()
        val punkIndex = 42.toBigInteger()
        market.getPunk(punkIndex).withSender(sellerSender).execute().verifySuccess()

        val (buyerAddress, buyerSender) = newSender()

        val punkPrice = BigInteger.valueOf(100500)
        depositInitialBalance(buyerAddress, punkPrice)

        // Place the punk for sale.
        val listSaleTimestamp = market.offerPunkForSale(punkIndex, punkPrice).withSender(sellerSender)
            .execute().verifySuccess().getTimestamp()

        // Buy the punk.
        val buyTimestamp = market.buyPunk(punkIndex).withSender(buyerSender).withValue(punkPrice)
            .execute().verifySuccess().getTimestamp()

        val make = Asset(CryptoPunksAssetType(marketAddress, punkIndex.toInt()), EthUInt256.ONE)
        val take = Asset(EthAssetType, EthUInt256(punkPrice))
        val orderSalt = EthUInt256.ZERO // TODO[punk]: not sure.

        val expectedSellOrder = Order(
            maker = sellerAddress,
            taker = null, // "Sell to anyone" order
            make = make,
            take = take,
            type = OrderType.CRYPTO_PUNKS,
            cancelled = false,
            data = OrderCryptoPunksData(marketAddress),
            createdAt = listSaleTimestamp,
            lastUpdateAt = buyTimestamp,
            platform = Platform.CRYPTO_PUNKS,
            fill = take.value, // Filled!

            // TODO[punk]: not sure about these values:
            makeStock = EthUInt256.ZERO,
            salt = orderSalt,
            start = null,
            end = null,
            signature = null,
            pending = emptyList(),
            makePriceUsd = null,
            takePriceUsd = null,
            makeUsd = null,
            takeUsd = null,
            priceHistory = emptyList(),
            externalOrderExecutedOnRarible = null,

            version = 1 // Changed after matching.
        )
        val expectedBuyOrder = Order(
            maker = buyerAddress,
            taker = sellerAddress,
            make = take,
            take = make,
            type = OrderType.CRYPTO_PUNKS,
            cancelled = false,
            data = OrderCryptoPunksData(marketAddress),
            createdAt = buyTimestamp,
            lastUpdateAt = buyTimestamp,
            platform = Platform.CRYPTO_PUNKS,
            fill = EthUInt256.ONE, // Filled!

            // TODO[punk]: not sure about these values:
            makeStock = EthUInt256.ZERO,
            salt = orderSalt,
            start = null,
            end = null,
            signature = null,
            pending = emptyList(),
            makePriceUsd = null,
            takePriceUsd = null,
            makeUsd = null,
            takeUsd = null,
            priceHistory = emptyList(),
            externalOrderExecutedOnRarible = null,

            version = 1 // Changed after matching.
        )

        Wait.waitAssert {
            val orders = orderRepository.findAll().toList()
            assertEquals(2, orders.size)
            val (sellOrder, buyOrder) = orders
            assertEquals(expectedSellOrder, sellOrder)
            assertEquals(expectedBuyOrder, buyOrder)
        }

        checkActivityWasPublished {
            this is OrderActivityMatchDto &&
                    date == buyTimestamp &&
                    source == OrderActivityDto.Source.CRYPTO_PUNKS &&

                    left.hash == expectedSellOrder.hash &&
                    left.maker == expectedSellOrder.maker &&
                    left.asset.assetType is CryptoPunksAssetTypeDto &&
                    left.asset.value == EthUInt256.ONE.value &&
                    left.type == OrderActivityMatchSideDto.Type.SELL &&

                    right.hash == expectedBuyOrder.hash &&
                    right.maker == expectedBuyOrder.maker &&
                    right.asset.assetType is EthAssetTypeDto &&
                    right.asset.value == punkPrice &&

                    /*
                    TODO[punk]: discuss. By the time the OrderActivityMatchSideDto is constructed, the corresponding order
                    produced by PunkBought event is not yet inserted to the repository.
                    See `OrderActivityConverter.convertHistory`
                     */
                    right.type == null
        }
    }

    protected suspend fun checkActivityWasPublished(predicate: ActivityDto.() -> Boolean) = coroutineScope {
        val activities = Collections.synchronizedList(arrayListOf<ActivityDto>())
        val job = async {
            consumer.receive().collect { activities.add(it.value) }
        }
        Wait.waitAssert {
            assertTrue(activities.any(predicate)) {
                "Searched-for activity is not found in\n" + activities.joinToString("\n")
            }
        }
        job.cancel()
    }

    private fun depositInitialBalance(to: Address, amount: BigInteger) {
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

    private suspend fun deployCryptoPunkMarket(): CryptoPunksMarket {
        val (_, creatorSender) = newSender()
        val market = CryptoPunksMarket.deployAndWait(creatorSender, poller).awaitFirst()
        market.allInitialOwnersAssigned().execute().awaitFirst()
        exchangeContractAddresses.cryptoPunks = market.address()
        return market
    }

    private fun newSender(privateKey0: BigInteger? = null): Pair<Address, MonoSigningTransactionSender> {
        val (privateKey, _, address) = generateNewKeys(privateKey0)
        val sender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )
        return address to sender
    }

    private fun generateNewKeys(privateKey0: BigInteger? = null): NewKeys {
        val privateKey = privateKey0 ?: Numeric.toBigInt(RandomUtils.nextBytes(32))
        val publicKey = Sign.publicKeyFromPrivate(privateKey)
        val signer = Address.apply(Keys.getAddressFromPrivateKey(privateKey))
        return NewKeys(privateKey, publicKey, signer)
    }

}