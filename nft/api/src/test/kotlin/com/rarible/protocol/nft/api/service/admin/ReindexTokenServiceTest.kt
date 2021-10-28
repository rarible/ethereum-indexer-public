package com.rarible.protocol.nft.api.service.admin

import com.rarible.core.task.Task
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.nft.core.model.ReduceTokenItemsTaskParams
import com.rarible.protocol.nft.core.model.ReindexTokenItemRoyaltiesTaskParam
import com.rarible.protocol.nft.core.model.ReindexTokenItemsTaskParams
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TempTaskRepository
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.service.token.TokenRegistrationService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import scalether.domain.Address
import scalether.domain.AddressFactory

class ReindexTokenServiceTest {
    private val tokenRegistrationService = mockk<TokenRegistrationService>()
    private val tokenRepository = mockk<TokenRepository>()
    private val taskRepository = mockk<TempTaskRepository>()

    private val service = ReindexTokenService(tokenRegistrationService, tokenRepository, taskRepository)

    @Test
    fun `should create token reindex task`() = runBlocking<Unit> {
        val token1 = AddressFactory.create()
        val token2 = AddressFactory.create()

        mockTokenRegistrationService(token1, TokenStandard.ERC721)
        mockTokenRegistrationService(token2, TokenStandard.ERC721)
        mockTokenRepositorySave()
        mockTaskRepositoryFindNothingByType(ReindexTokenItemsTaskParams.ADMIN_REINDEX_TOKEN_ITEMS)

        val task = service.createReindexTokenItemsTask(listOf(token1, token2), 100, false)
        assertThat(task.lastStatus).isEqualTo(TaskStatus.NONE)
        assertThat(task.running).isEqualTo(false)
        assertThat(task.state).isEqualTo(100L)

        with(ReindexTokenItemsTaskParams.fromParamString(task.param)) {
            assertThat(tokens).isEqualTo(listOf(token1, token2))
            assertThat(standard).isEqualTo(TokenStandard.ERC721)
        }
    }

    @Test
    fun `should create token reduce task`() = runBlocking<Unit> {
        val targetToken = AddressFactory.create()

        mockTokenRepositorySave()
        mockTaskRepositoryFindNothingByType(ReduceTokenItemsTaskParams.ADMIN_REDUCE_TOKEN_ITEMS)

        val task = service.createReduceTokenItemsTask(targetToken, false)
        assertThat(task.lastStatus).isEqualTo(TaskStatus.NONE)
        assertThat(task.running).isEqualTo(false)
        assertThat(task.state).isNull()

        with(ReduceTokenItemsTaskParams.fromParamString(task.param)) {
            assertThat(tokens).isEqualTo(tokens)
        }
    }

    @Test
    fun `should create token reduce task forcibly`() = runBlocking<Unit> {
        val targetToken = AddressFactory.create()

        mockTaskRepositoryFindTask(
            ReduceTokenItemsTaskParams.ADMIN_REDUCE_TOKEN_ITEMS,
            ReduceTokenItemsTaskParams(targetToken).toParamString(),
            running = false,
            lastStatus = TaskStatus.COMPLETED
        )
        mockTokenRepositorySave()

        val task = service.createReduceTokenItemsTask(targetToken, true)
        assertThat(task.lastStatus).isEqualTo(TaskStatus.NONE)
        assertThat(task.running).isEqualTo(false)
        assertThat(task.state).isNull()

        with(ReduceTokenItemsTaskParams.fromParamString(task.param)) {
            assertThat(oneToken).isEqualTo(targetToken)
        }
    }

    @Test
    fun `should create token items royalties reindex task`() = runBlocking<Unit> {
        val targetToken = AddressFactory.create()

        mockTokenRepositorySave()
        mockTaskRepositoryFindNothingByType(ReindexTokenItemRoyaltiesTaskParam.ADMIN_REINDEX_TOKEN_ITEM_ROYALTIES)

        val task = service.createReindexTokenItemRoyaltiesTask(targetToken, false)
        assertThat(task.lastStatus).isEqualTo(TaskStatus.NONE)
        assertThat(task.running).isEqualTo(false)
        assertThat(task.state).isNull()

        with(ReindexTokenItemRoyaltiesTaskParam.fromParamString(task.param)) {
            assertThat(tokens).isEqualTo(tokens)
        }
    }

    @Test
    fun `should throw exception if reduce token task exist`() = runBlocking<Unit> {
        val targetToken = AddressFactory.create()

        mockTaskRepositoryFindTask(
            ReduceTokenItemsTaskParams.ADMIN_REDUCE_TOKEN_ITEMS,
            ReduceTokenItemsTaskParams(targetToken).toParamString()
        )
        assertThrows<IllegalArgumentException> {
            runBlocking {
                service.createReduceTokenItemsTask(targetToken, false)
            }
        }
    }

    @Test
    fun `should throw exception if reindex royalties token task exist`() = runBlocking<Unit> {
        val targetToken = AddressFactory.create()

        mockTaskRepositoryFindTask(
            ReindexTokenItemRoyaltiesTaskParam.ADMIN_REINDEX_TOKEN_ITEM_ROYALTIES,
            ReindexTokenItemRoyaltiesTaskParam(targetToken).toParamString()
        )
        assertThrows<IllegalArgumentException> {
            runBlocking {
                service.createReindexTokenItemRoyaltiesTask(targetToken, false)
            }
        }
    }

    @Test
    fun `should throw exception if reindex token task exist`() = runBlocking<Unit> {
        val targetToken = AddressFactory.create()
        val existToken = AddressFactory.create()

        mockTokenRegistrationService(targetToken, TokenStandard.ERC1155)
        mockTokenRegistrationService(existToken, TokenStandard.ERC1155)

        mockTaskRepositoryFindTask(
            ReindexTokenItemsTaskParams.ADMIN_REINDEX_TOKEN_ITEMS,
            ReindexTokenItemsTaskParams(TokenStandard.ERC1155, listOf(existToken)).toParamString()
        )
        assertThrows<IllegalArgumentException> {
            runBlocking {
                service.createReindexTokenItemsTask(listOf(targetToken, existToken), 1, false)
            }
        }
    }

    private fun mockTokenRegistrationService(token: Address, standard: TokenStandard) {
        coEvery { tokenRegistrationService.getTokenStandard(eq(token)) } returns mono { standard }
    }

    private fun mockTokenRepositorySave() {
        coEvery { taskRepository.save(any()) } coAnswers { firstArg() }
    }

    private fun mockTaskRepositoryFindNothingByType(type: String) {
        coEvery { taskRepository.findByType(eq(type)) } returns flow {  }
    }

    private fun mockTaskRepositoryFindTask(
        type: String,
        param: String,
        running: Boolean = true,
        lastStatus: TaskStatus = TaskStatus.NONE
    ) {
        val flow = flow {
            emit(
                Task(
                    type = type,
                    running = running,
                    lastStatus = lastStatus,
                    param = param
                )
            )
        }
        coEvery { taskRepository.findByType(eq(type), eq(param)) } returns flow
        coEvery { taskRepository.findByType(eq(type)) } returns flow
    }
}
