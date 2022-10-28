package com.rarible.protocol.nft.migration.service

import com.rarible.protocol.nft.core.data.createRandomInconsistentItem
import com.rarible.protocol.nft.core.repository.InconsistentItemRepository
import com.rarible.protocol.nft.migration.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.migration.integration.IntegrationTest
import com.rarible.protocol.nft.migration.mongock.mongo.ChangeLog00025UpdateInconsistentItems
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

@IntegrationTest
class UpdateInconsistentItemsTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var inconsistentItemRepository: InconsistentItemRepository

    @Test
    fun `should set lastUpdatedAt where it is null`() = runBlocking<Unit> {
        // given
        repeat(10) {
            inconsistentItemRepository.save(createRandomInconsistentItem())
        }
        repeat(20) {
            inconsistentItemRepository.save(createRandomInconsistentItem().copy(lastUpdatedAt = null))
        }
        val query = Query.query(Criteria("lastUpdatedAt").`is`(null))

        // when
        val before = inconsistentItemRepository.search(query)
        ChangeLog00025UpdateInconsistentItems().updateInconsistentItems(inconsistentItemRepository, 3)
        val after = inconsistentItemRepository.search(query)
        val all = inconsistentItemRepository.search(Query())

        // then
        assertThat(before.size).isEqualTo(20)
        assertThat(after.size).isEqualTo(0)
        assertThat(all.size).isEqualTo(30)
    }
}
