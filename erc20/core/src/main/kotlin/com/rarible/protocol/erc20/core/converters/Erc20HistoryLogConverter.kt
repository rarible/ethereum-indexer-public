package com.rarible.protocol.erc20.core.converters

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.protocol.erc20.core.model.Erc20Deposit
import com.rarible.protocol.erc20.core.model.Erc20DepositHistoryLog
import com.rarible.protocol.erc20.core.model.Erc20HistoryLog
import com.rarible.protocol.erc20.core.model.Erc20IncomeTransfer
import com.rarible.protocol.erc20.core.model.Erc20IncomeTransferHistoryLog
import com.rarible.protocol.erc20.core.model.Erc20OutcomeTransfer
import com.rarible.protocol.erc20.core.model.Erc20OutcomeTransferHistoryLog
import com.rarible.protocol.erc20.core.model.Erc20Withdrawal
import com.rarible.protocol.erc20.core.model.Erc20WithdrawalHistoryLog

object Erc20HistoryLogConverter {
    fun convert(log: ReversedEthereumLogRecord): Erc20HistoryLog? = when (val logData = log.data) {
        is Erc20IncomeTransfer -> Erc20IncomeTransferHistoryLog(log, logData)
        is Erc20OutcomeTransfer -> Erc20OutcomeTransferHistoryLog(log, logData)
        is Erc20Deposit -> Erc20DepositHistoryLog(log, logData)
        is Erc20Withdrawal -> Erc20WithdrawalHistoryLog(log, logData)
        else -> null
    }
}
