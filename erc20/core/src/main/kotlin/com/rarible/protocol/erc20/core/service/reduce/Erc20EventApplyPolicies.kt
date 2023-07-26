package com.rarible.protocol.erc20.core.service.reduce

import com.rarible.blockchain.scanner.ethereum.reduce.policy.ConfirmEventApplyPolicy
import com.rarible.blockchain.scanner.ethereum.reduce.policy.RevertEventApplyPolicy
import com.rarible.protocol.erc20.core.configuration.Erc20IndexerProperties
import com.rarible.protocol.erc20.core.model.Erc20Event
import org.springframework.stereotype.Component

@Component
class Erc20ConfirmEventApplyPolicy(properties: Erc20IndexerProperties) :
    ConfirmEventApplyPolicy<Erc20Event>(properties.confirmationBlocks)

@Component
class Erc20RevertEventApplyPolicy : RevertEventApplyPolicy<Erc20Event>()
