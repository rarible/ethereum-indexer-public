package com.rarible.protocol.order.listener.service.descriptors.exchange.v2

import com.rarible.core.common.nowMillis
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.OrderActivityMatchDto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.dto.PrepareOrderTxFormDto
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.ItemType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV2
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.Part
import com.rarible.protocol.order.core.model.toOrderExactFields
import com.rarible.protocol.order.listener.integration.IntegrationTest
import com.rarible.protocol.order.listener.misc.sign
import io.mockk.coEvery
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import scalether.domain.Address
import scalether.domain.AddressFactory
import scalether.domain.request.Transaction
import java.math.BigDecimal
import java.math.BigInteger

@IntegrationTest
class ExchangeV2MatchDescriptorTest : AbstractExchangeV2Test() {

    @Test
    fun `partially match make-fill sell order - data V2`() = runBlocking<Unit> {
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
        coEvery { assetMakeBalanceProvider.getMakeBalance(any()) } returns sellOrder.make.value
        orderUpdateService.save(sellOrder).let {
            assertThat(it.makeStock).isEqualTo(EthUInt256.TEN)
        }

        token1155.mint(userSender1.from(), EthUInt256.ONE.value, EthUInt256.TEN.value, ByteArray(0))
            .execute().verifySuccess()
        token2.mint(userSender2.from(), sellOrder.take.value.value)
            .execute().verifySuccess()

        val signature = eip712Domain.hashToSign(Order.hash(sellOrder)).sign(privateKey1)

        val prepared = prepareTxService.prepareTransaction(
            sellOrder.copy(signature = signature).toOrderExactFields(),
            PrepareOrderTxFormDto(
                maker = userSender2.from(),
                amount = BigInteger.valueOf(4),
                payouts = emptyList(),
                originFees = emptyList()
            )
        )

        // Imitate the balance of the seller: 6 ERC1155
        coEvery { assetMakeBalanceProvider.getMakeBalance(any()) } returns EthUInt256.of(6)

        userSender2.sendTransaction(
            Transaction(
                exchange.address(),
                userSender2.from(),
                500000.toBigInteger(),
                BigInteger.ZERO,
                BigInteger.ZERO,
                prepared.transaction.data,
                null
            )
        ).verifySuccess()

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
            assertThat(left.data).isEqualTo(OrderRaribleV2DataV2(emptyList(), emptyList(), true))
            assertThat(left.adhoc).isFalse()
            assertThat(left.counterAdhoc).isTrue()

            assertThat(right.hash).isEqualTo(Order.hashKey(userSender2.from(), sellOrder.take.type, sellOrder.make.type, BigInteger.ZERO, sellOrder.data))
            assertThat(right.fill).isEqualTo(EthUInt256.of(40))
            assertThat(right.data).isEqualTo(OrderRaribleV2DataV2(emptyList(), emptyList(), true))
            assertThat(right.adhoc).isTrue()
            assertThat(right.counterAdhoc).isFalse()
        }

