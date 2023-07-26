package com.rarible.protocol.nft.core.service.item

import com.rarible.blockchain.scanner.ethereum.reduce.policy.ConfirmEventApplyPolicy
import com.rarible.blockchain.scanner.ethereum.reduce.policy.RevertEventApplyPolicy
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ItemEvent
import org.springframework.stereotype.Component

@Component
class ItemConfirmEventApplyPolicy(properties: NftIndexerProperties) :
    ConfirmEventApplyPolicy<ItemEvent>(properties.confirmationBlocks)

@Component
class ItemRevertEventApplyPolicy :
    RevertEventApplyPolicy<ItemEvent>()
