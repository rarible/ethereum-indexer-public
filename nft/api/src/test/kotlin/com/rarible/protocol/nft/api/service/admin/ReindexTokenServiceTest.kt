package com.rarible.protocol.nft.api.service.admin

import com.rarible.core.task.Task
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.nft.core.model.ReduceTokenItemsTaskParams
import com.rarible.protocol.nft.core.model.ReindexTokenRoyaltiesTaskParam
import com.rarible.protocol.nft.core.model.ReindexTokenTaskParams
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
    fun `should create toke reindex task`() = runBlocking<Unit> {
        val token1 = AddressFactory.create()
        val token2 = AddressFactory.create()

        mockTokenRegistrationService(token1, TokenStandard.ERC721)
        mockTokenRegistrationService(token2, TokenStandard.ERC721)
        mockTokenRepositorySave()
        mockTaskRepositoryFindNothingByType(ReindexTokenTaskParams.ADMIN_REINDEX_TOKEN)

        val task = service.createReindexTokenTask(listOf(token1, token2), 100)
        assertThat(task.lastStatus).isEqualTo(TaskStatus.NONE)
        assertThat(task.running).isEqualTo(false)
        assertThat(task.state).isEqualTo(100L)

        with(ReindexTokenTaskParams.fromParamString(task.param)) {
            assertThat(tokens).isEqualTo(listOf(token1, token2))
            assertThat(standard).isEqualTo(TokenStandard.ERC721)
        }
    }

    @Test
    fun `should create toke reduce task`() = runBlocking<Unit> {
        val targetToken = AddressFactory.create()

        mockTokenRepositorySave()
        mockTaskRepositoryFindNothingByType(ReduceTokenItemsTaskParams.ADMIN_REDUCE_TOKEN_ITEMS)

        val task = service.createReduceTokenItemsTask(targetToken)
        assertThat(task.lastStatus).isEqualTo(TaskStatus.NONE)
        assertThat(task.running).isEqualTo(false)
        assertThat(task.state).isNull()

        with(ReduceTokenItemsTaskParams.fromParamString(task.param)) {
            assertThat(token).isEqualTo(token)
        }
    }

    @Test
    fun `should create toke royalties task`() = runBlocking<Unit> {
        val targetToken = AddressFactory.create()

        mockTokenRepositorySave()
        mockTaskRepositoryFindNothingByType(ReindexTokenRoyaltiesTaskParam.ADMIN_REINDEX_TOKEN_ROYALTIES)

        val task = service.createReindexRoyaltiesTask(targetToken)
        assertThat(task.lastStatus).isEqualTo(TaskStatus.NONE)
        assertThat(task.running).isEqualTo(false)
        assertThat(task.state).isNull()

        with(ReindexTokenRoyaltiesTaskParam.fromParamString(task.param)) {
            assertThat(token).isEqualTo(token)
        }
    }

    @Test
    fun `should throw exception if reduce token task exist`() = runBlocking<Unit> {
        val targetToken = AddressFactory.create()

        mockTaskRepositoryFindRunningTask(
            ReduceTokenItemsTaskParams.ADMIN_REDUCE_TOKEN_ITEMS,
            ReduceTokenItemsTaskParams(targetToken).toParamString()
        )
        assertThrows<IllegalArgumentException> {
            runBlocking {
                service.createReduceTokenItemsTask(targetToken)
            }
        }
    }

    @Test
    fun `should throw exception if reindex royalties token task exist`() = runBlocking<Unit> {
        val targetToken = AddressFactory.create()

        mockTaskRepositoryFindRunningTask(
            ReindexTokenRoyaltiesTaskParam.ADMIN_REINDEX_TOKEN_ROYALTIES,
            ReindexTokenRoyaltiesTaskParam(targetToken).toParamString()
        )
        assertThrows<IllegalArgumentException> {
            runBlocking {
                service.createReindexRoyaltiesTask(targetToken)
            }
        }
    }

    @Test
    fun `should throw exception if reindex token task exist`() = runBlocking<Unit> {
        val targetToken = AddressFactory.create()
        val existToke = AddressFactory.create()

        mockTokenRegistrationService(targetToken, TokenStandard.ERC1155)
        mockTokenRegistrationService(existToke, TokenStandard.ERC1155)

        mockTaskRepositoryFindRunningTask(
            ReindexTokenTaskParams.ADMIN_REINDEX_TOKEN,
            ReindexTokenTaskParams(TokenStandard.ERC1155, listOf(existToke)).toParamString()
        )
        assertThrows<IllegalArgumentException> {
            runBlocking {
                service.createReindexTokenTask(listOf(targetToken, existToke), 1)
            }
        }
    }

    private fun mockTokenRegistrationService(token: Address, standard: TokenStandard) {
        coEvery { tokenRegistrationService.getTokenStandard(eq(token)) } returns mono { standard }
    }

    private fun mockTokenRepositorySave() {
        coEvery { taskRepository.save(any()) } coAnswers {
            it.invocation.args.first() as Task
        }
    }

    private fun mockTaskRepositoryFindNothingByType(type: String) {
        coEvery { taskRepository.findByType(eq(type)) } returns flow {  }
    }

    private fun mockTaskRepositoryFindRunningTask(type: String, param: String) {
        coEvery { taskRepository.findByType(eq(type)) } returns flow {
            emit(
                Task(
                    type = type,
                    running = true,
                    lastStatus = TaskStatus.NONE,
                    param = param
                )
            )
        }
    }
}
