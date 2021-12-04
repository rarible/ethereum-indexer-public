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
import scalether.util.Hash

enum class TokenStandard(
    val interfaceId: Bytes? = null,
    val functionSignatures: List<String> = emptyList()
) {
    /**
     * ERC721
     *
     * 0x80ac58cd ===
     *     balanceOf(address)
     *     ownerOf(uint256)
     *     approve(address,uint256)
     *     getApproved(uint256)
     *     setApprovalForAll(address,bool)
     *     isApprovedForAll(address,address)
     *     transferFrom(address,address,uint256)
     *     safeTransferFrom(address,address,uint256)
     *     safeTransferFrom(address,address,uint256,bytes)
     */
    ERC721(
        interfaceId = Binary.apply("0x80ac58cd"),
        functionSignatures = listOf(
            "balanceOf(address)",
            "ownerOf(uint256)",
            "approve(address,uint256)",
            "getApproved(uint256)",
            "setApprovalForAll(address,bool)",
            "isApprovedForAll(address,address)",
            "transferFrom(address,address,uint256)",
            "safeTransferFrom(address,address,uint256)",
            "safeTransferFrom(address,address,uint256,bytes)"
        )
    ),

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
    ERC1155(
        interfaceId = Binary.apply("0xd9b67a26"),
        functionSignatures = listOf(
            "balanceOf(address,uint256)",
            "balanceOfBatch(address[],uint256[])",
            "setApprovalForAll(address,bool)",
            "isApprovedForAll(address,address)",
            "safeTransferFrom(address,address,uint256,uint256,bytes)",
            "safeBatchTransferFrom(address,address,uint256[],uint256[],bytes)"
        )
    ),
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
    DEPRECATED(
        interfaceId = Binary.apply("0x9a20483d"),
        functionSignatures = listOf(
            "name()",
            "symbol()",
            "totalSupply()",
            "balanceOf(address)",
            "ownerOf(uint256)",
            "approve(address,uint256)",
            "transfer(address,uint256)",
            "transferFrom(address,address,uint256)",
            "tokensOfOwner(address)",
            "tokenMetadata(uint256,string)"
        )
    ),
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

fun calculateFunctionId(functionSignature: String): Binary =
    Binary.apply(Hash.sha3(functionSignature.toByteArray(Charsets.ISO_8859_1)).take(4).toByteArray())
