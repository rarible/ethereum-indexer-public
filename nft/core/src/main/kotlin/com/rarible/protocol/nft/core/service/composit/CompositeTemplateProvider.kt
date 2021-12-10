package com.rarible.protocol.nft.core.service.composit

import com.rarible.core.entity.reducer.service.EntityTemplateProvider
import com.rarible.protocol.nft.core.model.*
import org.springframework.stereotype.Component

@Component
class CompositeTemplateProvider : EntityTemplateProvider<CompositeEntityId, CompositeEntity> {
    override fun getEntityTemplate(id: CompositeEntityId): CompositeEntity {
        return CompositeEntity(
            item = id.itemId?.let { Item.empty(it.token, it.tokenId) } ,
            ownerships = id.ownershipIds.map { Ownership.empty(it.token, it.tokenId, it.owner) }
        )
    }
}
