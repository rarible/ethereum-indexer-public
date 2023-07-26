package com.rarible.protocol.erc20.core.repository

import com.rarible.protocol.erc20.core.integration.AbstractIntegrationTest
import com.rarible.protocol.erc20.core.integration.IntegrationTest
import com.rarible.protocol.erc20.core.repository.data.randomAllowance
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
internal class Erc20AllowanceRepositoryIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var repository: Erc20AllowanceRepository

    @Test
    fun crud() = runBlocking<Unit> {
        val allowance = randomAllowance()

        repository.save(allowance)

        val savedAllowance = repository.get(allowance.id)
        assertThat(savedAllowance).isEqualTo(allowance.copy(version = 0))

        repository.deleteByOwner(allowance.owner)

        assertThat(repository.get(allowance.id)).isNull()
    }
}
