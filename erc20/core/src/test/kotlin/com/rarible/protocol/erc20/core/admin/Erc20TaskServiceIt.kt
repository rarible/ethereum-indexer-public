package com.rarible.protocol.erc20.core.admin

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.blockchain.scanner.ethereum.task.EthereumReindexParam
import com.rarible.blockchain.scanner.reindex.BlockRange
import com.rarible.core.task.TaskStatus
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomLong
import com.rarible.protocol.erc20.core.admin.model.Erc20DuplicatedLogRecordsTaskParam
import com.rarible.protocol.erc20.core.integration.AbstractIntegrationTest
import com.rarible.protocol.erc20.core.integration.IntegrationTest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address

@IntegrationTest
class Erc20TaskServiceIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var taskRepository: Erc20TaskRepository

    @Autowired
    lateinit var erc20TaskService: Erc20TaskService

    private val mapper = ObjectMapper().registerKotlinModule()

    @Test
    fun `reindex erc20 token - create task`() = runBlocking<Unit> {
        val fromBlock = randomLong()
        val tokens = listOf(randomAddress(), randomAddress())

        val task = erc20TaskService.createReindexTask(tokens, fromBlock, null, false)

        assertReindexErc20TokenTask(tokens, fromBlock)
    }

    @Test
    fun `reindex erc20 token - tokens in progress`() = runBlocking<Unit> {
        val fromBlock = randomLong()
        val tokens = listOf(randomAddress(), randomAddress())

        erc20TaskService.createReindexTask(tokens, fromBlock, null, false)

        assertThrows<IllegalArgumentException> {
            runBlocking {
                erc20TaskService.createReindexTask(listOf(tokens[0]), fromBlock, null, false)
            }
        }
    }

    @Test
    fun `reduce erc20 balance - create task`() = runBlocking<Unit> {
        val token = randomAddress()
        erc20TaskService.createReduceTask(token, null, true)

        val saved = taskRepository.findByTypeAndParam(
            Erc20ReduceTaskParam.TASK_TYPE,
            Erc20ReduceTaskParam(token, null).toString()
        ).firstOrNull()

        assertThat(saved!!.lastStatus).isEqualTo(TaskStatus.NONE)
    }

    @Test
    fun `reduce erc20 balance - reduce in progress`() = runBlocking<Unit> {
        val token = randomAddress()
        erc20TaskService.createReduceTask(token, null, true)
        assertThrows<IllegalArgumentException> {
            runBlocking {
                erc20TaskService.createReduceTask(token, null, false)
            }
        }
    }

    @Test
    fun `deduplicate erc20 logs - create task`() = runBlocking<Unit> {
        erc20TaskService.createDeduplicateTask(update = false, force = true)

        val saved = taskRepository.findByTypeAndParam(
            Erc20DuplicatedLogRecordsTaskParam.ERC20_DUPLICATED_LOG_RECORDS_TASK,
            mapper.writeValueAsString(Erc20DuplicatedLogRecordsTaskParam(update = false))
        ).firstOrNull()

        assertThat(saved!!.lastStatus).isEqualTo(TaskStatus.NONE)
    }

    private suspend fun assertReindexErc20TokenTask(
        tokens: List<Address>,
        fromBlock: Long
    ) {
        val task = taskRepository.findByTypeAndParam(
            "BLOCK_SCANNER_REINDEX_TASK",
            mapper.writeValueAsString(
                EthereumReindexParam(
                    range = BlockRange(fromBlock, null, 250),
                    topics = emptyList(),
                    addresses = tokens
                )
            )
        ).toList()

        assertThat(task).hasSize(1)
        assertThat(task[0].lastStatus).isEqualTo(TaskStatus.NONE)
        assertThat(task[0].state).isEqualTo(fromBlock)
    }
}
