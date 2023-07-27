package com.rarible.protocol.nft.core.service.item.reduce.forward

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.core.service.item.ItemCreatorService
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ForwardCreatorsItemReducer(
    private val creatorService: ItemCreatorService,
    private val nftIndexerProperties: NftIndexerProperties
) : Reducer<ItemEvent, Item> {

    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return when (event) {
            is ItemEvent.ItemCreatorsEvent -> {
                val creators = event.creators
                entity.copy(creators = getCreator(entity.id, creators), creatorsFinal = true)
            }
            is ItemEvent.ItemMintEvent -> {
                val creators = if (!entity.creatorsFinal) {
                    if (!nftIndexerProperties.featureFlags.validateCreatorByTransactionSender ||
                        event.log.from == event.owner
                    ) {
                        listOf(Part.fullPart(event.owner))
                    } else {
                        emptyList()
                    }
                } else {
                    entity.creators
                }
                entity.copy(
                    mintedAt = entity.mintedAt ?: event.log.blockTimestamp?.let { Instant.ofEpochSecond(it) },
                    creators = getCreator(entity.id, creators)
                )
            }
            is ItemEvent.OpenSeaLazyItemMintEvent,
            is ItemEvent.ItemTransferEvent,
            is ItemEvent.ItemBurnEvent -> {
                entity
            }
            is ItemEvent.LazyItemBurnEvent, is ItemEvent.LazyItemMintEvent -> {
                throw IllegalArgumentException("This events can't be in this reducer")
            }
        }
    }

    private suspend fun getCreator(itemId: ItemId, default: List<Part>): List<Part> {
        return creatorService
            .getCreator(itemId).awaitFirstOrNull()
            ?.let { listOf(Part.fullPart(it)) }
            ?: default
    }
}
