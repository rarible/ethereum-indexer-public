package com.rarible.protocol.nft.core.service

import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.OwnershipEvent
import org.springframework.stereotype.Component

@Component
class OwnershipEntityEventRevertService(properties: NftIndexerProperties)
    : ConfirmEventRevertService<OwnershipEvent>(properties)
