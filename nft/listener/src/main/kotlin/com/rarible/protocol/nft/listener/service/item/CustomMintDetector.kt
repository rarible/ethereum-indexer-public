package com.rarible.protocol.nft.listener.service.item

import com.rarible.contracts.erc1155.TransferSingleEvent
import com.rarible.contracts.erc721.TransferEvent
import com.rarible.protocol.nft.core.misc.methodSignatureId
import io.daonomic.rpc.domain.Binary
import org.springframework.stereotype.Component
import scalether.domain.response.Transaction

@Component
class CustomMintDetector {

    companion object {

        // Ethereum
        internal val MINT_METHOD_ID_SIGNATURE: Binary = Binary.apply("0x731133e9")
        internal val DISTRIBUTE_NFT_METHOD_ID_SIGNATURE: Binary = Binary.apply("0x3a9e8069")

        // Polygon
        // Example: https://polygonscan.com/tx/0x7726beeb43d56ff516cc5c842ab756a0da5e8dc35efe9731e87104cddd06a726
        internal val AIRDROP_METHOD_ID_SIGNATURE: Binary = Binary.apply("0xc204642c")

        // Example: https://polygonscan.com/tx/0x8c330658db60ebd4befdf9edfd3753e7f1448e7fed1a840322b9390408fef1e8
        internal val POLYGON_MINT_METHOD_ID_SIGNATURE: Binary = Binary.apply("0xea66696c")
    }

    private val erc721methodDetectors = listOf<MethodMintDetector<TransferEvent>>(
        MethodMintDetector(DISTRIBUTE_NFT_METHOD_ID_SIGNATURE) { e, t -> e.from() == e.log().address() }
    )

    private val erc1155methodDetectors = listOf<MethodMintDetector<TransferSingleEvent>>(
        MethodMintDetector(DISTRIBUTE_NFT_METHOD_ID_SIGNATURE) { e, t -> e._from() == e.log().address() },
        MethodMintDetector(MINT_METHOD_ID_SIGNATURE) { e, t -> e._from() == e._to() },
        MethodMintDetector(AIRDROP_METHOD_ID_SIGNATURE) { e, t -> t.from() == e._from() },
        MethodMintDetector(POLYGON_MINT_METHOD_ID_SIGNATURE) { e, t -> t.from() == e._from() },
    )

    fun isErc1155Mint(event: TransferSingleEvent, transaction: Transaction): Boolean {
        return erc1155methodDetectors.find { it.isMint(event, transaction) } != null
    }

    fun isErc721Mint(event: TransferEvent, transaction: Transaction): Boolean {
        return erc721methodDetectors.find { it.isMint(event, transaction) } != null
    }

    class MethodMintDetector<E>(
        private val method: Binary,
        private val match: (event: E, transaction: Transaction) -> Boolean
    ) {

        fun isMint(event: E, transaction: Transaction): Boolean {
            return isMethodCalled(transaction, method) && match(event, transaction)
        }

        private fun isMethodCalled(transaction: Transaction, method: Binary): Boolean {
            return transaction.input().methodSignatureId() == method
        }
    }
}
