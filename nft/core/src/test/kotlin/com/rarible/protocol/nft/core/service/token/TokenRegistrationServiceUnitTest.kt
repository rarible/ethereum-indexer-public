package com.rarible.protocol.nft.core.service.token

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBinary
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.token.TokenRepository
import com.rarible.protocol.nft.core.service.token.filter.TokeByteCodeFilter
import io.daonomic.rpc.RpcCodeException
import io.daonomic.rpc.domain.Error
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import scala.Option
import scalether.domain.AddressFactory
import scalether.transaction.MonoTransactionSender
import java.io.IOException

@Suppress("ReactiveStreamsUnusedPublisher")
internal class TokenRegistrationServiceUnitTest {
    private val tokenRepository = mockk<TokenRepository>()
    private val sender = mockk<MonoTransactionSender>()
    private val listener = mockk<TokenEventListener> {
        coEvery { onTokenChanged(any()) } returns Unit
    }
    private val tokenByteCodeProvider = mockk<TokenByteCodeProvider> {
        coEvery { fetchByteCode(any()) } returns null
    }
    private val tokeByteCodeFilter = mockk<TokeByteCodeFilter> {
        every { isValid(any()) } returns true
    }
    private val tokenRegistrationService = TokenRegistrationService(
        tokenRepository = tokenRepository,
        tokenListener = listener,
        sender = sender,
        tokenByteCodeProvider = tokenByteCodeProvider,
        tokeByteCodeFilters = listOf(tokeByteCodeFilter),
        cacheMaxSize = 1
    )

    @Test
    fun `should return NONE token standard on RpcCodeException`() = runBlocking<Unit> {
        every { sender.call(any()) } returns Mono.error(RpcCodeException("", Error(1, "", Option.empty())))

        val token = AddressFactory.create()
        val standard = tokenRegistrationService.fetchStandard(token).awaitFirst()

        assertThat(standard).isEqualTo(TokenStandard.NONE)
    }

    @Test
    fun `should return NONE token standard on IllegalArgumentException`() = runBlocking<Unit> {
        every { sender.call(any()) } returns Mono.error(IllegalArgumentException(""))

        val token = AddressFactory.create()
        val standard = tokenRegistrationService.fetchStandard(token).awaitFirst()

        assertThat(standard).isEqualTo(TokenStandard.NONE)
    }

    @Test
    fun `detect scam`() = runBlocking<Unit> {
        val token = Token(
            id = randomAddress(),
            name = "Name",
            standard = TokenStandard.ERC721,
            scam = false
        )
        val code = randomBinary(100)

        every { tokeByteCodeFilter.isValid(code) } returns false
        coEvery { tokenByteCodeProvider.fetchByteCode(token.id) } returns code
        val updatedToken = tokenRegistrationService.detectScam(token).awaitFirst()

        assertThat(updatedToken.standard).isEqualTo(TokenStandard.NONE)
        assertThat(updatedToken?.scam).isTrue
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

    @Test
    fun `cache limit`() = runBlocking<Unit> {
        val limitedCache = TokenRegistrationService(
            tokenRepository, listener, mockk(), tokenByteCodeProvider, emptyList(), 3
        )
        every { tokenRepository.findById(any()) } answers {
            Token(
                id = firstArg(),
                name = "Name",
                standard = TokenStandard.ERC721
            ).toMono()
        }

        val address1 = randomAddress()
        limitedCache.getTokenStandard(address1).awaitFirst()
        limitedCache.getTokenStandard(randomAddress()).awaitFirst()
        limitedCache.getTokenStandard(randomAddress()).awaitFirst()
        verify(exactly = 3) { tokenRepository.findById(any()) }

        // address1 is cached
        limitedCache.getTokenStandard(address1).awaitFirst()
        verify(exactly = 3) { tokenRepository.findById(any()) }

        // evict all addresses
        limitedCache.getTokenStandard(randomAddress()).awaitFirst()
        limitedCache.getTokenStandard(randomAddress()).awaitFirst()
        limitedCache.getTokenStandard(randomAddress()).awaitFirst()
        verify(exactly = 6) { tokenRepository.findById(any()) }

        // address1 was not cached.
        limitedCache.getTokenStandard(address1).awaitFirst()
        verify(exactly = 7) { tokenRepository.findById(any()) }
    }
}
