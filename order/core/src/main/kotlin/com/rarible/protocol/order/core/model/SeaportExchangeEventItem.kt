package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.EthUInt256
import scalether.domain.Address
import java.math.BigInteger

sealed class SeaportExchangeEventItem {
    abstract val itemType: SeaportItemType
    abstract val token: Address
    abstract val identifier: BigInteger
    abstract val amount: BigInteger

    fun toAsset(): Asset {
        val value = EthUInt256.of(amount)
        val tokenId = EthUInt256.of(identifier)
        return when (itemType) {
            SeaportItemType.NATIVE -> Asset(EthAssetType, value)
            SeaportItemType.ERC20 -> Asset(Erc20AssetType(token), value)
            SeaportItemType.ERC721 -> Asset(Erc721AssetType(token, tokenId), value)
            SeaportItemType.ERC1155 -> Asset(Erc1155AssetType(token, tokenId), value)
            SeaportItemType.ERC721_WITH_CRITERIA,
            SeaportItemType.ERC1155_WITH_CRITERIA -> throw IllegalArgumentException("Unsupported item type $itemType")
        }
    }
}

data class SeaportSpentItem(
    override val itemType: SeaportItemType,
    override val token: Address,
    override val identifier: BigInteger,
    override val amount: BigInteger
) : SeaportExchangeEventItem()

data class SeaportReceivedItem(
    override val itemType: SeaportItemType,
    override val token: Address,
    override val identifier: BigInteger,
    override val amount: BigInteger,
    val recipient: Address
) : SeaportExchangeEventItem() {

    fun withAmount(amount: BigInteger): SeaportReceivedItem {
        return copy(amount = amount)

    }
}
