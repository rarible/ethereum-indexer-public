package com.rarible.protocol.nft.core.service.token

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBinary
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenByteCode
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.service.token.filter.TokeByteCodeFilter
import io.daonomic.rpc.RpcCodeException
import io.daonomic.rpc.domain.Error
import io.daonomic.rpc.domain.WordFactory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import reactor.core.publisher.Mono
import scala.Option
import scalether.domain.AddressFactory
import scalether.transaction.MonoTransactionSender
import java.io.IOException

@Suppress("ReactiveStreamsUnusedPublisher")
internal class TokenProviderTest {

    private val sender = mockk<MonoTransactionSender>()
    private val tokenByteCodeService = mockk<TokenByteCodeService> {
        coEvery { getByteCode(any()) } returns null
    }
    private val tokeByteCodeFilter = mockk<TokeByteCodeFilter> {
        every { isValid(any()) } returns true
    }
    private val tokenProvider = TokenProvider(
        sender = sender,
        tokenByteCodeService = tokenByteCodeService,
        tokeByteCodeFilters = listOf(tokeByteCodeFilter)
    )

    @Test
    fun `should return NONE token standard on RpcCodeException`() = runBlocking<Unit> {
        every { sender.call(any()) } returns Mono.error(RpcCodeException("", Error(1, "", Option.empty())))

        val token = AddressFactory.create()
        val standard = tokenProvider.fetchTokenStandard(token)

        assertThat(standard).isEqualTo(TokenStandard.NONE)
    }

    @Test
    fun `should return NONE token standard on IllegalArgumentException`() = runBlocking<Unit> {
        every { sender.call(any()) } returns Mono.error(IllegalArgumentException(""))

        val token = AddressFactory.create()
        val standard = tokenProvider.fetchTokenStandard(token)

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
        coEvery { tokenByteCodeService.getByteCode(token.id) } returns TokenByteCode(WordFactory.create(), code)
        val updatedToken = tokenProvider.detectScam(token).awaitFirst()

        assertThat(updatedToken.standard).isEqualTo(TokenStandard.NONE)
        assertThat(updatedToken?.scam).isTrue
    }

    @Test
    fun `should throw exception on other error`() = runBlocking<Unit> {
        every { sender.call(any()) } returns Mono.error(IOException())

        val token = AddressFactory.create()
        assertThrows<IOException> {
            runBlocking { tokenProvider.fetchTokenStandard(token) }
        }
    }

    @Test
    fun `should return standard on well known token`() = runBlocking<Unit> {
        every { sender.call(any()) } returns Mono.error(IllegalArgumentException())

        val (token, standard) = TokenProvider.WELL_KNOWN_TOKENS_WITHOUT_ERC165.entries.first()
        val fetchedStandard = tokenProvider.fetchTokenStandard(token)
        assertThat(fetchedStandard).isEqualTo(standard)
    }

}
