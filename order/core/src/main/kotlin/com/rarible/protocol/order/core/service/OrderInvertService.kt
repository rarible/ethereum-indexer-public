package com.rarible.protocol.order.core.service

import com.rarible.core.common.nowMillis
import com.rarible.protocol.order.core.model.OpenSeaOrderSide
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderData
import com.rarible.protocol.order.core.model.OrderOpenSeaV1DataV1
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV2
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.Part
import com.rarible.protocol.order.core.model.Transfer
import com.rarible.protocol.order.core.model.TransferCallData
import com.rarible.protocol.order.core.model.invert
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigInteger

@Component
class OrderInvertService(
    private val callDataEncoder: CallDataEncoder
) {
    fun invert(
        order: Order,
        maker: Address,
        amount: BigInteger,
        salt: Word,
        originFees: List<Part>
    ): Order {
        return when (order.type) {
            OrderType.RARIBLE_V2 -> invertRaribleV2(order, maker, amount, salt, originFees)
            OrderType.OPEN_SEA_V1 -> invertOpenSeaV1(order, maker, amount, salt)
            OrderType.RARIBLE_V1 -> throw IllegalArgumentException("Order type ${order.type} not supported to be inverted")
            OrderType.CRYPTO_PUNKS -> throw IllegalArgumentException("Crypto punks orders cannot be inverted")
        }
    }

    private fun invertRaribleV2(
        order: Order,
        maker: Address,
        amount: BigInteger,
        salt: Word,
        originFees: List<Part>
    ): Order = order.invert(maker, amount, salt, newData = order.data.withOriginFees(originFees))

    private fun OrderData.withOriginFees(newFees: List<Part>) = when (this) {
        is OrderRaribleV2DataV1 -> copy(originFees = newFees)
        is OrderRaribleV2DataV2 -> copy(originFees = newFees)
        else -> this
    }

    private fun invertOpenSeaV1(
        order: Order,
        maker: Address,
        amount: BigInteger,
        salt: Word
    ): Order {
        val originData = order.data as? OrderOpenSeaV1DataV1 ?: error("Order ${order.hash} has unexpected data type")

        val transferCallData = invertCallData(
            maker,
            amount,
            originData.side,
            originData.callData
        )
        val invertedData = originData.copy(
            feeRecipient = Address.ZERO(),
            side = originData.side.invert(),
            callData = transferCallData.callData,
            replacementPattern = transferCallData.replacementPattern
        )
        val applyResult = callDataEncoder.applyReplacementPatterns(
            TransferCallData(originData.callData, originData.replacementPattern),
            TransferCallData(invertedData.callData, invertedData.replacementPattern)
        )
        if (applyResult.isValid().not()) {
            throw IllegalArgumentException("Illegal data to revert order, callData doesn't matched")
        }
        return order.invert(maker, amount, salt, newData = invertedData)
            .run {
                copy(
                    // Recalculate OpenSea-specific hash
                    hash = Order.hash(this),
                    start = nowMillis().epochSecond - 1,
                    end = null
                )
            }
    }

    private fun invertCallData(
        maker: Address,
        amount: BigInteger,
        side: OpenSeaOrderSide,
        callData: Binary
    ): TransferCallData {
        val (from, to) = when (side) {
            OpenSeaOrderSide.SELL -> Address.ZERO() to maker
            OpenSeaOrderSide.BUY -> maker to Address.ZERO()
        }
        val invertedTransfer = when (val transfer = callDataEncoder.decodeTransfer(callData)) {
            is Transfer.Erc1155Transfer -> Transfer.Erc1155Transfer(
                from = from,
                to = to,
                tokenId = transfer.tokenId,
                value = amount,
                data = transfer.data
            )
            is Transfer.Erc721Transfer -> Transfer.Erc721Transfer(
                from = from,
                to = to,
                tokenId = transfer.tokenId,
                safe = transfer.safe
            )
            is Transfer.MerkleValidatorErc1155Transfer -> Transfer.MerkleValidatorErc1155Transfer(
                from = from,
                to = to,
                token = transfer.token,
                tokenId = transfer.tokenId,
                value = amount,
                root = transfer.root,
                proof = transfer.proof
            )
            is Transfer.MerkleValidatorErc721Transfer -> Transfer.MerkleValidatorErc721Transfer(
                from = from,
                to = to,
                token = transfer.token,
                tokenId = transfer.tokenId,
                root = transfer.root,
                proof = transfer.proof,
                safe = transfer.safe
            )
        }
        return callDataEncoder.encodeTransferCallData(invertedTransfer)
    }

    private fun OpenSeaOrderSide.invert(): OpenSeaOrderSide {
        return when (this) {
            OpenSeaOrderSide.SELL -> OpenSeaOrderSide.BUY
            OpenSeaOrderSide.BUY -> OpenSeaOrderSide.SELL
        }
    }
}
