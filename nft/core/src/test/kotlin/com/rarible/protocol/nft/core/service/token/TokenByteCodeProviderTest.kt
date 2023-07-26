package com.rarible.protocol.nft.core.service.token

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBinary
import io.daonomic.rpc.domain.Binary
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import scalether.core.MonoEthereum
import scalether.transaction.MonoTransactionSender
import java.lang.RuntimeException
import java.time.Duration

@Suppress("ReactiveStreamsUnusedPublisher")
internal class TokenByteCodeProviderTest {
    private val ethereum = mockk<MonoEthereum>()
    private val sender = mockk<MonoTransactionSender> {
        every { ethereum() } returns ethereum
    }
    private val provider = TokenByteCodeProvider(
        sender = sender,
        retryCount = 3,
        retryDelay = 0,
        codeCacheSize = 10,
        codeCacheExpireAfter = Duration.ofMinutes(1),
    )

    @Test
    fun `get and cache code - ok`() = runBlocking<Unit> {
        val address = randomAddress()
        val code = randomBinary()
        every { ethereum.ethGetCode(address, "latest") } returns Mono.just(code)
        val result = provider.fetchByteCode(address)
        assertThat(result).isEqualTo(code)

        val cachedResult = provider.fetchByteCode(address)
        assertThat(cachedResult).isEqualTo(code)

        verify(exactly = 1) { ethereum.ethGetCode(address, "latest") }
    }

    @Test
    fun `get code - ok with retry`() = runBlocking<Unit> {
        val address = randomAddress()
        val code = randomBinary()
        every { ethereum.ethGetCode(address, "latest") } returnsMany(listOf(
            Mono.just(Binary.empty()),
            Mono.error(RuntimeException("IO")),
            Mono.just(code)
        ))
        val result = provider.fetchByteCode(address)
        assertThat(result).isEqualTo(code)
        verify(exactly = 3) { ethereum.ethGetCode(address, "latest") }
    }
}
