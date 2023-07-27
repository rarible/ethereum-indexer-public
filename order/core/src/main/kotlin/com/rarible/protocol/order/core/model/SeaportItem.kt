package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.EthUInt256
import scalether.domain.Address
import java.math.BigInteger

sealed class SeaportItem {
    abstract val token: Address
    abstract val identifier: BigInteger
    abstract val itemType: SeaportItemType

    fun toAssetType(): AssetType {
        val tokenId = EthUInt256.of(identifier)
        return when (itemType) {
            SeaportItemType.NATIVE -> EthAssetType
            SeaportItemType.ERC20 -> Erc20AssetType(token)
            SeaportItemType.ERC721 -> Erc721AssetType(token, tokenId)
            SeaportItemType.ERC1155 -> Erc1155AssetType(token, tokenId)
            SeaportItemType.ERC721_WITH_CRITERIA,
            SeaportItemType.ERC1155_WITH_CRITERIA -> throw IllegalArgumentException("Unsupported item type $itemType")
        }
    }

    fun toAsset(amount: BigInteger): Asset {
        return Asset(toAssetType(), EthUInt256.of(amount))
    }

    fun isSupportedItem(): Boolean {
        return when (itemType) {
            SeaportItemType.NATIVE,
            SeaportItemType.ERC20,
            SeaportItemType.ERC721,
            SeaportItemType.ERC1155 -> true
            SeaportItemType.ERC721_WITH_CRITERIA,
            SeaportItemType.ERC1155_WITH_CRITERIA -> false
        }
    }
}

sealed class SeaportEventItem : SeaportItem() {
    abstract val amount: BigInteger

    fun toAsset(): Asset {
        return toAsset(amount)
    }
}

data class SeaportSpentItem(
    override val itemType: SeaportItemType,
    override val token: Address,
    override val identifier: BigInteger,
    override val amount: BigInteger
) : SeaportEventItem()

data class SeaportReceivedItem(
    override val itemType: SeaportItemType,
    override val token: Address,
    override val identifier: BigInteger,
    override val amount: BigInteger,
    val recipient: Address
) : SeaportEventItem() {

    fun withAmount(amount: BigInteger): SeaportReceivedItem {
        return copy(amount = amount)
    }
}

sealed class SeaportOrderItem : SeaportItem() {
    abstract val startAmount: BigInteger
    abstract val endAmount: BigInteger

    fun toAssetWithStartAmount(): Asset {
        return toAsset(startAmount)
    }
}

data class SeaportOffer(
    override val itemType: SeaportItemType,
    override val token: Address,
    override val identifier: BigInteger,
    override val startAmount: BigInteger,
    override val endAmount: BigInteger
) : SeaportOrderItem()

data class SeaportConsideration(
    override val itemType: SeaportItemType,
    override val token: Address,
    override val identifier: BigInteger,
    override val startAmount: BigInteger,
    override val endAmount: BigInteger,
    val recipient: Address
) : SeaportOrderItem() {
    fun withStartAmount(startAmount: BigInteger): SeaportConsideration {
        return copy(startAmount = startAmount)
    }
}
