package com.rarible.protocol.erc20.core.service.reduce

import com.rarible.core.entity.reducer.service.StreamFullReduceService
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.model.Erc20Event
import com.rarible.protocol.erc20.core.service.Erc20BalanceService
import org.springframework.stereotype.Component

@Component
class Erc20BalanceFullReduceService(
    entityService: Erc20BalanceService,
    entityIdService: Erc20BalanceIdService,
    templateProvider: Erc20BalanceTemplateProvider,
    reducer: Erc20BalanceReducer
) : StreamFullReduceService<BalanceId, Erc20Event, Erc20Balance>(
    entityService = entityService,
    entityIdService = entityIdService,
    templateProvider = templateProvider,
    reducer = reducer
)
