package com.rarible.protocol.nft.core.converters.model

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.BurnItemLazyMint
import com.rarible.protocol.nft.core.model.ItemCreators
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.ItemHistory
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemLazyMint
import com.rarible.protocol.nft.core.model.ItemRoyalty
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.model.OwnershipId
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigInteger

@Component
class ItemEventConverter(
    properties: NftIndexerProperties
) {
    private val openSeaLazyMintAddress = Address.apply(properties.openseaLazyMintAddress)

    fun convertToOwnershipId(source: ReversedEthereumLogRecord): OwnershipId? {
        return (source.data as? ItemHistory)?.let { it.owner?.let { owner -> OwnershipId(it.token, it.tokenId, owner) } }
    }

    fun convertToMintEvent(source: OwnershipEvent.TransferToEvent): ItemEvent.ItemMintEvent {
        return OwnershipId.parseId(source.entityId).let {
            ItemEvent.ItemMintEvent(
                supply = source.value,
                owner = it.owner,
                entityId = entityId(it.token, it.tokenId),
                log = source.log,
            )
        }
    }

    fun convert(source: ReversedEthereumLogRecord): ItemEvent? {
        return when (val data = source.data as? ItemHistory) {
            is ItemTransfer -> {
                when {
                    data.from == Address.ZERO() && data.owner == Address.ZERO() -> null

                    data.isMintTransfer() ->
                        ItemEvent.ItemMintEvent(
                            supply = data.value,
                            owner = data.owner,
                            log = source.log,
                            entityId = entityId(data.token, data.tokenId),
                            tokenUri = data.tokenUri
                        )

                    data.isBurnTransfer() ->
                        ItemEvent.ItemBurnEvent(
                            supply = data.value,
                            owner = data.from,
                            log = source.log,
                            entityId = entityId(data.token, data.tokenId)
                        )

                    source.log.address == openSeaLazyMintAddress && isLazyMintTokenAddress(data.tokenId) ->
                        ItemEvent.OpenSeaLazyItemMintEvent(
                            from = data.from,
                            supply = data.value,
                            log = source.log,
                            entityId = entityId(data.token, data.tokenId)
                        )

                    else -> null
                }
            }
            is ItemLazyMint -> {
                ItemEvent.LazyItemMintEvent(
                    supply = data.value,
                    creators = data.creators,
                    log = source.log,
                    entityId = entityId(data.token, data.tokenId)
                )
            }
            is BurnItemLazyMint -> {
                ItemEvent.LazyItemBurnEvent(
                    supply = data.value,
                    log = source.log,
                    entityId = entityId(data.token, data.tokenId)
                )
            }
            is ItemCreators -> {
                ItemEvent.ItemCreatorsEvent(
                    creators = data.creators,
                    log = source.log,
                    entityId = entityId(data.token, data.tokenId)
                )
            }
            is ItemRoyalty, null -> null
        }
    }

    fun convert(source: LogEvent): ItemEvent? {
        return convert(LogEventToReversedEthereumLogRecordConverter.convert(source))
    }

    private fun isLazyMintTokenAddress(tokenId: EthUInt256): Boolean {
        return (tokenId.value < BigInteger.valueOf(2).pow(96)).not()
    }

    private fun entityId(token: Address, tokenId: EthUInt256) = ItemId(token, tokenId).stringValue
}
