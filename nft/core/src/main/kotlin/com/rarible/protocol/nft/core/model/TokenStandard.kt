package com.rarible.protocol.nft.core.model

import com.rarible.protocol.contracts.collection.CreateERC1155RaribleEvent
import com.rarible.protocol.contracts.collection.CreateERC1155RaribleUserEvent
import com.rarible.protocol.contracts.collection.CreateERC1155_v1Event
import com.rarible.protocol.contracts.collection.CreateERC721RaribleEvent
import com.rarible.protocol.contracts.collection.CreateERC721RaribleUserEvent
import com.rarible.protocol.contracts.collection.CreateERC721_v4Event
import com.rarible.protocol.contracts.collection.CreateEvent
import com.rarible.protocol.nft.core.model.TokenFeature.*
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Bytes
import io.daonomic.rpc.domain.Word

enum class TokenStandard(val interfaceId: Bytes? = null) {
    /**
     * ERC721
     *
     * 0x80ac58cd ===
     *     bytes4(keccak256('balanceOf(address)')) == 0x70a08231
     *     bytes4(keccak256('ownerOf(uint256)')) == 0x6352211e
     *     bytes4(keccak256('approve(address,uint256)')) == 0x095ea7b3
     *     bytes4(keccak256('getApproved(uint256)')) == 0x081812fc
     *     bytes4(keccak256('setApprovalForAll(address,bool)')) == 0xa22cb465
     *     bytes4(keccak256('isApprovedForAll(address,address)')) == 0xe985e9c5
     *     bytes4(keccak256('transferFrom(address,address,uint256)')) == 0x23b872dd
     *     bytes4(keccak256('safeTransferFrom(address,address,uint256)')) == 0x42842e0e
     *     bytes4(keccak256('safeTransferFrom(address,address,uint256,bytes)')) == 0xb88d4fde
     */
    ERC721(Binary.apply("0x80ac58cd")),

    /**
     *  ERC1155
     *
     *  0xd9b67a26 ===
     *     bytes4(keccak256('balanceOf(address,uint256)')) == 0x00fdd58e
     *     bytes4(keccak256('balanceOfBatch(address[],uint256[])')) == 0x4e1273f4
     *     bytes4(keccak256('setApprovalForAll(address,bool)')) == 0xa22cb465
     *     bytes4(keccak256('isApprovedForAll(address,address)')) == 0xe985e9c5
     *     bytes4(keccak256('safeTransferFrom(address,address,uint256,uint256,bytes)')) == 0xf242432a
     *     bytes4(keccak256('safeBatchTransferFrom(address,address,uint256[],uint256[],bytes)')) == 0x2eb2c2d6
     */
    ERC1155(Binary.apply("0xd9b67a26")),
    CRYPTO_PUNKS,

    /**
     * DEPRECATED ERC721 (older set of methods)
     *
     * 0x9a20483d =
     *     bytes4(keccak256('name()')) ^
     *     bytes4(keccak256('symbol()')) ^
     *     bytes4(keccak256('totalSupply()')) ^
     *     bytes4(keccak256('balanceOf(address)')) ^
     *     bytes4(keccak256('ownerOf(uint256)')) ^
     *     bytes4(keccak256('approve(address,uint256)')) ^
     *     bytes4(keccak256('transfer(address,uint256)')) ^
     *     bytes4(keccak256('transferFrom(address,address,uint256)')) ^
     *     bytes4(keccak256('tokensOfOwner(address)')) ^
     *     bytes4(keccak256('tokenMetadata(uint256,string)'));
     */
    DEPRECATED(Binary.apply("0x9a20483d")),
    NONE;

    companion object {
        val CREATE_TOPIC_MAP = mapOf<Word, Pair<TokenStandard, Set<TokenFeature>>>(
            CreateEvent.id() to Pair(ERC721, setOf(APPROVE_FOR_ALL, SET_URI_PREFIX, BURN)),
            CreateERC721_v4Event.id() to Pair(
                ERC721,
                setOf(APPROVE_FOR_ALL, SET_URI_PREFIX, BURN, MINT_WITH_ADDRESS, SECONDARY_SALE_FEES)
            ),
            CreateERC1155_v1Event.id() to Pair(
                ERC1155,
                setOf(APPROVE_FOR_ALL, SET_URI_PREFIX, BURN, MINT_WITH_ADDRESS, SECONDARY_SALE_FEES)
            ),

            CreateERC721RaribleUserEvent.id() to Pair(
                ERC721,
                setOf(APPROVE_FOR_ALL, SET_URI_PREFIX, BURN, MINT_WITH_ADDRESS, SECONDARY_SALE_FEES, MINT_AND_TRANSFER)
            ),
            CreateERC721RaribleEvent.id() to Pair(
                ERC721,
                setOf(APPROVE_FOR_ALL, SET_URI_PREFIX, BURN, MINT_WITH_ADDRESS, SECONDARY_SALE_FEES, MINT_AND_TRANSFER)
            ),
            CreateERC1155RaribleUserEvent.id() to Pair(
                ERC1155,
                setOf(APPROVE_FOR_ALL, SET_URI_PREFIX, BURN, MINT_WITH_ADDRESS, SECONDARY_SALE_FEES, MINT_AND_TRANSFER)
            ),
            CreateERC1155RaribleEvent.id() to Pair(
                ERC1155,
                setOf(APPROVE_FOR_ALL, SET_URI_PREFIX, BURN, MINT_WITH_ADDRESS, SECONDARY_SALE_FEES, MINT_AND_TRANSFER)
            )
        )
    }
}
