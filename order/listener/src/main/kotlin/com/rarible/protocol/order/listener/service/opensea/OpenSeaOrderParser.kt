package com.rarible.protocol.order.listener.service.opensea

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.wrapper.ExchangeWrapper
import com.rarible.protocol.contracts.exchange.wyvern.OrdersMatchedEvent
import com.rarible.protocol.contracts.exchange.wyvern.WyvernExchange
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.misc.methodSignatureId
import com.rarible.protocol.order.core.model.OpenSeaMatchedOrders
import com.rarible.protocol.order.core.model.OpenSeaOrderFeeMethod
import com.rarible.protocol.order.core.model.OpenSeaOrderHowToCall
import com.rarible.protocol.order.core.model.OpenSeaOrderSaleKind
import com.rarible.protocol.order.core.model.OpenSeaOrderSide
import com.rarible.protocol.order.core.model.OpenSeaTransactionOrder
import com.rarible.protocol.order.core.model.Part
import com.rarible.protocol.order.core.service.CallDataEncoder
import com.rarible.protocol.order.core.trace.TraceCallService
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component
import scalether.abi.Uint256Type
import scalether.domain.Address
import java.math.BigInteger

@Component
class OpenSeaOrderParser(
    private val traceCallService: TraceCallService,
    private var callDataEncoder: CallDataEncoder,
    private val featureFlags: OrderIndexerProperties.FeatureFlags
) {
    suspend fun parseMatchedOrders(
        txHash: Word,
        txInput: Binary,
        event: OrdersMatchedEvent,
        index: Int,
        totalLogs: Int,
        eip712: Boolean
    ): OpenSeaMatchedOrders? {
        val inputs = if (ExchangeWrapper.singlePurchaseSignature().id() == txInput.methodSignatureId()) {
            listOf(txInput)
        } else {
            if (featureFlags.skipGetTrace) return null
            val signatureId = WyvernExchange.atomicMatch_Signature().id()
            traceCallService.findAllRequiredCallInputs(
                txHash = txHash,
                txInput = txInput,
                to = event.log().address(),
                ids = arrayOf(signatureId)
            )
        }
        assert(inputs.size == totalLogs) {
            "Number of events != number of traces for tx: $txHash"
        }
        val parsed = parseMatchedOrders(inputs[index])
        return if (eip712) {
            parsed.copy(
                buyOrder = replaceHash(order = parsed.buyOrder, hash = event.buyHash()),
                sellOrder = replaceHash(order = parsed.sellOrder, hash = event.sellHash())
            )
        } else {
            parsed
        }
    }

    private fun replaceHash(
        order: OpenSeaTransactionOrder,
        hash: ByteArray
    ): OpenSeaTransactionOrder =
        Word.apply(hash).let { eventHash ->
            if (eventHash == ZERO_WORD) order else order.copy(hash = eventHash)
        }

    fun parseMatchedOrders(input: Binary): OpenSeaMatchedOrders =
        when (input.methodSignatureId()) {
            WyvernExchange.atomicMatch_Signature().id() -> parseOrdersForAtomicMatch(input)
            ExchangeWrapper.singlePurchaseSignature().id() -> parseOrdersForSinglePurchase(input)
            else -> throw NotImplementedError("Not Implemented for function with signature = ${input.methodSignatureId()}")
        }

    fun parseOrdersForSinglePurchase(input: Binary): OpenSeaMatchedOrders {
        val signature = ExchangeWrapper.singlePurchaseSignature()
        val decoded = signature.`in`().decode(input, 4)
        val purchaseDetails = decoded.value()._1()
        val originFees = decoded.value()._2().map { convertToFeePart(it) }
        val data: ByteArray = purchaseDetails._3()

        val orders = parseOrdersForAtomicMatch(Binary.apply(data))

        val buyCallData = orders.buyOrder.callData
        val buyMaker = callDataEncoder.decodeTransfer(buyCallData).to

        return orders.copy(
            buyOrder = orders.buyOrder.copy(
                maker = buyMaker,
                originFees = originFees
            )
        )
    }

    private fun convertToFeePart(feeUint256: BigInteger): Part =
        Part(
            account = Address.apply(Uint256Type.encode(feeUint256).slice(12, 32)),
            value = EthUInt256.of(Uint256Type.encode(feeUint256).slice(0, 12).toBigInteger())
        )

    fun parseOrdersForAtomicMatch(input: Binary): OpenSeaMatchedOrders {
        val signature = WyvernExchange.atomicMatch_Signature()
        val decoded = signature.`in`().decode(input, 4)
        val addrs = decoded.value()._1()
        val uints = decoded.value()._2()
        val feeMethodsSidesKindsHowToCalls = decoded.value()._3()
        val calldataBuy = decoded.value()._4()
        val calldataSell = decoded.value()._5()
        val replacementPatternBuy = decoded.value()._6()
        val replacementPatternSell = decoded.value()._7()
        val staticExtradataBuy = decoded.value()._8()
        val staticExtradataSell = decoded.value()._9()
        val rssMetadata = decoded.value()._11()

        val buyOrder = OpenSeaTransactionOrder(
            exchange = addrs[0],
            maker = addrs[1],
            taker = addrs[2],
            makerRelayerFee = uints[0],
            takerRelayerFee = uints[1],
            makerProtocolFee = uints[2],
            takerProtocolFee = uints[3],
            feeRecipient = addrs[3],
            feeMethod = OpenSeaOrderFeeMethod.fromBigInteger(feeMethodsSidesKindsHowToCalls[0]),
            side = OpenSeaOrderSide.fromBigInteger(feeMethodsSidesKindsHowToCalls[1]),
            saleKind = OpenSeaOrderSaleKind.fromBigInteger(feeMethodsSidesKindsHowToCalls[2]),
            target = addrs[4],
            howToCall = OpenSeaOrderHowToCall.fromBigInteger(feeMethodsSidesKindsHowToCalls[3]),
            callData = Binary.apply(calldataBuy),
            replacementPattern = Binary.apply(replacementPatternBuy),
            staticTarget = addrs[5],
            staticExtraData = Binary.apply(staticExtradataBuy),
            paymentToken = addrs[6],
            basePrice = uints[4],
            extra = uints[5],
            listingTime = uints[6],
            expirationTime = uints[7],
            salt = uints[8]
        )
        val sellOrder = OpenSeaTransactionOrder(
            exchange = addrs[7],
            maker = addrs[8],
            taker = addrs[9],
            makerRelayerFee = uints[9],
            takerRelayerFee = uints[10],
            makerProtocolFee = uints[11],
            takerProtocolFee = uints[12],
            feeRecipient = addrs[10],
            feeMethod = OpenSeaOrderFeeMethod.fromBigInteger(feeMethodsSidesKindsHowToCalls[4]),
            side = OpenSeaOrderSide.fromBigInteger(feeMethodsSidesKindsHowToCalls[5]),
            saleKind = OpenSeaOrderSaleKind.fromBigInteger(feeMethodsSidesKindsHowToCalls[6]),
            target = addrs[11],
            howToCall = OpenSeaOrderHowToCall.fromBigInteger(feeMethodsSidesKindsHowToCalls[7]),
            callData = Binary.apply(calldataSell),
            replacementPattern = Binary.apply(replacementPatternSell),
            staticTarget = addrs[12],
            staticExtraData = Binary.apply(staticExtradataSell),
            paymentToken = addrs[13],
            basePrice = uints[13],
            extra = uints[14],
            listingTime = uints[15],
            expirationTime = uints[16],
            salt = uints[17]
        )
        val origin = Binary.apply(rssMetadata[4])
        return OpenSeaMatchedOrders(
            buyOrder = buyOrder,
            sellOrder = sellOrder,
            origin = origin
        )
    }

    fun parseCancelOrder(input: Binary): OpenSeaTransactionOrder? {
        val signature = WyvernExchange.cancelOrder_Signature()
        if (signature.id() != input.methodSignatureId()) return null

        val decoded = signature.`in`().decode(input, 4)
        val addrs = decoded.value()._1()
        val uints = decoded.value()._2()
        val feeMethod = decoded.value()._3()
        val side = decoded.value()._4()
        val saleKind = decoded.value()._5()
        val howToCall = decoded.value()._6()
        val calldata = decoded.value()._7()
        val replacementPattern = decoded.value()._8()
        val staticExtradata = decoded.value()._9()

        return OpenSeaTransactionOrder(
            exchange = addrs[0],
            maker = addrs[1],
            taker = addrs[2],
            makerRelayerFee = uints[0],
            takerRelayerFee = uints[1],
            makerProtocolFee = uints[2],
            takerProtocolFee = uints[3],
            feeRecipient = addrs[3],
            feeMethod = OpenSeaOrderFeeMethod.fromBigInteger(feeMethod),
            side = OpenSeaOrderSide.fromBigInteger(side),
            saleKind = OpenSeaOrderSaleKind.fromBigInteger(saleKind),
            target = addrs[4],
            howToCall = OpenSeaOrderHowToCall.fromBigInteger(howToCall),
            callData = Binary.apply(calldata),
            replacementPattern = Binary.apply(replacementPattern),
            staticTarget = addrs[5],
            staticExtraData = Binary.apply(staticExtradata),
            paymentToken = addrs[6],
            basePrice = uints[4],
            extra = uints[5],
            listingTime = uints[6],
            expirationTime = uints[7],
            salt = uints[8]
        )
    }

    companion object {
        private val ZERO_WORD: Word = Word.apply("0x0000000000000000000000000000000000000000000000000000000000000000")
    }
}
