package com.rarible.protocol.nft.api.service.admin

import com.rarible.core.task.Task
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TempTaskRepository
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.service.token.TokenRegistrationService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.AddressFactory

class ReindexTokenServiceTest : SpringContainerBaseTest() {
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
        mockTokenRepository()

        val task = service.createReindexTokenTask(listOf(token1, token2), 100)
        assertThat(task.lastStatus).isEqualTo(TaskStatus.NONE)
    }

    private fun mockTokenRegistrationService(token: Address, standard: TokenStandard) {
        coEvery { tokenRegistrationService.getTokenStandard(eq(token)) } returns mono { standard }
    }

    private fun mockTokenRepository() {
        coEvery { taskRepository.save(any()) } coAnswers {
            it.invocation.args.first() as Task
        }
    }
}
