package com.rarible.protocol.nft.api.service.domain

import com.rarible.protocol.nft.api.exceptions.ValidationApiException
import com.rarible.protocol.nft.api.model.DomainResolveResult
import com.rarible.protocol.nft.api.model.DomainType
import com.rarible.protocol.nft.core.data.randomEnsDomain
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CompositeDomainResolverTest {
    private val resolver = mockk<DomainResolver> {
        every { types } returns listOf(DomainType.ENS)
    }
    private val composite = CompositeDomainResolver(listOf(resolver))

    @Test
    fun `resolve - ok`() = runBlocking<Unit> {
        val name = randomEnsDomain()
        val expectedResult = DomainResolveResult("0x")

        every { resolver.isValidName(name) } returns true
        coEvery { resolver.resolve(name) } returns expectedResult

        val result = composite.resolve(name)
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `resolve - false, invalid name`() {
        val name = randomEnsDomain()
        every { resolver.isValidName(name) } returns false

        assertThrows<ValidationApiException> {
            runBlocking {
                composite.resolve(name)
            }
        }
    }

    @Test
    fun `resolve - false, unsupported tld`() {
        val name = "test.xyz"
        assertThrows<ValidationApiException> {
            runBlocking {
                composite.resolve(name)
            }
        }
    }
}
