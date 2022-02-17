package com.rarible.protocol.order.listener.service.descriptors.exchange.opensea

import com.rarible.core.common.nowMillis
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.OrderActivityCancelListDto
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.ItemType
import com.rarible.protocol.order.core.model.OpenSeaOrderFeeMethod
import com.rarible.protocol.order.core.model.OpenSeaOrderHowToCall
import com.rarible.protocol.order.core.model.OpenSeaOrderSaleKind
import com.rarible.protocol.order.core.model.OpenSeaOrderSide
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.OrderOpenSeaV1DataV1
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.Transfer
import com.rarible.protocol.order.core.service.CallDataEncoder
import com.rarible.protocol.order.core.service.CommonSigner
import com.rarible.protocol.order.core.service.PrepareTxService
import com.rarible.protocol.order.listener.integration.IntegrationTest
import com.rarible.protocol.order.listener.misc.sign
import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import scalether.domain.request.Transaction
import java.math.BigInteger

@IntegrationTest
internal class WyvernExchangeOrderCancelDescriptorTest : AbstractOpenSeaV1Test() {
    @Autowired
    private lateinit var prepareTxService: PrepareTxService
    @Autowired
    private lateinit var callDataEncoder: CallDataEncoder
    @Autowired
    private lateinit var commonSigner: CommonSigner

    @Test
    fun `should cancel sell order`() = runBlocking {
        val sellMaker = userSender1.from()
        val target = token721.address()
        val tokenId = EthUInt256.ONE
        val paymentToken = token1.address()

        val sellTransfer = Transfer.Erc721Transfer(
            from = sellMaker,
            to = Address.ZERO(),
            tokenId = tokenId.value,
            safe = false
        )
        val sellCallData = callDataEncoder.encodeTransferCallData(sellTransfer)

        val sellOrderVersion = OrderVersion(
            maker = sellMaker,
            taker = null,
            make = Asset(Erc721AssetType(target, tokenId), EthUInt256.ONE),
            take = Asset(Erc20AssetType(paymentToken), EthUInt256.TEN),
            type = OrderType.OPEN_SEA_V1,
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
                extra = BigInteger.ZERO,
                target = null
            ),
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null
        )
        val hash = Order.hash(sellOrderVersion)
        val hashToSign = commonSigner.openSeaHashToSign(hash)
        logger.info("Sell order hash: $hash, hash to sing: $hashToSign")

        val signature = hashToSign.sign(privateKey1)

        val signedSellOrderVersion = sellOrderVersion.copy(signature = signature, hash = hash)
        val sellOrder = orderUpdateService.save(signedSellOrderVersion)

        val response = prepareTxService.prepareCancelTransaction(sellOrder)

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
            assertThat(event.hash).isEqualTo(sellOrder.hash)

            val canceledOrder = orderRepository.findById(sellOrder.hash)
            assertThat(canceledOrder?.cancelled).isTrue()

            checkActivityWasPublished {
                assertThat(this).isInstanceOfSatisfying(OrderActivityCancelListDto::class.java) {
                    assertThat(hash).isEqualTo(sellOrder.hash)
                }
            }
        }
    }
}
