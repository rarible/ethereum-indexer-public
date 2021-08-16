package com.rarible.protocol.order.listener.service.descriptors.exchange.v2

import com.rarible.core.common.nowMillis
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.v2.events.MatchEvent
import com.rarible.protocol.dto.OrderActivityMatchDto
import com.rarible.protocol.dto.PrepareOrderTxFormDto
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.service.PrepareTxService
import com.rarible.protocol.order.listener.integration.IntegrationTest
import com.rarible.protocol.order.listener.misc.setField
import com.rarible.protocol.order.listener.misc.sign
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
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

        val orderLeft = Order(
            maker = userSender1.from(),
            taker = null,
            make = Asset(Erc20AssetType(token1.address()), EthUInt256.TEN),
            take = Asset(Erc721AssetType(token721.address(), EthUInt256.ONE), EthUInt256.ONE),
            makeStock = EthUInt256.TEN,
            type = OrderType.RARIBLE_V2,
            fill = EthUInt256.ZERO,
            cancelled = false,
            salt = EthUInt256.TEN,
            start = null,
            end = null,
            data = OrderRaribleV2DataV1(emptyList(), emptyList()),
            signature = null,
            createdAt = nowMillis(),
            lastUpdateAt = nowMillis()
        )

        val orderRight = orderLeft.invert(userSender2.from()).copy(
            data = OrderRaribleV2DataV1(
                listOf(Part(userSender2.from(), EthUInt256.of(10000))),
                listOf(Part(AddressFactory.create(), EthUInt256.of(0)))
            )
        )

        orderRepository.save(orderLeft)
        orderRepository.save(orderRight)

        token1.mint(userSender1.from(), TEN.pow(2)).execute().verifySuccess()
        token721.mint(userSender2.from(), ONE, "test").execute().verifySuccess()

        val signature = eip712Domain.hashToSign(Order.hash(orderLeft)).sign(privateKey1)

        val prepared = prepareTxService.prepareTransaction(
            orderLeft.copy(signature = signature),
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
            assertThat(right?.fill).isEqualTo(EthUInt256.TEN)

            assertThat(left?.make)
                .isEqualTo(orderLeft.make.copy(value = EthUInt256.TEN))
            assertThat(left?.take)
                .isEqualTo(orderLeft.take.copy(value = EthUInt256.ONE))
            assertThat(right?.make)
                .isEqualTo(orderRight.make.copy(value = EthUInt256.ONE))
            assertThat(right?.take)
                .isEqualTo(orderRight.take.copy(value = EthUInt256.TEN))

            assertThat(left?.makeValue).isEqualTo(BigDecimal("0.000000000000000010"))
            assertThat(left?.takeValue).isEqualTo(BigDecimal(1))

            assertThat(left?.makeValue).isEqualTo(right?.takeValue)
            assertThat(left?.takeValue).isEqualTo(right?.makeValue)

            checkActivityWasPublished(orderLeft, MatchEvent.id(), OrderActivityMatchDto::class.java)
        }
    }
}

fun Order.invert(maker: Address) = this.copy(
    maker = maker,
    make = take,
    take = make,
    hash = Order.hashKey(maker, take.type, make.type, salt.value)
)
