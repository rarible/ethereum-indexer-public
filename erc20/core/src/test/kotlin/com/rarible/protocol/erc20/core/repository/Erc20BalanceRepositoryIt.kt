package com.rarible.protocol.erc20.core.repository

import com.rarible.core.test.ext.MongoTest
import com.rarible.protocol.erc20.core.configuration.CoreConfiguration
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.repository.data.createBalance
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.bson.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.test.context.ContextConfiguration
import scalether.domain.Address

@MongoTest
@DataMongoTest
@ContextConfiguration(classes = [CoreConfiguration::class])
internal class Erc20BalanceRepositoryIt {

    @Autowired
    lateinit var repository: Erc20BalanceRepository

    @Autowired
    lateinit var mongo: ReactiveMongoTemplate

    @Test
    fun `should save and get balance`() = runBlocking<Unit> {
        val balance = createBalance()

        repository.save(balance)

        val savedBalance = repository.get(balance.id)
        Assertions.assertThat(savedBalance).isEqualToIgnoringGivenFields(balance, Erc20Balance::version.name)
    }

    @Test
    fun `test balance raw format`() = runBlocking<Unit> {
        val balance = createBalance()

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
