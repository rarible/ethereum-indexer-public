package com.rarible.protocol.erc20.core.model

import com.rarible.core.reduce.model.ReduceSnapshot
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("erc20_balance_snapshot")
data class BalanceReduceSnapshot(
    @Id
    override val id: BalanceId,
    override val data: Erc20Balance,
    override val mark: Long
) : ReduceSnapshot<Erc20Balance, Long, BalanceId>()