package com.rarible.protocol.order.listener.service.descriptors.exchange.crypto.punks

import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.listener.integration.IntegrationTest
import com.rarible.protocol.order.listener.integration.TestPropertiesConfiguration
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import scalether.domain.request.Transaction
import java.math.BigInteger

@IntegrationTest
@FlowPreview
class CryptoPunkOnChainOrderTest : AbstractCryptoPunkTest() {

    @Test
    fun `buy crypto punk which is on sale`() = runBlocking {
        val (sellerAddress, sellerSender) = newSender()
        val punkIndex = 42.toBigInteger()
        cryptoPunksMarket.getPunk(punkIndex).withSender(sellerSender).execute().verifySuccess()

        val punkPrice = BigInteger.valueOf(100500)
        val listOrderTimestamp = cryptoPunksMarket.offerPunkForSale(punkIndex, punkPrice)
            .withSender(sellerSender).execute().verifySuccess().getTimestamp()

        val make = Asset(CryptoPunksAssetType(cryptoPunksMarket.address(), EthUInt256(punkIndex)), EthUInt256.ONE)
        val take = Asset(EthAssetType, EthUInt256(punkPrice))

        val punkPriceUsd = punkPrice.toBigDecimal(18) * TestPropertiesConfiguration.ETH_CURRENCY_RATE

        val listOrder = Wait.waitFor { orderRepository.findActive().singleOrNull() }!!
        val expectedListOrder = Order(
            maker = sellerAddress,
            taker = null,
            make = make,
            take = take,
            type = OrderType.CRYPTO_PUNKS,
            fill = EthUInt256.ZERO,
            cancelled = false,
            data = OrderCryptoPunksData,

            makeStock = EthUInt256.ONE,
            salt = CRYPTO_PUNKS_SALT,
            start = null,
            end = null,
            signature = null,
            createdAt = listOrderTimestamp,
            lastUpdateAt = listOrderTimestamp,
            pending = emptyList(),
            makePriceUsd = punkPriceUsd,
            takePriceUsd = null,
            makeUsd = null,
            takeUsd = punkPriceUsd,
            priceHistory = createPriceHistory(listOrderTimestamp, make, take),
            platform = Platform.CRYPTO_PUNKS
        )
        assertEquals(expectedListOrder, listOrder)

        checkActivityWasPublished {
            this is OrderActivityListDto &&
                    date == listOrderTimestamp &&
                    source == OrderActivityDto.Source.CRYPTO_PUNKS &&
                    hash == expectedListOrder.hash &&
                    maker == expectedListOrder.maker &&
                    this.make.assetType is CryptoPunksAssetTypeDto &&
                    this.make.value == expectedListOrder.make.value.value &&
                    this.take.assetType is EthAssetTypeDto &&
                    this.take.value == expectedListOrder.take.value.value &&
                    price == punkPrice.toBigDecimal(18) &&
                    this.priceUsd == punkPriceUsd
        }

        val (buyerAddress, buyerSender) = newSender()
        depositInitialBalance(buyerAddress, punkPrice)

        val preparedBuyTx = prepareTxService.prepareTransaction(
            listOrder,
            PrepareOrderTxFormDto(buyerAddress, BigInteger.ONE, emptyList(), emptyList())
        )
        assertEquals(take, preparedBuyTx.asset)
        assertEquals(cryptoPunksMarket.address(), preparedBuyTx.transaction.to)
        val buyTimestamp = buyerSender.sendTransaction(
            Transaction(
                preparedBuyTx.transaction.to,
                buyerAddress,
                500000.toBigInteger(),
                BigInteger.ZERO,
                punkPrice,
                preparedBuyTx.transaction.data,
                null
            )
        ).verifySuccess().getTimestamp()

        val expectedSellOrder = expectedListOrder.copy(
            fill = take.value,
            makeStock = EthUInt256.ZERO,
            lastUpdateAt = buyTimestamp,
            taker = null
        )
        val expectedBuyOrder = Order(
            maker = buyerAddress,
            taker = sellerAddress,
            make = take,
            take = make,
            type = OrderType.CRYPTO_PUNKS,
            cancelled = false,
            data = OrderCryptoPunksData,
            createdAt = buyTimestamp,
            lastUpdateAt = buyTimestamp,
            platform = Platform.CRYPTO_PUNKS,
            fill = EthUInt256.ONE, // Filled!

            makeStock = EthUInt256.ZERO,
            salt = CRYPTO_PUNKS_SALT,
            start = null,
            end = null,
            signature = null,
            pending = emptyList(),
            makePriceUsd = null,
            takePriceUsd = punkPriceUsd,
            makeUsd = punkPriceUsd,
            takeUsd = null,
            priceHistory = createPriceHistory(buyTimestamp, take, make)
        )
        Wait.waitAssert {
            val orders = orderRepository.findAll().toList().sortedBy { it.createdAt }
            assertEquals(2, orders.size)
            val (sellOrder, buyOrder) = orders
            assertEquals(expectedSellOrder, sellOrder)
            assertEquals(expectedBuyOrder, buyOrder)
        }

        cryptoPunksMarket.withdraw().withSender(sellerSender).execute().verifySuccess()
        assertEquals(buyerAddress, cryptoPunksMarket.punkIndexToAddress(punkIndex).awaitSingle())
        assertEquals(punkPrice, getEthBalance(sellerAddress))
        assertEquals(BigInteger.ZERO, getEthBalance(buyerAddress))

        Wait.waitAssert {
            val items = exchangeHistoryRepository.findByItemType(ItemType.ORDER_SIDE_MATCH).collectList().awaitFirst()
            Assertions.assertThat(items).hasSize(3)
            Assertions.assertThat(items.filter { it.data is OnChainOrder }).hasSize(1)
            Assertions.assertThat(items.filter { it.data is OrderSideMatch }).hasSize(2)

            val sides = items.filter { it.data is OrderSideMatch }
                .map { it.data as OrderSideMatch }.associateBy { it.side }

            val left = sides.getValue(OrderSide.LEFT)
            val right = sides.getValue(OrderSide.RIGHT)

            assertEquals(sellerAddress, left.maker)
            assertEquals(buyerAddress, left.taker)
            assertEquals(make, left.make)
            assertEquals(take, left.take)
            assertEquals(true, left.externalOrderExecutedOnRarible)

            assertEquals(buyerAddress, right.maker)
            assertEquals(sellerAddress, right.taker)
            assertEquals(take, right.make)
            assertEquals(make, right.take)
            assertEquals(true, right.externalOrderExecutedOnRarible)
        }

        checkActivityWasPublished {
            this is OrderActivityMatchDto
                    && this.source == OrderActivityDto.Source.CRYPTO_PUNKS
                    && this.left.hash == listOrder.hash
                    && this.left.maker == sellerAddress

                    && this.right.hash == Order.hashKey(buyerAddress, take.type, make.type, listOrder.salt.value)
                    && this.right.maker == buyerAddress

                    && left.type == OrderActivityMatchSideDto.Type.SELL
                    && right.type == OrderActivityMatchSideDto.Type.BID
        }
    }

