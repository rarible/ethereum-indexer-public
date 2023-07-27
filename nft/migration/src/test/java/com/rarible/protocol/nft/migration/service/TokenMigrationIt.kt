package com.rarible.protocol.nft.migration.service

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.migration.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.migration.integration.IntegrationTest
import com.rarible.protocol.nft.migration.mongock.mongo.ChangeLog00022dbUpdateAt
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.AggregationUpdate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

@IntegrationTest
class TokenMigrationIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var template: ReactiveMongoTemplate

    @Test
    fun `update dbUpdateField if null`() = runBlocking<Unit> {
        val tokensQuantities = 60

        repeat(tokensQuantities) {
            tokenRepository.save(Token.empty().copy(id = randomAddress())).awaitFirst()
        }

        val queryMulti = Query(Criteria.where(Token::dbUpdatedAt.name).exists(true))
        val multiUpdate = AggregationUpdate.update().unset(Token::dbUpdatedAt.name)
        template.updateMulti(queryMulti, multiUpdate, Token.COLLECTION).awaitFirst()

        ChangeLog00022dbUpdateAt().updateToken(template)

        val updatedTokens = tokenRepository.findAll().asFlow().toList()
        updatedTokens.forEach { assertThat(it.dbUpdatedAt).isNotNull() }
    }
}
