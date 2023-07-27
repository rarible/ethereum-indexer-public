package com.rarible.protocol.nft.core.service.token.reduce

import com.rarible.blockchain.scanner.ethereum.reduce.policy.ConfirmEventApplyPolicy
import com.rarible.blockchain.scanner.ethereum.reduce.policy.RevertEventApplyPolicy
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.TokenEvent
import org.springframework.stereotype.Component

@Component
class TokenConfirmEventApplyPolicy(properties: NftIndexerProperties) :
    ConfirmEventApplyPolicy<TokenEvent>(properties.confirmationBlocks)

@Component
class TokenRevertEventApplyPolicy : RevertEventApplyPolicy<TokenEvent>()
