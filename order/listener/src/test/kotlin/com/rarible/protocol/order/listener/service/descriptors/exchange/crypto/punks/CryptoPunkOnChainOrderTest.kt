package com.rarible.protocol.order.listener.service.descriptors.exchange.crypto.punks

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.CryptoPunksAssetTypeDto
import com.rarible.protocol.dto.EthAssetTypeDto
import com.rarible.protocol.dto.OrderActivityBidDto
import com.rarible.protocol.dto.OrderActivityCancelBidDto
import com.rarible.protocol.dto.OrderActivityCancelListDto
import com.rarible.protocol.dto.OrderActivityDto
import com.rarible.protocol.dto.OrderActivityListDto
import com.rarible.protocol.dto.OrderActivityMatchDto
import com.rarible.protocol.dto.OrderActivityMatchSideDto
import com.rarible.protocol.dto.PrepareOrderTxFormDto
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.CRYPTO_PUNKS_SALT
import com.rarible.protocol.order.core.model.CryptoPunksAssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.ItemType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderCryptoPunksData
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.listener.integration.IntegrationTest
import com.rarible.protocol.order.listener.integration.TestPropertiesConfiguration
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import scalether.domain.request.Transaction
import java.math.BigDecimal
import java.math.BigInteger

@IntegrationTest
class CryptoPunkOnChainOrderTest : AbstractCryptoPunkTest() {

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
            makePrice = BigDecimal("1.00000E-13"),
            makeUsd = null,
            takeUsd = punkPriceUsd,
            priceHistory = createPriceHistory(listOrderTimestamp, make, take),
            platform = Platform.CRYPTO_PUNKS,
            lastEventId = listOrder.lastEventId
        )
        assertThat(listOrder).isEqualTo(expectedListOrder)

        checkActivityWasPublished {
            assertThat(this).isInstanceOfSatisfying(OrderActivityListDto::class.java) {
                assertThat(it.date).isEqualTo(listOrderTimestamp)
                assertThat(it.source).isEqualTo(OrderActivityDto.Source.CRYPTO_PUNKS)
                assertThat(it.hash).isEqualTo(expectedListOrder.hash)
                assertThat(it.maker).isEqualTo(expectedListOrder.maker)
                assertThat(it.make.assetType).isInstanceOf(CryptoPunksAssetTypeDto::class.java)
                assertThat(it.make.value).isEqualTo(expectedListOrder.make.value.value)
                assertThat(it.take.assetType).isInstanceOf(EthAssetTypeDto::class.java)
                assertThat(it.take.value).isEqualTo(expectedListOrder.take.value.value)
                assertThat(it.price).isEqualTo(punkPrice.toBigDecimal(18))
                assertThat(it.priceUsd).isEqualTo(punkPriceUsd)
            }
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

        Wait.waitAssert {
            val orders = orderRepository.findAll().toList().sortedBy { it.createdAt }
            assertEquals(1, orders.size)
            val sellOrder = orders.single()

            val expectedSellOrder = expectedListOrder.copy(
                fill = take.value,
                makeStock = EthUInt256.ZERO,
                lastUpdateAt = buyTimestamp,
                taker = null,
                lastEventId = sellOrder.lastEventId
            )
            assertThat(sellOrder).isEqualTo(expectedSellOrder)
        }

        cryptoPunksMarket.withdraw().withSender(sellerSender).execute().verifySuccess()
        assertEquals(buyerAddress, cryptoPunksMarket.punkIndexToAddress(punkIndex).awaitSingle())
        assertEquals(punkPrice, getEthBalance(sellerAddress))
        assertEquals(BigInteger.ZERO, getEthBalance(buyerAddress))

        Wait.waitAssert {
            val items = exchangeHistoryRepository.findByItemType(ItemType.ORDER_SIDE_MATCH).collectList().awaitFirst()
            assertThat(items).hasSize(2)

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

            assertFalse(left.adhoc!!)
            assertTrue(left.counterAdhoc!!)

            assertTrue(right.adhoc!!)
            assertFalse(right.counterAdhoc!!)
        }

        checkActivityWasPublished {
            assertThat(this).isInstanceOfSatisfying(OrderActivityMatchDto::class.java) {
                assertThat(it.source).isEqualTo(OrderActivityDto.Source.CRYPTO_PUNKS)
                assertThat(it.left.hash).isEqualTo(listOrder.hash)
                assertThat(it.left.maker).isEqualTo(sellerAddress)

                assertThat(it.right.hash).isEqualTo(
                    Order.hashKey(
                        buyerAddress,
                        take.type,
                        make.type,
                        listOrder.salt.value
                    )
                )
                assertThat(it.right.maker).isEqualTo(buyerAddress)

                assertThat(it.left.type).isEqualTo(OrderActivityMatchSideDto.Type.SELL)
                assertThat(it.right.type).isEqualTo(OrderActivityMatchSideDto.Type.BID)
            }
        }
    }

    @Test
    fun `cancel sell order for a crypto punk`() = runBlocking<Unit> {
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
            assertThat(this).isInstanceOfSatisfying(OrderActivityCancelListDto::class.java) {
                assertThat(it.source).isEqualTo(OrderActivityDto.Source.CRYPTO_PUNKS)
                assertThat(it.hash).isEqualTo(listOrder.hash)
                assertThat(it.maker).isEqualTo(sellerAddress)
                assertThat(it.make).isInstanceOf(CryptoPunksAssetTypeDto::class.java)
                assertThat(it.take).isInstanceOf(EthAssetTypeDto::class.java)
            }
        }
    }

    @Test
    fun `create sell order and then change price`() = runBlocking<Unit> {
        val (sellerAddress, sellerSender) = newSender()
        val punkIndex = 42.toBigInteger()
        cryptoPunksMarket.getPunk(punkIndex).withSender(sellerSender).execute().verifySuccess()

        val firstPrice = BigInteger.valueOf(100000)
        val firstSellTimestamp = cryptoPunksMarket.offerPunkForSale(punkIndex, firstPrice)
            .withSender(sellerSender).execute().verifySuccess().getTimestamp()

        val firstPriceUsd = firstPrice.toBigDecimal(18) * TestPropertiesConfiguration.ETH_CURRENCY_RATE
        val firstMakePrice = firstPrice.toBigDecimal(18)

        val make = Asset(CryptoPunksAssetType(cryptoPunksMarket.address(), EthUInt256(punkIndex)), EthUInt256.ONE)
        val take = Asset(EthAssetType, EthUInt256(firstPrice))

        val firstOrder = Wait.waitFor { orderRepository.findActive().singleOrNull() }!!
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
            createdAt = firstSellTimestamp,
            lastUpdateAt = firstSellTimestamp,
            pending = emptyList(),
            makePriceUsd = firstPriceUsd,
            takePriceUsd = null,
            makePrice = firstMakePrice,
            makeUsd = null,
            takeUsd = firstPriceUsd,
            priceHistory = createPriceHistory(firstSellTimestamp, make, take),
            platform = Platform.CRYPTO_PUNKS,
            lastEventId = firstOrder.lastEventId
        )
        assertThat(firstOrder).isEqualTo(expectedListOrder)

        // Make the second sell order with new price.
        val secondPrice = BigInteger.valueOf(200000)
        val secondSellTimestamp = cryptoPunksMarket.offerPunkForSale(punkIndex, secondPrice)
            .withSender(sellerSender).execute().verifySuccess().getTimestamp()

        Wait.waitAssert {
            val secondOrder = Wait.waitFor { orderRepository.findActive().singleOrNull() }!!
            val secondPriceUsd = secondPrice.toBigDecimal(18) * TestPropertiesConfiguration.ETH_CURRENCY_RATE
            val secondMakePrice = secondPrice.toBigDecimal(18)
            val secondTake = take.copy(value = EthUInt256(secondPrice))
            assertThat(secondOrder).isEqualTo(
                expectedListOrder.copy(
                    take = secondTake,
                    createdAt = secondSellTimestamp,
                    lastUpdateAt = secondSellTimestamp,
                    makePriceUsd = secondPriceUsd,
                    takeUsd = secondPriceUsd,
                    makePrice = secondMakePrice,
                    priceHistory = createPriceHistory(secondSellTimestamp, make, secondTake),
                    lastEventId = secondOrder.lastEventId
                )
            )
        }
    }

    @Test
    fun `accept bid for a crypto punk`() = runBlocking<Unit> {
        val (ownerAddress, ownerSender) = newSender()
        val punkIndex = 42.toBigInteger()
        cryptoPunksMarket.getPunk(punkIndex).withSender(ownerSender).execute().verifySuccess()

        val (bidderAddress, bidderSender) = newSender()
        val bidPrice = 100000.toBigInteger()
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
            takePrice = BigDecimal("1.00000E-13"),
            makeUsd = punkPriceUsd,
            takeUsd = null,
            priceHistory = createPriceHistory(bidTimestamp, bidMake, bidTake),
            platform = Platform.CRYPTO_PUNKS,
            lastEventId = bidOrder.lastEventId
        )
        assertThat(bidOrder).isEqualTo(expectedBidOrder)

        checkActivityWasPublished {
            assertThat(this).isInstanceOfSatisfying(OrderActivityBidDto::class.java) {
                assertThat(it.source).isEqualTo(OrderActivityDto.Source.CRYPTO_PUNKS)
                assertThat(it.hash).isEqualTo(bidOrder.hash)
                assertThat(it.maker).isEqualTo(bidderAddress)
                assertThat(it.make.assetType).isInstanceOf(EthAssetTypeDto::class.java)
                assertThat(it.make.value).isEqualTo(bidPrice)
                assertThat(it.take.assetType).isInstanceOf(CryptoPunksAssetTypeDto::class.java)
                assertThat(it.price).isEqualTo(bidPrice.toBigDecimal(18))
                assertThat(it.priceUsd).isEqualTo(punkPriceUsd)
            }
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

        Wait.waitAssert {
            val orders = orderRepository.findAll().toList().sortedBy { it.createdAt }
            assertThat(orders).hasSize(1)
            val buyOrder = orders.single()

            val expectedBuyOrder = expectedBidOrder.copy(
                fill = bidTake.value,
                makeStock = EthUInt256.ZERO,
                lastUpdateAt = acceptBidTimestamp,
                taker = null,
                lastEventId = buyOrder.lastEventId
            )
            assertThat(buyOrder).isEqualTo(expectedBuyOrder)
        }

        cryptoPunksMarket.withdraw().withSender(ownerSender).execute().verifySuccess()
        assertEquals(bidderAddress, cryptoPunksMarket.punkIndexToAddress(punkIndex).awaitSingle())
        assertEquals(bidPrice, getEthBalance(ownerAddress))
        assertEquals(BigInteger.ZERO, getEthBalance(bidderAddress))

        Wait.waitAssert {
            val items = exchangeHistoryRepository.findByItemType(ItemType.ORDER_SIDE_MATCH).collectList().awaitFirst()
            assertThat(items).hasSize(2)

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

            assertTrue(left.adhoc!!)
            assertFalse(left.counterAdhoc!!)

            assertFalse(right.adhoc!!)
            assertTrue(right.counterAdhoc!!)
        }

        checkActivityWasPublished {
            assertThat(this).isInstanceOfSatisfying(OrderActivityMatchDto::class.java) {
                assertThat(it.source).isEqualTo(OrderActivityDto.Source.CRYPTO_PUNKS)
                assertThat(it.left.hash).isEqualTo(
                    Order.hashKey(
                        ownerAddress,
                        bidTake.type,
                        bidMake.type,
                        bidOrder.salt.value
                    )
                )
                assertThat(it.left.maker).isEqualTo(ownerAddress)

                assertThat(it.right.hash).isEqualTo(bidOrder.hash)
                assertThat(it.right.maker).isEqualTo(bidderAddress)
            }
        }
    }

    @Test
    fun `cancel bid for a crypto punk`() = runBlocking<Unit> {
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
            assertThat(this).isInstanceOfSatisfying(OrderActivityCancelBidDto::class.java) {
                assertThat(it.source).isEqualTo(OrderActivityDto.Source.CRYPTO_PUNKS)
                assertThat(it.hash).isEqualTo(bidOrder.hash)
                assertThat(it.maker).isEqualTo(bidderAddress)
                assertThat(it.make).isInstanceOf(EthAssetTypeDto::class.java)
                assertThat(it.take).isInstanceOf(CryptoPunksAssetTypeDto::class.java)
            }
        }
    }

    @Test
    fun `another user makes a bid with higher price, then the first bid must be cancelled`() = runBlocking<Unit> {
        val (_, ownerSender) = newSender()
        val punkIndex = 42.toBigInteger()
        cryptoPunksMarket.getPunk(punkIndex).withSender(ownerSender).execute().verifySuccess()

        val (firstBidderAddress, firstBidderSender) = newSender()
        val firstBidPrice = 100000.toBigInteger()
        depositInitialBalance(firstBidderAddress, firstBidPrice)
        val firstBidTimestamp =
            cryptoPunksMarket.enterBidForPunk(punkIndex).withSender(firstBidderSender).withValue(firstBidPrice)
                .execute().verifySuccess().getTimestamp()
        val firstBidOrder = Wait.waitFor { orderRepository.findActive().singleOrNull() }!!

        val firstBidMake = Asset(EthAssetType, EthUInt256(firstBidPrice))
        val bidTake = Asset(CryptoPunksAssetType(cryptoPunksMarket.address(), EthUInt256(punkIndex)), EthUInt256.ONE)

        val firstBidTakePriceUsd = firstBidPrice.toBigDecimal(18) * TestPropertiesConfiguration.ETH_CURRENCY_RATE

        val expectedFirstBidOrder = Order(
            maker = firstBidderAddress,
            taker = null,
            make = firstBidMake,
            take = bidTake,
            type = OrderType.CRYPTO_PUNKS,
            fill = EthUInt256.ZERO,
            cancelled = false,
            data = OrderCryptoPunksData,

            makeStock = EthUInt256(firstBidPrice),
            salt = CRYPTO_PUNKS_SALT,
            start = null,
            end = null,
            signature = null,
            createdAt = firstBidTimestamp,
            lastUpdateAt = firstBidTimestamp,
            pending = emptyList(),
            makePriceUsd = null,
            takePriceUsd = firstBidTakePriceUsd,
            takePrice = BigDecimal("1.00000E-13"),
            makeUsd = firstBidTakePriceUsd,
            takeUsd = null,
            priceHistory = createPriceHistory(firstBidTimestamp, firstBidMake, bidTake),
            platform = Platform.CRYPTO_PUNKS,
            lastEventId = firstBidOrder.lastEventId
        )
        assertThat(firstBidOrder).isEqualTo(expectedFirstBidOrder)

        // Second user makes a bid with higher price => th first user's bid must be cancelled.
        val (secondBidderAddress, secondBidderSender) = newSender()
        val secondBidPrice = 200000.toBigInteger()
        depositInitialBalance(secondBidderAddress, secondBidPrice)
        val secondBidTimestamp =
            cryptoPunksMarket.enterBidForPunk(punkIndex).withSender(secondBidderSender).withValue(secondBidPrice)
                .execute().verifySuccess().getTimestamp()

        val secondMake = firstBidMake.copy(value = EthUInt256(secondBidPrice))
        val secondBidTakePriceUsd = secondBidPrice.toBigDecimal(18) * TestPropertiesConfiguration.ETH_CURRENCY_RATE
        val secondTakePrice = secondBidPrice.toBigDecimal(18)
        val secondBidHash = Order.hashKey(secondBidderAddress, secondMake.type, bidTake.type, CRYPTO_PUNKS_SALT.value)

        Wait.waitAssert {
            val firstBidCancelledOrder = orderRepository.findById(firstBidOrder.hash)
            assertThat(firstBidCancelledOrder).isEqualTo(
                firstBidOrder.copy(
                    cancelled = true,
                    status = OrderStatus.CANCELLED,
                    makeStock = EthUInt256.ZERO,
                    lastUpdateAt = secondBidTimestamp,
                    lastEventId = firstBidCancelledOrder?.lastEventId
                )
            )

            val activeOrders = orderRepository.findActive().toList()
            assertThat(activeOrders).hasSize(1)

            val secondBidOrder = activeOrders.single()
            assertThat(secondBidOrder).isEqualTo(
                expectedFirstBidOrder.copy(
                    maker = secondBidderAddress,
                    make = secondMake,
                    hash = secondBidHash,
                    makeStock = secondMake.value,

                    createdAt = secondBidTimestamp,
                    lastUpdateAt = secondBidTimestamp,
                    takePriceUsd = secondBidTakePriceUsd,
                    makeUsd = secondBidTakePriceUsd,
                    takePrice = secondTakePrice,
                    priceHistory = createPriceHistory(secondBidTimestamp, secondMake, bidTake),
                    lastEventId = secondBidOrder.lastEventId
                )
            )
        }

        checkActivityWasPublished {
            assertThat(this).isInstanceOfSatisfying(OrderActivityBidDto::class.java) {
                assertThat(it.hash).isEqualTo(secondBidHash)
            }
        }

        checkActivityWasPublished {
            assertThat(this).isInstanceOfSatisfying(OrderActivityCancelBidDto::class.java) {
                assertThat(it.hash).isEqualTo(firstBidOrder.hash)
            }
        }
    }

    @Test
    fun `transfer punk to bidder while the bid is active, must cancel the bid`() = runBlocking<Unit> {
        val (_, ownerSender) = newSender()
        val punkIndex = 42.toBigInteger()
        cryptoPunksMarket.getPunk(punkIndex).withSender(ownerSender).execute().verifySuccess()

        val (bidderAddress, bidderSender) = newSender()
        val bidPrice = 100000.toBigInteger()
        depositInitialBalance(bidderAddress, bidPrice)
        val bidTimestamp = cryptoPunksMarket.enterBidForPunk(punkIndex)
            .withSender(bidderSender).withValue(bidPrice)
            .execute().verifySuccess().getTimestamp()
        val bidOrder = Wait.waitFor { orderRepository.findActive().singleOrNull() }!!

        val bidMake = Asset(EthAssetType, EthUInt256(bidPrice))
        val bidTake = Asset(CryptoPunksAssetType(cryptoPunksMarket.address(), EthUInt256(punkIndex)), EthUInt256.ONE)

        val bidTakePriceUsd = bidPrice.toBigDecimal(18) * TestPropertiesConfiguration.ETH_CURRENCY_RATE

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
            takePriceUsd = bidTakePriceUsd,
            takePrice = BigDecimal("1.00000E-13"),
            makeUsd = bidTakePriceUsd,
            takeUsd = null,
            priceHistory = createPriceHistory(bidTimestamp, bidMake, bidTake),
            platform = Platform.CRYPTO_PUNKS,
            lastEventId = bidOrder.lastEventId
        )
        assertThat(bidOrder).isEqualTo(expectedBidOrder)

        // Transfer punk to the bidder for free.
        val transferTimestamp = cryptoPunksMarket.transferPunk(bidderAddress, punkIndex)
            .withSender(ownerSender).execute().verifySuccess().getTimestamp()

        Wait.waitAssert {
            val bidCancelledOrder = orderRepository.findById(bidOrder.hash)
            assertThat(bidCancelledOrder).isEqualTo(
                bidOrder.copy(
                    cancelled = true,
                    status = OrderStatus.CANCELLED,
                    makeStock = EthUInt256.ZERO,
                    lastUpdateAt = transferTimestamp,
                    lastEventId = bidCancelledOrder?.lastEventId
                )
            )
        }
    }

    @Test
    fun `transfer punk from seller must cancel the sell order`() = runBlocking<Unit> {
        val (sellerAddress, sellerSender) = newSender()
        val punkIndex = 42.toBigInteger()
        cryptoPunksMarket.getPunk(punkIndex).withSender(sellerSender).execute().verifySuccess()

        val sellPrice = BigInteger.valueOf(100000)
        val sellTimestamp = cryptoPunksMarket.offerPunkForSale(punkIndex, sellPrice)
            .withSender(sellerSender).execute().verifySuccess().getTimestamp()

        val priceUsd = sellPrice.toBigDecimal(18) * TestPropertiesConfiguration.ETH_CURRENCY_RATE
        val makePrice = sellPrice.toBigDecimal(18)

        val make = Asset(CryptoPunksAssetType(cryptoPunksMarket.address(), EthUInt256(punkIndex)), EthUInt256.ONE)
        val take = Asset(EthAssetType, EthUInt256(sellPrice))

        val sellOrder = Wait.waitFor { orderRepository.findActive().singleOrNull() }!!
        val expectedSellOrder = Order(
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
            createdAt = sellTimestamp,
            lastUpdateAt = sellTimestamp,
            pending = emptyList(),
            makePriceUsd = priceUsd,
            takePriceUsd = null,
            makePrice = makePrice,
            makeUsd = null,
            takeUsd = priceUsd,
            priceHistory = createPriceHistory(sellTimestamp, make, take),
            platform = Platform.CRYPTO_PUNKS,
            lastEventId = sellOrder.lastEventId
        )
        assertThat(sellOrder).isEqualTo(expectedSellOrder)

        val (newOwnerAddress) = newSender()

        // Transfer punk to a new owner for free => it must cancel the sell order.
        val transactionReceipt = cryptoPunksMarket.transferPunk(newOwnerAddress, punkIndex)
            .withSender(sellerSender).execute().verifySuccess()
        val transferTimestamp = transactionReceipt.getTimestamp()

        Wait.waitAssert {
            val sellCancelledOrder = orderRepository.findById(sellOrder.hash)
            assertThat(sellCancelledOrder).isEqualTo(
                sellOrder.copy(
                    cancelled = true,
                    status = OrderStatus.CANCELLED,
                    makeStock = EthUInt256.ZERO,
                    lastUpdateAt = transferTimestamp,
                    lastEventId = sellCancelledOrder?.lastEventId
                )
            )
        }

        checkActivityWasPublished {
            assertThat(this).isInstanceOfSatisfying(OrderActivityCancelListDto::class.java) {
                assertThat(it.source).isEqualTo(OrderActivityDto.Source.CRYPTO_PUNKS)
                assertThat(it.hash).isEqualTo(sellOrder.hash)
            }
        }
    }

    @Test
    fun `sell order created, then bid order created, accept bid partially fills the sell order`() = runBlocking<Unit> {
        val (sellerAddress, sellerSender) = newSender()
        val punkIndex = 42.toBigInteger()
        cryptoPunksMarket.getPunk(punkIndex).withSender(sellerSender).execute().verifySuccess()

        val sellPrice = BigInteger.valueOf(200000)
        val sellTimestamp = cryptoPunksMarket.offerPunkForSale(punkIndex, sellPrice)
            .withSender(sellerSender).execute().verifySuccess().getTimestamp()

        val priceUsd = sellPrice.toBigDecimal(18) * TestPropertiesConfiguration.ETH_CURRENCY_RATE
        val makePrice = sellPrice.toBigDecimal(18)

        val make = Asset(CryptoPunksAssetType(cryptoPunksMarket.address(), EthUInt256(punkIndex)), EthUInt256.ONE)
        val take = Asset(EthAssetType, EthUInt256(sellPrice))

        val sellOrder = Wait.waitFor { orderRepository.findActive().singleOrNull() }!!
        val expectedSellOrder = Order(
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
            createdAt = sellTimestamp,
            lastUpdateAt = sellTimestamp,
            pending = emptyList(),
            makePriceUsd = priceUsd,
            takePriceUsd = null,
            makePrice = makePrice,
            makeUsd = null,
            takeUsd = priceUsd,
            priceHistory = createPriceHistory(sellTimestamp, make, take),
            platform = Platform.CRYPTO_PUNKS,
            lastEventId = sellOrder.lastEventId
        )
        assertThat(sellOrder).isEqualTo(expectedSellOrder)

        val (bidderAddress, bidderSender) = newSender()
        val bidPrice = 100000.toBigInteger()
        depositInitialBalance(bidderAddress, bidPrice)
        val bidTimestamp = cryptoPunksMarket.enterBidForPunk(punkIndex).withSender(bidderSender).withValue(bidPrice)
            .execute().verifySuccess().getTimestamp()
        val bidOrder = Wait.waitFor {
            val activeOrders = orderRepository.findActive().toList()
            (activeOrders - sellOrder).singleOrNull()
        }!!

        val bidMake = Asset(EthAssetType, EthUInt256(bidPrice))
        val bidTake = Asset(CryptoPunksAssetType(cryptoPunksMarket.address(), EthUInt256(punkIndex)), EthUInt256.ONE)

        val bidTakePriceUsd = bidPrice.toBigDecimal(18) * TestPropertiesConfiguration.ETH_CURRENCY_RATE
        val bidTakePrice = BigDecimal("1.00000E-13")

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
            takePriceUsd = bidTakePriceUsd,
            takePrice = bidTakePrice,
            makeUsd = bidTakePriceUsd,
            takeUsd = null,
            priceHistory = createPriceHistory(bidTimestamp, bidMake, bidTake),
            platform = Platform.CRYPTO_PUNKS,
            lastEventId = bidOrder.lastEventId
        )
        assertThat(bidOrder).isEqualTo(expectedBidOrder)

        // Seller accepts the bid.
        val acceptBidTimestamp = cryptoPunksMarket.acceptBidForPunk(punkIndex, bidPrice).withSender(sellerSender)
            .execute().verifySuccess().getTimestamp()

        Wait.waitAssert {
            val sellCancelledOrder = orderRepository.findById(sellOrder.hash)
            assertThat(sellCancelledOrder).isEqualTo(
                sellOrder.copy(
                    cancelled = true,
                    status = OrderStatus.CANCELLED,
                    makeStock = EthUInt256.ZERO,
                    fill = EthUInt256.ZERO,
                    lastUpdateAt = acceptBidTimestamp,
                    lastEventId = sellCancelledOrder?.lastEventId
                )
            )

            val filledBidOrder = orderRepository.findById(bidOrder.hash)
            assertThat(filledBidOrder).isEqualTo(
                expectedBidOrder.copy(
                    maker = bidderAddress,
                    make = bidMake,
                    makeStock = EthUInt256.ZERO,
                    fill = EthUInt256.ONE,
                    status = OrderStatus.FILLED,

                    lastUpdateAt = acceptBidTimestamp,
                    takePriceUsd = bidTakePriceUsd,
                    makeUsd = bidTakePriceUsd,
                    takePrice = bidTakePrice,
                    lastEventId = filledBidOrder?.lastEventId
                )
            )
        }

        checkActivityWasPublished {
            assertThat(this).isInstanceOfSatisfying(OrderActivityMatchDto::class.java) {
                assertThat(it.left.hash).isEqualTo(sellOrder.hash)
                assertThat(it.right.hash).isEqualTo(bidOrder.hash)
            }
        }
    }

    @Test
    fun `sell order update - allowance to sell to a specific address replaces the old order`() = runBlocking<Unit> {
        val (sellerAddress, sellerSender, _) = newSender()
        val punkIndex = 42.toBigInteger()
        val firstPrice = BigInteger.valueOf(100)
        val secondPrice = BigInteger.valueOf(200)

        cryptoPunksMarket.getPunk(punkIndex).withSender(sellerSender).execute().verifySuccess()

        cryptoPunksMarket.offerPunkForSale(punkIndex, firstPrice)
            .withSender(sellerSender)
            .execute().verifySuccess().getTimestamp()

        val taker = randomAddress()
        val saleToAddressTimestamp = cryptoPunksMarket.offerPunkForSaleToAddress(punkIndex, secondPrice, taker)
            .withSender(sellerSender)
            .execute().verifySuccess().getTimestamp()

        val make = Asset(CryptoPunksAssetType(cryptoPunksMarket.address(), EthUInt256(punkIndex)), EthUInt256.ONE)
        val take = Asset(EthAssetType, EthUInt256(secondPrice))

        val punkPriceUsd = secondPrice.toBigDecimal(18) * TestPropertiesConfiguration.ETH_CURRENCY_RATE

        Wait.waitAssert {
            val activeOrders = orderRepository.findActive().toList()
            assertThat(activeOrders).hasSize(1)
            val order = activeOrders.single()
            val expectedOrder = Order(
                maker = sellerAddress,
                taker = taker, // 'taker' is defined as the granted address for this sale order.
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
                createdAt = saleToAddressTimestamp,
                lastUpdateAt = saleToAddressTimestamp,
                pending = emptyList(),
                makePriceUsd = punkPriceUsd,
                takePriceUsd = null,
                makePrice = secondPrice.toBigDecimal(18),
                takePrice = null,
                makeUsd = null,
                takeUsd = punkPriceUsd,
                priceHistory = createPriceHistory(saleToAddressTimestamp, make, take),
                platform = Platform.CRYPTO_PUNKS,
                lastEventId = order.lastEventId
            )
            assertThat(order).isEqualTo(expectedOrder)
        }
    }

    @Test
    fun `sell order update - allowance to sell to Rarible transfer proxy just cancels the old order`() = runBlocking<Unit> {
        val (sellerAddress, sellerSender, _) = newSender()
        val punkIndex = 42.toBigInteger()
        val firstPrice = BigInteger.valueOf(100)

        cryptoPunksMarket.getPunk(punkIndex).withSender(sellerSender).execute().verifySuccess()

        val forSaleTimestamp = cryptoPunksMarket.offerPunkForSale(punkIndex, firstPrice)
            .withSender(sellerSender)
            .execute().verifySuccess().getTimestamp()

        val punkTransferProxyAddress = randomAddress()
        transferProxyAddresses.cryptoPunksTransferProxy = punkTransferProxyAddress

        val saleToAddressTimestamp = cryptoPunksMarket.offerPunkForSaleToAddress(punkIndex, BigInteger.ZERO, punkTransferProxyAddress)
            .withSender(sellerSender)
            .execute().verifySuccess().getTimestamp()

        val make = Asset(CryptoPunksAssetType(cryptoPunksMarket.address(), EthUInt256(punkIndex)), EthUInt256.ONE)
        val take = Asset(EthAssetType, EthUInt256(firstPrice))

        val punkPriceUsd = firstPrice.toBigDecimal(18) * TestPropertiesConfiguration.ETH_CURRENCY_RATE

        Wait.waitAssert {
            val allOrders = orderRepository.findAll().toList()
            assertThat(allOrders).hasSize(1)
            val order = allOrders.single()
            val expectedOrder = Order(
                maker = sellerAddress,
                taker = null,
                make = make,
                take = take,
                type = OrderType.CRYPTO_PUNKS,
                fill = EthUInt256.ZERO,
                cancelled = true,
                data = OrderCryptoPunksData,

                makeStock = EthUInt256.ZERO,
                salt = CRYPTO_PUNKS_SALT,
                start = null,
                end = null,
                signature = null,
                createdAt = forSaleTimestamp,
                lastUpdateAt = saleToAddressTimestamp,
                pending = emptyList(),
                makePriceUsd = punkPriceUsd,
                takePriceUsd = null,
                makePrice = firstPrice.toBigDecimal(18),
                takePrice = null,
                makeUsd = null,
                takeUsd = punkPriceUsd,
                priceHistory = createPriceHistory(forSaleTimestamp, make, take),
                platform = Platform.CRYPTO_PUNKS,
                lastEventId = order.lastEventId
            )
            assertThat(order).isEqualTo(expectedOrder)
        }
    }

    @Test
    fun `crypto punk listed for sale to a specific address`() = runBlocking<Unit> {
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
            makePrice = punkPrice.toBigDecimal(18),
            takePrice = null,
            makeUsd = null,
            takeUsd = punkPriceUsd,
            priceHistory = createPriceHistory(listOrderTimestamp, make, take),
            platform = Platform.CRYPTO_PUNKS,
            lastEventId = listOrder.lastEventId
        )
        assertThat(listOrder).isEqualTo(expectedListOrder)

        // We can check here that 'OrderActivityListDto' with a specific granted address has been published,
        // but currently, such activities are ignored because frontend does not support "for-specific-address" sale orders.
    }

    @Test
    fun `sell order closed after punk transferring`() = runBlocking<Unit> {
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

    @Test
    fun `bid order closed after buying the punk by the bidder`() = runBlocking<Unit> {
        val (_, ownerSender) = newSender()
        val punkIndex = 42.toBigInteger()
        cryptoPunksMarket.getPunk(punkIndex).withSender(ownerSender).execute().verifySuccess()

        val (bidderAddress, bidderSender) = newSender()
        val bidPrice = 100500.toBigInteger()
        depositInitialBalance(bidderAddress, bidPrice)
        cryptoPunksMarket.enterBidForPunk(punkIndex).withSender(bidderSender).withValue(bidPrice)
            .execute().verifySuccess()
        val bidOrder = Wait.waitFor { orderRepository.findActive().singleOrNull() }!!

        // List the punk for sale.
        val sellPrice = 200000.toBigInteger()
        cryptoPunksMarket.offerPunkForSale(punkIndex, sellPrice).withSender(ownerSender).execute().verifySuccess()
        depositInitialBalance(bidderAddress, sellPrice)
        cryptoPunksMarket.buyPunk(punkIndex).withSender(bidderSender).withValue(sellPrice).execute().verifySuccess()

        Wait.waitAssert {
            assertThat(orderRepository.findActive().toList()).isEmpty()
            val cancelledBidOrder = orderRepository.findById(bidOrder.hash)
            assertThat(cancelledBidOrder?.cancelled).isTrue()
        }
    }

    @Nested
    inner class OrderReopenedTest {
        @Test
        fun `sell order re-opened`() = runBlocking<Unit> {
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
        fun `bid order re-opened`() = runBlocking<Unit> {
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
        fun `punk sold then bought by the same user and put on sale again`() = runBlocking<Unit> {
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
