package com.rarible.protocol.erc20.core.repository

import com.rarible.protocol.erc20.core.integration.AbstractIntegrationTest
import com.rarible.protocol.erc20.core.integration.IntegrationTest
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.repository.data.randomBalance
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.bson.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address

@IntegrationTest
internal class Erc20BalanceRepositoryIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var repository: Erc20BalanceRepository

    @Test
    fun `should save and get balance`() = runBlocking<Unit> {
        val balance = randomBalance()

        repository.save(balance)

        val savedBalance = repository.get(balance.id)
        Assertions.assertThat(savedBalance).isEqualToIgnoringGivenFields(balance, Erc20Balance::version.name)
    }

    @Test
    fun `test balance raw format`() = runBlocking<Unit> {
        val balance = randomBalance()

        repository.save(balance)

        val document = mongo.findById(
            balance.id,
            Document::class.java,
            mongo.getCollectionName(Erc20Balance::class.java)
        ).block()

        assertEquals(balance.id.stringValue, document.getString("_id"))
        assertEquals(balance.token, Address.apply(document.getString(Erc20Balance::token.name)))
        assertEquals(balance.owner, Address.apply(document.getString(Erc20Balance::owner.name)))
    }
}
