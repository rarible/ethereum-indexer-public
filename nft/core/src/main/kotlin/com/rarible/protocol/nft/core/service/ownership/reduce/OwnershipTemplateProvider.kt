package com.rarible.protocol.nft.core.service.ownership.reduce

import com.rarible.core.entity.reducer.service.EntityTemplateProvider
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipId
import org.springframework.stereotype.Component

@Component
class OwnershipTemplateProvider : EntityTemplateProvider<OwnershipId, Ownership> {
    override fun getEntityTemplate(id: OwnershipId): Ownership {
        return Ownership.empty(id.token, id.tokenId, id.owner)
    }
}
