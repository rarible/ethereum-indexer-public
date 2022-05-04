package com.rarible.protocol.order.listener.service.descriptors.exchange.opensea

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.wrapper.ExchangeWrapper
import com.rarible.protocol.contracts.exchange.wyvern.WyvernExchange
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.dto.PrepareOrderTxFormDto
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.misc.toSignatureData
import com.rarible.protocol.order.core.model.OpenSeaOrderSide
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderOpenSeaV1DataV1
import com.rarible.protocol.order.core.model.PrepareTxResponse
import com.rarible.protocol.order.core.model.PreparedTx
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.service.PrepareTxService
import io.daonomic.rpc.domain.Binary
import org.web3j.crypto.Sign
import scala.Tuple11
import scala.Tuple2
import scala.Tuple3
import scalether.domain.Address
import java.math.BigInteger

class WrapperHelper(
    val prepareTxService: PrepareTxService,
    val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses
) {

    fun prepareTxForWrappedExchange(
        order: Order,
        form: PrepareOrderTxFormDto,
        price: BigInteger
    ): PrepareTxResponse {

        val invertedOrder = prepareTxService.prepareInvertedOrder(
            order = order,
            form = form
        )

        val atomicSwapEncodedData = prepareDataForAtomicMatchSignature(
            order = order,
            invertedOrder = invertedOrder,
            form = form
        )

        val marketId = BigInteger.valueOf(1) // wyvern
        val fees = emptyArray<BigInteger>()

        val inputData = Tuple2(
            Tuple3(
                marketId, // Markets marketId;
                price, // uint256 amount;  ???   // TODO Check amount value
                atomicSwapEncodedData.bytes() // bytes data;
            ),
            fees// uint[] memory fees
        )

        val encodedData = ExchangeWrapper.singlePurchaseSignature().encode(inputData)

        return PrepareTxResponse(
            transferProxyAddress = null,
            asset = invertedOrder.make,
            transaction = PreparedTx(
                to = exchangeContractAddresses.exchangeWrapper!!,
                data = encodedData
            )
        )
    }

    private fun prepareDataForAtomicMatchSignature(
        order: Order,
        invertedOrder: Order,
        form: PrepareOrderTxFormDto
    ): Binary {
        if (form.payouts.isNotEmpty() && form.payouts != listOf(PartDto(form.maker, 10000))) {
            throw IllegalArgumentException("payouts not supported by OpenSea orders")
        }

        val data = order.data as OrderOpenSeaV1DataV1
        val invertedData = invertedOrder.data as OrderOpenSeaV1DataV1

        val signature = order.signature?.toSignatureData() ?: PrepareTxService.EMPTY_SIGNATURE

        val (buyOrder, sellOrder) = when (data.side) {
            OpenSeaOrderSide.SELL -> invertedOrder to order
            OpenSeaOrderSide.BUY -> order to invertedOrder
        }
        val (buyData, sellData) = when (data.side) {
            OpenSeaOrderSide.SELL -> invertedData to data
            OpenSeaOrderSide.BUY -> data to invertedData
        }

        val callDataOpenSea = prepareEncodedOpenSeaCallData(
            buyData = buyData,
            sellData = sellData,
            buyOrder = buyOrder,
            sellOrder = sellOrder,
            signature = signature
        )

        return WyvernExchange.atomicMatch_Signature().encode(callDataOpenSea)
    }

    private fun prepareEncodedOpenSeaCallData(
        buyData: OrderOpenSeaV1DataV1,
        sellData: OrderOpenSeaV1DataV1,
        buyOrder: Order,
        sellOrder: Order,
        signature: Sign.SignatureData
    ): Tuple11<Array<Address>, Array<BigInteger>, Array<BigInteger>, ByteArray, ByteArray, ByteArray, ByteArray, ByteArray, ByteArray, Array<BigInteger>, Array<ByteArray>> =
        Tuple11(
            arrayOf(
                buyData.exchange,                             // 0xdd54d660178b28f6033a953b0e55073cfa7e3744
                exchangeContractAddresses.exchangeWrapper!!,  // 0x92ce36ceae648d6a57316cb67bd40199737c17a4
                buyOrder.taker ?: Address.ZERO(),             // 0x2cf5490f75d96bd8a0aefee96c1ea383d3409efc
                buyData.feeRecipient,                // 0x0000000000000000000000000000000000000000
                buyData.target ?: buyOrder.take.type.token,   // 0x45b594792a5cdc008d0de1c1d69faa3d16b3ddc1
                buyData.staticTarget,                // 0x0000000000000000000000000000000000000000
                buyOrder.make.type.token,            // 0x0000000000000000000000000000000000000000

                sellData.exchange,                            // 0xdd54d660178b28f6033a953b0e55073cfa7e3744
                sellOrder.maker,                              // 0x2cf5490f75d96bd8a0aefee96c1ea383d3409efc
                sellOrder.taker ?: Address.ZERO(),   // 0x0000000000000000000000000000000000000000
                sellData.feeRecipient,                        // 0x5b3256965e7c3cf26e11fcaf296dfc8807c01073
                sellData.target ?: sellOrder.make.type.token, // 0x45b594792a5cdc008d0de1c1d69faa3d16b3ddc1
                sellData.staticTarget,              // 0x0000000000000000000000000000000000000000
                sellOrder.take.type.token           // 0x0000000000000000000000000000000000000000
            ),
            arrayOf(
                buyData.makerRelayerFee,  // 250
                buyData.takerRelayerFee,  // 0
                buyData.makerProtocolFee, // 0
                buyData.takerProtocolFee, // 0
                buyOrder.make.value.value,// 100000000000000
                buyData.extra,            // 0
                buyOrder.start?.toBigInteger() ?: BigInteger.valueOf(0), // 1650201755
                buyOrder.end?.toBigInteger() ?: BigInteger.valueOf(0),   // 1652793847
//                buyOrder.salt.value,      // 23741570621504096017579347520175475668997079901727111482740544775140580203952
                EthUInt256.of("23741570621504096017579347520175475668997079901727111482740544775140580203952").value,

                sellData.makerRelayerFee,   // 250
                sellData.takerRelayerFee,   // 0
                sellData.makerProtocolFee,  // 0
                sellData.takerProtocolFee,  // 0
                sellOrder.take.value.value, // 100000000000000
                sellData.extra,             // 0
                sellOrder.start?.toBigInteger() ?: BigInteger.valueOf(0), // 1650201755
                sellOrder.end?.toBigInteger() ?: BigInteger.valueOf(0),  // 1652793847
//                sellOrder.salt.value       // 23741570621504096017579347520175475668997079901727111482740544775140580203952
                EthUInt256.of("23741570621504096017579347520175475668997079901727111482740544775140580203952").value,
            ),
            arrayOf(
                buyData.feeMethod.value,  // 1
                buyData.side.value,       // 0
                buyData.saleKind.value,   // 0
                buyData.howToCall.value,  // 1
                sellData.feeMethod.value, // 1
                sellData.side.value,      // 1
                sellData.saleKind.value,  // 0
                sellData.howToCall.value  // 1
            ),
            buyData.callData.bytes(),           // 0xfb16a595000000000000000000000000000000000000000000000000000000000000000000000000000000000000000047846a7457660f1c585377cd173aa4811580ca31000000000000000000000000c8f3e6f0391c51ca58a72487171ff99adeb8d15a0000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000000
            sellData.callData.bytes(),          // 0xfb16a5950000000000000000000000002cf5490f75d96bd8a0aefee96c1ea383d3409efc0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c8f3e6f0391c51ca58a72487171ff99adeb8d15a0000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000000
            buyData.replacementPattern.bytes(), // 0x00000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
            sellData.replacementPattern.bytes(),// 0x000000000000000000000000000000000000000000000000000000000000000000000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
            buyData.staticExtraData.bytes(),    // 0
            sellData.staticExtraData.bytes(),   // 0
            arrayOf(
                BigInteger(byteArrayOf(signature.v)),
                BigInteger(byteArrayOf(signature.v))
            ),
            arrayOf(
                signature.r,
                signature.s,
                signature.r,
                signature.s,
                PrepareTxService.RARIBLE_PLATFORM_METADATA
            )
        )
}
