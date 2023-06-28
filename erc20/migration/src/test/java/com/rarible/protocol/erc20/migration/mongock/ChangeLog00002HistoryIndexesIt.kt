package com.rarible.protocol.erc20.migration.mongock

import com.rarible.protocol.erc20.core.repository.Erc20TransferHistoryRepository
import com.rarible.protocol.erc20.migration.test.AbstractIntegrationTest
import com.rarible.protocol.erc20.migration.test.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@IntegrationTest
class ChangeLog00002HistoryIndexesIt : AbstractIntegrationTest() {

    @Test
    fun `ensure indexes`() = runBlocking<Unit> {
        val indexes = mongo.indexOps(Erc20TransferHistoryRepository.COLLECTION).indexInfo.collectList().awaitFirst()
        assertThat(indexes.find { it.name == "data.owner_1" }).isNotNull
    }
}
