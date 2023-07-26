package com.rarible.protocol.nft.core.service.ownership

import com.rarible.blockchain.scanner.ethereum.reduce.policy.ConfirmEventApplyPolicy
import com.rarible.blockchain.scanner.ethereum.reduce.policy.RevertEventApplyPolicy
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.OwnershipEvent
import org.springframework.stereotype.Component

@Component
class OwnershipConfirmEventApplyPolicy(properties: NftIndexerProperties) :
    ConfirmEventApplyPolicy<OwnershipEvent>(properties.confirmationBlocks)

@Component
class OwnershipRevertEventApplyPolicy : RevertEventApplyPolicy<OwnershipEvent>()
