package com.rarible.protocol.nft.api.e2e.domain

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.nft.api.test.AbstractIntegrationTest
import com.rarible.protocol.nft.api.test.End2EndTest
import com.rarible.protocol.nft.core.data.randomEnsDomain
import io.mockk.every
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.web3j.ens.EnsResolver
import scalether.domain.Address

@End2EndTest
class DomainControllerFt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var endResolver: EnsResolver

    @Test
    fun `resolve - ok`() = runBlocking<Unit> {
        val name = randomEnsDomain()
        val address = randomAddress().prefixed()
        every { endResolver.resolve(name) } returns address
        val result = nftDomainControllerApi.resolveDomainByName(name).awaitSingle()
        assertThat(result.registrant).isEqualTo(address)
    }

    @Test
    fun `resolve - empty result`() = runBlocking<Unit> {
        val name = randomEnsDomain()
        val address = Address.ZERO().prefixed()
        every { endResolver.resolve(name) } returns address
        val result = nftDomainControllerApi.resolveDomainByName(name).awaitSingle()
        assertThat(result.registrant).isBlank()
    }
}
