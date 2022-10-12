package com.rarible.protocol.nft.core.service.item.reduce.forward

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.service.token.TokenRegistrationService
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Component
import java.math.BigInteger

@Component
class ForwardValueItemReducer(
    private val tokenRegistrationService: TokenRegistrationService
) : Reducer<ItemEvent, Item> {
    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return when (event) {
            is ItemEvent.ItemMintEvent -> entity.copy(supply = entity.supply + event.supply)
            is ItemEvent.ItemBurnEvent -> entity.copy(supply = entity.supply - event.supply)
            is ItemEvent.ItemCreatorsEvent,
            is ItemEvent.OpenSeaLazyItemMintEvent -> entity
            is ItemEvent.ItemTransferEvent -> {
                val standard = tokenRegistrationService.getTokenStandard(entity.token).awaitSingle()

                if (entity.supply.value == BigInteger.ZERO && standard == TokenStandard.ERC721) {
                    entity.copy(supply = entity.supply + event.value)
                } else {
                    entity
                }
            }

            is ItemEvent.LazyItemBurnEvent, is ItemEvent.LazyItemMintEvent ->
                throw IllegalArgumentException("This events can't be in this reducer")
        }
    }
}

