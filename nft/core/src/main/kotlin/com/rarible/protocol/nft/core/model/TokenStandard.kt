package com.rarible.protocol.nft.core.model

import com.rarible.protocol.contracts.collection.*
import com.rarible.protocol.contracts.erc1155.rarible.factory.Create1155RaribleProxyEvent
import com.rarible.protocol.contracts.erc1155.rarible.factory.user.Create1155RaribleUserProxyEvent
import com.rarible.protocol.contracts.erc721.rarible.factory.Create721RaribleProxyEvent
import com.rarible.protocol.contracts.erc721.rarible.factory.user.Create721RaribleUserProxyEvent
import com.rarible.protocol.nft.core.model.TokenFeature.*
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Bytes
import io.daonomic.rpc.domain.Word

enum class TokenStandard(val interfaceId: Bytes? = null) {
    ERC721(Binary.apply("0x80ac58cd")),
    ERC1155(Binary.apply("0xd9b67a26")),
    CRYPTO_PUNKS,
    DEPRECATED(Binary.apply("0x9a20483d")),
    NONE;

    companion object {
        val CREATE_TOPIC_MAP = mapOf<Word, Pair<TokenStandard, Set<TokenFeature>>>(
            CreateEvent.id() to Pair(ERC721, setOf(APPROVE_FOR_ALL, SET_URI_PREFIX, BURN)),
            CreateERC721_v4Event.id() to Pair(ERC721, setOf(APPROVE_FOR_ALL, SET_URI_PREFIX, BURN, MINT_WITH_ADDRESS, SECONDARY_SALE_FEES)),
            CreateERC1155_v1Event.id() to Pair(ERC1155, setOf(APPROVE_FOR_ALL, SET_URI_PREFIX, BURN, MINT_WITH_ADDRESS, SECONDARY_SALE_FEES)),

            CreateERC721RaribleUserEvent.id() to Pair(ERC721, setOf(APPROVE_FOR_ALL, SET_URI_PREFIX, BURN, MINT_WITH_ADDRESS, SECONDARY_SALE_FEES, MINT_AND_TRANSFER)),
            CreateERC721RaribleEvent.id() to Pair(ERC721, setOf(APPROVE_FOR_ALL, SET_URI_PREFIX, BURN, MINT_WITH_ADDRESS, SECONDARY_SALE_FEES, MINT_AND_TRANSFER)),
            CreateERC1155RaribleUserEvent.id() to Pair(ERC1155, setOf(APPROVE_FOR_ALL, SET_URI_PREFIX, BURN, MINT_WITH_ADDRESS, SECONDARY_SALE_FEES, MINT_AND_TRANSFER)),
            CreateERC1155RaribleEvent.id() to Pair(ERC1155, setOf(APPROVE_FOR_ALL, SET_URI_PREFIX, BURN, MINT_WITH_ADDRESS, SECONDARY_SALE_FEES, MINT_AND_TRANSFER)),

            Create721RaribleProxyEvent.id() to Pair(ERC721, setOf(APPROVE_FOR_ALL, SET_URI_PREFIX, BURN, MINT_WITH_ADDRESS, SECONDARY_SALE_FEES, MINT_AND_TRANSFER)),
            Create721RaribleUserProxyEvent.id() to Pair(ERC721, setOf(APPROVE_FOR_ALL, SET_URI_PREFIX, BURN, MINT_WITH_ADDRESS, SECONDARY_SALE_FEES, MINT_AND_TRANSFER)),
            Create1155RaribleProxyEvent.id() to Pair(ERC1155, setOf(APPROVE_FOR_ALL, SET_URI_PREFIX, BURN, MINT_WITH_ADDRESS, SECONDARY_SALE_FEES, MINT_AND_TRANSFER)),
            Create1155RaribleUserProxyEvent.id() to Pair(ERC1155, setOf(APPROVE_FOR_ALL, SET_URI_PREFIX, BURN, MINT_WITH_ADDRESS, SECONDARY_SALE_FEES, MINT_AND_TRANSFER))
        )
    }
}
