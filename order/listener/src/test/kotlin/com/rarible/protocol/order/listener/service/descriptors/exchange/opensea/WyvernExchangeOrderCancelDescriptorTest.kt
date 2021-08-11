package com.rarible.protocol.order.listener.service.descriptors.exchange.opensea

import com.rarible.core.common.nowMillis
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.wyvern.OrderCancelledEvent
import com.rarible.protocol.dto.OrderActivityCancelListDto
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.service.CallDataEncoder
import com.rarible.protocol.order.core.service.CommonSigner
import com.rarible.protocol.order.core.service.PrepareTxService
import com.rarible.protocol.order.listener.integration.IntegrationTest
import com.rarible.protocol.order.listener.misc.sign
import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import scalether.domain.request.Transaction
import java.math.BigInteger

@FlowPreview
@IntegrationTest
internal class WyvernExchangeOrderCancelDescriptorTest : AbstractOpenSeaV1Test() {
    @Autowired
    private lateinit var prepareTxService: PrepareTxService
    @Autowired
    private lateinit var callDataEncoder: CallDataEncoder
    @Autowired
    private lateinit var commonSigner: CommonSigner

    @Test
    fun `should cancel sell order`() = runBlocking<Unit> {
        val sellMaker = userSender1.from()
        val target = token721.address()
        val tokenId = EthUInt256.ONE
        val paymentToken = token1.address()

        val sellTransfer = Transfer(
            type = Transfer.Type.ERC721,
            from = sellMaker,
            to = Address.ZERO(),
            tokenId = tokenId.value,
            value = BigInteger.ONE,
            data = Binary.apply()
        )
        val sellCallData = callDataEncoder.encodeTransferCallData(sellTransfer)

        val sellOrder = Order(
            maker = sellMaker,
            taker = null,
            make = Asset(Erc721AssetType(target, tokenId), EthUInt256.ONE),
            take = Asset(Erc20AssetType(paymentToken), EthUInt256.TEN),
            makeStock = EthUInt256.ONE,
            type = OrderType.OPEN_SEA_V1,
            fill = EthUInt256.ZERO,
            cancelled = false,
            salt = EthUInt256.TEN,
            start = nowMillis().epochSecond - 10,
            end = null,
            signature = null,
            data = OrderOpenSeaV1DataV1(
                exchange = exchange.address(),
                makerRelayerFee = BigInteger.valueOf(250),
                takerRelayerFee = BigInteger.ZERO,
                makerProtocolFee = BigInteger.ZERO,
                takerProtocolFee = BigInteger.ZERO,
                feeRecipient = exchange.address(),
                feeMethod = OpenSeaOrderFeeMethod.SPLIT_FEE,
                side = OpenSeaOrderSide.SELL,
                saleKind = OpenSeaOrderSaleKind.FIXED_PRICE,
                howToCall = OpenSeaOrderHowToCall.CALL,
                callData = sellCallData.callData,
                replacementPattern = sellCallData.replacementPattern,
                staticTarget = Address.ZERO(),
                staticExtraData = Binary.apply(),
                extra = BigInteger.ZERO
            ),
            createdAt = nowMillis(),
            lastUpdateAt = nowMillis()
        )
        val hash = Order.hash(sellOrder)
        val hashToSign = commonSigner.openSeaHashToSign(hash)
        logger.info("Sell order hash: $hash, hash to sing: $hashToSign")

        val signature = hashToSign.sign(privateKey1)

        val signedSellOrder = sellOrder.copy(signature = signature, hash = hash)
        orderRepository.save(signedSellOrder)

        val response = prepareTxService.prepareCancelTransaction(signedSellOrder)

        userSender1.sendTransaction(
            Transaction(
                exchange.address(),
                userSender1.from(),
                8000000.toBigInteger(),
                BigInteger.ONE,
                BigInteger.ZERO,
                response.data,
                null
            )
        ).verifySuccess()

        Wait.waitAssert {
            val items = exchangeHistoryRepository.findByItemType(ItemType.CANCEL).collectList().awaitFirst()
            assertThat(items).hasSize(1)

            val event = items.first().data as OrderCancel
            assertThat(event.hash).isEqualTo(signedSellOrder.hash)

            val canceledOrder = orderRepository.findById(signedSellOrder.hash)
            assertThat(canceledOrder?.cancelled).isTrue()

            checkActivityWasPublished(signedSellOrder, OrderCancelledEvent.id(), OrderActivityCancelListDto::class.java)
        }
    }
}
