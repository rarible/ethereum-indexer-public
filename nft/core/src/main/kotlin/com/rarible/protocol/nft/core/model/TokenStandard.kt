package com.rarible.protocol.nft.core.model

import com.rarible.protocol.contracts.collection.CreateERC1155RaribleEvent
import com.rarible.protocol.contracts.collection.CreateERC1155RaribleUserEvent
import com.rarible.protocol.contracts.collection.CreateERC1155_v1Event
import com.rarible.protocol.contracts.collection.CreateERC721RaribleEvent
import com.rarible.protocol.contracts.collection.CreateERC721RaribleUserEvent
import com.rarible.protocol.contracts.collection.CreateERC721_v4Event
import com.rarible.protocol.contracts.collection.CreateEvent
import com.rarible.protocol.nft.core.model.TokenFeature.APPROVE_FOR_ALL
import com.rarible.protocol.nft.core.model.TokenFeature.BURN
import com.rarible.protocol.nft.core.model.TokenFeature.MINT_AND_TRANSFER
import com.rarible.protocol.nft.core.model.TokenFeature.MINT_WITH_ADDRESS
import com.rarible.protocol.nft.core.model.TokenFeature.SECONDARY_SALE_FEES
import com.rarible.protocol.nft.core.model.TokenFeature.SET_URI_PREFIX
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
            CreateERC1155RaribleEvent.id() to Pair(ERC1155, setOf(APPROVE_FOR_ALL, SET_URI_PREFIX, BURN, MINT_WITH_ADDRESS, SECONDARY_SALE_FEES, MINT_AND_TRANSFER))
        )
    }
}
