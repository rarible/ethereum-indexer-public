package com.rarible.protocol.erc20.listener.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.logging.withTraceId
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.erc20.core.admin.Erc20TaskService
import com.rarible.protocol.erc20.core.admin.model.Erc20DuplicatedLogRecordsTaskParam
import com.rarible.protocol.erc20.core.converters.Erc20HistoryLogConverter
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.repository.Erc20TransferHistoryRepository
import com.rarible.protocol.erc20.core.service.Erc20BalanceService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class Erc20DuplicatedLogRecordsTaskHandler(
    private val objectMapper: ObjectMapper,
    private val erc20TransferHistoryRepository: Erc20TransferHistoryRepository,
    private val erc20TaskService: Erc20TaskService,
    private val erc20BalanceService: Erc20BalanceService,
) : TaskHandler<String> {
    override val type: String = Erc20DuplicatedLogRecordsTaskParam.ERC20_DUPLICATED_LOG_RECORDS_TASK

    override fun runLongTask(from: String?, param: String): Flow<String> = flow {
        val taskParam = objectMapper.readValue(param, Erc20DuplicatedLogRecordsTaskParam::class.java)
        erc20TransferHistoryRepository.findAll(from).collect { log ->
            val possibleDuplicates = erc20TransferHistoryRepository.findPossibleDuplicates(log)
            if (possibleDuplicates.isNotEmpty()) {
                val duplicates = possibleDuplicates.filter {
                    log == it.copy(
                        id = log.id,
                        createdAt = log.createdAt,
                        updatedAt = log.updatedAt,
                        minorLogIndex = log.minorLogIndex
                    )
                }
                if (duplicates.isNotEmpty()) {
                    val history = Erc20HistoryLogConverter.convert(log)!!.history
                    val balanceId = BalanceId(token = history.token, owner = history.owner)
                    val blockchainBalance = erc20BalanceService.getBlockchainBalance(balanceId)
                    val dbBalance = erc20BalanceService.get(balanceId)?.balance
                    if (blockchainBalance != dbBalance) {
                        val logs = (duplicates + log).sortedBy { it.minorLogIndex }
                        val mainLog = logs[0]
                        val duplicatedLogs = logs.subList(1, logs.size)
                        logger.info("Found duplicated logs. Main log: $mainLog. Duplicates: $duplicatedLogs")
                        if (taskParam.update) {
                            logger.info("Removing duplicates")
                            erc20TransferHistoryRepository.removeAll(duplicatedLogs)

                            logger.info("Schedule reduce for ${history.token}:${history.owner}")
                            erc20TaskService.createReduceTask(
                                token = history.token,
                                owner = history.owner,
                                force = true
                            )
                        }
                    }
                }
            }
            emit(log.id)
        }
    }.withTraceId()

    companion object {
        private val logger = LoggerFactory.getLogger(Erc20DuplicatedLogRecordsTaskHandler::class.java)
    }
}
