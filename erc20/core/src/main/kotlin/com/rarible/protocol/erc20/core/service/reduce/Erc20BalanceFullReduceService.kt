package com.rarible.protocol.erc20.core.service.reduce

import com.rarible.core.entity.reducer.service.StreamFullReduceService
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.model.Erc20MarkedEvent
import com.rarible.protocol.erc20.core.service.Erc20BalanceService
import org.springframework.stereotype.Component

@Component
class Erc20BalanceFullReduceService(
    entityService: Erc20BalanceService,
    entityIdService: Erc20BalanceIdService,
    templateProvider: Erc20BalanceTemplateProvider,
    reducer: Erc20BalanceReducer
) : StreamFullReduceService<BalanceId, Erc20MarkedEvent, Erc20Balance>(
    entityService = entityService,
    entityIdService = entityIdService,
    templateProvider = templateProvider,
    reducer = reducer
) {

    override fun isChanged(current: Erc20Balance?, result: Erc20Balance?): Boolean {
        return current?.balance != result?.balance
    }
}
