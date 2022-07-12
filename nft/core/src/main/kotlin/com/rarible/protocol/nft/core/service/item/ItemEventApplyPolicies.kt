package com.rarible.protocol.nft.core.service.item

import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.service.policy.ConfirmEventApplyPolicy
import com.rarible.protocol.nft.core.service.policy.RevertEventApplyPolicy
import org.springframework.stereotype.Component

@Component
class ItemConfirmEventApplyPolicy(properties: NftIndexerProperties) :
    ConfirmEventApplyPolicy<ItemEvent>(properties.confirmationBlocks)

@Component
class ItemRevertEventApplyPolicy :
    RevertEventApplyPolicy<ItemEvent>()