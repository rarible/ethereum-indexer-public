package com.rarible.protocol.order.listener.service.descriptors.exchange.v2

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.OrderActivityMatchDto
import com.rarible.protocol.dto.OrderActivityMatchSideDto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.dto.PrepareOrderTxFormDto
import com.rarible.protocol.order.core.converters.model.PartConverter
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.ItemType
import com.rarible.protocol.order.core.model.MakeBalanceState
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV2
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV3Buy
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV3Sell
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.Part
import com.rarible.protocol.order.core.model.getOriginFees
import com.rarible.protocol.order.core.model.toOrderExactFields
import com.rarible.protocol.order.listener.misc.sign
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.verify
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import reactor.core.publisher.Mono
import scala.Tuple15
import scalether.domain.Address
import scalether.domain.AddressFactory
import scalether.domain.request.Transaction
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

abstract class ExchangeV2BaseMatchDescriptorTests : AbstractExchangeV2Test() {
    abstract fun hashToSign(structHash: Word): Word
    abstract fun fills(hash: ByteArray): Mono<BigInteger>
    abstract fun exchangeAddress(): Address

    fun `test partially match order - data V3 sell`() = runBlocking {
        val data = OrderRaribleV2DataV3Sell(
            originFeeFirst = Part(randomAddress(), EthUInt256.of(250)),
            originFeeSecond = Part(randomAddress(), EthUInt256.of(250)),
            maxFeesBasePoint = EthUInt256.of(1000),
            marketplaceMarker = Word.apply(randomWord()),
            payout = null,
        )
        val sellOrder = OrderVersion(
            maker = userSender1.from(),
            taker = null,
            make = Asset(Erc1155AssetType(token1155.address(), EthUInt256.ONE), EthUInt256.TEN),
            take = Asset(Erc20AssetType(token2.address()), EthUInt256.of(100)),
            type = OrderType.RARIBLE_V2,
            salt = EthUInt256.TEN,
            start = null,
            end = null,
            data = data,
            signature = null,
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null
        )
        // to make the makeStock = 10
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } returns MakeBalanceState(sellOrder.make.value)
        save(sellOrder).let {
            assertThat(it.makeStock).isEqualTo(EthUInt256.TEN)
        }
        token1155
            .mint(userSender1.from(), EthUInt256.ONE.value, EthUInt256.TEN.value, ByteArray(0))
            .execute().verifySuccess()
        token2
            .mint(userSender2.from(), sellOrder.take.value.value)
            .execute().verifySuccess()

        val signature = hashToSign(Order.hash(sellOrder)).sign(privateKey1)

