package com.rarible.protocol.erc20.listener.scanner

import com.rarible.blockchain.scanner.ethereum.reconciliation.OnReconciliationListener
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import com.rarible.protocol.erc20.core.service.reduce.asEthereumLogRecord
import com.rarible.protocol.erc20.listener.service.Erc20BalanceReduceService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class FullReduceReconciliationListener(
    private val reducer: Erc20BalanceReduceService
) : OnReconciliationListener {
    override suspend fun onLogRecordEvent(groupId: String, logRecordEvents: List<LogRecordEvent>) = coroutineScope<Unit> {
        val reduceHistory = logRecordEvents
            .map { event -> event.record.asEthereumLogRecord() }
            .mapNotNull { record -> record.data as? Erc20TokenHistory }
            .map { history -> BalanceId(history.token, history.owner) }
            .distinct()

        reduceHistory
            .map { async {
                val balance = reducer.update(it.token, it.owner)
                logger.info("Reconciled balance: balanceId={}:{}, balance={}",
                    balance?.token, balance?.owner, balance?.balance?.value
                )
            } }
            .awaitAll()
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(FullReduceReconciliationListener::class.java)
    }
}
