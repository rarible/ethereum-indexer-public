package com.rarible.protocol.nft.core.service

import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ItemEvent
import org.springframework.stereotype.Component

@Component
class ItemEntityEventRevertService(properties: NftIndexerProperties) :
    EntityEventRevertService<ItemEvent>(properties)