        val formData = PrepareOrderTxFormDto(
            maker = userSender2.from(),
            amount = BigInteger.valueOf(4),
            payouts = emptyList(),
            originFees = listOf(PartDto(randomAddress(), 250), PartDto(randomAddress(), 250))
        )
        val prepared = prepareTxService.prepareTransaction(
            sellOrder.copy(signature = signature).toOrderExactFields(), formData
        )
        // Imitate the balance of the seller: 6 ERC1155
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } returns MakeBalanceState(EthUInt256.of(6))

        userSender2.sendTransaction(
            Transaction(
                exchangeAddress(),
                userSender2.from(),
                500000.toBigInteger(),
                BigInteger.ZERO,
                BigInteger.ZERO,
                prepared.transaction.data,
                null
            )
        ).verifySuccess()

        val rightOrderData = OrderRaribleV2DataV3Buy(
            payout = null,
            originFeeFirst = formData.originFees[0].let { PartConverter.convert(it) },
            originFeeSecond = formData.originFees[1].let { PartConverter.convert(it) },
            marketplaceMarker = data.marketplaceMarker
        )
        val rightOrigioonFees = listOfNotNull(rightOrderData.originFeeFirst, rightOrderData.originFeeSecond)
        val rightOrderHash = Order.hashKey(
            userSender2.from(),
            sellOrder.take.type,
            sellOrder.make.type,
            BigInteger.ZERO,
            rightOrderData
        )
        assertThat(fills(sellOrder.hash.bytes()).awaitFirst()).isEqualTo(BigInteger.valueOf(4))

        Wait.waitAssert {
            val historyItems = exchangeHistoryRepository.findByItemType(ItemType.ORDER_SIDE_MATCH)
                .collectList().awaitFirst()
            assertThat(historyItems).hasSize(2)

            val (left, right) = historyItems.map { it.data as OrderSideMatch }
                .associateBy { it.side }
                .let { it[OrderSide.LEFT] to it[OrderSide.RIGHT] }
            assertThat(left).isNotNull; left!!
            assertThat(right).isNotNull; right!!

            assertThat(left.hash).isEqualTo(sellOrder.hash)
            assertThat(left.fill).isEqualTo(EthUInt256.of(4))
            assertThat(left.originFees).isEqualTo(sellOrder.data.getOriginFees())
            assertThat(left.marketplaceMarker).isEqualTo(data.marketplaceMarker)
            assertThat(left.adhoc).isFalse
            assertThat(left.counterAdhoc).isTrue
            verify {
                @Suppress("ReactiveStreamsUnusedPublisher")
                currencyApi.getCurrencyRate(any(), any(), left.date.toEpochMilli())
            }
            assertThat(right.hash).isEqualTo(rightOrderHash)
            assertThat(right.fill).isEqualTo(EthUInt256.of(4))
            assertThat(right.originFees).isEqualTo(rightOrigioonFees)
            assertThat(right.marketplaceMarker).isEqualTo(data.marketplaceMarker)
            assertThat(right.adhoc).isTrue()
            assertThat(right.counterAdhoc).isFalse()
            verify {
                @Suppress("ReactiveStreamsUnusedPublisher")
                currencyApi.getCurrencyRate(any(), any(), right.date.toEpochMilli())
            }
        }
        Wait.waitAssert {
            val filledOrder = orderRepository.findById(sellOrder.hash)
            assertThat(filledOrder).isNotNull; filledOrder!!
            assertThat(filledOrder.fill).isEqualTo(EthUInt256.of(4))
            assertThat(filledOrder.makeStock).isEqualTo(EthUInt256.of(6))
            assertThat(filledOrder.status).isEqualTo(OrderStatus.ACTIVE)
        }
        checkActivityWasPublished {
            assertThat(this).isInstanceOfSatisfying(OrderActivityMatchDto::class.java) {
                assertThat(it.left.hash).isEqualTo(sellOrder.hash)
                assertThat(it.left.type).isEqualTo(OrderActivityMatchSideDto.Type.SELL)
                assertThat(it.right.hash).isEqualTo(rightOrderHash)
                assertThat(it.right.type).isEqualTo(OrderActivityMatchSideDto.Type.BID)
            }
        }
    }

    fun `test directPurchase`() = runBlocking {
        val data = OrderRaribleV2DataV3Sell(
            originFeeFirst = null,
            originFeeSecond = null,
            maxFeesBasePoint = EthUInt256.of(1000),
            marketplaceMarker = Word.apply(randomWord()),
            payout = null,
        )
        val sellOrder = OrderVersion(
            maker = userSender1.from(),
            taker = null,
            make = Asset(Erc1155AssetType(token1155.address(), EthUInt256.ONE), EthUInt256.TEN),
            take = Asset(Erc20AssetType(token2.address()), EthUInt256.of(100)),
            type = OrderType.RARIBLE_V2,
            salt = EthUInt256.TEN,
            start = null,
            end = null,
            data = data,
            signature = null,
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null
        )
        // to make the makeStock = 10
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } returns MakeBalanceState(sellOrder.make.value)
        save(sellOrder).let {
            assertThat(it.makeStock).isEqualTo(EthUInt256.TEN)
        }

        token1155.mint(userSender1.from(), EthUInt256.ONE.value, EthUInt256.TEN.value, ByteArray(0))
            .execute().verifySuccess()
        token2.mint(userSender2.from(), sellOrder.take.value.value)
            .execute().verifySuccess()

        val signature = hashToSign(Order.hash(sellOrder)).sign(privateKey1)

        val buyOrderData = OrderRaribleV2DataV3Buy(
            originFeeFirst = null,
            originFeeSecond = null,
            marketplaceMarker = Word.apply(randomWord()),
            payout = Part(userSender2.from(), EthUInt256.of(10000)),
        )

        // Imitate the balance of the seller: 0 ERC1155
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } returns MakeBalanceState(EthUInt256.ZERO)

        exchange.directPurchase(Tuple15(
            sellOrder.maker,
            sellOrder.make.value.value,
            sellOrder.make.type.type.id.bytes(),
            sellOrder.make.type.data.bytes(),
            sellOrder.take.value.value,
            token2.address(),
            sellOrder.salt.value,
            sellOrder.start?.toBigInteger() ?: BigInteger.ZERO,
            sellOrder.end?.toBigInteger() ?: BigInteger.ZERO,
            sellOrder.data.toDataVersion(),
            sellOrder.data.toEthereum().bytes(),
            signature.bytes(),
            sellOrder.take.value.value,
            sellOrder.make.value.value,
            buyOrderData.toEthereum().bytes()
        )).withSender(userSender2).execute().verifySuccess()

        val rightOrderHash = Order.hashKey(
            Address.ZERO(),
            sellOrder.take.type,
            sellOrder.make.type,
            BigInteger.ZERO,
            buyOrderData
        )
        assertThat(fills(sellOrder.hash.bytes()).awaitFirst()).isEqualTo(BigInteger.TEN)

        Wait.waitAssert {
            val historyItems = exchangeHistoryRepository.findByItemType(ItemType.ORDER_SIDE_MATCH)
                .collectList().awaitFirst()
            assertThat(historyItems).hasSize(2)

            val (left, right) = historyItems.map { it.data as OrderSideMatch }
                .associateBy { it.side }
                .let { it[OrderSide.LEFT] to it[OrderSide.RIGHT] }
            assertThat(left).isNotNull; left!!
            assertThat(right).isNotNull; right!!

            assertThat(left.hash).isEqualTo(sellOrder.hash)
            assertThat(left.maker).isEqualTo(sellOrder.maker)
            assertThat(left.taker).isEqualTo(userSender2.from())
            assertThat(left.fill).isEqualTo(EthUInt256.TEN)
            assertThat(left.side).isEqualTo(OrderSide.LEFT)
            assertThat(left.adhoc).isFalse
            assertThat(left.counterAdhoc).isTrue
            verify {
                @Suppress("ReactiveStreamsUnusedPublisher")
                currencyApi.getCurrencyRate(any(), any(), left.date.toEpochMilli())
            }
            assertThat(right.hash).isEqualTo(rightOrderHash)
            assertThat(right.maker).isEqualTo(userSender2.from())
            assertThat(right.taker).isEqualTo(sellOrder.maker)
            assertThat(right.fill).isEqualTo(EthUInt256.TEN)
            assertThat(right.side).isEqualTo(OrderSide.RIGHT)
            assertThat(right.adhoc).isTrue
            assertThat(right.counterAdhoc).isFalse
            verify {
                @Suppress("ReactiveStreamsUnusedPublisher")
                currencyApi.getCurrencyRate(any(), any(), right.date.toEpochMilli())
            }
        }
        Wait.waitAssert {
            val filledOrder = orderRepository.findById(sellOrder.hash)
            assertThat(filledOrder).isNotNull; filledOrder!!
            assertThat(filledOrder.fill).isEqualTo(EthUInt256.TEN)
            assertThat(filledOrder.makeStock).isEqualTo(EthUInt256.ZERO)
            assertThat(orderDtoConverter.convert(filledOrder).fillValue).isEqualTo(BigDecimal.valueOf(10))
            assertThat(filledOrder.status).isEqualTo(OrderStatus.FILLED)
        }
        checkActivityWasPublished {
            assertThat(this).isInstanceOfSatisfying(OrderActivityMatchDto::class.java) {
                assertThat(it.left.hash).isEqualTo(sellOrder.hash)
                assertThat(it.left.type).isEqualTo(OrderActivityMatchSideDto.Type.SELL)
                assertThat(it.right.hash).isEqualTo(rightOrderHash)
                assertThat(it.right.type).isEqualTo(OrderActivityMatchSideDto.Type.BID)
            }
        }
    }

    fun `test directAcceptBid`() = runBlocking {
        val data = OrderRaribleV2DataV3Buy(
            originFeeFirst = Part(randomAddress(), EthUInt256.of(150)),
            originFeeSecond = null,
            marketplaceMarker = Word.apply(randomWord()),
            payout = null,
        )
        val bidOrder = OrderVersion(
            maker = userSender1.from(),
            taker = null,
            make = Asset(Erc20AssetType(token1.address()), EthUInt256.TEN),
            take = Asset(Erc721AssetType(token721.address(), EthUInt256.ONE), EthUInt256.ONE),
            type = OrderType.RARIBLE_V2,
            salt = EthUInt256.TEN,
            start = null,
            end = null,
            data = data,
            signature = null,
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null
        )
        val rightOrderData = OrderRaribleV2DataV3Sell(
            payout = null,
            originFeeFirst = null,
            originFeeSecond = null,
            maxFeesBasePoint = EthUInt256.of(1000),
            marketplaceMarker = data.marketplaceMarker
        )

        save(bidOrder)

        token1.mint(userSender1.from(), BigInteger.TEN).execute().verifySuccess()
        token721.mint(userSender2.from(), BigInteger.ONE).execute().verifySuccess()

        val signature = hashToSign(Order.hash(bidOrder)).sign(privateKey1)

        exchange.directAcceptBid(Tuple15(
            bidOrder.maker,
            bidOrder.take.value.value, // bidNftAmount
            bidOrder.take.type.type.id.bytes(),
            bidOrder.take.type.data.bytes(),
            bidOrder.make.value.value, // bidPaymentAmount
            token1.address(),
            bidOrder.salt.value,
            bidOrder.start?.toBigInteger() ?: BigInteger.ZERO,
            bidOrder.end?.toBigInteger() ?: BigInteger.ZERO,
            bidOrder.data.toDataVersion(),
            bidOrder.data.toEthereum().bytes(), // buy order data
            signature.bytes(),
            bidOrder.make.value.value, // sellOrderPaymentAmount
            bidOrder.take.value.value, // sellOrderNftAmount
            rightOrderData.toEthereum().bytes()
        )).withSender(userSender2).execute().verifySuccess()

        assertThat(fills(bidOrder.hash.bytes()).awaitFirst()).isEqualTo(BigInteger.ONE)

        Wait.waitAssert {
            val items = exchangeHistoryRepository.findByItemType(ItemType.ORDER_SIDE_MATCH).collectList().awaitFirst()
            assertThat(items).hasSize(2)

            val (left, right) = items.map { it.data as OrderSideMatch }.associateBy { it.side }
                .let { it[OrderSide.LEFT] to it[OrderSide.RIGHT] }
            assertThat(left).isNotNull; left!!
            assertThat(right).isNotNull; right!!

            assertThat(left.maker).isEqualTo(userSender2.from())
            assertThat(left.fill).isEqualTo(EthUInt256.ONE)
            assertThat(left.take).isEqualTo(bidOrder.make.copy(value = EthUInt256.TEN))
            assertThat(left.make).isEqualTo(bidOrder.take.copy(value = EthUInt256.ONE))
            assertThat(left.takeValue).isEqualTo(BigDecimal("0.000000000000000010"))
            assertThat(left.makeValue).isEqualTo(BigDecimal(1))
            verify {
                @Suppress("ReactiveStreamsUnusedPublisher")
                currencyApi.getCurrencyRate(any(), any(), left.date.toEpochMilli())
            }

            assertThat(right.maker).isEqualTo(bidOrder.maker)
            assertThat(right.fill).isEqualTo(EthUInt256.ONE)
            assertThat(right.take).isEqualTo(bidOrder.take.copy(value = EthUInt256.ONE))
            assertThat(right.make).isEqualTo(bidOrder.make.copy(value = EthUInt256.TEN))
            verify {
                @Suppress("ReactiveStreamsUnusedPublisher")
                currencyApi.getCurrencyRate(any(), any(), right.date.toEpochMilli())
            }

            assertThat(right.takeValue).isEqualTo(left.makeValue)
            assertThat(right.makeValue).isEqualTo(left.takeValue)

            assertThat(left.adhoc!!).isTrue
            assertThat(right.counterAdhoc!!).isTrue

            assertThat(right.adhoc!!).isFalse
            assertThat(left.counterAdhoc!!).isFalse
        }
        Wait.waitAssert {
            val filledOrder = orderRepository.findById(bidOrder.hash)
            assertThat(filledOrder).isNotNull; filledOrder!!
            assertThat(filledOrder.fill).isEqualTo(EthUInt256.ONE)
            assertThat(filledOrder.makeStock).isEqualTo(EthUInt256.ZERO)
            assertThat(filledOrder.status).isEqualTo(OrderStatus.FILLED)
        }
    }

    fun `test fully match order sell order - data V3`() = runBlocking {
        val data = OrderRaribleV2DataV3Sell(
            originFeeFirst = null,
            originFeeSecond = null,
            maxFeesBasePoint = EthUInt256.of(1000),
            marketplaceMarker = Word.apply(randomWord()),
            payout = null,
        )
        val sellOrder = OrderVersion(
            maker = userSender1.from(),
            taker = null,
            make = Asset(Erc1155AssetType(token1155.address(), EthUInt256.ONE), EthUInt256.TEN),
            take = Asset(Erc20AssetType(token2.address()), EthUInt256.of(100)),
            type = OrderType.RARIBLE_V2,
            salt = EthUInt256.TEN,
            start = null,
            end = null,
            data = data,
            signature = null,
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null
        )
        // to make the makeStock = 10
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } returns MakeBalanceState(sellOrder.make.value)
        save(sellOrder).let {
            assertThat(it.makeStock).isEqualTo(EthUInt256.TEN)
        }

        token1155.mint(userSender1.from(), EthUInt256.ONE.value, EthUInt256.TEN.value, ByteArray(0))
            .execute().verifySuccess()
        token2.mint(userSender2.from(), sellOrder.take.value.value)
            .execute().verifySuccess()

        val signature = hashToSign(Order.hash(sellOrder)).sign(privateKey1)

        val prepared = prepareTxService.prepareTransaction(
            sellOrder.copy(signature = signature).toOrderExactFields(),
            PrepareOrderTxFormDto(
                maker = userSender2.from(),
                amount = sellOrder.make.value.value,
                payouts = emptyList(),
                originFees = emptyList()
            )
        )

        // Imitate the balance of the seller: 0 ERC1155
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } returns MakeBalanceState(EthUInt256.ZERO)

        userSender2.sendTransaction(
            Transaction(
                exchangeAddress(),
                userSender2.from(),
                500000.toBigInteger(),
                BigInteger.ZERO,
                BigInteger.ZERO,
                prepared.transaction.data,
                null
            )
        ).verifySuccess()

        val rightOrderData = OrderRaribleV2DataV3Buy(
            payout = null,
            originFeeFirst = null,
            originFeeSecond = null,
            marketplaceMarker = data.marketplaceMarker
        )
        val rightOrderHash = Order.hashKey(
            userSender2.from(),
            sellOrder.take.type,
            sellOrder.make.type,
            BigInteger.ZERO,
            rightOrderData
        )
        assertThat(fills(sellOrder.hash.bytes()).awaitFirst()).isEqualTo(BigInteger.TEN)

        Wait.waitAssert {
            val historyItems = exchangeHistoryRepository.findByItemType(ItemType.ORDER_SIDE_MATCH)
                .collectList().awaitFirst()
            assertThat(historyItems).hasSize(2)

            val (left, right) = historyItems.map { it.data as OrderSideMatch }
                .associateBy { it.side }
                .let { it[OrderSide.LEFT] to it[OrderSide.RIGHT] }
            assertThat(left).isNotNull; left!!
            assertThat(right).isNotNull; right!!

            assertThat(left.hash).isEqualTo(sellOrder.hash)
            assertThat(left.fill).isEqualTo(EthUInt256.TEN)
            assertThat(left.side).isEqualTo(OrderSide.LEFT)
            assertThat(left.adhoc).isFalse
            assertThat(left.counterAdhoc).isTrue
            verify {
                @Suppress("ReactiveStreamsUnusedPublisher")
                currencyApi.getCurrencyRate(any(), any(), left.date.toEpochMilli())
            }
            assertThat(right.hash).isEqualTo(rightOrderHash)
            assertThat(right.fill).isEqualTo(EthUInt256.TEN)
            assertThat(right.side).isEqualTo(OrderSide.RIGHT)
            assertThat(right.adhoc).isTrue
            assertThat(right.counterAdhoc).isFalse
            verify {
                @Suppress("ReactiveStreamsUnusedPublisher")
                currencyApi.getCurrencyRate(any(), any(), right.date.toEpochMilli())
            }
        }
        Wait.waitAssert {
            val filledOrder = orderRepository.findById(sellOrder.hash)
            assertThat(filledOrder).isNotNull; filledOrder!!
            assertThat(filledOrder.fill).isEqualTo(EthUInt256.TEN)
            assertThat(filledOrder.makeStock).isEqualTo(EthUInt256.ZERO)
            assertThat(orderDtoConverter.convert(filledOrder).fillValue).isEqualTo(BigDecimal.valueOf(10))
            assertThat(filledOrder.status).isEqualTo(OrderStatus.FILLED)
        }
        checkActivityWasPublished {
            assertThat(this).isInstanceOfSatisfying(OrderActivityMatchDto::class.java) {
                assertThat(it.left.hash).isEqualTo(sellOrder.hash)
                assertThat(it.left.type).isEqualTo(OrderActivityMatchSideDto.Type.SELL)
                assertThat(it.right.hash).isEqualTo(rightOrderHash)
                assertThat(it.right.type).isEqualTo(OrderActivityMatchSideDto.Type.BID)
            }
        }
    }

    fun `test match order sell with zero right maker`() = runBlocking {
        val data = OrderRaribleV2DataV3Sell(
            originFeeFirst = null,
            originFeeSecond = null,
            maxFeesBasePoint = EthUInt256.of(1000),
            marketplaceMarker = Word.apply(randomWord()),
            payout = null,
        )
        val sellOrder = OrderVersion(
            maker = userSender1.from(),
            taker = null,
            make = Asset(Erc1155AssetType(token1155.address(), EthUInt256.ONE), EthUInt256.TEN),
            take = Asset(Erc20AssetType(token2.address()), EthUInt256.of(100)),
            type = OrderType.RARIBLE_V2,
            salt = EthUInt256.TEN,
            start = null,
            end = null,
            data = data,
            signature = null,
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null
        )
        // to make the makeStock = 10
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } returns MakeBalanceState(sellOrder.make.value)
        save(sellOrder).let {
            assertThat(it.makeStock).isEqualTo(EthUInt256.TEN)
        }

        token1155.mint(userSender1.from(), EthUInt256.ONE.value, EthUInt256.TEN.value, ByteArray(0))
            .execute().verifySuccess()
        token2.mint(userSender2.from(), sellOrder.take.value.value)
            .execute().verifySuccess()

        val signature = hashToSign(Order.hash(sellOrder)).sign(privateKey1)

        val prepared = prepareTxService.prepareTransaction(
            sellOrder.copy(signature = signature).toOrderExactFields(),
            PrepareOrderTxFormDto(
                maker = Address.ZERO(),
                amount = sellOrder.make.value.value,
                payouts = emptyList(),
                originFees = emptyList()
            )
        )

        // Imitate the balance of the seller: 0 ERC1155
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } returns MakeBalanceState(EthUInt256.ZERO)

        userSender2.sendTransaction(
            Transaction(
                exchangeAddress(),
                userSender2.from(),
                500000.toBigInteger(),
                BigInteger.ZERO,
                BigInteger.ZERO,
                prepared.transaction.data,
                null
            )
        ).verifySuccess()

        Wait.waitAssert {
            val historyItems = exchangeHistoryRepository.findByItemType(ItemType.ORDER_SIDE_MATCH).collectList().awaitFirst()
            assertThat(historyItems).hasSize(2)

            val (left, right) = historyItems.map { it.data as OrderSideMatch }
                .associateBy { it.side }
                .let { it[OrderSide.LEFT] to it[OrderSide.RIGHT] }

            assertThat(left).isNotNull; left!!
            assertThat(right).isNotNull; right!!

            assertThat(left.maker).isEqualTo(sellOrder.maker)
            assertThat(left.taker).isEqualTo(userSender2.from())

            assertThat(right.maker).isEqualTo(userSender2.from())
            assertThat(right.taker).isEqualTo(sellOrder.maker)
        }
    }

    fun `test partially match bid order - data V3`() = runBlocking {
        val data = OrderRaribleV2DataV3Buy(
            originFeeFirst = Part(randomAddress(), EthUInt256.of(150)),
            originFeeSecond = null,
            marketplaceMarker = Word.apply(randomWord()),
            payout = null,
        )
        val bidOrder = OrderVersion(
            maker = userSender1.from(),
            taker = null,
            make = Asset(Erc20AssetType(token1.address()), EthUInt256.of(100)),
            take = Asset(Erc1155AssetType(token1155.address(), EthUInt256.ONE), EthUInt256.TEN),
            type = OrderType.RARIBLE_V2,
            salt = EthUInt256.TEN,
            start = null,
            end = Instant.now().plusSeconds(1000).epochSecond,
            data = data,
            signature = null,
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null
        )
        val rightOrderData = OrderRaribleV2DataV3Sell(
            payout = null,
            originFeeFirst = Part(Address.ONE(), EthUInt256.of(150)),
            originFeeSecond = null,
            maxFeesBasePoint = EthUInt256.of(1000),
            marketplaceMarker = data.marketplaceMarker
        )
        val rightOrderHash = Order.hashKey(
            userSender2.from(),
            bidOrder.take.type,
            bidOrder.make.type,
            BigInteger.ZERO,
            rightOrderData
        )
        // to make the makeStock = 100
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } returns MakeBalanceState(bidOrder.make.value + data.originFeeFirst!!.value.value)
        save(bidOrder).let {
            assertThat(it.makeStock).isEqualTo(EthUInt256.of(100))
        }

        token1.mint(userSender1.from(), bidOrder.make.value.value)
            .execute().verifySuccess()
        token1155.mint(userSender2.from(), EthUInt256.ONE.value, EthUInt256.TEN.value, ByteArray(0))
            .execute().verifySuccess()

        val signature = eip712Domain.hashToSign(Order.hash(bidOrder)).sign(privateKey1)

        val prepared = prepareTxService.prepareTransaction(
            bidOrder.copy(signature = signature).toOrderExactFields(),
            PrepareOrderTxFormDto(
                maker = userSender2.from(),
                amount = BigInteger.valueOf(4),
                payouts = emptyList(),
                originFees = listOf(PartDto(Address.ONE(), 150))
            )
        )

        // Imitate the balance of the bidder: 60 ERC20 + 1 ERC20 for fee
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } returns MakeBalanceState(EthUInt256.of(60) + EthUInt256.of(1))

        userSender2.sendTransaction(
            Transaction(
                exchangeAddress(),
                userSender2.from(),
                500000.toBigInteger(),
                BigInteger.ZERO,
                BigInteger.ZERO,
                prepared.transaction.data,
                null
            )
        ).verifySuccess()

        assertThat(fills(bidOrder.hash.bytes()).awaitFirst()).isEqualTo(BigInteger.valueOf(4))

        Wait.waitAssert {
            val historyItems = exchangeHistoryRepository.findByItemType(ItemType.ORDER_SIDE_MATCH)
                .collectList().awaitFirst()
            assertThat(historyItems).hasSize(2)

            val (left, right) = historyItems.map { it.data as OrderSideMatch }
                .associateBy { it.side }
                .let { it[OrderSide.LEFT] to it[OrderSide.RIGHT] }
            assertThat(left).isNotNull; left!!
            assertThat(right).isNotNull; right!!

            assertThat(left.hash).isEqualTo(bidOrder.hash)
            assertThat(left.fill).isEqualTo(EthUInt256.of(4))
            assertThat(left.adhoc).isFalse()
            assertThat(left.counterAdhoc).isTrue()
            verify {
                @Suppress("ReactiveStreamsUnusedPublisher")
                currencyApi.getCurrencyRate(any(), any(), left.date.toEpochMilli())
            }

            assertThat(right.hash).isEqualTo(rightOrderHash)
            assertThat(right.fill).isEqualTo(EthUInt256.of(4))
            assertThat(right.adhoc).isTrue
            assertThat(right.counterAdhoc).isFalse
            verify {
                @Suppress("ReactiveStreamsUnusedPublisher")
                currencyApi.getCurrencyRate(any(), any(), right.date.toEpochMilli())
            }
        }
        Wait.waitAssert {
            val filledOrder = orderRepository.findById(bidOrder.hash)
            assertThat(filledOrder).isNotNull; filledOrder!!
            assertThat(filledOrder.fill).isEqualTo(EthUInt256.of(4))
            assertThat(filledOrder.makeStock).isEqualTo(EthUInt256.of(60))
            assertThat(filledOrder.status).isEqualTo(OrderStatus.ACTIVE)
        }
        checkActivityWasPublished {
            assertThat(this).isInstanceOfSatisfying(OrderActivityMatchDto::class.java) {
                assertThat(it.left.hash).isEqualTo(bidOrder.hash)
                assertThat(it.left.type).isEqualTo(OrderActivityMatchSideDto.Type.BID)
                assertThat(it.right.hash).isEqualTo(rightOrderHash)
                assertThat(it.right.type).isEqualTo(OrderActivityMatchSideDto.Type.SELL)
            }
        }
    }

    fun `test fully match bid order - data V3`() = runBlocking {
        val data = OrderRaribleV2DataV3Buy(
            originFeeFirst = Part(randomAddress(), EthUInt256.of(150)),
            originFeeSecond = null,
            marketplaceMarker = Word.apply(randomWord()),
            payout = null,
        )
        val bidOrder = OrderVersion(
            maker = userSender1.from(),
            taker = null,
            make = Asset(Erc20AssetType(token1.address()), EthUInt256.TEN),
            take = Asset(Erc721AssetType(token721.address(), EthUInt256.ONE), EthUInt256.ONE),
            type = OrderType.RARIBLE_V2,
            salt = EthUInt256.TEN,
            start = null,
            end = null,
            data = data,
            signature = null,
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null
        )
        val rightOrderData = OrderRaribleV2DataV3Sell(
            payout = null,
            originFeeFirst = null,
            originFeeSecond = null,
            maxFeesBasePoint = EthUInt256.of(1000),
            marketplaceMarker = data.marketplaceMarker
        )
        val rightOrderHash = Order.hashKey(
            userSender2.from(),
            bidOrder.take.type,
            bidOrder.make.type,
            BigInteger.ZERO,
            rightOrderData
        )
        save(bidOrder)

        token1.mint(userSender1.from(), BigInteger.TEN).execute().verifySuccess()
        token721.mint(userSender2.from(), BigInteger.ONE).execute().verifySuccess()

        val signature = hashToSign(Order.hash(bidOrder)).sign(privateKey1)

        val prepared = prepareTxService.prepareTransaction(
            bidOrder.copy(signature = signature).toOrderExactFields(),
            PrepareOrderTxFormDto(
                userSender2.from(),
                BigInteger.ONE,
                emptyList(),
                emptyList()
            )
        )
        userSender2.sendTransaction(
            Transaction(
                exchangeAddress(),
                userSender2.from(),
                500000.toBigInteger(),
                BigInteger.ZERO,
                BigInteger.ZERO,
                prepared.transaction.data,
                null
            )
        ).verifySuccess()

        assertThat(fills(bidOrder.hash.bytes()).awaitFirst()).isEqualTo(BigInteger.ONE)

        Wait.waitAssert {
            val items = exchangeHistoryRepository.findByItemType(ItemType.ORDER_SIDE_MATCH).collectList().awaitFirst()
            assertThat(items).hasSize(2)

            val (left, right) = items.map { it.data as OrderSideMatch }.associateBy { it.side }
                .let { it[OrderSide.LEFT] to it[OrderSide.RIGHT] }
            assertThat(left).isNotNull; left!!
            assertThat(right).isNotNull; right!!

            assertThat(left.fill).isEqualTo(EthUInt256.ONE)
            assertThat(left.make).isEqualTo(bidOrder.make.copy(value = EthUInt256.TEN))
            assertThat(left.take).isEqualTo(bidOrder.take.copy(value = EthUInt256.ONE))
            assertThat(left.makeValue).isEqualTo(BigDecimal("0.000000000000000010"))
            assertThat(left.takeValue).isEqualTo(BigDecimal(1))
            verify {
                @Suppress("ReactiveStreamsUnusedPublisher")
                currencyApi.getCurrencyRate(any(), any(), left.date.toEpochMilli())
            }

            assertThat(right.fill).isEqualTo(EthUInt256.ONE)
            assertThat(right.make).isEqualTo(bidOrder.take.copy(value = EthUInt256.ONE))
            assertThat(right.take).isEqualTo(bidOrder.make.copy(value = EthUInt256.TEN))
            verify {
                @Suppress("ReactiveStreamsUnusedPublisher")
                currencyApi.getCurrencyRate(any(), any(), right.date.toEpochMilli())
            }

            assertThat(right.takeValue).isEqualTo(left.makeValue)
            assertThat(right.makeValue).isEqualTo(left.takeValue)

            assertThat(left.adhoc!!).isFalse()
            assertThat(right.counterAdhoc!!).isFalse()

            assertThat(right.adhoc!!).isTrue()
            assertThat(left.counterAdhoc!!).isTrue()
        }
        Wait.waitAssert {
            val filledOrder = orderRepository.findById(bidOrder.hash)
            assertThat(filledOrder).isNotNull; filledOrder!!
            assertThat(filledOrder.fill).isEqualTo(EthUInt256.ONE)
            assertThat(filledOrder.makeStock).isEqualTo(EthUInt256.ZERO)
            assertThat(filledOrder.status).isEqualTo(OrderStatus.FILLED)
        }
        checkActivityWasPublished {
            assertThat(this).isInstanceOfSatisfying(OrderActivityMatchDto::class.java) {
                assertThat(it.left.hash).isEqualTo(bidOrder.hash).withFailMessage("wrong left hash")
                assertThat(it.left.type).isEqualTo(OrderActivityMatchSideDto.Type.BID)
                assertThat(it.right.hash).isEqualTo(rightOrderHash).withFailMessage("wrong right hash")
                assertThat(it.right.type).isEqualTo(OrderActivityMatchSideDto.Type.SELL)
            }
        }
    }

    fun `test partially match make-fill sell order - data V2`() = runBlocking {
        /*
        Sell order: 10 NFT -> 100 ERC20

        Then sell 4 NFT.
        fill (by make) must be 4
        makeStock must be 6
         */
        val sellOrder = OrderVersion(
            maker = userSender1.from(),
            taker = null,
            make = Asset(Erc1155AssetType(token1155.address(), EthUInt256.ONE), EthUInt256.TEN),
            take = Asset(Erc20AssetType(token2.address()), EthUInt256.of(100)),
            type = OrderType.RARIBLE_V2,
            salt = EthUInt256.TEN,
            start = null,
            end = null,
            data = OrderRaribleV2DataV2(
                originFees = listOf(Part(randomAddress(), EthUInt256.of(250)), Part(randomAddress(), EthUInt256.of(250))),
                payouts = emptyList(),
                isMakeFill = true
            ),
            signature = null,
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null
        )

        // to make the makeStock = 10
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } returns MakeBalanceState(sellOrder.make.value)
        save(sellOrder).let {
            assertThat(it.makeStock).isEqualTo(EthUInt256.TEN)
        }

        token1155.mint(userSender1.from(), EthUInt256.ONE.value, EthUInt256.TEN.value, ByteArray(0))
            .execute().verifySuccess()
        token2.mint(userSender2.from(), sellOrder.take.value.value)
            .execute().verifySuccess()

        val signature = hashToSign(Order.hash(sellOrder)).sign(privateKey1)

        val formData = PrepareOrderTxFormDto(
            maker = userSender2.from(),
            amount = BigInteger.valueOf(4),
            payouts = emptyList(),
            originFees = listOf(PartDto(randomAddress(), 250), PartDto(randomAddress(), 250))
        )
        val prepared = prepareTxService.prepareTransaction(
            sellOrder.copy(signature = signature).toOrderExactFields(), formData
        )
        // Imitate the balance of the seller: 6 ERC1155
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } returns MakeBalanceState(EthUInt256.of(6))

        userSender2.sendTransaction(
            Transaction(
                exchangeAddress(),
                userSender2.from(),
                500000.toBigInteger(),
                BigInteger.ZERO,
                BigInteger.ZERO,
                prepared.transaction.data,
                null
            )
        ).verifySuccess()
        val rightOrderHash = Order.hashKey(
            userSender2.from(),
            sellOrder.take.type,
            sellOrder.make.type,
            BigInteger.ZERO,
            OrderRaribleV2DataV2(
                originFees = formData.originFees.map { PartConverter.convert(it) },
                payouts = emptyList(),
                isMakeFill = true
            )
        )
        assertThat(fills(sellOrder.hash.bytes()).awaitFirst()).isEqualTo(BigInteger.valueOf(4))

        Wait.waitAssert {
            val historyItems = exchangeHistoryRepository.findByItemType(ItemType.ORDER_SIDE_MATCH)
                .collectList().awaitFirst()
            assertThat(historyItems).hasSize(2)

            val (left, right) = historyItems.map { it.data as OrderSideMatch }
                .associateBy { it.side }
                .let { it[OrderSide.LEFT] to it[OrderSide.RIGHT] }
            assertThat(left).isNotNull; left!!
            assertThat(right).isNotNull; right!!

            assertThat(left.hash).isEqualTo(sellOrder.hash)
            assertThat(left.fill).isEqualTo(EthUInt256.of(4))
            assertThat(left.originFees).isEqualTo(sellOrder.data.getOriginFees())
            assertThat(left.adhoc).isFalse
            assertThat(left.counterAdhoc).isTrue
            verify {
                @Suppress("ReactiveStreamsUnusedPublisher")
                currencyApi.getCurrencyRate(any(), any(), left.date.toEpochMilli())
            }

            assertThat(right.hash).isEqualTo(rightOrderHash)
            assertThat(right.fill).isEqualTo(EthUInt256.of(40))
            assertThat(left.originFees).isEqualTo(sellOrder.data.getOriginFees())
            assertThat(right.adhoc).isTrue()
            assertThat(right.counterAdhoc).isFalse()
            verify {
                @Suppress("ReactiveStreamsUnusedPublisher")
                currencyApi.getCurrencyRate(any(), any(), right.date.toEpochMilli())
            }
        }
        Wait.waitAssert {
            val filledOrder = orderRepository.findById(sellOrder.hash)
            assertThat(filledOrder).isNotNull; filledOrder!!
            assertThat(filledOrder.fill).isEqualTo(EthUInt256.of(4))
            assertThat(filledOrder.makeStock).isEqualTo(EthUInt256.of(6))
            assertThat(filledOrder.status).isEqualTo(OrderStatus.ACTIVE)
        }
        checkActivityWasPublished {
            assertThat(this).isInstanceOfSatisfying(OrderActivityMatchDto::class.java) {
                assertThat(it.left.hash).isEqualTo(sellOrder.hash)
                assertThat(it.left.type).isEqualTo(OrderActivityMatchSideDto.Type.SELL)
                assertThat(it.right.hash).isEqualTo(rightOrderHash)
                assertThat(it.right.type).isEqualTo(OrderActivityMatchSideDto.Type.BID)
            }
        }
    }

    fun `test fully match make-fill sell order - data V2`() = runBlocking {
        /*
        Sell order: 10 NFT -> 100 ERC20
         */
        val sellOrder = OrderVersion(
            maker = userSender1.from(),
            taker = null,
            make = Asset(Erc1155AssetType(token1155.address(), EthUInt256.ONE), EthUInt256.TEN),
            take = Asset(Erc20AssetType(token2.address()), EthUInt256.of(100)),
            type = OrderType.RARIBLE_V2,
            salt = EthUInt256.TEN,
            start = null,
            end = null,
            data = OrderRaribleV2DataV2(emptyList(), emptyList(), isMakeFill = true),
            signature = null,
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null
        )

        // to make the makeStock = 10
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } returns MakeBalanceState(sellOrder.make.value)
        save(sellOrder).let {
            assertThat(it.makeStock).isEqualTo(EthUInt256.TEN)
        }

        token1155.mint(userSender1.from(), EthUInt256.ONE.value, EthUInt256.TEN.value, ByteArray(0))
            .execute().verifySuccess()
        token2.mint(userSender2.from(), sellOrder.take.value.value)
            .execute().verifySuccess()

        val signature = hashToSign(Order.hash(sellOrder)).sign(privateKey1)

        val prepared = prepareTxService.prepareTransaction(
            sellOrder.copy(signature = signature).toOrderExactFields(),
            PrepareOrderTxFormDto(
                maker = userSender2.from(),
                amount = sellOrder.make.value.value,
                payouts = emptyList(),
                originFees = emptyList()
            )
        )

        // Imitate the balance of the seller: 0 ERC1155
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } returns MakeBalanceState(EthUInt256.ZERO)

        userSender2.sendTransaction(
            Transaction(
                exchangeAddress(),
                userSender2.from(),
                500000.toBigInteger(),
                BigInteger.ZERO,
                BigInteger.ZERO,
                prepared.transaction.data,
                null
            )
        ).verifySuccess()
        val rightOrderHash = Order.hashKey(userSender2.from(), sellOrder.take.type, sellOrder.make.type, BigInteger.ZERO, sellOrder.data)

        assertThat(fills(sellOrder.hash.bytes()).awaitFirst()).isEqualTo(BigInteger.TEN)

        Wait.waitAssert {
            val historyItems = exchangeHistoryRepository.findByItemType(ItemType.ORDER_SIDE_MATCH)
                .collectList().awaitFirst()
            assertThat(historyItems).hasSize(2)

            val (left, right) = historyItems.map { it.data as OrderSideMatch }
                .associateBy { it.side }
                .let { it[OrderSide.LEFT] to it[OrderSide.RIGHT] }
            assertThat(left).isNotNull; left!!
            assertThat(right).isNotNull; right!!

            assertThat(left.hash).isEqualTo(sellOrder.hash)
            assertThat(left.fill).isEqualTo(EthUInt256.TEN)
            assertThat(left.adhoc).isFalse
            assertThat(left.counterAdhoc).isTrue
            verify {
                @Suppress("ReactiveStreamsUnusedPublisher")
                currencyApi.getCurrencyRate(any(), any(), left.date.toEpochMilli())
            }

            assertThat(right.hash).isEqualTo(rightOrderHash)
            assertThat(right.fill).isEqualTo(EthUInt256.of(100))
            assertThat(right.adhoc).isTrue
            assertThat(right.counterAdhoc).isFalse()
            verify {
                @Suppress("ReactiveStreamsUnusedPublisher")
                currencyApi.getCurrencyRate(any(), any(), right.date.toEpochMilli())
            }
        }

        Wait.waitAssert {
            val filledOrder = orderRepository.findById(sellOrder.hash)
            assertThat(filledOrder).isNotNull; filledOrder!!
            assertThat(filledOrder.fill).isEqualTo(EthUInt256.TEN)
            assertThat(filledOrder.makeStock).isEqualTo(EthUInt256.ZERO)
            assertThat(filledOrder.status).isEqualTo(OrderStatus.FILLED)
            assertThat(orderDtoConverter.convert(filledOrder).fillValue).isEqualTo(BigDecimal.valueOf(10))
        }

        checkActivityWasPublished {
            assertThat(this).isInstanceOfSatisfying(OrderActivityMatchDto::class.java) {
                assertThat(it.left.hash).isEqualTo(sellOrder.hash)
                assertThat(it.left.type).isEqualTo(OrderActivityMatchSideDto.Type.SELL)
                assertThat(it.right.hash).isEqualTo(rightOrderHash)
                assertThat(it.right.type).isEqualTo(OrderActivityMatchSideDto.Type.BID)
            }
        }
    }

    fun `test partially match take-fill bid order - data V2`() = runBlocking {
        /*
        Bid order: 100 ERC20 -> 10 NFT

        Partially accept bid for 4 NFTs.
        Remaining 'makeStock' must be 60.
        Bid fill (by take side) must be 4.
         */
        val bidOrder = OrderVersion(
            maker = userSender1.from(),
            taker = null,
            make = Asset(Erc20AssetType(token1.address()), EthUInt256.of(100)),
            take = Asset(Erc1155AssetType(token1155.address(), EthUInt256.ONE), EthUInt256.TEN),
            type = OrderType.RARIBLE_V2,
            salt = EthUInt256.TEN,
            start = null,
            end = Instant.now().plusSeconds(1000).epochSecond,
            data = OrderRaribleV2DataV2(emptyList(), emptyList(), isMakeFill = false),
            signature = null,
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null
        )
        val rightOrderHash = Order.hashKey(userSender2.from(), bidOrder.take.type, bidOrder.make.type, BigInteger.ZERO, bidOrder.data)

        // to make the makeStock = 100
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } returns MakeBalanceState(bidOrder.make.value)
        save(bidOrder).let {
            assertThat(it.makeStock).isEqualTo(EthUInt256.of(100))
        }

        token1.mint(userSender1.from(), bidOrder.make.value.value)
            .execute().verifySuccess()
        token1155.mint(userSender2.from(), EthUInt256.ONE.value, EthUInt256.TEN.value, ByteArray(0))
            .execute().verifySuccess()

        val signature = hashToSign(Order.hash(bidOrder)).sign(privateKey1)

        val prepared = prepareTxService.prepareTransaction(
            bidOrder.copy(signature = signature).toOrderExactFields(),
            PrepareOrderTxFormDto(
                maker = userSender2.from(),
                amount = BigInteger.valueOf(4),
                payouts = emptyList(),
                originFees = emptyList()
            )
        )

        // Imitate the balance of the bidder: 60 ERC20
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } returns MakeBalanceState(EthUInt256.of(60))

        userSender2.sendTransaction(
            Transaction(
                exchangeAddress(),
                userSender2.from(),
                500000.toBigInteger(),
                BigInteger.ZERO,
                BigInteger.ZERO,
                prepared.transaction.data,
                null
            )
        ).verifySuccess()

        assertThat(fills(bidOrder.hash.bytes()).awaitFirst()).isEqualTo(BigInteger.valueOf(4))

        Wait.waitAssert {
            val historyItems = exchangeHistoryRepository.findByItemType(ItemType.ORDER_SIDE_MATCH)
                .collectList().awaitFirst()
            assertThat(historyItems).hasSize(2)

            val (left, right) = historyItems.map { it.data as OrderSideMatch }
                .associateBy { it.side }
                .let { it[OrderSide.LEFT] to it[OrderSide.RIGHT] }
            assertThat(left).isNotNull; left!!
            assertThat(right).isNotNull; right!!

            assertThat(left.hash).isEqualTo(bidOrder.hash)
            assertThat(left.fill).isEqualTo(EthUInt256.of(4))
            assertThat(left.adhoc).isFalse()
            assertThat(left.counterAdhoc).isTrue()
            verify {
                @Suppress("ReactiveStreamsUnusedPublisher")
                currencyApi.getCurrencyRate(any(), any(), left.date.toEpochMilli())
            }

            assertThat(right.hash).isEqualTo(Order.hashKey(userSender2.from(), bidOrder.take.type, bidOrder.make.type, BigInteger.ZERO, bidOrder.data))
            assertThat(right.fill).isEqualTo(EthUInt256.of(40))
            assertThat(right.adhoc).isTrue()
            assertThat(right.counterAdhoc).isFalse()
            verify {
                @Suppress("ReactiveStreamsUnusedPublisher")
                currencyApi.getCurrencyRate(any(), any(), right.date.toEpochMilli())
            }
        }

        Wait.waitAssert {
            val filledOrder = orderRepository.findById(bidOrder.hash)
            assertThat(filledOrder).isNotNull; filledOrder!!
            assertThat(filledOrder.fill).isEqualTo(EthUInt256.of(4))
            assertThat(filledOrder.makeStock).isEqualTo(EthUInt256.of(60))
            assertThat(filledOrder.status).isEqualTo(OrderStatus.ACTIVE)
        }

        checkActivityWasPublished {
            assertThat(this).isInstanceOfSatisfying(OrderActivityMatchDto::class.java) {
                assertThat(it.left.hash).isEqualTo(bidOrder.hash)
                assertThat(it.left.type).isEqualTo(OrderActivityMatchSideDto.Type.BID)
                assertThat(it.right.hash).isEqualTo(rightOrderHash)
                assertThat(it.right.type).isEqualTo(OrderActivityMatchSideDto.Type.SELL)
            }
        }
    }

    fun `test fully match take-fill bid order - data V1`() = runBlocking {
        /*
        Bid order: 10 ERC20 -> 1 NFT

        Fully match this order.
         */
        val bidOrder = OrderVersion(
            maker = userSender1.from(),
            taker = null,
            make = Asset(Erc20AssetType(token1.address()), EthUInt256.TEN),
            take = Asset(Erc721AssetType(token721.address(), EthUInt256.ONE), EthUInt256.ONE),
            type = OrderType.RARIBLE_V2,
            salt = EthUInt256.TEN,
            start = null,
            end = null,
            data = OrderRaribleV2DataV1(emptyList(), emptyList()),
            signature = null,
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null
        )
        val rightOrderHash = Order.hashKey(userSender2.from(), bidOrder.take.type, bidOrder.make.type, BigInteger.ZERO, bidOrder.data)

        save(bidOrder)

        token1.mint(userSender1.from(), BigInteger.TEN).execute().verifySuccess()
        token721.mint(userSender2.from(), BigInteger.ONE).execute().verifySuccess()

        val signature = hashToSign(Order.hash(bidOrder)).sign(privateKey1)

        val prepared = prepareTxService.prepareTransaction(
            bidOrder.copy(signature = signature).toOrderExactFields(),
            PrepareOrderTxFormDto(userSender2.from(), BigInteger.ONE, emptyList(), emptyList())
        )
        userSender2.sendTransaction(
            Transaction(
                exchangeAddress(),
                userSender2.from(),
                500000.toBigInteger(),
                BigInteger.ZERO,
                BigInteger.ZERO,
                prepared.transaction.data,
                null
            )
        ).verifySuccess()

        assertThat(fills(bidOrder.hash.bytes()).awaitFirst()).isEqualTo(BigInteger.ONE)

        Wait.waitAssert {
            val items = exchangeHistoryRepository.findByItemType(ItemType.ORDER_SIDE_MATCH).collectList().awaitFirst()
            assertThat(items).hasSize(2)

            val (left, right) = items.map { it.data as OrderSideMatch }.associateBy { it.side }
                .let { it[OrderSide.LEFT] to it[OrderSide.RIGHT] }
            assertThat(left).isNotNull; left!!
            assertThat(right).isNotNull; right!!

            assertThat(left.fill).isEqualTo(EthUInt256.ONE)
            assertThat(left.make).isEqualTo(bidOrder.make.copy(value = EthUInt256.TEN))
            assertThat(left.take).isEqualTo(bidOrder.take.copy(value = EthUInt256.ONE))
            assertThat(left.makeValue).isEqualTo(BigDecimal("0.000000000000000010"))
            assertThat(left.takeValue).isEqualTo(BigDecimal(1))
            verify {
                @Suppress("ReactiveStreamsUnusedPublisher")
                currencyApi.getCurrencyRate(any(), any(), left.date.toEpochMilli())
            }

            assertThat(right.fill).isEqualTo(EthUInt256.TEN)
            assertThat(right.make).isEqualTo(bidOrder.take.copy(value = EthUInt256.ONE))
            assertThat(right.take).isEqualTo(bidOrder.make.copy(value = EthUInt256.TEN))
            verify {
                @Suppress("ReactiveStreamsUnusedPublisher")
                currencyApi.getCurrencyRate(any(), any(), right.date.toEpochMilli())
            }

            assertThat(right.takeValue).isEqualTo(left.makeValue)
            assertThat(right.makeValue).isEqualTo(left.takeValue)

            assertThat(left.adhoc!!).isFalse()
            assertThat(right.counterAdhoc!!).isFalse()

            assertThat(right.adhoc!!).isTrue()
            assertThat(left.counterAdhoc!!).isTrue()
        }

        Wait.waitAssert {
            val filledOrder = orderRepository.findById(bidOrder.hash)
            assertThat(filledOrder).isNotNull; filledOrder!!
            assertThat(filledOrder.fill).isEqualTo(EthUInt256.ONE)
            assertThat(filledOrder.makeStock).isEqualTo(EthUInt256.ZERO)
            assertThat(filledOrder.status).isEqualTo(OrderStatus.FILLED)
        }

        checkActivityWasPublished {
            assertThat(this).isInstanceOfSatisfying(OrderActivityMatchDto::class.java) {
                assertThat(it.left.hash).isEqualTo(bidOrder.hash)
                assertThat(it.left.type).isEqualTo(OrderActivityMatchSideDto.Type.BID)
                assertThat(it.right.hash).isEqualTo(rightOrderHash)
                assertThat(it.right.type).isEqualTo(OrderActivityMatchSideDto.Type.SELL)
            }
        }
    }

    fun `test fully match take-fill bid order with payout - data V1`() = runBlocking {
        val leftPayout = AddressFactory.create()
        val leftOriginFees = AddressFactory.create()
        val rightPayout = AddressFactory.create()
        val rightOriginFees = AddressFactory.create()
        /*
        Bid order: 10 ERC20 -> 1 NFT
         */
        val bidOrder = OrderVersion(
            maker = userSender1.from(),
            taker = null,
            make = Asset(Erc20AssetType(token1.address()), EthUInt256.TEN),
            take = Asset(Erc721AssetType(token721.address(), EthUInt256.ONE), EthUInt256.ONE),
            type = OrderType.RARIBLE_V2,
            salt = EthUInt256.TEN,
            start = null,
            end = null,
            data = OrderRaribleV2DataV1(
                originFees = listOf(Part(leftOriginFees, EthUInt256.of(250))),
                payouts = listOf(Part(leftPayout, EthUInt256.of(10000)))
            ),
            signature = null,
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null
        )
        val rightOrderHash = Order.hashKey(userSender2.from(), bidOrder.take.type, bidOrder.make.type, BigInteger.ZERO, bidOrder.data)
        save(bidOrder)

        token1.mint(userSender1.from(), BigInteger.valueOf(100)).execute().verifySuccess()
        token721.mint(userSender2.from(), BigInteger.ONE).execute().verifySuccess()

        val signature = hashToSign(Order.hash(bidOrder)).sign(privateKey1)

        val prepared = prepareTxService.prepareTransaction(
            bidOrder.copy(signature = signature).toOrderExactFields(),
            PrepareOrderTxFormDto(
                maker = userSender2.from(),
                amount = BigInteger.ONE,
                payouts = listOf(PartDto(rightPayout, 10000)),
                originFees = listOf(PartDto(rightOriginFees, 250))
            )
        )
        userSender2.sendTransaction(
            Transaction(
                exchangeAddress(),
                userSender2.from(),
                500000.toBigInteger(),
                BigInteger.ZERO,
                BigInteger.ZERO,
                prepared.transaction.data,
                null
            )
        ).verifySuccess()

        assertThat(fills(bidOrder.hash.bytes()).awaitFirst()).isEqualTo(BigInteger.ONE)

        // Assert the payouts are made.
        assertThat(token1.balanceOf(rightPayout).call().awaitFirst()).isEqualTo(BigInteger.valueOf(10))
        assertThat(token721.ownerOf(BigInteger.ONE).call().awaitFirst()).isEqualTo(leftPayout)

        Wait.waitAssert {
            val items = exchangeHistoryRepository.findByItemType(ItemType.ORDER_SIDE_MATCH).collectList().awaitFirst()
            assertThat(items).hasSize(2)

            val (left, right) = items.map { it.data as OrderSideMatch }.associateBy { it.side }
                .let { it[OrderSide.LEFT] to it[OrderSide.RIGHT] }
            assertThat(left).isNotNull; left!!
            assertThat(right).isNotNull; right!!

            assertThat(left.originFees).isEqualTo(listOf(Part(leftOriginFees, EthUInt256.of(250))))
            assertThat(left.maker).isEqualTo(leftPayout)
            assertThat(left.taker).isEqualTo(rightPayout)
            verify {
                @Suppress("ReactiveStreamsUnusedPublisher")
                currencyApi.getCurrencyRate(any(), any(), left.date.toEpochMilli())
            }

            assertThat(right.maker).isEqualTo(rightPayout)
            assertThat(right.taker).isEqualTo(leftPayout)
            assertThat(right.originFees).isEqualTo(listOf(Part(rightOriginFees, EthUInt256.of(250))))
            verify {
                @Suppress("ReactiveStreamsUnusedPublisher")
                currencyApi.getCurrencyRate(any(), any(), right.date.toEpochMilli())
            }
        }

        checkActivityWasPublished {
            assertThat(this).isInstanceOfSatisfying(OrderActivityMatchDto::class.java) {
                assertThat(it.left.hash).isEqualTo(bidOrder.hash)
                assertThat(it.left.type).isEqualTo(OrderActivityMatchSideDto.Type.BID)
                assertThat(it.right.hash).isEqualTo(rightOrderHash)
                assertThat(it.right.type).isEqualTo(OrderActivityMatchSideDto.Type.SELL)
            }
        }
    }

    fun OrderVersion.invert(maker: Address) = this.copy(
        id = ObjectId(), // recreate ID.
        maker = maker,
        make = take,
        take = make,
        hash = Order.hashKey(maker, take.type, make.type, salt.value, data)
    )
}
