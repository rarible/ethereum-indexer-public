package com.rarible.protocol.erc20.listener.service.balance

import com.rarible.core.reduce.service.ReduceService
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.BalanceReduceSnapshot
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.model.Erc20ReduceEvent

typealias Erc20BalanceReduceService = ReduceService<Erc20ReduceEvent, BalanceReduceSnapshot, Long, Erc20Balance, BalanceId>