    @Test
    fun `cancel sell order for a crypto punk`() = runBlocking {
        val (sellerAddress, sellerSender) = newSender()
        val punkIndex = 42.toBigInteger()
        cryptoPunksMarket.getPunk(punkIndex).withSender(sellerSender).execute().verifySuccess()

        val punkPrice = BigInteger.valueOf(100500)
        cryptoPunksMarket.offerPunkForSale(punkIndex, punkPrice).withSender(sellerSender).execute().verifySuccess()
        val listOrder = Wait.waitFor { orderRepository.findActive().singleOrNull() }!!

        val preparedCancelTx = prepareTxService.prepareCancelTransaction(listOrder)
        assertEquals(cryptoPunksMarket.address(), preparedCancelTx.to)
        sellerSender.sendTransaction(
            Transaction(
                preparedCancelTx.to,
                sellerAddress,
                500000.toBigInteger(),
                BigInteger.ZERO,
                BigInteger.ZERO,
                preparedCancelTx.data,
                null
            )
        ).verifySuccess()

        Wait.waitAssert {
            val cancelledOrder = orderRepository.findById(listOrder.hash)
            assertNotNull(cancelledOrder)
            assertTrue(cancelledOrder!!.cancelled)
        }

        checkActivityWasPublished {
            this is OrderActivityCancelListDto &&
                    this.source == OrderActivityDto.Source.CRYPTO_PUNKS &&
                    this.hash == listOrder.hash &&
                    this.maker == sellerAddress &&
                    this.make is CryptoPunksAssetTypeDto &&
                    this.take is EthAssetTypeDto
        }
    }

