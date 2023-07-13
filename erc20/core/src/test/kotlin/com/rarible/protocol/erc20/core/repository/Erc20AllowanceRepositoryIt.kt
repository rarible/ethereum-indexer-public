package com.rarible.protocol.erc20.core.repository

import com.rarible.protocol.erc20.core.integration.AbstractIntegrationTest
import com.rarible.protocol.erc20.core.integration.IntegrationTest
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.repository.data.randomAllowance
import com.rarible.protocol.erc20.core.repository.data.randomBalance
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address

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
