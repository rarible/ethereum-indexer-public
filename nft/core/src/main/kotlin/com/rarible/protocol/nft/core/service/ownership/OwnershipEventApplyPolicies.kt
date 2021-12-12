package com.rarible.protocol.nft.core.service.ownership

import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.service.policy.*
import org.springframework.stereotype.Component

@Component
class OwnershipConfirmEventApplyPolicy(properties: NftIndexerProperties) :
    ConfirmEventApplyPolicy<OwnershipEvent>(properties.confirmationBlocks)

@Component
class OwnershipRevertEventApplyPolicy :
    RevertEventApplyPolicy<OwnershipEvent>()

@Component
class OwnershipPendingEventApplyPolicy :
    PendingEventApplyPolicy<OwnershipEvent>()

@Component
class OwnershipInactiveEventApplyPolicy :
    InactiveEventApplyPolicy<OwnershipEvent>()