    @Test
    fun `accept bid for a crypto punk`() = runBlocking {
        val (ownerAddress, ownerSender) = newSender()
        val punkIndex = 42.toBigInteger()
        cryptoPunksMarket.getPunk(punkIndex).withSender(ownerSender).execute().verifySuccess()

        val (bidderAddress, bidderSender) = newSender()
        val bidPrice = 100500.toBigInteger()
        depositInitialBalance(bidderAddress, bidPrice)
        val bidTimestamp = cryptoPunksMarket.enterBidForPunk(punkIndex).withSender(bidderSender).withValue(bidPrice)
            .execute().verifySuccess().getTimestamp()

        val bidOrder = Wait.waitFor { orderRepository.findActive().singleOrNull() }!!

        val bidMake = Asset(EthAssetType, EthUInt256(bidPrice))
        val bidTake = Asset(CryptoPunksAssetType(cryptoPunksMarket.address(), EthUInt256(punkIndex)), EthUInt256.ONE)

        val punkPriceUsd = bidPrice.toBigDecimal(18) * TestPropertiesConfiguration.ETH_CURRENCY_RATE

        val expectedBidOrder = Order(
            maker = bidderAddress,
            taker = null,
            make = bidMake,
            take = bidTake,
            type = OrderType.CRYPTO_PUNKS,
            fill = EthUInt256.ZERO,
            cancelled = false,
            data = OrderCryptoPunksData,

            makeStock = EthUInt256(bidPrice),
            salt = CRYPTO_PUNKS_SALT,
            start = null,
            end = null,
            signature = null,
            createdAt = bidTimestamp,
            lastUpdateAt = bidTimestamp,
            pending = emptyList(),
            makePriceUsd = null,
            takePriceUsd = punkPriceUsd,
            makeUsd = punkPriceUsd,
            takeUsd = null,
            priceHistory = createPriceHistory(bidTimestamp, bidMake, bidTake),
            platform = Platform.CRYPTO_PUNKS
        )
        assertEquals(expectedBidOrder, bidOrder)

        checkActivityWasPublished {
            this is OrderActivityBidDto
                    && this.hash == bidOrder.hash
                    && this.maker == bidderAddress
                    && this.make.assetType is EthAssetTypeDto
                    && this.make.value == bidPrice
                    && this.take.assetType is CryptoPunksAssetTypeDto
                    && this.source == OrderActivityDto.Source.CRYPTO_PUNKS
                    && this.price == bidPrice.toBigDecimal(18)
                    && this.priceUsd == punkPriceUsd
        }

        val preparedAcceptBidTx = prepareTxService.prepareTransaction(
            bidOrder,
            PrepareOrderTxFormDto(ownerAddress, BigInteger.ONE, emptyList(), emptyList())
        )
        assertEquals(bidTake, preparedAcceptBidTx.asset)
        assertEquals(cryptoPunksMarket.address(), preparedAcceptBidTx.transaction.to)
        val acceptBidTimestamp = ownerSender.sendTransaction(
            Transaction(
                preparedAcceptBidTx.transaction.to,
                ownerAddress,
                500000.toBigInteger(),
                BigInteger.ZERO,
                BigInteger.ZERO,
                preparedAcceptBidTx.transaction.data,
                null
            )
        ).verifySuccess().getTimestamp()

        val expectedBuyOrder = expectedBidOrder.copy(
            fill = bidTake.value,
            makeStock = EthUInt256.ZERO,
            lastUpdateAt = acceptBidTimestamp,
            taker = null
        )
        val expectedSellOrder = Order(
            maker = ownerAddress,
            taker = bidderAddress,
            make = bidTake,
            take = bidMake,
            type = OrderType.CRYPTO_PUNKS,
            cancelled = false,
            data = OrderCryptoPunksData,
            createdAt = acceptBidTimestamp,
            lastUpdateAt = acceptBidTimestamp,
            platform = Platform.CRYPTO_PUNKS,
            fill = bidMake.value,

            makeStock = EthUInt256.ZERO,
            salt = CRYPTO_PUNKS_SALT,
            start = null,
            end = null,
            signature = null,
            pending = emptyList(),
            makePriceUsd = punkPriceUsd,
            takePriceUsd = null,
            makeUsd = null,
            takeUsd = punkPriceUsd,
            priceHistory = createPriceHistory(acceptBidTimestamp, bidTake, bidMake)
        )
        Wait.waitAssert {
            val orders = orderRepository.findAll().toList().sortedBy { it.createdAt }
            assertEquals(2, orders.size)
            val (buyOrder, sellOrder) = orders
            assertEquals(expectedBuyOrder, buyOrder)
            assertEquals(expectedSellOrder, sellOrder)
        }

        cryptoPunksMarket.withdraw().withSender(ownerSender).execute().verifySuccess()
        assertEquals(bidderAddress, cryptoPunksMarket.punkIndexToAddress(punkIndex).awaitSingle())
        assertEquals(bidPrice, getEthBalance(ownerAddress))
        assertEquals(BigInteger.ZERO, getEthBalance(bidderAddress))

        Wait.waitAssert {
            val items = exchangeHistoryRepository.findByItemType(ItemType.ORDER_SIDE_MATCH).collectList().awaitFirst()
            Assertions.assertThat(items).hasSize(3)
            Assertions.assertThat(items.filter { it.data is OnChainOrder }).hasSize(1)
            Assertions.assertThat(items.filter { it.data is OrderSideMatch }).hasSize(2)

            val sides = items.filter { it.data is OrderSideMatch }
                .map { it.data as OrderSideMatch }
                .associateBy { it.side }

            val left = sides.getValue(OrderSide.LEFT)
            val right = sides.getValue(OrderSide.RIGHT)

            assertEquals(ownerAddress, left.maker)
            assertEquals(bidTake, left.make)
            assertEquals(bidderAddress, left.taker)
            assertEquals(true, left.externalOrderExecutedOnRarible)

            assertEquals(bidderAddress, right.maker)
            assertEquals(ownerAddress, right.taker)
            assertEquals(bidTake, right.take)
            assertEquals(true, right.externalOrderExecutedOnRarible)

            // Real buy/sell price may differ. See workaround at [CryptoPunkBoughtLogDescriptor].
            assertEquals(bidMake, left.take)
            assertEquals(bidMake, right.make)
        }

        checkActivityWasPublished {
            this is OrderActivityMatchDto
                    && this.source == OrderActivityDto.Source.CRYPTO_PUNKS
                    && this.left.hash == Order.hashKey(ownerAddress, bidTake.type, bidMake.type, bidOrder.salt.value)
                    && this.left.maker == ownerAddress

                    && this.right.hash == bidOrder.hash
                    && this.right.maker == bidderAddress
        }
    }

