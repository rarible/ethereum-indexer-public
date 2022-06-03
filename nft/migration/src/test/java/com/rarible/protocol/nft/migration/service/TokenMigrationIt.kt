package com.rarible.protocol.nft.migration.service

import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.migration.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.migration.integration.IntegrationTest
import com.rarible.protocol.nft.migration.mongock.mongo.ChangeLog00022dbUpdateAt
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import java.time.Instant

@IntegrationTest
class TokenMigrationIt  : AbstractIntegrationTest() {

    @Autowired
    lateinit var template: ReactiveMongoTemplate

    @Test
    fun `update dbUpdateField if null`() = runBlocking<Unit> {
        val tokensQuantities = 23

        repeat(tokensQuantities) {
            tokenRepository.save(Token.empty().copy(dbUpdatedAt = null))
        }

        ChangeLog00022dbUpdateAt().updateToken(template)
        tokenRepository.findAll().asFlow().toList().forEach { assertThat(it.dbUpdatedAt).isEqualTo(Instant.EPOCH) }
    }
}
