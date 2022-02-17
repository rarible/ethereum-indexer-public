package com.rarible.protocol.order.core.service

import com.rarible.contracts.erc1155.IERC1155
import com.rarible.contracts.erc721.IERC721
import com.rarible.protocol.contracts.Tuples
import com.rarible.protocol.contracts.external.opensea.MerkleValidator
import com.rarible.protocol.order.core.misc.clearAfter
import com.rarible.protocol.order.core.misc.plus
import com.rarible.protocol.order.core.model.ApplyReplacementPatternResult
import com.rarible.protocol.order.core.model.Transfer
import com.rarible.protocol.order.core.model.TransferCallData
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.bouncycastle.util.Arrays
import org.springframework.stereotype.Component
import scala.Tuple3
import scala.Tuple5
import scala.Tuple6
import scala.Tuple7
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
        return when (transfer) {
             is Transfer.Erc721Transfer -> TransferCallData(
                callData = ERC721_TRANSFER_SIGNATURE.encode(
                    Tuple3(
                        transfer.from,
                        transfer.to,
                        transfer.tokenId
                    )
                ),
                replacementPattern = METHOD_SIGNATURE + Tuples.erc721ReplacementPattern().encode(
                    Tuple3(
                        transfer.from.replacementValue.bytes(),
                        transfer.to.replacementValue.bytes(),
                        transfer.tokenId.replacementValue
                    )
                )
            )
            is Transfer.Erc1155Transfer -> TransferCallData(
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
                        transfer.from.replacementValue.bytes(),
                        transfer.to.replacementValue.bytes(),
                        transfer.tokenId.replacementValue,
                        transfer.value.replacementValue,
                        transfer.data.replacementValue.bytes()
                    )
                ).clearAfter(127)
            )
            is Transfer.MerkleValidatorErc1155Transfer -> TransferCallData(
                callData = ERC1155_MT_TRANSFER_SIGNATURE.encode(
                    Tuple7(
                        transfer.from,
                        transfer.to,
                        transfer.token,
                        transfer.tokenId,
                        transfer.amount,
                        transfer.root.bytes(),
                        transfer.proof.map { it.bytes() }.toTypedArray()
                    )
                ),
                replacementPattern = METHOD_SIGNATURE + Tuples.erc1155ReplacementPattern().encode(
                    Tuple5(
                        transfer.from.replacementValue.bytes(),
                        transfer.to.replacementValue.bytes(),
                        transfer.tokenId.replacementValue,
                        transfer.amount.replacementValue,
                        Binary.empty().replacementValue.bytes()
                    )
                ).clearAfter(127)
            )
            is Transfer.MerkleValidatorErc721Transfer -> TransferCallData(
                callData = (if (transfer.safe) ERC721_MT_SAFE_TRANSFER_SIGNATURE else ERC721_MT_TRANSFER_SIGNATURE).encode(
                    Tuple6(
                        transfer.from,
                        transfer.to,
                        transfer.token,
                        transfer.tokenId,
                        transfer.root.bytes(),
                        transfer.proof.map { it.bytes() }.toTypedArray()
                    )
                ),
                replacementPattern = METHOD_SIGNATURE + Tuples.erc721ReplacementPattern().encode(
                    Tuple3(
                        transfer.from.replacementValue.bytes(),
                        transfer.to.replacementValue.bytes(),
                        transfer.tokenId.replacementValue,
                    )
                ).clearAfter(127)
            )
        }
    }

    fun decodeTransfer(callData: Binary): Transfer {
        val id = callData.slice(0, 4)
        return when (id) {
            ERC721_MT_TRANSFER_SIGNATURE.id(), ERC721_MT_SAFE_TRANSFER_SIGNATURE.id() -> {
                val encoded = ERC721_MT_TRANSFER_SIGNATURE.`in`().decode(callData, 4)
                Transfer.MerkleValidatorErc721Transfer(
                    from = encoded.value()._1(),
                    to = encoded.value()._2(),
                    token = encoded.value()._3(),
                    tokenId = encoded.value()._4(),
                    root = Word(encoded.value()._5()),
                    proof = encoded.value()._6().map { Word(it) },
                    safe = id == ERC721_MT_SAFE_TRANSFER_SIGNATURE.id()
                )
            }
            ERC1155_MT_TRANSFER_SIGNATURE.id() -> {
                val encoded = ERC1155_MT_TRANSFER_SIGNATURE.`in`().decode(callData, 4)
                Transfer.MerkleValidatorErc1155Transfer(
                    from = encoded.value()._1(),
                    to = encoded.value()._2(),
                    token = encoded.value()._3(),
                    tokenId = encoded.value()._4(),
                    amount = encoded.value()._5(),
                    root = Word(encoded.value()._6()),
                    proof = encoded.value()._7().map { Word(it) },
                )
            }
            ERC721_TRANSFER_SIGNATURE.id(), IERC721.safeTransferFromSignature() -> {
                val encoded = IERC721.safeTransferFromSignature().`in`().decode(callData, 4)
                Transfer.Erc721Transfer(
                    from = encoded.value()._1(),
                    to = encoded.value()._2(),
                    tokenId = encoded.value()._3(),
                    safe = id == IERC721.safeTransferFromSignature()
                )
            }
            ERC1155_TRANSFER_SIGNATURE.id() -> {
                val encoded = IERC1155.safeTransferFromSignature().`in`().decode(callData, 4)
                Transfer.Erc1155Transfer(
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

        val ERC721_MT_TRANSFER_SIGNATURE: Signature<Tuple6<Address, Address, Address, BigInteger, ByteArray, Array<ByteArray>>, Boolean> = MerkleValidator.matchERC721UsingCriteriaSignature()
        val ERC721_MT_SAFE_TRANSFER_SIGNATURE: Signature<Tuple6<Address, Address, Address, BigInteger, ByteArray, Array<ByteArray>>, Boolean> = MerkleValidator.matchERC721WithSafeTransferUsingCriteriaSignature()
        val ERC1155_MT_TRANSFER_SIGNATURE: Signature<Tuple7<Address, Address, Address, BigInteger, BigInteger, ByteArray, Array<ByteArray>>, Boolean> = MerkleValidator.matchERC1155UsingCriteriaSignature()

        val ERC721_TRANSFER_SIGNATURE: Signature<Tuple3<Address, Address, BigInteger>, BoxedUnit> = IERC721.transferFromSignature()
        val ERC1155_TRANSFER_SIGNATURE: Signature<Tuple5<Address, Address, BigInteger, BigInteger, ByteArray>, BoxedUnit> = IERC1155.safeTransferFromSignature()
        val BIG_INTEGER_DEFAULT_VALUE: BigInteger = BigInteger.valueOf(0)
    }
}


