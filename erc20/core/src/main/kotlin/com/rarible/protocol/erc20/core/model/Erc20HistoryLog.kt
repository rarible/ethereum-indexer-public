package com.rarible.protocol.erc20.core.model

import com.rarible.ethereum.listener.log.domain.LogEvent

sealed class Erc20HistoryLog {
    abstract val log: LogEvent
    abstract val history: Erc20TokenHistory
}

data class Erc20IncomeTransferHistoryLog(
    override val log: LogEvent,
    override val history: Erc20IncomeTransfer
) : Erc20HistoryLog()

data class Erc20OutcomeTransferHistoryLog(
    override val log: LogEvent,
    override val history: Erc20OutcomeTransfer
) : Erc20HistoryLog()

data class Erc20DepositHistoryLog(
    override val log: LogEvent,
    override val history: Erc20Deposit
) : Erc20HistoryLog()

data class Erc20WithdrawalHistoryLog(
    override val log: LogEvent,
    override val history: Erc20Withdrawal
) : Erc20HistoryLog()

data class Erc20TokenApprovalHistoryLog(
    override val log: LogEvent,
    override val history: Erc20TokenApproval
) : Erc20HistoryLog()

