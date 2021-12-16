package com.rarible.protocol.nft.core.converters.model

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.nft.core.model.BurnItemLazyMint
import com.rarible.protocol.nft.core.model.ItemCreators
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.ItemHistory
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemLazyMint
import com.rarible.protocol.nft.core.model.ItemRoyalty
import com.rarible.protocol.nft.core.model.ItemTransfer
import scalether.domain.Address

object ItemEventConverter {
    fun convert(source: ReversedEthereumLogRecord): ItemEvent? {
        return when (val data = source.data as? ItemHistory) {
            is ItemTransfer -> {
                when {
                    data.from == Address.ZERO() && data.owner == Address.ZERO() -> null

                    data.from == Address.ZERO() -> ItemEvent.ItemMintEvent(
                        supply = data.value,
                        owner = data.owner,
                        log = source.log,
                        entityId = ItemId(data.token, data.tokenId).stringValue
                    )
                    data.owner == Address.ZERO() -> ItemEvent.ItemBurnEvent(
                        supply = data.value,
                        owner = data.from,
                        log = source.log,
                        entityId = ItemId(data.token, data.tokenId).stringValue
                    )
                    else -> ItemEvent.ItemTransferEvent(
                        value = data.value,
                        from = data.from,
                        to = data.owner,
                        log = source.log,
                        entityId = ItemId(data.token, data.tokenId).stringValue
                    )
                }
            }
            is ItemLazyMint -> {
                ItemEvent.LazyItemMintEvent(
                    supply = data.value,
                    creators = data.creators,
                    log = source.log,
                    entityId = ItemId(data.token, data.tokenId).stringValue
                )
            }
            is BurnItemLazyMint -> {
                ItemEvent.LazyItemBurnEvent(
                    supply = data.value,
                    log = source.log,
                    entityId = ItemId(data.token, data.tokenId).stringValue
                )
            }
            is ItemCreators -> {
                ItemEvent.ItemCreatorsEvent(
                    creators = data.creators,
                    log = source.log,
                    entityId = ItemId(data.token, data.tokenId).stringValue
                )
            }
            is ItemRoyalty, null -> null
        }
    }

    fun convert(source: LogEvent): ItemEvent? {
        return convert(LogEventToReversedEthereumLogRecordConverter.convert(source))
    }
}
