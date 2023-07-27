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
import java.math.BigInteger

@Component
class ZeroExOrderParser(
    private val traceCallService: TraceCallService
) {

    suspend fun parseMatchOrdersData(
        txHash: Word,
        txInput: Binary,
        txFrom: Address,
        event: FillEvent,
        index: Int,
        totalLogs: Int
    ): List<ZeroExMatchOrdersData> {
        val calledMethodSignatureId = txInput.methodSignatureId()
        val supportedParsingMethods = listOf(
            ZeroExFeeWrapper.matchOrdersSignature().id(),
            Exchange.fillOrderSignature().id(),
            Exchange.matchOrdersSignature().id(),
            Exchange.executeTransactionSignature().id(),
            Exchange.batchFillOrdersSignature().id(),
        )
        if (calledMethodSignatureId !in supportedParsingMethods) {
            throw IllegalStateException("Unsupported method $calledMethodSignatureId for parsing $txHash")
        }

        val inputs = traceCallService.findAllRequiredCallInputs(
            txHash = txHash,
            txInput = txInput,
            to = event.log().address(),
            ids = arrayOf(calledMethodSignatureId!!)
        )
        return parse(
            txHash = txHash,
            txFrom = txFrom,
            calledMethodSignatureId = calledMethodSignatureId,
            inputs = inputs,
            index = index,
            totalLogs = totalLogs
        )
    }

    private fun parse(
        txHash: Word,
        txFrom: Address,
        calledMethodSignatureId: Binary,
        inputs: List<Binary>,
        index: Int,
        totalLogs: Int
    ): List<ZeroExMatchOrdersData> = when (calledMethodSignatureId) {
        ZeroExFeeWrapper.matchOrdersSignature().id() -> {
            require(inputs.size * 2 == totalLogs) {
                "Wrong number of inputs. Must be 'inputs.size * 2 == totalLogs' for wrapperMatchOrders. " +
                    "Tx: $txHash, inputs size: ${inputs.size}, totalLogs: $totalLogs"
            }
            val input = inputs[index / 2]
            parseWrapperMatchOrdersInput(input)
        }
        Exchange.matchOrdersSignature().id() -> {
            require(inputs.size * 2 == totalLogs) {
                "Wrong number of inputs. Must be 'inputs.size * 2 == totalLogs' for exchangeMatchOrders. " +
                    "Tx: $txHash, inputs size: ${inputs.size}, totalLogs: $totalLogs"
            }
            val input = inputs[index / 2]
            parseExchangeMatchOrdersInput(input)
        }
        Exchange.fillOrderSignature().id() -> {
            require(inputs.size == totalLogs) {
                "Wrong number of inputs. Must be 'inputs.size == totalLogs' for fillOrder. " +
                    "Tx: $txHash, inputs size: ${inputs.size}, totalLogs: $totalLogs"
            }
            val input = inputs[index]
            parseFillOrderInput(input, txFrom)
        }
        Exchange.batchFillOrdersSignature().id() -> {
            require(inputs.size == totalLogs) {
                "Wrong number of inputs. Must be 'inputs.size == totalLogs' for batchFillOrder. " +
                    "Tx: $txHash, inputs size: ${inputs.size}, totalLogs: $totalLogs"
            }
            val input = inputs[index]
            parseBatchFillOrdersInput(input, txFrom)
        }
        Exchange.executeTransactionSignature().id() -> {
            val input = inputs[index]
            val signature = Exchange.executeTransactionSignature()
            val decoded = signature.`in`().decode(input, 4).value()

            val transaction = decoded._1()
            val transactionSigner = transaction._4()
            val transactionData = Binary.apply(transaction._5())

            parseExecuteTransactionData(input = transactionData, txFrom = transactionSigner, txHash = txHash)
        }
        else -> throw IllegalStateException("Unsupported method $calledMethodSignatureId for parsing. txHash: $txHash")
    }

    private fun parseWrapperMatchOrdersInput(input: Binary): List<ZeroExMatchOrdersData> {
        val signature = ZeroExFeeWrapper.matchOrdersSignature()
        val decoded = signature.`in`().decode(input, 4).value()

        val leftOrder = parseOrder(decoded._1())
        val rightOrder = parseOrder(decoded._2())
        val leftSignature = Binary.apply(decoded._3())
        val rightSignature = Binary.apply(decoded._4())
        val feeData = decoded._5().map { parseFeeData(it) }
        val paymentTokenAddress = decoded._6()

        return listOf(
            ZeroExMatchOrdersData(
                leftOrder = leftOrder,
                takerAddress = null,
                rightOrder = rightOrder,
                leftSignature = leftSignature,
                rightSignature = rightSignature,
                feeData = feeData,
                paymentTokenAddress = paymentTokenAddress,
            )
        )
    }

    private fun parseExchangeMatchOrdersInput(input: Binary): List<ZeroExMatchOrdersData> {
        val signature = Exchange.matchOrdersSignature()
        val decoded = signature.`in`().decode(input, 4).value()

        val leftOrder = parseOrder(decoded._1())
        val rightOrder = parseOrder(decoded._2())
        val leftSignature = Binary.apply(decoded._3())
        val rightSignature = Binary.apply(decoded._4())

        return listOf(
            ZeroExMatchOrdersData(
                leftOrder = leftOrder,
                takerAddress = null,
                rightOrder = rightOrder,
                leftSignature = leftSignature,
                rightSignature = rightSignature,
            )
        )
    }

    private fun parseFillOrderInput(input: Binary, txFrom: Address): List<ZeroExMatchOrdersData> {
        val signature = Exchange.fillOrderSignature()
        val decoded = signature.`in`().decode(input, 4).value()

        val leftOrder = parseOrder(decoded._1())
        val leftSignature = Binary.apply(decoded._3())

        return listOf(
            ZeroExMatchOrdersData(
                leftOrder = leftOrder,
                takerAddress = txFrom,
                leftSignature = leftSignature
            )
        )
    }

    private fun parseBatchFillOrdersInput(input: Binary, txFrom: Address): List<ZeroExMatchOrdersData> {
        val signature = Exchange.batchFillOrdersSignature()
        val decoded = signature.`in`().decode(input, 4).value()
        val orders = decoded._1()
        val signatures = decoded._3()
        return orders.mapIndexed { i, orderData ->
            ZeroExMatchOrdersData(
                leftOrder = parseOrder(orderData),
                takerAddress = txFrom,
                leftSignature = Binary.apply(signatures[i])
            )
        }
    }

    private fun parseExecuteTransactionData(input: Binary, txFrom: Address, txHash: Word): List<ZeroExMatchOrdersData> =
        when (input.methodSignatureId()) {
            ZeroExFeeWrapper.matchOrdersSignature().id() -> parseWrapperMatchOrdersInput(input)
            Exchange.matchOrdersSignature().id() -> parseExchangeMatchOrdersInput(input)
            Exchange.fillOrderSignature().id() -> parseFillOrderInput(input, txFrom)
            Exchange.batchFillOrdersSignature().id() -> parseBatchFillOrdersInput(input, txFrom)
            else -> throw IllegalStateException(
                "Unsupported method ${input.methodSignatureId()} for parsing of execute transaction. txHash: $txHash"
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
