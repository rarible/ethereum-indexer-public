package com.rarible.protocol.nft.core.repository.token

import com.rarible.protocol.nft.core.data.createTokenByteCode
import com.rarible.protocol.nft.core.test.AbstractIntegrationTest
import com.rarible.protocol.nft.core.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class TokenByteCodeRepositoryIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var tokenByteCodeRepository: TokenByteCodeRepository

    @Test
    fun `save and get`() = runBlocking<Unit> {
        val code = createTokenByteCode()

        tokenByteCodeRepository.save(code)

        val saved = tokenByteCodeRepository.get(code.hash)

        Assertions.assertThat(saved?.hash).isEqualTo(code.hash)
        Assertions.assertThat(saved?.code).isEqualTo(code.code)
    }

    @Test
    fun `evaluate existence`() = runBlocking<Unit> {
        val code = createTokenByteCode()

        Assertions.assertThat(
            tokenByteCodeRepository.exist(code.hash)
        ).isFalse()

        tokenByteCodeRepository.save(code)

        Assertions.assertThat(
            tokenByteCodeRepository.exist(code.hash)
        ).isTrue()
    }
}