    @Test
    fun `cancel bid for a crypto punk`() = runBlocking {
        val (_, ownerSender) = newSender()
        val punkIndex = 42.toBigInteger()
        cryptoPunksMarket.getPunk(punkIndex).withSender(ownerSender).execute().verifySuccess()

        val (bidderAddress, bidderSender) = newSender()
        val bidPrice = 100500.toBigInteger()
        depositInitialBalance(bidderAddress, bidPrice)
        cryptoPunksMarket.enterBidForPunk(punkIndex).withSender(bidderSender).withValue(bidPrice)
            .execute().verifySuccess()
        val bidOrder = Wait.waitFor { orderRepository.findActive().singleOrNull() }!!

        val preparedCancelTx = prepareTxService.prepareCancelTransaction(bidOrder)
        assertEquals(cryptoPunksMarket.address(), preparedCancelTx.to)
        bidderSender.sendTransaction(
            Transaction(
                preparedCancelTx.to,
                bidderAddress,
                500000.toBigInteger(),
                BigInteger.ZERO,
                BigInteger.ZERO,
                preparedCancelTx.data,
                null
            )
        ).verifySuccess()

        Wait.waitAssert {
            val cancelledOrder = orderRepository.findById(bidOrder.hash)
            assertNotNull(cancelledOrder)
            assertTrue(cancelledOrder!!.cancelled)
        }

        checkActivityWasPublished {
            this is OrderActivityCancelBidDto &&
                    this.source == OrderActivityDto.Source.CRYPTO_PUNKS &&
                    this.hash == bidOrder.hash &&
                    this.maker == bidderAddress &&
                    this.make is EthAssetTypeDto &&
                    this.take is CryptoPunksAssetTypeDto
        }
    }

