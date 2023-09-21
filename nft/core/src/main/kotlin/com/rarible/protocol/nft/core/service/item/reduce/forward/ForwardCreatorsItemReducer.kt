package com.rarible.protocol.nft.core.service.item.reduce.forward

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.core.service.item.ItemCreatorService
import com.rarible.protocol.nft.core.service.token.TokenService
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ForwardCreatorsItemReducer(
    private val creatorService: ItemCreatorService,
    private val nftIndexerProperties: NftIndexerProperties,
) : Reducer<ItemEvent, Item> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return when (event) {
            is ItemEvent.ItemCreatorsEvent -> {
                val creators = event.creators
                entity.copy(creators = getCreator(entity.id, creators), creatorsFinal = true)
            }

            is ItemEvent.ItemMintEvent -> {
                val creators =
                    if (entity.creatorsFinal ||
                        nftIndexerProperties.featureFlags.firstMinterIsCreator && entity.creators.isNotEmpty()
                    ) {
                        entity.creators
                    } else if (nftIndexerProperties.featureFlags.validateCreatorByTransactionSender &&
                        event.log.from != event.owner
                    ) {
                        emptyList()
                    } else {
                        listOf(Part.fullPart(event.owner))
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
        val start = System.currentTimeMillis()
        val result = creatorService
            .getCreator(itemId).awaitFirstOrNull()
            ?.let { listOf(Part.fullPart(it)) }
            ?: default
        logger.info("Fetched creator for Item {}: {} ({}ms)", itemId, result, System.currentTimeMillis() - start)
        return result
    }
}
