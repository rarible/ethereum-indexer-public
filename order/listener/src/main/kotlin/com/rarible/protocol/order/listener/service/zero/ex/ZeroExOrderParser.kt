package com.rarible.protocol.order.listener.service.zero.ex

import com.rarible.protocol.contracts.exchange.zero.ex.FillEvent
import com.rarible.protocol.contracts.exchange.zero.ex.ZeroExFeeWrapper
import com.rarible.protocol.order.core.model.ZeroExFeeData
import com.rarible.protocol.order.core.model.ZeroExMatchOrdersData
import com.rarible.protocol.order.core.model.ZeroExOrder
import com.rarible.protocol.order.core.trace.TraceCallService
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component
import scala.Tuple14
import scala.Tuple2
import scalether.domain.Address
import java.math.BigInteger

@Component
class ZeroExOrderParser(
    private val traceCallService: TraceCallService
) {

    suspend fun parseMatchOrdersData(
        txHash: Word,
        txInput: Binary,
        event: FillEvent,
        index: Int,
        totalLogs: Int
    ): ZeroExMatchOrdersData {
        val signature = ZeroExFeeWrapper.matchOrdersSignature()
        val inputs = traceCallService.findAllRequiredCallInputs(
            txHash = txHash,
            txInput = txInput,
            to = event.log().address(),
            id = signature.id()
        )
        require(inputs.size * 2 == totalLogs) {
            "Number of events != number of traces for tx: $txHash. inputs size: ${inputs.size}, totalLogs: $totalLogs"
        }
        return parse(inputs[index / 2])
    }

    fun parse(input: Binary): ZeroExMatchOrdersData {
        val signature = ZeroExFeeWrapper.matchOrdersSignature()
        val decoded = signature.`in`().decode(input, 4).value()

        val leftOrder = parseOrder(decoded._1())
        val rightOrder = parseOrder(decoded._2())
        val leftSignature = Binary.apply(decoded._3())
        val rightSignature = Binary.apply(decoded._4())
        val feeData = decoded._5().map { parseFeeData(it) }
        val paymentTokenAddress = decoded._6()

        return ZeroExMatchOrdersData(
            leftOrder = leftOrder,
            rightOrder = rightOrder,
            leftSignature = leftSignature,
            rightSignature = rightSignature,
            feeData = feeData,
            paymentTokenAddress = paymentTokenAddress,
        )
    }

    private fun parseOrder(data: Tuple14<Address, Address, Address, Address, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, ByteArray, ByteArray, ByteArray, ByteArray>): ZeroExOrder =
        ZeroExOrder(
            makerAddress = data._1(),
            takerAddress = data._2(),
            feeRecipientAddress = data._3(),
            senderAddress = data._4(),
            makerAssetAmount = data._5(),
            takerAssetAmount = data._6(),
            makerFee = data._7(),
            takerFee = data._8(),
            expirationTimeSeconds = data._9(),
            salt = data._10(),
            makerAssetData = Binary.apply(data._11()),
            takerAssetData = Binary.apply(data._12()),
            makerFeeAssetData = Binary.apply(data._13()),
            takerFeeAssetData = Binary.apply(data._14()),
        )

    private fun parseFeeData(data: Tuple2<Address, BigInteger>): ZeroExFeeData =
        ZeroExFeeData(
            recipient = data._1(),
            paymentTokenAmount = data._2()
        )
}