    @Test
    fun `crypto punk listed for sale to a specific address`() = runBlocking {
        val (sellerAddress, sellerSender) = newSender()
        val punkIndex = 42.toBigInteger()
        cryptoPunksMarket.getPunk(punkIndex).withSender(sellerSender).execute().verifySuccess()

        val (grantedAddress, _) = newSender()

        val punkPrice = BigInteger.valueOf(100500)
        val listOrderTimestamp = cryptoPunksMarket.offerPunkForSaleToAddress(punkIndex, punkPrice, grantedAddress)
            .withSender(sellerSender).execute().verifySuccess().getTimestamp()

        val make = Asset(CryptoPunksAssetType(cryptoPunksMarket.address(), EthUInt256(punkIndex)), EthUInt256.ONE)
        val take = Asset(EthAssetType, EthUInt256(punkPrice))

        val punkPriceUsd = punkPrice.toBigDecimal(18) * TestPropertiesConfiguration.ETH_CURRENCY_RATE

        val listOrder = Wait.waitFor { orderRepository.findActive().singleOrNull() }!!
        val expectedListOrder = Order(
            maker = sellerAddress,
            taker = grantedAddress, // 'taker' is defined as the granted address for this sale order.
            make = make,
            take = take,
            type = OrderType.CRYPTO_PUNKS,
            fill = EthUInt256.ZERO,
            cancelled = false,
            data = OrderCryptoPunksData,

            makeStock = EthUInt256.ONE,
            salt = CRYPTO_PUNKS_SALT,
            start = null,
            end = null,
            signature = null,
            createdAt = listOrderTimestamp,
            lastUpdateAt = listOrderTimestamp,
            pending = emptyList(),
            makePriceUsd = punkPriceUsd,
            takePriceUsd = null,
            makeUsd = null,
            takeUsd = punkPriceUsd,
            priceHistory = createPriceHistory(listOrderTimestamp, make, take),
            platform = Platform.CRYPTO_PUNKS
        )
        assertEquals(expectedListOrder, listOrder)

        // We can check here that 'OrderActivityListDto' with a specific granted address has been published,
        // but currently, such activities are ignored because frontend does not support "for-specific-address" sale orders.
    }

    @Test
    internal fun `sell order closed after punk transferring`() = runBlocking {
        val (_, sellerSender) = newSender()
        val punkIndex = 42.toBigInteger()
        cryptoPunksMarket.getPunk(punkIndex).withSender(sellerSender).execute().verifySuccess()

        val punkPrice = BigInteger.valueOf(100500)
        cryptoPunksMarket.offerPunkForSale(punkIndex, punkPrice)
            .withSender(sellerSender).execute().verifySuccess().getTimestamp()

        val sellOrder = Wait.waitFor { orderRepository.findActive().singleOrNull() }!!
        assertEquals(EthUInt256.ONE, sellOrder.makeStock)
        assertNull(sellOrder.taker)

        val (anotherAddress) = newSender()
        cryptoPunksMarket.transferPunk(anotherAddress, punkIndex).withSender(sellerSender).execute().verifySuccess()

        Wait.waitAssert {
            val order = orderRepository.findById(sellOrder.hash)
            assertNotNull(order)
            assertEquals(EthUInt256.ZERO, order!!.makeStock)
            // Order cancelled by 'punkNoLongerForSale' function called during the 'transferPunk' execution,
            assertTrue(order.cancelled)
        }
    }

