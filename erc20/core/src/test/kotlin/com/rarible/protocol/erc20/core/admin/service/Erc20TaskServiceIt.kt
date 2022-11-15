package com.rarible.protocol.erc20.core.admin.service

import com.rarible.core.task.TaskStatus
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomLong
import com.rarible.protocol.erc20.core.admin.model.ReduceErc20BalanceTaskParam
import com.rarible.protocol.erc20.core.admin.model.ReindexErc20TokenTaskParam
import com.rarible.protocol.erc20.core.admin.repository.Erc20TaskRepository
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
class Erc20TaskServiceIt: AbstractIntegrationTest() {

    @Autowired
    lateinit var taskRepository: Erc20TaskRepository

    @Autowired
    lateinit var erc20TaskService: Erc20TaskService

    @Test
    fun `reindex erc20 token - create tasks`() = runBlocking<Unit> {
        val fromBlock = randomLong()
        val tokens = listOf(randomAddress(), randomAddress())

        val tasks = erc20TaskService.createReindexErc20TokenTasks(tokens, fromBlock, false)

        assertThat(tasks).hasSize(4)
        assertReindexErc20TokenTasks(tokens, fromBlock)
    }

    @Test
    fun `reindex erc20 token - tokens in progress`() = runBlocking<Unit> {
        val fromBlock = randomLong()
        val tokens = listOf(randomAddress(), randomAddress())

        erc20TaskService.createReindexErc20TokenTasks(tokens, fromBlock, false)

        assertThrows<IllegalArgumentException> {
            runBlocking {
                erc20TaskService.createReindexErc20TokenTasks(listOf(tokens[0]), fromBlock, false)
            }
        }
    }

    @Test
    fun `reduce erc20 balance - create task`() = runBlocking<Unit> {
        val token = randomAddress()
        erc20TaskService.createReduceErc20BalanceTask(token, true)

        val saved = taskRepository.findByType(
            ReduceErc20BalanceTaskParam.ADMIN_BALANCE_REDUCE,
            ReduceErc20BalanceTaskParam(token).toParamString()
        ).firstOrNull()

        assertThat(saved!!.lastStatus).isEqualTo(TaskStatus.NONE)
    }

    @Test
    fun `reduce erc20 balance - reduce in progress`() = runBlocking<Unit> {
        val token = randomAddress()
        erc20TaskService.createReduceErc20BalanceTask(token, true)
        assertThrows<IllegalArgumentException> {
            runBlocking {
                erc20TaskService.createReduceErc20BalanceTask(token, false)
            }
        }

    }

    private suspend fun assertReindexErc20TokenTasks(
        tokens: List<Address>,
        fromBlock: Long?
    ) {
        ReindexErc20TokenTaskParam.Descriptor.values().forEach {
            val task = taskRepository.findByType(
                ReindexErc20TokenTaskParam.ADMIN_REINDEX_ERC20_TOKENS,
                ReindexErc20TokenTaskParam(it, tokens).toParamString()
            ).toList()

            assertThat(task).hasSize(1)
            assertThat(task[0].lastStatus).isEqualTo(TaskStatus.NONE)
            assertThat(task[0].state).isEqualTo(fromBlock)
        }
    }

}