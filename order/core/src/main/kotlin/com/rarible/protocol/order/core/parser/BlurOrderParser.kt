package com.rarible.protocol.order.core.parser

import com.rarible.protocol.contracts.exchange.blur.v1.BlurV1
import com.rarible.protocol.order.core.misc.methodSignatureId
import com.rarible.protocol.order.core.model.BlurExecution
import com.rarible.protocol.order.core.model.BlurFee
import com.rarible.protocol.order.core.model.BlurInput
import com.rarible.protocol.order.core.model.BlurOrder
import com.rarible.protocol.order.core.model.BlurOrderSide
import com.rarible.protocol.order.core.model.order.logger
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import scala.Tuple13
import scala.Tuple2
import scala.Tuple7
import scalether.domain.Address
import java.math.BigInteger

object BlurOrderParser {
    fun parserOrder(input: Binary, tx: Word): List<BlurOrder> {
        return when (input.methodSignatureId()) {
            BlurV1.cancelOrderSignature().id() -> {
                val value = getDecodedValue(BlurV1.cancelOrderSignature(), input)
                listOf(convert(value))
            }
            BlurV1.cancelOrdersSignature().id() -> {
                val value = getDecodedValue(BlurV1.cancelOrdersSignature(), input)
                value.map { convert(it) }
            }
            else -> throw IllegalArgumentException("Unsupported method in tx $tx")
        }
    }

    fun parseExecutions(input: Binary, tx: Word): List<BlurExecution> {
        return try {
            when (input.methodSignatureId()) {
                BlurV1.executeSignature().id() -> {
                    val value = getDecodedValue(BlurV1.executeSignature(), input)
                    listOf(convert(value))
                }
                BlurV1.bulkExecuteSignature().id() -> {
                    val values = getDecodedValue(BlurV1.bulkExecuteSignature(), input)
                    values.map { convert(it) }
                }
                else -> throw IllegalArgumentException("Unsupported method in tx $tx")
            }
        } catch (ex: Throwable) {
            logger.error("Can't parser Blur input for tx=$tx", ex)
            emptyList()
        }
    }

    private fun convert(value: Tuple2<Tuple7<Tuple13<Address, BigInteger, Address, Address, BigInteger, BigInteger, Address, BigInteger, BigInteger, BigInteger, Array<Tuple2<BigInteger, Address>>, BigInteger, ByteArray>, BigInteger, ByteArray, ByteArray, ByteArray, BigInteger, BigInteger>, Tuple7<Tuple13<Address, BigInteger, Address, Address, BigInteger, BigInteger, Address, BigInteger, BigInteger, BigInteger, Array<Tuple2<BigInteger, Address>>, BigInteger, ByteArray>, BigInteger, ByteArray, ByteArray, ByteArray, BigInteger, BigInteger>>): BlurExecution {
        return BlurExecution(
            sell = convertInput(value._1()),
            buy = convertInput(value._2())
        )
    }

    private fun convertInput(value: Tuple7<Tuple13<Address, BigInteger, Address, Address, BigInteger, BigInteger, Address, BigInteger, BigInteger, BigInteger, Array<Tuple2<BigInteger, Address>>, BigInteger, ByteArray>, BigInteger, ByteArray, ByteArray, ByteArray, BigInteger, BigInteger>): BlurInput {
        return BlurInput(
            v = value._2(),
            r = Binary.apply(value._3()),
            s = Binary.apply(value._4())
        )
    }

    fun convert(value: Tuple13<Address, BigInteger, Address, Address, BigInteger, BigInteger, Address, BigInteger, BigInteger, BigInteger, Array<Tuple2<BigInteger, Address>>, BigInteger, ByteArray>): BlurOrder {
        return BlurOrder(
            trader = value._1(),
            side = BlurOrderSide.fromValue(value._2()),
            matchingPolicy = value._3(),
            collection = value._4(),
            tokenId = value._5(),
            amount = value._6(),
            paymentToken = value._7(),
            price = value._8(),
            listingTime = value._9(),
            expirationTime = value._10(),
            fees = value._11().map { convert(it) },
            salt = value._12(),
            extraParams = Binary.apply(value._13()),
        )
    }

    private fun convert(value: Tuple2<BigInteger, Address>): BlurFee {
        return BlurFee(
            rate = value._1(),
            recipient = value._2(),
        )
    }

    private fun <T> getDecodedValue(signature: scalether.abi.Signature<T,*>, input: Binary): T {
        return signature.`in`().decode(input, 4).value()
    }
}