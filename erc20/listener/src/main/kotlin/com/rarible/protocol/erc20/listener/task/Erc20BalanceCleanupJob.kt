package com.rarible.protocol.erc20.listener.task

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.erc20.core.repository.Erc20ApprovalHistoryRepository
import com.rarible.protocol.erc20.core.repository.Erc20BalanceRepository
import com.rarible.protocol.erc20.core.repository.Erc20TransferHistoryRepository
import com.rarible.protocol.erc20.listener.configuration.Erc20BalanceCleanupJobProperties
import com.rarible.protocol.erc20.listener.service.IgnoredOwnersResolver
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.time.delay
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
@ExperimentalCoroutinesApi
class Erc20BalanceCleanupJob(
    private val cleaners: List<Erc20BalanceCleaner>,
    private val properties: Erc20BalanceCleanupJobProperties,
    meterRegistry: MeterRegistry,
) : SequentialDaemonWorker(
    meterRegistry = meterRegistry,
    properties = DaemonWorkerProperties().copy(
        pollingPeriod = properties.pollingPeriod,
        errorDelay = properties.errorDelay
    ),
    workerName = "erc20-balance-cleanup-job"
) {

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationStarted() = start()

    override suspend fun handle() {
        if (!properties.enabled) {
            return
        }
        cleaners.forEach { cleaner ->
            try {
                cleaner.cleanup()
                delay(pollingPeriod)
            } catch (ex: Throwable) {
                throw RuntimeException(ex)
            }
        }
    }
}

interface Erc20BalanceCleaner {
    suspend fun cleanup()
}

@Component
class Erc20IgnoredOwnerBalanceCleaner(
    private val ownerBalanceResolver: IgnoredOwnersResolver,
    private val erc20BalanceRepository: Erc20BalanceRepository,
    private val erc20ApprovalHistoryRepository: Erc20ApprovalHistoryRepository,
    private val erc20TransferHistoryRepository: Erc20TransferHistoryRepository
) : Erc20BalanceCleaner {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun cleanup() {
        val ignored = ownerBalanceResolver.resolve()
        logger.info("Starting to cleanup erc20 data for {} ignored owners", ignored.size)
        ignored.forEach {
            val approvals = erc20ApprovalHistoryRepository.deleteByOwner(it)
            val transfers = erc20TransferHistoryRepository.deleteByOwner(it)
            val balances = erc20BalanceRepository.deleteByOwner(it)
            if (approvals > 0 || transfers > 0 || balances > 0) {
                logger.info(
                    "Cleaned up {} approvals, {} transfers and {} balances for owner {}",
                    approvals, transfers, balances, it
                )
            }
        }
    }
}
