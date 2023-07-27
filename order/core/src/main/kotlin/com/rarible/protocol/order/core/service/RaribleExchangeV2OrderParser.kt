package com.rarible.protocol.order.core.service

import com.rarible.protocol.contracts.exchange.v2.events.MatchEvent
import com.rarible.protocol.contracts.exchange.v2.rev3.ExchangeV2
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.AmmNftAssetType
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.CollectionAssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.GenerativeArtAssetType
import com.rarible.protocol.order.core.model.NftAssetType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.RaribleMatchedOrders
import com.rarible.protocol.order.core.model.order.logger
import com.rarible.protocol.order.core.model.toAssetType
import com.rarible.protocol.order.core.parser.ExchangeV2OrderParser
import com.rarible.protocol.order.core.trace.TraceCallService
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component
import com.rarible.protocol.contracts.exchange.v2.rev3.MatchEvent as MatchEventRev3

@Component
class RaribleExchangeV2OrderParser(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val traceCallService: TraceCallService,
) {

    suspend fun parseMatchedOrders(
        txHash: Word,
        txInput: Binary,
        event: MatchEventRev3
    ): RaribleMatchedOrders? {
        val parsed = parse(txHash, txInput, event)
        val leftHash = Word.apply(event.leftHash())
        val rightHash = Word.apply(event.rightHash())
        return parsed.firstOrNull { leftHash == it.left.hash && rightHash == it.right.hash }
    }

    suspend fun parseMatchedOrders(txHash: Word, txInput: Binary, event: MatchEvent): RaribleMatchedOrders? {
        val parsed = parse(txHash, txInput, event)
        val leftAssetType = event.leftAsset().toAssetType()
        val rightAssetType = event.rightAsset().toAssetType()

        return parsed.firstOrNull { orders ->
            val leftHash = Order.hashKey(
                event.leftMaker(),
                if (orders.left.makeAssetType.isCollection) leftAssetType.tryToConvertInCollection() else leftAssetType,
                if (orders.left.takeAssetType.isCollection) rightAssetType.tryToConvertInCollection() else rightAssetType,
                orders.left.salt.value,
                orders.left.data
            )
            val rightHash = Order.hashKey(
                event.rightMaker(),
                if (orders.right.makeAssetType.isCollection) rightAssetType.tryToConvertInCollection() else rightAssetType,
                if (orders.right.takeAssetType.isCollection) leftAssetType.tryToConvertInCollection() else leftAssetType,
                orders.right.salt.value,
                orders.right.data
            )
            Word.apply(event.leftHash()) == leftHash && Word.apply(event.rightHash()) == rightHash
        }
    }

    private suspend fun parse(txHash: Word, txInput: Binary, event: Any): List<RaribleMatchedOrders> {
        val inputs = getInputs(txHash, txInput)
        val parsed = inputs.map { ExchangeV2OrderParser.parseMatchedOrders(it) }
        val forLog = { parsed.map { Triple(it, it.left.hash, it.right.hash) } }
        logger.info("Hash: $txHash; Event: $event; Parsed Matched Orders: ${forLog()}")
        return parsed
    }

    private suspend fun getInputs(txHash: Word, txInput: Binary): List<Binary> {
        return traceCallService.findAllRequiredCallInputs(
            txHash,
            txInput,
            exchangeContractAddresses.v2,
            ExchangeV2.matchOrdersSignature().id(),
            ExchangeV2.directPurchaseSignature().id(),
            ExchangeV2.directAcceptBidSignature().id(),
        )
    }

    private fun AssetType.tryToConvertInCollection(): AssetType {
        return when (this) {
            is NftAssetType -> CollectionAssetType(token)
            is AmmNftAssetType -> CollectionAssetType(token)
            is CollectionAssetType,
            is Erc20AssetType,
            is EthAssetType,
            is GenerativeArtAssetType -> this
        }
    }

    private val AssetType.isCollection: Boolean
        get() = this is CollectionAssetType
}
