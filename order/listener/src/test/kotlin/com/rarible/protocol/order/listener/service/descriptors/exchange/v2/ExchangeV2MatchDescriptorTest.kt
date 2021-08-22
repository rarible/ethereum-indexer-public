package com.rarible.protocol.order.listener.service.descriptors.exchange.v2

import com.rarible.core.common.nowMillis
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.v2.events.MatchEvent
import com.rarible.protocol.dto.OrderActivityMatchDto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.dto.PrepareOrderTxFormDto
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.service.PrepareTxService
import com.rarible.protocol.order.listener.integration.IntegrationTest
import com.rarible.protocol.order.listener.misc.setField
import com.rarible.protocol.order.listener.misc.sign
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import scalether.domain.AddressFactory
import scalether.domain.request.Transaction
import java.math.BigDecimal
import java.math.BigInteger
import java.math.BigInteger.ONE
import java.math.BigInteger.TEN

@IntegrationTest
class ExchangeV2MatchDescriptorTest : AbstractExchangeV2Test() {

    @Autowired
    private lateinit var prepareTxService: PrepareTxService

    @Test
    fun convert() = runBlocking {
        setField(prepareTxService, "eip712Domain", eip712Domain)

        val orderLeftVersion = OrderVersion(
            maker = userSender1.from(),
            taker = null,
            make = Asset(Erc20AssetType(token1.address()), EthUInt256.TEN),
            take = Asset(Erc721AssetType(token721.address(), EthUInt256.ONE), EthUInt256.ONE),
            type = OrderType.RARIBLE_V2,
            salt = EthUInt256.TEN,
            start = null,
            end = null,
            data = OrderRaribleV2DataV1(emptyList(), listOf(Part(userSender2.from(), EthUInt256.ONE))),
            signature = null,
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makeUsd = null,
            takeUsd = null
        )

        val orderRightVersion = orderLeftVersion
            .invert(userSender2.from()).copy(
                data = OrderRaribleV2DataV1(
                    listOf(Part(userSender2.from(), EthUInt256.of(10000))),
                    listOf(Part(AddressFactory.create(), EthUInt256.of(0)))
                )
            )

        orderUpdateService.save(orderLeftVersion)
        orderUpdateService.save(orderRightVersion)

        token1.mint(userSender1.from(), TEN.pow(2)).execute().verifySuccess()
        token721.mint(userSender2.from(), ONE, "test").execute().verifySuccess()

        val signature = eip712Domain.hashToSign(Order.hash(orderLeftVersion)).sign(privateKey1)

        val prepared = prepareTxService.prepareTransaction(
            orderLeftVersion.copy(signature = signature).toOrderExactFields(),
            PrepareOrderTxFormDto(userSender2.from(), ONE, emptyList(), emptyList())
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

            val map = items
                .map { it.data as OrderSideMatch }
                .associateBy { it.side }

            val left = map[OrderSide.LEFT]
            val right = map[OrderSide.RIGHT]

            assertThat(left?.fill).isEqualTo(EthUInt256.ONE)
            assertThat(left?.data).isEqualTo(orderLeftVersion.data)
            assertThat(right?.fill).isEqualTo(EthUInt256.TEN)
            assertThat(right?.data).isEqualTo(OrderRaribleV2DataV1(emptyList(), emptyList()))

            assertThat(left?.make)
                .isEqualTo(orderLeftVersion.make.copy(value = EthUInt256.TEN))
            assertThat(left?.take)
                .isEqualTo(orderLeftVersion.take.copy(value = EthUInt256.ONE))
            assertThat(right?.make)
                .isEqualTo(orderRightVersion.make.copy(value = EthUInt256.ONE))
            assertThat(right?.take)
                .isEqualTo(orderRightVersion.take.copy(value = EthUInt256.TEN))

            assertThat(left?.makeValue).isEqualTo(BigDecimal("0.000000000000000010"))
            assertThat(left?.takeValue).isEqualTo(BigDecimal(1))

            assertThat(left?.makeValue).isEqualTo(right?.takeValue)
            assertThat(left?.takeValue).isEqualTo(right?.makeValue)

            checkActivityWasPublished(orderLeftVersion.toOrderExactFields(), MatchEvent.id(), OrderActivityMatchDto::class.java)
        }
    }

    @Test
    fun convertWithPayouts() = runBlocking {
        setField(prepareTxService, "eip712Domain", eip712Domain)
        val leftPayout = AddressFactory.create()
        val rightPayout = AddressFactory.create()

        val orderLeftVersion = OrderVersion(
            maker = userSender1.from(),
            taker = null,
            make = Asset(Erc20AssetType(token1.address()), EthUInt256.TEN),
            take = Asset(Erc721AssetType(token721.address(), EthUInt256.ONE), EthUInt256.ONE),
            type = OrderType.RARIBLE_V2,
            salt = EthUInt256.TEN,
            start = null,
            end = null,
            data = OrderRaribleV2DataV1(payouts = listOf(Part(leftPayout, EthUInt256.of(10000))), originFees = listOf(Part(userSender2.from(), EthUInt256.ONE))),
            signature = null,
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makeUsd = null,
            takeUsd = null
        )
        orderUpdateService.save(orderLeftVersion)

        token1.mint(userSender1.from(), TEN.pow(2)).execute().verifySuccess()
        token721.mint(userSender2.from(), ONE, "test").execute().verifySuccess()

        val signature = eip712Domain.hashToSign(Order.hash(orderLeftVersion)).sign(privateKey1)

        val prepared = prepareTxService.prepareTransaction(
            orderLeftVersion.copy(signature = signature).toOrderExactFields(),
            PrepareOrderTxFormDto(
                maker = userSender2.from(),
                amount = ONE,
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

        Wait.waitAssert {
            val items = exchangeHistoryRepository.findByItemType(ItemType.ORDER_SIDE_MATCH).collectList().awaitFirst()
            assertThat(items).hasSize(2)

            val map = items
                .map { it.data as OrderSideMatch }
                .associateBy { it.side }

            val left = map[OrderSide.LEFT]
            val right = map[OrderSide.RIGHT]

            assertThat(left?.data).isEqualTo(orderLeftVersion.data)
            assertThat(right?.data).isEqualTo(OrderRaribleV2DataV1(listOf(Part(rightPayout, EthUInt256.of(10000))), emptyList()))

            assertThat(left?.maker).isEqualTo(leftPayout)
            assertThat(left?.taker).isEqualTo(rightPayout)

            assertThat(right?.maker).isEqualTo(rightPayout)
            assertThat(right?.taker).isEqualTo(leftPayout)
        }
    }
}

fun OrderVersion.invert(maker: Address) = this.copy(
    id = ObjectId(), // recreate ID.
    maker = maker,
    make = take,
    take = make,
    hash = Order.hashKey(maker, take.type, make.type, salt.value)
)