    @Nested
    inner class OrderReopenedTest {
        @Test
        fun `sell order re-opened`() = runBlocking {
            val (_, ownerSender) = newSender()
            val punkIndex = 42.toBigInteger()
            cryptoPunksMarket.getPunk(punkIndex).withSender(ownerSender).execute().verifySuccess()

            val punkPrice = BigInteger.valueOf(100500)

            // List the punk for sale.
            cryptoPunksMarket.offerPunkForSale(punkIndex, punkPrice).withSender(ownerSender).execute().verifySuccess()
            val sellOrder = Wait.waitFor { orderRepository.findActive().single() }

            // Cancel the list order.
            cryptoPunksMarket.punkNoLongerForSale(punkIndex).withSender(ownerSender).execute().verifySuccess()
            Wait.waitAssert { assertTrue(orderRepository.findById(sellOrder.hash)!!.cancelled) }

            // List the punk for sale again => order is re-opened (cancelled = false).
            cryptoPunksMarket.offerPunkForSale(punkIndex, punkPrice).withSender(ownerSender).execute().verifySuccess()
            Wait.waitAssert { assertFalse(orderRepository.findById(sellOrder.hash)!!.cancelled) }
        }

        @Test
        fun `bid order re-opened`() = runBlocking {
            val (_, ownerSender) = newSender()
            val punkIndex = 42.toBigInteger()
            cryptoPunksMarket.getPunk(punkIndex).withSender(ownerSender).execute().verifySuccess()

            val (bidderAddress, bidderSender) = newSender()
            val bidPrice = BigInteger.valueOf(100500)
            depositInitialBalance(bidderAddress, bidPrice)

            // Bid the punk.
            cryptoPunksMarket.enterBidForPunk(punkIndex).withSender(bidderSender).withValue(bidPrice).execute()
                .verifySuccess()
            val bidOrder = Wait.waitFor { orderRepository.findActive().single() }

            // Cancel the bid order.
            cryptoPunksMarket.withdrawBidForPunk(punkIndex).withSender(bidderSender).execute().verifySuccess()
            Wait.waitAssert { assertTrue(orderRepository.findById(bidOrder.hash)!!.cancelled) }

            // Bid the punk again => order is re-opened (cancelled = false).
            cryptoPunksMarket.enterBidForPunk(punkIndex).withSender(bidderSender).withValue(bidPrice).execute()
                .verifySuccess()
            Wait.waitAssert { assertFalse(orderRepository.findById(bidOrder.hash)!!.cancelled) }
        }

        @Test
        fun `punk sold then bought by the same user and put on sale again`() = runBlocking {
            val (ownerAddress, ownerSender) = newSender()
            val punkIndex = 42.toBigInteger()
            cryptoPunksMarket.getPunk(punkIndex).withSender(ownerSender).execute().verifySuccess()

            val punkPrice = BigInteger.valueOf(100500)

            // List the punk for sale.
            cryptoPunksMarket.offerPunkForSale(punkIndex, punkPrice).withSender(ownerSender).execute().verifySuccess()

            // Sell the punk.
            val (buyerAddress, buyerSender) = newSender()
            depositInitialBalance(buyerAddress, punkPrice)
            cryptoPunksMarket.buyPunk(punkIndex).withSender(buyerSender).withValue(punkPrice).execute().verifySuccess()
            cryptoPunksMarket.withdraw().withSender(ownerSender).execute().verifySuccess()

            // Bid the punk back.
            cryptoPunksMarket.enterBidForPunk(punkIndex).withSender(ownerSender).withValue(punkPrice).execute()
                .verifySuccess()

            // The new owner accepts the bid.
            cryptoPunksMarket.acceptBidForPunk(punkIndex, punkPrice).withSender(buyerSender).execute().verifySuccess()

            // List the punk for sale again with higher price.
            val newPrice = punkPrice.multiply(BigInteger.valueOf(2))
            val listOrderTimestamp = cryptoPunksMarket.offerPunkForSale(punkIndex, newPrice).withSender(ownerSender)
                .execute().verifySuccess().getTimestamp()
            Wait.waitAssert {
                val activeOrders = orderRepository.findActive().toList()
                assertEquals(1, activeOrders.size)
                val sellOrder = activeOrders.single()
                assertEquals(listOrderTimestamp, sellOrder.createdAt)
                assertFalse(sellOrder.cancelled)
                assertEquals(ownerAddress, sellOrder.maker)
                assertEquals(
                    Asset(
                        CryptoPunksAssetType(cryptoPunksMarket.address(), EthUInt256(punkIndex)),
                        EthUInt256.ONE
                    ), sellOrder.make
                )
                assertNull(sellOrder.taker)
                assertEquals(Asset(EthAssetType, EthUInt256(newPrice)), sellOrder.take)
            }
        }
    }
}
