package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.EthUInt256
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import scalether.domain.Address
import java.math.BigInteger

data class BlurV2Take(
    val orders: List<BlurV2Order>,
    val exchanges: List<BlurV2Exchange>,
    val takerFee: BlurV2FeeRate,
    val signatures: Binary,
    val tokenRecipient: Address = Address.ZERO()
)

data class BlurV2TakeAsk(
    val orders: List<Order>,
    val exchanges: List<BlurV2Exchange>,
    val takerFee: BlurV2FeeRate,
    val signatures: Binary,
    val tokenRecipient: Address
)

data class BlurV2TakeAskSingle(
    val order: Order,
    val exchange: BlurV2Exchange,
    val takerFee: BlurV2FeeRate,
    val signature: Binary,
    val tokenRecipient: Address
)

data class BlurV2TakeBid(
    val orders: List<Order>,
    val exchanges: List<BlurV2Exchange>,
    val takerFee: BlurV2FeeRate,
    val signatures: Binary
)

data class BlurV2TakeBidSingle(
    val order: Order,
    val exchange: BlurV2Exchange,
    val takerFee: BlurV2FeeRate,
    val signature: Binary
)

data class BlurV2Exchange(
    val index: BigInteger,
    val listing: BlurV2Listing,
    val taker: BlurV2Taker
)

data class BlurV2Listing(
    val index: BigInteger,
    val tokenId: BigInteger,
    val amount: BigInteger,
    val price: BigInteger
)

data class BlurV2Taker(
    val tokenId: BigInteger,
    val amount: BigInteger
)

data class BlurV2Order(
    val trader: Address,
    val collection: Address,
    val numberOfListings: BigInteger,
    val expirationTime: BigInteger,
    val assetType: BlurV2AssetType,
    val makerFee: BlurV2FeeRate,
    val salt: BigInteger
)

data class BlurV2ExecutionEvent(
    val transfer: BlurV2Transfer,
    val orderHash: Word,
    val listingIndex: BigInteger,
    val price: BigInteger,
    val makerFee: BlurV2FeeRate?,
    val takerFee: BlurV2FeeRate?,
    val orderType: BlurV2OrderType
) {
    fun ethPayment(): Asset {
        return Asset(
            type = EthAssetType,
            value = EthUInt256.of(price)
        )
    }

    fun erc20Payment(token: Address): Asset {
        return Asset(
            type = Erc20AssetType(token),
            value = EthUInt256.of(price)
        )
    }

    fun nft(): Asset {
        return Asset(
            type = when (transfer.assetType) {
                BlurV2AssetType.ERC721 -> Erc721AssetType(
                    token = transfer.collection,
                    tokenId = EthUInt256.of(transfer.id)
                )
                BlurV2AssetType.ERC1155 -> Erc1155AssetType(
                    token = transfer.collection,
                    tokenId = EthUInt256.of(transfer.id)
                )
            },
            value = EthUInt256.of(transfer.amount)
        )
    }
}

data class BlurV2Transfer(
    val trader: Address,
    val id: BigInteger,
    val amount: BigInteger,
    val collection: Address,
    val assetType: BlurV2AssetType,
)

data class BlurV2FeeRate(
    val recipient: Address,
    val rate: BigInteger,
)

enum class BlurV2OrderType(val value: Int){
    ASK(0),
    BID(1),
    ;

    companion object {

        fun fromValue(value: BigInteger): BlurV2OrderType {
            return when (value.intValueExact()) {
                ASK.value -> ASK
                BID.value -> BID
                else -> throw IllegalArgumentException("Invalid value for OrderType: $value")
            }
        }
    }
}

enum class BlurV2AssetType(val value: Int) {
    ERC721(0),
    ERC1155(1),
    ;

    companion object {

        fun fromValue(value: BigInteger): BlurV2AssetType {
            return when (value.intValueExact()) {
                ERC721.value -> ERC721
                ERC1155.value -> ERC1155
                else -> throw IllegalArgumentException("Invalid value for AssetType: $value")
            }
        }
    }
}

