package com.rarible.protocol.nft.core.service.token

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.repository.token.TokenByteCodeRepository
import com.rarible.protocol.nft.core.service.token.filter.ScamByteCodeHashCache
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.util.Hash

class TokenByteCodeServiceTest {
    private val byteCodeProvider = mockk<TokenByteCodeProvider>()
    private val tokenByteCodeRepository = mockk<TokenByteCodeRepository>()
    private val scamByteCodeHashCache = mockk<ScamByteCodeHashCache>()
    private val featureFlags = mockk<FeatureFlags>()

    private val service =
        TokenByteCodeService(byteCodeProvider, tokenByteCodeRepository, scamByteCodeHashCache, featureFlags)

    @Test
    fun `get and save byte code`() = runBlocking<Unit> {
        val address = randomAddress()
        val code = Binary.apply("0x1234")
        val hash = Word.apply(Hash.sha3(code.bytes()))

        coEvery { byteCodeProvider.fetchByteCode(address) } returns code
        coEvery { tokenByteCodeRepository.save(any()) } returns mockk()
        coEvery { tokenByteCodeRepository.exist(any()) } returns false
        every { featureFlags.saveTokenByteCode } returns true

        val result = service.registerByteCode(address)
        assertThat(result?.code).isEqualTo(code)
        assertThat(result?.hash).isEqualTo(hash)

        coVerify {
            tokenByteCodeRepository.save(withArg {
                assertThat(it.hash).isEqualTo(hash)
            })
        }
    }

    @Test
    fun `just get byte code`() = runBlocking<Unit> {
        val address = randomAddress()
        val code = Binary.apply("0x1234")
        val hash = Word.apply(Hash.sha3(code.bytes()))

        coEvery { byteCodeProvider.fetchByteCode(address) } returns code
        every { featureFlags.saveTokenByteCode } returns false

        val result = service.registerByteCode(address)
        assertThat(result?.code).isEqualTo(code)
        assertThat(result?.hash).isEqualTo(hash)

        coVerify(exactly = 0) { tokenByteCodeRepository.save(any()) }
    }
}