        Wait.waitAssert {
            val filledOrder = orderRepository.findById(sellOrder.hash)
            assertThat(filledOrder).isNotNull; filledOrder!!
            assertThat(filledOrder.fill).isEqualTo(EthUInt256.of(4))
            assertThat(filledOrder.makeStock).isEqualTo(EthUInt256.of(6))
            assertThat(filledOrder.status).isEqualTo(OrderStatus.ACTIVE)
        }
    }

    @Test
    fun `partially match take-fill bid order - data V2`() = runBlocking<Unit> {
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
            end = null,
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

        // to make the makeStock = 100
        coEvery { assetMakeBalanceProvider.getMakeBalance(any()) } returns bidOrder.make.value
        orderUpdateService.save(bidOrder).let {
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
                originFees = emptyList()
            )
        )

        // Imitate the balance of the bidder: 60 ERC20
        coEvery { assetMakeBalanceProvider.getMakeBalance(any()) } returns EthUInt256.of(60)

        userSender2.sendTransaction(
            Transaction(
                exchange.address(),
                userSender2.from(),
                500000.toBigInteger(),
                BigInteger.ZERO,
                BigInteger.ZERO,
                prepared.transaction.data,
                null
            )
        ).verifySuccess()

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
            assertThat(left.data).isEqualTo(OrderRaribleV2DataV2(emptyList(), emptyList(), isMakeFill = false))
            assertThat(left.adhoc).isFalse()
            assertThat(left.counterAdhoc).isTrue()

            assertThat(right.hash).isEqualTo(Order.hashKey(userSender2.from(), bidOrder.take.type, bidOrder.make.type, BigInteger.ZERO, bidOrder.data))
            assertThat(right.fill).isEqualTo(EthUInt256.of(40))
            assertThat(right.data).isEqualTo(OrderRaribleV2DataV2(emptyList(), emptyList(), isMakeFill = false))
            assertThat(right.adhoc).isTrue()
            assertThat(right.counterAdhoc).isFalse()
        }

        Wait.waitAssert {
            val filledOrder = orderRepository.findById(bidOrder.hash)
            assertThat(filledOrder).isNotNull; filledOrder!!
            assertThat(filledOrder.fill).isEqualTo(EthUInt256.of(4))
            assertThat(filledOrder.makeStock).isEqualTo(EthUInt256.of(60))
            assertThat(filledOrder.status).isEqualTo(OrderStatus.ACTIVE)
        }
    }

    @Test
    fun `fully match take-fill bid order - data V1`() = runBlocking<Unit> {
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

        orderUpdateService.save(bidOrder)

        token1.mint(userSender1.from(), BigInteger.TEN).execute().verifySuccess()
        token721.mint(userSender2.from(), BigInteger.ONE, "test").execute().verifySuccess()

        val signature = eip712Domain.hashToSign(Order.hash(bidOrder)).sign(privateKey1)

        val prepared = prepareTxService.prepareTransaction(
            bidOrder.copy(signature = signature).toOrderExactFields(),
            PrepareOrderTxFormDto(userSender2.from(), BigInteger.ONE, emptyList(), emptyList())
        )
        userSender2.sendTransaction(
            Transaction(
                exchange.address(),
                userSender2.from(),
                500000.toBigInteger(),
                BigInteger.ZERO,
                BigInteger.ZERO,
                prepared.transaction.data,
                null
            )
        ).verifySuccess()

        Wait.waitAssert {
            val items = exchangeHistoryRepository.findByItemType(ItemType.ORDER_SIDE_MATCH).collectList().awaitFirst()
            assertThat(items).hasSize(2)

            val (left, right) = items.map { it.data as OrderSideMatch }.associateBy { it.side }
                .let { it[OrderSide.LEFT] to it[OrderSide.RIGHT] }
            assertThat(left).isNotNull; left!!
            assertThat(right).isNotNull; right!!

            assertThat(left.fill).isEqualTo(EthUInt256.ONE)
            assertThat(left.data).isEqualTo(bidOrder.data)
            assertThat(left.make).isEqualTo(bidOrder.make.copy(value = EthUInt256.TEN))
            assertThat(left.take).isEqualTo(bidOrder.take.copy(value = EthUInt256.ONE))
            assertThat(left.makeValue).isEqualTo(BigDecimal("0.000000000000000010"))
            assertThat(left.takeValue).isEqualTo(BigDecimal(1))

            assertThat(right.fill).isEqualTo(EthUInt256.TEN)
            assertThat(right.data).isEqualTo(OrderRaribleV2DataV1(emptyList(), emptyList()))
            assertThat(right.make).isEqualTo(bidOrder.take.copy(value = EthUInt256.ONE))
            assertThat(right.take).isEqualTo(bidOrder.make.copy(value = EthUInt256.TEN))

            assertThat(right.takeValue).isEqualTo(left.makeValue)
            assertThat(right.makeValue).isEqualTo(left.takeValue)

            assertThat(left.adhoc!!).isFalse()
            assertThat(right.counterAdhoc!!).isFalse()

            assertThat(right.adhoc!!).isTrue()
            assertThat(left.counterAdhoc!!).isTrue()

            checkActivityWasPublished {
                assertThat(this).isInstanceOfSatisfying(OrderActivityMatchDto::class.java) {
                    assertThat(left.hash).isEqualTo(bidOrder.hash)
                }
            }
        }

        Wait.waitAssert {
            val filledOrder = orderRepository.findById(bidOrder.hash)
            assertThat(filledOrder).isNotNull; filledOrder!!
            assertThat(filledOrder.fill).isEqualTo(EthUInt256.ONE)
            assertThat(filledOrder.makeStock).isEqualTo(EthUInt256.ZERO)
            assertThat(filledOrder.status).isEqualTo(OrderStatus.FILLED)
        }
    }

    @Test
    fun `fully match take-fill bid order with payout - data V1`() = runBlocking<Unit> {
        val leftPayout = AddressFactory.create()
        val rightPayout = AddressFactory.create()

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
                payouts = listOf(Part(leftPayout, EthUInt256.of(10000))),
                originFees = emptyList()
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
        orderUpdateService.save(bidOrder)

        token1.mint(userSender1.from(), BigInteger.valueOf(100)).execute().verifySuccess()
        token721.mint(userSender2.from(), BigInteger.ONE, "test").execute().verifySuccess()

        val signature = eip712Domain.hashToSign(Order.hash(bidOrder)).sign(privateKey1)

        val prepared = prepareTxService.prepareTransaction(
            bidOrder.copy(signature = signature).toOrderExactFields(),
            PrepareOrderTxFormDto(
                maker = userSender2.from(),
                amount = BigInteger.ONE,
                payouts = listOf(PartDto(rightPayout, 10000)),
                originFees = emptyList()
            )
        )
        userSender2.sendTransaction(
            Transaction(
                exchange.address(),
                userSender2.from(),
                500000.toBigInteger(),
                BigInteger.ZERO,
                BigInteger.ZERO,
                prepared.transaction.data,
                null
            )
        ).verifySuccess()

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

            assertThat(left.data).isEqualTo(bidOrder.data)
            assertThat(right.data).isEqualTo(
                OrderRaribleV2DataV1(
                    payouts = listOf(Part(rightPayout, EthUInt256.of(10000))),
                    originFees = emptyList()
                )
            )

            assertThat(left.maker).isEqualTo(leftPayout)
            assertThat(left.taker).isEqualTo(rightPayout)

            assertThat(right.maker).isEqualTo(rightPayout)
            assertThat(right.taker).isEqualTo(leftPayout)
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
