package com.rarible.protocol.nft.core.service.composit

import com.rarible.core.entity.reducer.service.EntityTemplateProvider
import com.rarible.protocol.nft.core.model.*
import org.springframework.stereotype.Component

@Component
class CompositeTemplateProvider : EntityTemplateProvider<ItemId, CompositeEntity> {
    override fun getEntityTemplate(id: ItemId): CompositeEntity {
        return CompositeEntity(id = id, item = null, ownerships = emptyList())
    }
}
