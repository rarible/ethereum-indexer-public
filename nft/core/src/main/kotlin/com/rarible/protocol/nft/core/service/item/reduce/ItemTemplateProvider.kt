package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.core.entity.reducer.service.EntityTemplateProvider
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemId
import org.springframework.stereotype.Component

@Component
class ItemTemplateProvider : EntityTemplateProvider<ItemId, Item> {
    override fun getEntityTemplate(id: ItemId): Item {
        return Item.empty(id.token, id.tokenId)
    }
}
