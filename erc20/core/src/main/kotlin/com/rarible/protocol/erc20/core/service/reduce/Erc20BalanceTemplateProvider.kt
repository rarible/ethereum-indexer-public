package com.rarible.protocol.erc20.core.service.reduce

import com.rarible.core.entity.reducer.service.EntityTemplateProvider
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.Erc20Balance
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class Erc20BalanceTemplateProvider : EntityTemplateProvider<BalanceId, Erc20Balance> {
    override fun getEntityTemplate(id: BalanceId, version: Long?): Erc20Balance {
        return Erc20Balance(
            owner = id.owner,
            token = id.token,
            version = version,
            balance = EthUInt256.ZERO,
            createdAt = Instant.EPOCH,
            lastUpdatedAt = Instant.EPOCH,
            revertableEvents = emptyList(),
            blockNumber = null
        )
    }
}
