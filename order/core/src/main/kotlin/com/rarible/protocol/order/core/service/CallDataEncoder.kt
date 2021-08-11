package com.rarible.protocol.order.core.service

import com.rarible.contracts.erc1155.IERC1155
import com.rarible.contracts.erc721.IERC721
import com.rarible.protocol.contracts.Tuples
import com.rarible.protocol.order.core.misc.clearAfter
import com.rarible.protocol.order.core.misc.plus
import com.rarible.protocol.order.core.model.ApplyReplacementPatternResult
import com.rarible.protocol.order.core.model.Transfer
import com.rarible.protocol.order.core.model.TransferCallData
import io.daonomic.rpc.domain.Binary
import org.bouncycastle.util.Arrays
import org.springframework.stereotype.Component
import scala.Tuple3
import scala.Tuple5
import scala.runtime.BoxedUnit
import scalether.abi.Signature
import scalether.domain.Address
import java.math.BigInteger
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.experimental.xor

@Component
class CallDataEncoder {
    fun encodeTransferCallData(transfer: Transfer): TransferCallData {
        val fromReplacementValue = transfer.from.replacementValue
        val toReplacementValue = transfer.to.replacementValue
        val tokenIdReplacementValue = transfer.tokenId.replacementValue
        val valueReplacementValue = transfer.value.replacementValue
        val dataReplacementValue = transfer.data.replacementValue

        return when (transfer.type) {
            Transfer.Type.ERC721 -> TransferCallData(
                callData = ERC721_TRANSFER_SIGNATURE.encode(
                    Tuple3(
                        transfer.from,
                        transfer.to,
                        transfer.tokenId
                    )
                ),
                replacementPattern = METHOD_SIGNATURE + Tuples.erc721ReplacementPattern().encode(
                    Tuple3(
                        fromReplacementValue.bytes(),
                        toReplacementValue.bytes(),
                        tokenIdReplacementValue
                    )
                )
            )
            Transfer.Type.ERC1155 -> TransferCallData(
                callData = ERC1155_TRANSFER_SIGNATURE.encode(
                    Tuple5(
                        transfer.from,
                        transfer.to,
                        transfer.tokenId,
                        transfer.value,
                        transfer.data.bytes()
                    )
                ),
                replacementPattern = METHOD_SIGNATURE + Tuples.erc1155ReplacementPattern().encode(
                    Tuple5(
                        fromReplacementValue.bytes(),
                        toReplacementValue.bytes(),
                        tokenIdReplacementValue,
                        valueReplacementValue,
                        dataReplacementValue.bytes()
                    )
                ).clearAfter(127)
            )
        }
    }

    fun decodeTransfer(callData: Binary): Transfer {
        return when (callData.slice(0, 4)) {
            ERC721_TRANSFER_SIGNATURE.id(), IERC721.safeTransferFromSignature() -> {
                val encoded = IERC721.safeTransferFromSignature().`in`().decode(callData, 4)
                Transfer(
                    type = Transfer.Type.ERC721,
                    from = encoded.value()._1(),
                    to = encoded.value()._2(),
                    tokenId = encoded.value()._3(),
                    value = BigInteger.ONE,
                    data = Binary.apply()
                )
            }
            ERC1155_TRANSFER_SIGNATURE.id() -> {
                val encoded = IERC1155.safeTransferFromSignature().`in`().decode(callData, 4)
                Transfer(
                    type = Transfer.Type.ERC1155,
                    from = encoded.value()._1(),
                    to = encoded.value()._2(),
                    tokenId = encoded.value()._3(),
                    value = encoded.value()._4(),
                    data = Binary.apply(encoded.value()._5())
                )
            }
            else -> throw IllegalArgumentException("Unsupported call data: $callData")
        }
    }

    fun applyReplacementPatterns(
        transferCallData1: TransferCallData,
        transferCallData2: TransferCallData
    ): ApplyReplacementPatternResult {
        val callData1 = Arrays.copyOfRange(transferCallData1.callData.bytes(), 0, transferCallData1.callData.bytes().size)
        val callData2 = Arrays.copyOfRange(transferCallData2.callData.bytes(), 0, transferCallData2.callData.bytes().size)

        if (transferCallData1.replacementPattern.length() > 0) {
            applyMask(callData1, callData2, transferCallData1.replacementPattern.bytes())
        }
        if (transferCallData2.replacementPattern.length() > 0) {
            applyMask(callData2, callData1, transferCallData2.replacementPattern.bytes())
        }
        return ApplyReplacementPatternResult(Binary.apply(callData1), Binary.apply(callData2))
    }

    private fun applyMask(array: ByteArray, desired: ByteArray, mask: ByteArray) {
        require(array.size == desired.size) { "array and desired must be the same size" }
        require(array.size == mask.size) { "array and mask must be the same size" }

        for (i in array.indices) {
            array[i] = ((mask[i] xor 0xff.toByte()) and array[i]) or (mask[i] and desired[i])
        }
    }

    private val Address.replacementValue: Binary
        get() = if (this != Address.ZERO()) DEFAULT_VALUE else MASK

    private val BigInteger.replacementValue: BigInteger
        get() = BIG_INTEGER_DEFAULT_VALUE

    private val Binary.replacementValue: Binary
        get() = if (length() > 0) Binary.apply(ByteArray(this.length())) else this

    private companion object {
        val METHOD_SIGNATURE: Binary = Binary.apply(ByteArray(4))
        val MASK: Binary = Binary.apply("0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")
        val DEFAULT_VALUE: Binary = Binary.apply("0x0000000000000000000000000000000000000000000000000000000000000000")

        val ERC721_TRANSFER_SIGNATURE: Signature<Tuple3<Address, Address, BigInteger>, BoxedUnit> = IERC721.transferFromSignature()
        val ERC1155_TRANSFER_SIGNATURE: Signature<Tuple5<Address, Address, BigInteger, BigInteger, ByteArray>, BoxedUnit> = IERC1155.safeTransferFromSignature()
        val BIG_INTEGER_DEFAULT_VALUE: BigInteger = BigInteger.valueOf(0)
    }
}


