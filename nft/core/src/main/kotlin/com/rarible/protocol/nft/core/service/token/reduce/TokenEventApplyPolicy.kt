package com.rarible.protocol.nft.core.service.token.reduce

import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.TokenEvent
import com.rarible.protocol.nft.core.service.policy.ConfirmEventApplyPolicy
import com.rarible.protocol.nft.core.service.policy.RevertEventApplyPolicy
import org.springframework.stereotype.Component

@Component
class TokenConfirmEventApplyPolicy(properties: NftIndexerProperties) :
    ConfirmEventApplyPolicy<TokenEvent>(properties.confirmationBlocks)

@Component
class TokenRevertEventApplyPolicy :
    RevertEventApplyPolicy<TokenEvent>()