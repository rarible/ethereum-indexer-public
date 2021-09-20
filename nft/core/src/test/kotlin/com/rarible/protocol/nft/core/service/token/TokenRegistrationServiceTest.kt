package com.rarible.protocol.nft.core.service.token

import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TokenRepository
import io.daonomic.rpc.RpcCodeException
import io.daonomic.rpc.domain.Error
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import reactor.core.publisher.Mono
import scala.Option
import scalether.domain.Address
import scalether.domain.AddressFactory
import scalether.transaction.MonoTransactionSender
import java.io.IOException

internal class TokenRegistrationServiceTest {
    private val tokenRepository = mockk<TokenRepository>()
    private val sender = mockk<MonoTransactionSender>()
    private val tokenRegistrationService = TokenRegistrationService(tokenRepository, sender)

    @Test
    fun `should return NONE toke standard on RpcCodeException`() = runBlocking<Unit> {
        every { sender.call(any()) } returns Mono.error(RpcCodeException("", Error(1, "", Option.empty())))

        val token = AddressFactory.create()
        val standard = tokenRegistrationService.fetchStandard(token).awaitFirst()

        assertThat(standard).isEqualTo(TokenStandard.NONE)
    }

    @Test
    fun `should return NONE toke standard on IllegalArgumentException`() = runBlocking<Unit> {
        every { sender.call(any()) } returns Mono.error(IllegalArgumentException(""))

        val token = AddressFactory.create()
        val standard = tokenRegistrationService.fetchStandard(token).awaitFirst()

        assertThat(standard).isEqualTo(TokenStandard.NONE)
    }

    @Test
    fun `should throw exception on other error`() = runBlocking<Unit> {
        every { sender.call(any()) } returns Mono.error(IOException())

        val token = AddressFactory.create()
        assertThrows<IOException> {
            runBlocking { tokenRegistrationService.fetchStandard(token).awaitFirst() }
        }
    }

    @Test
    fun `should return standard on well known token`() = runBlocking<Unit> {
        every { sender.call(any()) } returns Mono.error(IllegalArgumentException())

        val (token, standard) = TokenRegistrationService.WELL_KNOWN_TOKENS_WITHOUT_ERC165.entries.first()
        val fetchedStandard = tokenRegistrationService.fetchStandard(token).awaitFirst()
        assertThat(fetchedStandard).isEqualTo(standard)
    }
}
