package com.rarible.protocol.order.listener.service.zero.ex

import com.rarible.protocol.contracts.exchange.zero.ex.Exchange
import com.rarible.protocol.contracts.exchange.zero.ex.FillEvent
import com.rarible.protocol.contracts.exchange.zero.ex.ZeroExFeeWrapper
import com.rarible.protocol.order.core.misc.methodSignatureId
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
import java.lang.IllegalStateException
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
        val calledMethodSignatureId = txInput.methodSignatureId()
        val supportedParsingMethods = listOf(
            ZeroExFeeWrapper.matchOrdersSignature().id(),
            Exchange.fillOrderSignature().id(),
            Exchange.matchOrdersSignature().id()
        )
        if (calledMethodSignatureId !in supportedParsingMethods) {
            throw IllegalStateException("Unsupported method $calledMethodSignatureId for parsing")
        }

        val inputs = traceCallService.findAllRequiredCallInputs(
            txHash = txHash,
            txInput = txInput,
            to = event.log().address(),
            id = calledMethodSignatureId!!
        )
        require(
            calledMethodSignatureId == Exchange.fillOrderSignature().id() && inputs.size == totalLogs ||
                calledMethodSignatureId in
                listOf(ZeroExFeeWrapper.matchOrdersSignature().id(), Exchange.matchOrdersSignature().id()) &&
                inputs.size * 2 == totalLogs
        ) {
            "Number of events != number of traces for tx: $txHash. inputs size: ${inputs.size}, totalLogs: $totalLogs, " +
                "calledMethodSignatureId: $calledMethodSignatureId"
        }
        return parse(
            txHash = txHash,
            calledMethodSignatureId = calledMethodSignatureId,
            inputs = inputs,
            index = index,
            totalLogs = totalLogs
        )
    }

    private fun parse(
        txHash: Word,
        calledMethodSignatureId: Binary,
        inputs: List<Binary>,
        index: Int,
        totalLogs: Int
    ): ZeroExMatchOrdersData = when (calledMethodSignatureId) {
        ZeroExFeeWrapper.matchOrdersSignature().id() -> {
            require(inputs.size * 2 == totalLogs) {
                "Wrong number of inputs. Must be 'inputs.size * 2 == totalLogs' for wrapperMatchOrders. " +
                    "Tx: $txHash, inputs size: ${inputs.size}, totalLogs: $totalLogs"
            }
            val input = inputs[index / 2]
            val signature = ZeroExFeeWrapper.matchOrdersSignature()
            val decoded = signature.`in`().decode(input, 4).value()

            val leftOrder = parseOrder(decoded._1())
            val rightOrder = parseOrder(decoded._2())
            val leftSignature = Binary.apply(decoded._3())
            val rightSignature = Binary.apply(decoded._4())
            val feeData = decoded._5().map { parseFeeData(it) }
            val paymentTokenAddress = decoded._6()

            ZeroExMatchOrdersData(
                leftOrder = leftOrder,
                rightOrder = rightOrder,
                leftSignature = leftSignature,
                rightSignature = rightSignature,
                feeData = feeData,
                paymentTokenAddress = paymentTokenAddress,
            )
        }
        Exchange.matchOrdersSignature().id() -> {
            require(inputs.size * 2 == totalLogs) {
                "Wrong number of inputs. Must be 'inputs.size * 2 == totalLogs' for exchangeMatchOrders. " +
                    "Tx: $txHash, inputs size: ${inputs.size}, totalLogs: $totalLogs"
            }
            val input = inputs[index / 2]
            val signature = Exchange.matchOrdersSignature()
            val decoded = signature.`in`().decode(input, 4).value()

            val leftOrder = parseOrder(decoded._1())
            val rightOrder = parseOrder(decoded._2())
            val leftSignature = Binary.apply(decoded._3())
            val rightSignature = Binary.apply(decoded._4())

            ZeroExMatchOrdersData(
                leftOrder = leftOrder,
                rightOrder = rightOrder,
                leftSignature = leftSignature,
                rightSignature = rightSignature,
            )
        }
        Exchange.fillOrderSignature().id() -> {
            require(inputs.size == totalLogs) {
                "Wrong number of inputs. Must be 'inputs.size == totalLogs' for fillOrder. " +
                    "Tx: $txHash, inputs size: ${inputs.size}, totalLogs: $totalLogs"
            }
            val input = inputs[index]
            val signature = Exchange.fillOrderSignature()
            val decoded = signature.`in`().decode(input, 4).value()

            val leftOrder = parseOrder(decoded._1())
            val leftSignature = Binary.apply(decoded._3())

            ZeroExMatchOrdersData(
                leftOrder = leftOrder,
                leftSignature = leftSignature,
            )
        }
        else -> throw IllegalStateException("Unsupported method $calledMethodSignatureId for parsing")
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