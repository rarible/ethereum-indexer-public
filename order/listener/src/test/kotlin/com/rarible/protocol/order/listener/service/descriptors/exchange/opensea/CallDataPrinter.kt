package com.rarible.protocol.order.listener.service.descriptors.exchange.opensea

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.wrapper.ExchangeWrapper
import com.rarible.protocol.contracts.exchange.wyvern.WyvernExchange
import com.rarible.protocol.order.core.model.Part
import com.rarible.protocol.order.core.model.Transfer
import com.rarible.protocol.order.core.service.CallDataEncoder
import io.daonomic.rpc.domain.Binary
import scalether.abi.Uint256Type
import scalether.domain.Address
import java.math.BigInteger

class CallDataPrinter {

    private var callDataEncoder = CallDataEncoder()

    fun printSinglePurchase(input: Binary) {
        val signature = ExchangeWrapper.singlePurchaseSignature()
        val decoded = signature.`in`().decode(input, 4)
        val purchaseDetails = decoded.value()._1()
        val originFees = decoded.value()._2().map { convertToFeePart(it) }
        val marketId = purchaseDetails._1()
        val amount = purchaseDetails._2()
        val data: ByteArray = purchaseDetails._3()

        println("====")
        println("MarketId:   $marketId")
        println("Amount:     $amount")
        println("OriginFees: $originFees")
        printAtomicMatchData(Binary.apply(data))
    }

    fun printAtomicMatchData(input: Binary) {
        val signature = WyvernExchange.atomicMatch_Signature()
        val decode = signature.decode(input)
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

        println("====")
        println("ADDRESSes:")
        println("(")
        println("buyData.exchange,          ${addrs[0]}")
        println("buyOrder.maker,            ${addrs[1]}")
        println("buyOrder.taker,            ${addrs[2]}")
        println("buyData.feeRecipient,      ${addrs[3]}")
        println("buyData.target             ${addrs[4]}")
        println("buyData.staticTarget,      ${addrs[5]}")
        println("buyOrder.make.type.token,  ${addrs[6]}")
        println()
        println("sellData.exchange,         ${addrs[7]}")
        println("sellOrder.maker,           ${addrs[8]}")
        println("sellOrder.taker            ${addrs[9]}")
        println("sellData.feeRecipient,     ${addrs[10]}")
        println("sellData.target            ${addrs[11]}")
        println("sellData.staticTarget,     ${addrs[12]}")
        println("sellOrder.take.type.token  ${addrs[13]}")
        println(")")
        println("UINTs:")
        println("(")
        println("buyData.makerRelayerFee,   ${uints[0]}")
        println("buyData.takerRelayerFee,   ${uints[1]}")
        println("buyData.makerProtocolFee,  ${uints[2]}")
        println("buyData.takerProtocolFee,  ${uints[3]}")
        println("buyOrder.make.value.value, ${uints[4]}")
        println("buyData.extra,             ${uints[5]}")
        println("buyOrder.start?            ${uints[6]}")
        println("buyOrder.end?              ${uints[7]}")
        println("buyOrder.salt.value,       ${uints[8]}")
        println()
        println("sellData.makerRelayerFee,  ${uints[9]}")
        println("sellData.takerRelayerFee,  ${uints[10]}")
        println("sellData.makerProtocolFee, ${uints[11]}")
        println("sellData.takerProtocolFee, ${uints[12]}")
        println("sellOrder.take.value.value,${uints[13]}")
        println("sellData.extra,            ${uints[14]}")
        println("sellOrder.start?           ${uints[15]}")
        println("sellOrder.end?             ${uints[16]}")
        println("sellOrder.salt.value       ${uints[17]}")
        println(")")
        println("feeMethodsSidesKindsHowToCalls:")
        println("(")
        println("buyData.feeMethod.value,   ${feeMethodsSidesKindsHowToCalls[0]}")
        println("buyData.side.value,        ${feeMethodsSidesKindsHowToCalls[1]}")
        println("buyData.saleKind.value,    ${feeMethodsSidesKindsHowToCalls[2]}")
        println("buyData.howToCall.value,   ${feeMethodsSidesKindsHowToCalls[3]}")
        println()
        println("sellData.feeMethod.value,  ${feeMethodsSidesKindsHowToCalls[4]}")
        println("sellData.side.value,       ${feeMethodsSidesKindsHowToCalls[5]}")
        println("sellData.saleKind.value,   ${feeMethodsSidesKindsHowToCalls[6]}")
        println("sellData.howToCall.value   ${feeMethodsSidesKindsHowToCalls[7]}")
        println(")")
        printInnerCallData(Binary.apply(calldataBuy), "buy")
        printInnerCallData(Binary.apply(calldataSell), "sell")

        println("buyData.replacementPattern ${Binary.apply(replacementPatternBuy)}")
        println("sellData.replacementPattern${Binary.apply(replacementPatternSell)}")
        println("buyData.staticExtraData    ${Binary.apply(staticExtradataBuy)}")
        println("sellData.staticExtraData   ${Binary.apply(staticExtradataSell)}")
        println()
        println("BigInteger(byteArrayOf(signature.v))")
        println("BigInteger(byteArrayOf(signature.v))")
        println()
        println("signature.r,")
        println("signature.s,")
        println("signature.r,")
        println("signature.s,")
        println("RARIBLE_PLATFORM_METADATA")
    }

    private fun printInnerCallData(input: Binary, type: String) {
        val transfer = callDataEncoder.decodeTransfer(input)
        println("(")
        if (transfer is Transfer.MerkleValidatorErc721Transfer) {
            println("${type}transfer.from              ${transfer.from}")
            println("${type}transfer.to                ${transfer.to}")
            println("${type}transfer.token             ${transfer.token}")
            println("${type}transfer.tokenId           ${transfer.tokenId}")
            println("${type}transfer.root              ${transfer.root}")
            println("${type}transfer.proof             ${transfer.proof}")
            println("${type}transfer.safe              ${transfer.safe}")
            println("${type}transfer.value             ${transfer.value}")
        }
        println(")")
    }

    private fun convertToFeePart(feeUint256: BigInteger): Part =
        Part(
            account = Address.apply(Uint256Type.encode(feeUint256).slice(12, 32)),
            value = EthUInt256.of(Uint256Type.encode(feeUint256).slice(0, 12).toBigInteger())
        )
}
